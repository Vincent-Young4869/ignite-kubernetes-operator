package org.example.igniteoperator.conditions;

import io.fabric8.kubernetes.api.model.ListOptionsBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.dependentresource.IgniteSaResource;
import org.example.igniteoperator.dependentresource.IgniteStatefulSetResource;
import org.example.igniteoperator.utils.type.lifecycle.ResourceLifecycleState;
import org.jetbrains.annotations.Nullable;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
public class PostDeleteCondition implements Condition<IgniteStatefulSetResource, IgniteResource> {
    
    private final int maxAttempts = 3;
    private final RetryTemplate retryTemplate;
    
    public PostDeleteCondition() {
        this.retryTemplate = new RetryTemplateBuilder()
                .maxAttempts(maxAttempts)
                .build();
    }
    
    @Override
    public boolean isMet(DependentResource<IgniteStatefulSetResource, IgniteResource> dependentResource, IgniteResource resource, Context<IgniteResource> context) {
        log.info("enter post delete condition");
        KubernetesClient client = context.getClient();
        setupRetryStrategy(resource);
        retryTemplate.execute(retryContext -> checkIfIgniteNodeCleanedUp(resource, client));
        return true;
    }
    
    private Void checkIfIgniteNodeCleanedUp(IgniteResource resource, KubernetesClient client) {
        List<Pod> pods = client.pods().inNamespace(resource.getMetadata().getNamespace())
                .withLabel("name", resource.getMetadata().getName())
                .list(new ListOptionsBuilder().withLimit(500L).withResourceVersion("0").build())
                .getItems();
        if (!pods.isEmpty()) {
            log.info("Pods not yet cleaned up: {}", pods);
            
            IgniteResource latestResource = client.resource(resource).get();
            latestResource.getStatus().updateLifecycleState(ResourceLifecycleState.TERMINATING);
            client.resource(latestResource).updateStatus();
            
            throw new RuntimeException("Pods are still present, retrying...");
        }
        
        log.info("Pods cleaned up successfully.");
        return null;
    }
    
    private void setupRetryStrategy(IgniteResource resource) {
        long terminalGracefulShutdown = resource.getSpec().getIgniteNodeSpec().getTerminalGracefulShutdown();
        System.out.println("grace shutdown: " + terminalGracefulShutdown);
        long backOffPeriodMillis = (terminalGracefulShutdown * 1000L);
        this.retryTemplate.setBackOffPolicy(new FixedBackOffPolicy() {{
            setBackOffPeriod(backOffPeriodMillis);
        }});
    }
}
