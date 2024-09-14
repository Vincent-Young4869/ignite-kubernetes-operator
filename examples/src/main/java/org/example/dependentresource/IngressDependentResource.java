package org.example.dependentresource;

import static org.example.Utils.makeDesiredIngress;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.example.WebPageManagedDependentsReconciler;
import org.example.customresource.WebPage;

// this annotation only activates when using managed dependents and is not otherwise needed
@KubernetesDependent(labelSelector = WebPageManagedDependentsReconciler.SELECTOR)
public class IngressDependentResource extends CRUDKubernetesDependentResource<Ingress, WebPage> {

  public IngressDependentResource() {
    super(Ingress.class);
  }

  @Override
  protected Ingress desired(WebPage webPage, Context<WebPage> context) {
    return makeDesiredIngress(webPage);
  }

}
