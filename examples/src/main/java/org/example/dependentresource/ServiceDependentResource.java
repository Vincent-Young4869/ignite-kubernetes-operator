package org.example.dependentresource;

import static org.example.BuilderHelper.loadTemplate;
import static org.example.Utils.deploymentName;
import static org.example.Utils.serviceName;
import static org.example.WebPageManagedDependentsReconciler.SELECTOR;

import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.example.customresource.WebPage;
import java.util.HashMap;
import java.util.Map;

// this annotation only activates when using managed dependents and is not otherwise needed
@KubernetesDependent(labelSelector = SELECTOR)
public class ServiceDependentResource extends
    io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource<Service, WebPage> {

  public ServiceDependentResource() {
    super(Service.class);
  }

  @Override
  protected Service desired(WebPage webPage, Context<WebPage> context) {
    Map<String, String> serviceLabels = new HashMap<>();
    serviceLabels.put(SELECTOR, "true");
    Service service = loadTemplate(Service.class, "templates/service.yaml");
    service.getMetadata().setName(serviceName(webPage));
    service.getMetadata().setNamespace(webPage.getMetadata().getNamespace());
    service.getMetadata().setLabels(serviceLabels);
    Map<String, String> labels = new HashMap<>();
    labels.put("app", deploymentName(webPage));
    service.getSpec().setSelector(labels);
    return service;
  }
}
