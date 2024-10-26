package org.yyc.ignite.operator.e2e.tests.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.spring.CucumberContextConfiguration;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.e2e.tests.config.SpringConfig;

import java.util.concurrent.TimeUnit;

import static org.yyc.ignite.operator.e2e.tests.utils.BuildIgniteResourceUtils.DEFAULT_NAMESPACE;

@SpringBootTest
@CucumberContextConfiguration
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = {SpringConfig.class})
@RequiredArgsConstructor
public class UtilsSteps {
    @Autowired
    private KubernetesClient kubernetesClient;
    
    @Then("Sleep for {int} seconds")
    public static void sleepForSeconds(int seconds) throws InterruptedException {
        Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
    }
    
    @Given("There is no IgniteResource with name {string}")
    public void checkGivenIgniteResourceNonExisted(String resourceName) {
        Resource<IgniteResource> resource = kubernetesClient
                .resources(IgniteResource.class)
                .inNamespace(DEFAULT_NAMESPACE)
                .withName(resourceName);
        Assert.assertNull(resource.get());
    }
}
