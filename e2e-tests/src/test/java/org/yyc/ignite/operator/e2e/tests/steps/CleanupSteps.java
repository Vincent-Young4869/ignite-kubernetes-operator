package org.yyc.ignite.operator.e2e.tests.steps;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Scenario;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.yyc.ignite.operator.api.customresource.IgniteResource;

import java.util.Objects;
import java.util.Optional;

import static org.yyc.ignite.operator.e2e.tests.steps.UtilsSteps.sleepForSeconds;
import static org.yyc.ignite.operator.e2e.tests.utils.BuildIgniteResourceUtils.DEFAULT_NAMESPACE;
import static org.yyc.ignite.operator.e2e.tests.utils.BuildIgniteResourceUtils.NAMESPACES_FOR_TEST;
import static org.yyc.ignite.operator.e2e.tests.utils.Constants.CLEAN_UP_RESOURCE_ORDER;
import static org.yyc.ignite.operator.e2e.tests.utils.TagsUtils.RESOURCE_NAME_NOT_FOUND_MESSAGE;
import static org.yyc.ignite.operator.e2e.tests.utils.TagsUtils.getResourceNameFromScenario;

@Slf4j
public class CleanupSteps {
    @Autowired
    private KubernetesClient kubernetesClient;
    
    @AfterAll
    public static void cleanupNamespaces() throws InterruptedException {
        KubernetesClient kubernetesClient = new KubernetesClientBuilder().build();
        for (String namespace : NAMESPACES_FOR_TEST) {
            Namespace ns = new NamespaceBuilder()
                    .withNewMetadata()
                    .withName(namespace)
                    .endMetadata()
                    .build();
            kubernetesClient.namespaces().delete(ns);
        }
    }
    
    public void deleteIgniteResource(String resourceName, String namespace) {
        Resource<IgniteResource> resource = kubernetesClient
                .resources(IgniteResource.class)
                .inNamespace(namespace)
                .withName(resourceName);
        if (Objects.isNull(resource)) {
            log.warn("Trying to delete resource {} but it does not exist", resourceName);
            return;
        }
        kubernetesClient.resource(resource.get()).delete();
    }
    
    @After(value = "@cleanupIgniteResource", order = CLEAN_UP_RESOURCE_ORDER)
    public void cleanupIgniteResource(Scenario scenario) {
        String resourceName = getResourceNameFromScenario(scenario);
        Objects.requireNonNull(resourceName, RESOURCE_NAME_NOT_FOUND_MESSAGE);
        deleteIgniteResource(resourceName, DEFAULT_NAMESPACE);
    }
}
