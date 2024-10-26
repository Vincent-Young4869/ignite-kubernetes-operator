package org.yyc.ignite.operator.e2e.tests.steps;

import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.type.lifecycle.IgniteClusterLifecycleStateEnum;

import java.util.Optional;

public class CheckSteps {
    @Autowired
    private KubernetesClient kubernetesClient;
    
    @Then("Ignite user should observe {string} status for IgniteResource {string}")
    public void targetResourceIsRunning(String status, String resourceName) {
        Optional<Resource<IgniteResource>> resourceOptional = kubernetesClient
                .resources(IgniteResource.class)
                .resources()
                .filter(r -> r.get().getMetadata().getName().equals(resourceName))
                .findFirst();
        Assert.assertTrue(resourceOptional.isPresent());
        Resource<IgniteResource> resource = resourceOptional.get();
        IgniteClusterLifecycleStateEnum actualStatus = resource.get().getStatus().getIgniteClusterLifecycleState();
        IgniteClusterLifecycleStateEnum expectedStatus = IgniteClusterLifecycleStateEnum.valueOf(status);
        Assert.assertEquals(expectedStatus, actualStatus);
    }
}
