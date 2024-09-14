package org.example.dependentresource;

import static org.example.BuilderHelper.loadTemplate;
import static org.example.Utils.configMapName;
import static org.example.Utils.deploymentName;
import static org.example.WebPageManagedDependentsReconciler.SELECTOR;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.example.customresource.WebPage;
import java.util.HashMap;
import java.util.Map;

// this annotation only activates when using managed dependents and is not otherwise needed
@KubernetesDependent(labelSelector = SELECTOR)
public class DeploymentDependentResource
    extends CRUDKubernetesDependentResource<Deployment, WebPage> {

  public DeploymentDependentResource() {
    super(Deployment.class);
  }

  @Override
  protected Deployment desired(WebPage webPage, Context<WebPage> context) {
    Map<String, String> labels = new HashMap<>();
    labels.put(SELECTOR, "true");
    var deploymentName = deploymentName(webPage);
    Deployment deployment = loadTemplate(Deployment.class, "templates/deployment.yaml");
    deployment.getMetadata().setName(deploymentName);
    deployment.getMetadata().setNamespace(webPage.getMetadata().getNamespace());
    deployment.getMetadata().setLabels(labels);
    deployment.getSpec().getSelector().getMatchLabels().put("app", deploymentName);

    deployment
        .getSpec()
        .getTemplate()
        .getMetadata()
        .getLabels()
        .put("app", deploymentName);
    deployment
        .getSpec()
        .getTemplate()
        .getSpec()
        .getVolumes()
        .get(0)
        .setConfigMap(
            new ConfigMapVolumeSourceBuilder().withName(configMapName(webPage)).build());

    return deployment;
  }
}
