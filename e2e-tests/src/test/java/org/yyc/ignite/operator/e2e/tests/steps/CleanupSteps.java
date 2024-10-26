package org.yyc.ignite.operator.e2e.tests.steps;

import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.yyc.ignite.operator.api.customresource.IgniteResource;

import java.util.Objects;
import java.util.Optional;

import static org.yyc.ignite.operator.e2e.tests.utils.Constants.CLEAN_UP_RESOURCE_ORDER;
import static org.yyc.ignite.operator.e2e.tests.utils.TagsUtils.getResourceNameFromScenario;

@Slf4j
// @RequiredArgsConstructor
public class CleanupSteps {
    @Autowired
    private KubernetesClient kubernetesClient;
    
    public void deleteIgniteResource(String resourceName) {
        Optional<Resource<IgniteResource>> resourceOptional = kubernetesClient
                .resources(IgniteResource.class)
                .resources()
                .filter(r -> r.get().getMetadata().getName().equals(resourceName))
                .findFirst();
        if (resourceOptional.isEmpty()) {
            // log.warn("Trying to delete resource {} but it does not exist", resourceName);
            return;
        }
        kubernetesClient.resource(resourceOptional.get().get()).delete();
    }
    
    @After(value = "@cleanupIgniteResource", order = CLEAN_UP_RESOURCE_ORDER)
    public void cleanupIgniteResource(Scenario scenario) {
        String name = getResourceNameFromScenario(scenario);
        Objects.requireNonNull(name, "Tags @resourceName need be specified in scenarios");
        
    }
}
