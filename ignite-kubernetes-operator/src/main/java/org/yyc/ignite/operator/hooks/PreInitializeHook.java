package org.yyc.ignite.operator.hooks;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.status.IgniteStatus;
import org.yyc.ignite.operator.dependentresource.IgniteSaResource;
import org.yyc.ignite.operator.dependentresource.IgniteStatefulSetResource;
import org.yyc.ignite.operator.api.type.lifecycle.IgniteClusterLifecycleStateEnum;

import java.util.Objects;

import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.buildDependentResourceName;
import static org.yyc.ignite.operator.api.utils.TimeUtils.currentTimestamp;

@Slf4j
public class PreInitializeHook implements Condition<IgniteSaResource, IgniteResource> {
    
    @Override
    public boolean isMet(DependentResource<IgniteSaResource, IgniteResource> dependentResource,
                         IgniteResource igniteResource,
                         Context<IgniteResource> context) {
        KubernetesClient client = context.getClient();

        if (isIgniteClusterNotExist(igniteResource, client)) {
            initIgniteStatus(igniteResource);
            client.resource(igniteResource).updateStatus();
            return true;
        }
        
        assert Objects.nonNull(igniteResource.getStatus());
        switch (igniteResource.getStatus().getIgniteClusterLifecycleState()) {
            // TODO: FAILED -> DEPLOYING is currently not supported
            //  due to the current k8s design that statefulset won't clean up crushed pods and recreate new ones
            //  after investigation, there is a pending PR that can potentially solve this issue
            //  link to the ticket: https://github.com/kubernetes/enhancements/issues/3541, https://github.com/kubernetes/kubernetes/issues/120123
            case TERMINATING, FAILED:
                return false;
            case INACTIVE_RUNNING, ACTIVE_RUNNING:
                if (isRunningIgniteClusterHealthy(igniteResource, client)) {
                    return true;
                }
                igniteResource.getStatus().updateLifecycleState(IgniteClusterLifecycleStateEnum.RECOVERING);
                break;
            case CREATED, DEPLOYING, RECOVERING:
                return true;
            case INITIALIZING:
                igniteResource.getStatus().updateLifecycleState(IgniteClusterLifecycleStateEnum.DEPLOYING);
                break;
        }
        
        client.resource(igniteResource).updateStatus();
        return true;
    }
    
    private boolean isIgniteClusterNotExist(IgniteResource igniteResource, KubernetesClient client) {
        String statefulSetName = buildDependentResourceName(igniteResource, IgniteStatefulSetResource.COMPONENT);
        String namespace = igniteResource.getMetadata().getNamespace();
        StatefulSet statefulSet = client.apps().statefulSets()
                .inNamespace(namespace)
                .withName(statefulSetName)
                .get();
        return Objects.isNull(statefulSet);
    }
    
    private static void initIgniteStatus(IgniteResource igniteResource) {
        IgniteStatus status = IgniteStatus.builder()
                .igniteClusterLifecycleState(IgniteClusterLifecycleStateEnum.CREATED)
                .lastLifecycleStateTimestamp(currentTimestamp())
                .build();
        igniteResource.setStatus(status);
    }
    
    private boolean isRunningIgniteClusterHealthy(IgniteResource igniteResource, KubernetesClient client) {
        String statefulSetName = buildDependentResourceName(igniteResource, IgniteStatefulSetResource.COMPONENT);
        String namespace = igniteResource.getMetadata().getNamespace();
        StatefulSet statefulSet = client.apps().statefulSets()
                .inNamespace(namespace)
                .withName(statefulSetName)
                .get();
        return Objects.equals(statefulSet.getStatus().getReadyReplicas(), statefulSet.getSpec().getReplicas());
    }
}
