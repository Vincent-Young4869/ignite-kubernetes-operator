package org.yyc.ignite.operator.conditions;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;
import org.yyc.ignite.operator.customresource.IgniteResource;
import org.yyc.ignite.operator.dependentresource.IgniteStatefulSetResource;
import org.yyc.ignite.operator.utils.type.lifecycle.ResourceLifecycleState;

import java.util.List;

@Slf4j
public class PostDeleteCondition implements Condition<IgniteStatefulSetResource, IgniteResource> {
    
    @Override
    public boolean isMet(DependentResource<IgniteStatefulSetResource, IgniteResource> dependentResource, IgniteResource resource, Context<IgniteResource> context) {
        log.info("enter post delete condition");
        KubernetesClient client = context.getClient();
        List<Pod> pods = client.pods().inNamespace(resource.getMetadata().getNamespace())
                .withLabel("name", resource.getMetadata().getName())
                .list()
                .getItems();
        if (!pods.isEmpty()) {
            log.info("Pods not yet cleaned up: {}", pods.stream().map(p -> p.getMetadata().getName()).toList());
            
            IgniteResource latestResource = client.resource(resource).get();
            latestResource.getStatus().updateLifecycleState(ResourceLifecycleState.TERMINATING);
            client.resource(latestResource).updateStatus();
            
            return false;
        }
        log.info("Pods cleaned up successfully.");
        return true;
    }
}
