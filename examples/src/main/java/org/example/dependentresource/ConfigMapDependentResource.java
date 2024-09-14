package org.example.dependentresource;

import static org.example.Utils.configMapName;
import static org.example.WebPageManagedDependentsReconciler.SELECTOR;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.example.customresource.WebPage;
import java.util.HashMap;
import java.util.Map;

// this annotation only activates when using managed dependents and is not otherwise needed
@KubernetesDependent(labelSelector = SELECTOR)
public class ConfigMapDependentResource
    extends CRUDKubernetesDependentResource<ConfigMap, WebPage> {

  public ConfigMapDependentResource() {
    super(ConfigMap.class);
  }

  @Override
  protected ConfigMap desired(WebPage webPage, Context<WebPage> context) {
    Map<String, String> data = new HashMap<>();
    data.put("index.html", webPage.getSpec().getHtml());
    Map<String, String> labels = new HashMap<>();
    labels.put(SELECTOR, "true");
    return new ConfigMapBuilder()
        .withMetadata(
            new ObjectMetaBuilder()
                .withName(configMapName(webPage))
                .withNamespace(webPage.getMetadata().getNamespace())
                .withLabels(labels)
                .build())
        .withData(data)
        .build();
  }
}
