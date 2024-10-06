package org.example.igniteoperator.conditions;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.customresource.IgniteStatus;
import org.example.igniteoperator.dependentresource.IgniteSaResource;
import org.example.igniteoperator.dependentresource.IgniteStatefulSetResource;
import org.example.igniteoperator.utils.type.lifecycle.ResourceLifecycleState;

import java.util.Objects;

import static org.example.igniteoperator.utils.DependentResourceUtils.buildDependentResourceName;
import static org.example.igniteoperator.utils.TimeUtils.currentTimestamp;

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
        if (igniteResource.getStatus().getResourceLifecycleState().equals(ResourceLifecycleState.TERMINATING)) {
            return false;
        } else if (isRunningIgniteClusterUnhealthy(igniteResource, client)) {
            igniteResource.getStatus().updateLifecycleState(ResourceLifecycleState.RECOVERING);
        } else {
            igniteResource.getStatus().updateLifecycleState(ResourceLifecycleState.DEPLOYING);
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
                .resourceLifecycleState(ResourceLifecycleState.CREATED)
                .lastLifecycleStateTimestamp(currentTimestamp())
                .build();
        igniteResource.setStatus(status);
    }
    
    private boolean isRunningIgniteClusterUnhealthy(IgniteResource igniteResource, KubernetesClient client) {
        String statefulSetName = buildDependentResourceName(igniteResource, IgniteStatefulSetResource.COMPONENT);
        String namespace = igniteResource.getMetadata().getNamespace();
        StatefulSet statefulSet = client.apps().statefulSets()
                .inNamespace(namespace)
                .withName(statefulSetName)
                .get();
        return !Objects.equals(statefulSet.getStatus().getReadyReplicas(), statefulSet.getSpec().getReplicas())
                && igniteResource.getStatus().getResourceLifecycleState().equals(ResourceLifecycleState.ACTIVE_RUNNING);
    }
}
