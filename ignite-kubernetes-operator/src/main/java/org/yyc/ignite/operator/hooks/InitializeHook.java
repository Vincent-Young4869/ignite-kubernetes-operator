package org.yyc.ignite.operator.hooks;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.type.lifecycle.IgniteClusterLifecycleStateEnum;
import org.yyc.ignite.operator.dependentresource.IgniteSaResource;

import static org.yyc.ignite.operator.api.utils.LifecycleManageUtils.statusTransitTo;

public class InitializeHook implements Condition<IgniteSaResource, IgniteResource> {
    @Override
    public boolean isMet(DependentResource<IgniteSaResource, IgniteResource> dependentResource,
                         IgniteResource igniteResource,
                         Context<IgniteResource> context) {
        if (igniteResource.getStatus().getIgniteClusterLifecycleState().equals(IgniteClusterLifecycleStateEnum.CREATED)) {
            KubernetesClient client = context.getClient();
            statusTransitTo(igniteResource, IgniteClusterLifecycleStateEnum.INITIALIZING);
            IgniteResource latestResource = client.resource(igniteResource).get();
            client.resource(latestResource).updateStatus();
        }
        return true;
    }
}
