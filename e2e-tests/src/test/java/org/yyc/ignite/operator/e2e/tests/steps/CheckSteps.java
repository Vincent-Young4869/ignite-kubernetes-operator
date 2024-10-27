package org.yyc.ignite.operator.e2e.tests.steps;

import io.cucumber.java.en.Then;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.type.lifecycle.IgniteClusterLifecycleStateEnum;
import org.yyc.ignite.operator.dependentresource.IgniteConfigMapResource;
import org.yyc.ignite.operator.dependentresource.IgniteStatefulSetResource;
import org.yyc.ignite.operator.e2e.tests.config.SharedScenarioContext;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.buildDependentResourceName;
import static org.yyc.ignite.operator.api.utils.XmlUpdateUtils.getDataRegionSizeFromXml;
import static org.yyc.ignite.operator.dependentresource.IgniteConfigMapResource.NODE_CONFIG_FILE_NAME;
import static org.yyc.ignite.operator.e2e.tests.utils.BuildIgniteResourceUtils.DEFAULT_NAMESPACE;
import static org.yyc.ignite.operator.e2e.tests.utils.K8sResourceUtils.getConfigMapResource;
import static org.yyc.ignite.operator.e2e.tests.utils.K8sResourceUtils.getStatefulSetResource;

public class CheckSteps {
    @Autowired
    private KubernetesClient kubernetesClient;
    @Autowired
    private SharedScenarioContext sharedScenarioContext;
    
    @Then("Ignite user should observe {string} status for IgniteResource {string}")
    public void targetResourceIsRunning(String status, String resourceName) {
        Resource<IgniteResource> resource = getIgniteResourceResource(resourceName, DEFAULT_NAMESPACE);
        
        IgniteClusterLifecycleStateEnum actualStatus = resource.get().getStatus().getIgniteClusterLifecycleState();
        IgniteClusterLifecycleStateEnum expectedStatus = IgniteClusterLifecycleStateEnum.valueOf(status);
        Assert.assertEquals(expectedStatus, actualStatus);
    }
    
    @Then("Ignite user should observe healthy status for all IgniteResources with correct configurations respectively")
    public void resourcesFromSharedScenarioContextAreHealthy() {
        for (IgniteResource resource : sharedScenarioContext.getTestResources()) {
            Resource<IgniteResource> k8sResource = getIgniteResourceResource(resource.getMetadata().getName(), resource.getMetadata().getNamespace());
            // check status
            IgniteClusterLifecycleStateEnum actualStatus = k8sResource.get().getStatus().getIgniteClusterLifecycleState();
            IgniteClusterLifecycleStateEnum expectedStatus = resource.getSpec().getPersistenceSpec().isPersistenceEnabled()
                    ? IgniteClusterLifecycleStateEnum.INACTIVE_RUNNING
                    : IgniteClusterLifecycleStateEnum.ACTIVE_RUNNING;
            Assert.assertEquals(expectedStatus, actualStatus);
            // check replica
            StatefulSet statefulSetResource = getStatefulSetResource(kubernetesClient,
                    buildDependentResourceName(resource, IgniteStatefulSetResource.COMPONENT),
                    resource.getMetadata().getNamespace());
            Assert.assertEquals(resource.getSpec().getReplicas(), statefulSetResource.getStatus().getReadyReplicas());
            //check persistence
            if (resource.getSpec().getPersistenceSpec().isPersistenceEnabled()) {
                Assert.assertFalse(statefulSetResource.getSpec().getVolumeClaimTemplates().isEmpty());
            } else {
                Assert.assertTrue(statefulSetResource.getSpec().getVolumeClaimTemplates().isEmpty());
            }
            // check configmap data region size
            ConfigMap configMapResource = getConfigMapResource(kubernetesClient,
                    buildDependentResourceName(resource, IgniteConfigMapResource.COMPONENT),
                    resource.getMetadata().getNamespace());
            String dataRegionSizeFromXml = getDataRegionSizeFromXml(configMapResource.getData().get(NODE_CONFIG_FILE_NAME));
            Assert.assertEquals(
                    String.format("#{%s}", resource.getSpec().getIgniteConfigMapSpec().getRelationalDataRegionSize()),
                    dataRegionSizeFromXml);
        }
    }
    
    
    private Resource<IgniteResource> getIgniteResourceResource(String name, String namespace) {
        Resource<IgniteResource> k8sResource = kubernetesClient
                .resources(IgniteResource.class)
                .inNamespace(namespace)
                .withName(name);
        Objects.requireNonNull(k8sResource, "Ignite k8sResource " + name + " is null when trying to get its status");
        return k8sResource;
    }
}
