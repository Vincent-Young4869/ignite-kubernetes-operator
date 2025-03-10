package org.yyc.ignite.operator.e2e.tests.steps;

import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.When;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.e2e.tests.config.SharedScenarioContext;

import static org.yyc.ignite.operator.e2e.tests.utils.BuildIgniteResourceUtils.NAMESPACES_FOR_TEST;
import static org.yyc.ignite.operator.e2e.tests.utils.BuildIgniteResourceUtils.buildDefaultIgniteResource;

public class CreateSteps {
    @Autowired
    private KubernetesClient kubernetesClient;
    @Autowired
    private SharedScenarioContext sharedScenarioContext;
    
    @BeforeAll
    public static void setupNamespaces() {
        try (KubernetesClient kubernetesClient = new KubernetesClientBuilder().build()) {
            for (String namespace : NAMESPACES_FOR_TEST) {
                Namespace ns = new NamespaceBuilder()
                        .withNewMetadata()
                        .withName(namespace) // replace with your desired namespace name
                        .endMetadata()
                        .build();
                kubernetesClient.namespaces().createOrReplace(ns);
            }
        }
    }
    
    @When("Create an IgniteResource with name {string}")
    public void createIgniteResource(String resourceName) {
        IgniteResource igniteResource = buildDefaultIgniteResource(resourceName);
        createResourceInK8s(igniteResource);
    }
    
    @When("Create IgniteResources concurrently with these configurations")
    public void createMultipleIgniteResourceFromSharedScenarioContext() {
        sharedScenarioContext.getTestResources()
                .parallelStream()
                .forEach(this::createResourceInK8s);
    }
    
    private void createResourceInK8s(IgniteResource igniteResource) {
        kubernetesClient.resource(igniteResource).create();
    }
}
