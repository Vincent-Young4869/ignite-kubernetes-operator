package org.example;

import static org.example.Utils.*;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import org.example.customresource.WebPage;
import org.example.dependentresource.*;

/**
 * Shows how to implement a reconciler with managed dependent resources.
 */
@ControllerConfiguration(dependents = {
    @Dependent(name = "cm",  type = ConfigMapDependentResource.class),
    @Dependent(type = StatefulsetDependentResource.class, dependsOn = {"cm"}),
    @Dependent(type = ServiceDependentResource.class),
    @Dependent(type = IngressDependentResource.class,
        reconcilePrecondition = ExposedIngressCondition.class)
})
public class WebPageManagedDependentsReconciler
    implements Reconciler<WebPage>, ErrorStatusHandler<WebPage>, Cleaner<WebPage> {

  public static final String SELECTOR = "managed";

  @Override
  public ErrorStatusUpdateControl<WebPage> updateErrorStatus(WebPage resource,
      Context<WebPage> context, Exception e) {
    return handleError(resource, e);
  }

  @Override
  public UpdateControl<WebPage> reconcile(WebPage webPage, Context<WebPage> context)
      throws Exception {
    // simulateErrorIfRequested(webPage);
    //
    // final var name = context.getSecondaryResource(ConfigMap.class).orElseThrow()
    //     .getMetadata().getName();
    // return UpdateControl.patchStatus(createWebPageForStatusUpdate(webPage, name));
    return UpdateControl.patchStatus(webPage);
  }

  @Override
  public DeleteControl cleanup(WebPage resource, Context<WebPage> context) {
    return DeleteControl.defaultDelete();
  }
}
