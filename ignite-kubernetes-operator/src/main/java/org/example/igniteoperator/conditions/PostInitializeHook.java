package org.example.igniteoperator.conditions;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.dependentresource.IgniteSaResource;
import org.example.igniteoperator.utils.type.lifecycle.ResourceLifecycleState;

public class PostInitializeHook implements Condition<IgniteSaResource, IgniteResource> {
    @Override
    public boolean isMet(DependentResource<IgniteSaResource, IgniteResource> dependentResource,
                         IgniteResource igniteResource,
                         Context<IgniteResource> context) {
        // KubernetesClient client = context.getClient();
        // if (igniteResource.getStatus().getResourceLifecycleState().equals(ResourceLifecycleState.CREATED)) {
        //     igniteResource.getStatus().updateLifecycleState(ResourceLifecycleState.INITIALIZING);
        // }
        // client.resource(igniteResource).updateStatus();
        return true;
    }
}
