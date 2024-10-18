package org.yyc.ignite.operator.conditions;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;
import org.yyc.ignite.operator.customresource.IgniteResource;
import org.yyc.ignite.operator.dependentresource.IgniteStatefulSetResource;
import org.yyc.ignite.operator.utils.Constants;
import org.yyc.ignite.operator.utils.type.lifecycle.ResourceLifecycleState;

import java.util.Objects;

import static org.yyc.ignite.operator.utils.DependentResourceUtils.buildDependentResourceName;
import static org.yyc.ignite.operator.utils.TimeUtils.isReconcileDurationExceeded;

@Slf4j
public class PostDeploymentHook implements Condition<IgniteStatefulSetResource, IgniteResource> {
    @Override
    public boolean isMet(DependentResource<IgniteStatefulSetResource, IgniteResource> dependentResource,
                         IgniteResource igniteResource,
                         Context<IgniteResource> context) {
        KubernetesClient client = context.getClient();
        
        String statefulSetName = buildDependentResourceName(igniteResource, IgniteStatefulSetResource.COMPONENT);
        String namespace = igniteResource.getMetadata().getNamespace();
        StatefulSet statefulSet = client.apps().statefulSets()
                .inNamespace(namespace)
                .withName(statefulSetName)
                .get();
        if (Objects.isNull(statefulSet)) {
            return false;
        }
        
        boolean isIgniteClusterReady = Objects.equals(statefulSet.getStatus().getReadyReplicas(), statefulSet.getSpec().getReplicas());
        if (isIgniteClusterReady) {
            ResourceLifecycleState nextState = igniteResource.getSpec().getPersistenceSpec().isPersistenceEnabled()
                    ? ResourceLifecycleState.INACTIVE_RUNNING
                    : ResourceLifecycleState.ACTIVE_RUNNING;
            igniteResource.getStatus().updateLifecycleState(nextState);
            client.resource(igniteResource).updateStatus();
            return true;
        }
        
        String lastReconciledTimestamp = igniteResource.getStatus().getLastLifecycleStateTimestamp();
        if (isReconcileDurationExceeded(lastReconciledTimestamp, Constants.RECONCILE_MAX_RETRY_DURATION)) {
            igniteResource.getStatus().updateLifecycleState(ResourceLifecycleState.FAILED);
        } else {
            igniteResource.getStatus().updateLifecycleState(ResourceLifecycleState.DEPLOYING);
        }
        client.resource(igniteResource).updateStatus();
        return false;
    }
}
