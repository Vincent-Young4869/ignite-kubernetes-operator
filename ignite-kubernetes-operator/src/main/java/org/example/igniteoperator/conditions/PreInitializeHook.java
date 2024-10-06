package org.example.igniteoperator.conditions;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.customresource.IgniteStatus;
import org.example.igniteoperator.dependentresource.IgniteSaResource;
import org.example.igniteoperator.dependentresource.IgniteStatefulSetResource;
import org.example.igniteoperator.utils.type.lifecycle.ResourceLifecycleState;

import java.util.Objects;

import static org.example.igniteoperator.utils.DependentResourceUtils.buildDependentResourceName;
import static org.example.igniteoperator.utils.TimeUtils.currentTimestamp;

public class PreInitializeHook implements Condition<IgniteSaResource, IgniteResource> {
    
    @Override
    public boolean isMet(DependentResource<IgniteSaResource, IgniteResource> dependentResource,
                         IgniteResource igniteResource,
                         Context<IgniteResource> context) {
        KubernetesClient client = context.getClient();
        
        String statefulSetName = buildDependentResourceName(igniteResource, IgniteStatefulSetResource.COMPONENT);
        String namespace = igniteResource.getMetadata().getNamespace();
        StatefulSet statefulSet = client.apps().statefulSets()
                .inNamespace(namespace)
                .withName(statefulSetName)
                .get();
        
        System.out.println(igniteResource.getStatus());
        
        ResourceLifecycleState state = Objects.isNull(statefulSet)
                ? ResourceLifecycleState.CREATED
                : ResourceLifecycleState.DEPLOYING;
        
        if (Objects.isNull(igniteResource.getStatus())) {
            IgniteStatus status = IgniteStatus.builder()
                    .resourceLifecycleState(state)
                    .lastLifecycleStateTimestamp(currentTimestamp())
                    .build();
            
            igniteResource.setStatus(status);
        } else {
            igniteResource.getStatus().updateLifecycleState(state);
        }
        
        client.resource(igniteResource).updateStatus();
        return true;
    }
}
