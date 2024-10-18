package org.yyc.ignite.operator.conditions;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import org.yyc.ignite.operator.customresource.IgniteResource;
import org.yyc.ignite.operator.dependentresource.IgniteSaResource;
import org.yyc.ignite.operator.utils.type.lifecycle.ResourceLifecycleState;

public class InitializeHook implements Condition<IgniteSaResource, IgniteResource> {
    @Override
    public boolean isMet(DependentResource<IgniteSaResource, IgniteResource> dependentResource,
                         IgniteResource igniteResource,
                         Context<IgniteResource> context) {
        if (igniteResource.getStatus().getResourceLifecycleState().equals(ResourceLifecycleState.CREATED)) {
            KubernetesClient client = context.getClient();
            igniteResource.getStatus().updateLifecycleState(ResourceLifecycleState.INITIALIZING);
            IgniteResource latestResource = client.resource(igniteResource).get();
            client.resource(latestResource).updateStatus();
        }
        return true;
    }
}
