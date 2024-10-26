package org.yyc.ignite.operator.e2e.tests.steps;

import io.cucumber.java.en.When;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.yyc.ignite.operator.api.customresource.IgniteResource;

import static org.yyc.ignite.operator.e2e.tests.utils.BuildIgniteResourceUtils.buildDefaultIgniteResource;

@Slf4j
// @RequiredArgsConstructor
public class CreateSteps {
    @Autowired
    private KubernetesClient kubernetesClient;
    
    @When("Create an IgniteResource with name {string}")
    public void createIgniteResource(String resourceName) {
        IgniteResource igniteResource = buildDefaultIgniteResource(resourceName);
        kubernetesClient.resource(igniteResource).create();
        // log.info("Ignite Resource {} is created", resourceName);
    }
}
