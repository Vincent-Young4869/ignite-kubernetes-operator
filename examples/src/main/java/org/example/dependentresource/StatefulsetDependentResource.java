package org.example.dependentresource;

import static org.example.BuilderHelper.loadTemplate;
import static org.example.Utils.deploymentName;
import static org.example.WebPageManagedDependentsReconciler.SELECTOR;

import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.example.customresource.WebPage;

// this annotation only activates when using managed dependents and is not otherwise needed
@KubernetesDependent(labelSelector = SELECTOR)
public class StatefulsetDependentResource
    extends CRUDKubernetesDependentResource<StatefulSet, WebPage> {

  public StatefulsetDependentResource() {
    super(StatefulSet.class);
  }

  @Override
  protected StatefulSet desired(WebPage webPage, Context<WebPage> context) {
    Map<String, String> labels = new HashMap<>();
    labels.put(SELECTOR, "true");
    var deploymentName = deploymentName(webPage);
    StatefulSet statefulSet = loadTemplate(StatefulSet.class, "templates/statefulset.yaml");
    statefulSet.getMetadata().setName(deploymentName);
    statefulSet.getMetadata().setNamespace(webPage.getMetadata().getNamespace());
    statefulSet.getMetadata().setLabels(labels);
    statefulSet.getSpec().getSelector().getMatchLabels().put("app", deploymentName);

    Map<String, Quantity> resourceLimit = new HashMap<>();
    resourceLimit.put("cpu", new Quantity(webPage.getSpec().getIgniteNodeCpu()));
    resourceLimit.put("memory", new Quantity(webPage.getSpec().getIgniteNodeMemory()));
    Map<String, Quantity> resourceRequest = new HashMap<>();
    resourceRequest.put("cpu", new Quantity(webPage.getSpec().getIgniteNodeCpu()));
    resourceRequest.put("memory", new Quantity(webPage.getSpec().getIgniteNodeMemory()));

    statefulSet
        .getSpec()
        .getTemplate()
        .getMetadata()
        .getLabels()
        .put("app", deploymentName);
    statefulSet
            .getSpec()
            .getTemplate()
            .getMetadata()
            .getAnnotations()
            .put("configMapHash", calculateConfigMapHash(webPage.getSpec().getHtml()));
    statefulSet
        .getSpec()
        .getTemplate()
        .setSpec(new PodSpecBuilder(statefulSet.getSpec().getTemplate().getSpec())
            .editContainer(0)
            .withNewResources()
            .addToRequests(resourceRequest)
            .addToLimits(resourceLimit)
            .endResources()
            .and()
            .build());
        // .getSpec()
        // .getVolumes()
        // .get(0)
        // .setConfigMap(
        //     new ConfigMapVolumeSourceBuilder().withName(configMapName(webPage)).build());

    return statefulSet;
  }
  
  private String calculateConfigMapHash(String data) {
    // String data = configMap.getData().values().stream().sorted().collect(Collectors.joining());
    return DigestUtils.sha256Hex(data);
  }
}
