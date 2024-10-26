package org.yyc.ignite.operator.e2e.tests.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.yyc.ignite.operator.api.customresource.IgniteResource;

import java.util.concurrent.TimeUnit;

@Slf4j
// @RequiredArgsConstructor
public class UtilsSteps {
    @Autowired
    private KubernetesClient kubernetesClient;
    
    @Given("There is no IgniteResource with name {string}")
    public void checkGivenIgniteResourceNonExisted(String resourceName) {
        boolean isResourceExist = kubernetesClient
                .resources(IgniteResource.class)
                .resources()
                .anyMatch(r -> r.get().getMetadata().getName().equals(resourceName));
        Assert.assertFalse(isResourceExist);
    }
    
    @Then("Sleep for {int} seconds")
    public void sleepForSeconds(int seconds) throws InterruptedException {
        // log.info("sleep for {} seconds", seconds);
        Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    }
}
