package org.yyc.ignite.operator.hooks;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.type.K8sMetadataLabelEnum;
import org.yyc.ignite.operator.api.type.lifecycle.IgniteClusterLifecycleStateEnum;
import org.yyc.ignite.operator.dependentresource.IgniteStatefulSetResource;

import java.util.List;

import static org.yyc.ignite.operator.api.utils.LifecycleManageUtils.statusTransitTo;

@Slf4j
public class PostDeleteHook implements Condition<IgniteStatefulSetResource, IgniteResource> {
    
    @Override
    public boolean isMet(DependentResource<IgniteStatefulSetResource, IgniteResource> dependentResource, IgniteResource resource, Context<IgniteResource> context) {
        log.info("enter post delete condition");
        KubernetesClient client = context.getClient();
        List<Pod> pods = client.pods().inNamespace(resource.getMetadata().getNamespace())
                .withLabel(K8sMetadataLabelEnum.NAME.labelName(), resource.getMetadata().getName())
                .list()
                .getItems();
        if (!pods.isEmpty()) {
            log.warn("Pods not yet cleaned up: {}", pods.stream().map(p -> p.getMetadata().getName()).toList());
            
            IgniteResource latestResource = client.resource(resource).get();
            statusTransitTo(latestResource, IgniteClusterLifecycleStateEnum.TERMINATING);
            client.resource(latestResource).updateStatus();
            
            return false;
        }
        log.info("Pods cleaned up successfully.");
        return true;
    }
}
