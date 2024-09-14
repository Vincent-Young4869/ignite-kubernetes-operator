package org.example.igniteoperator.reconciler;

import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.dependentresource.*;
import org.springframework.stereotype.Component;

@ControllerConfiguration(dependents = {
    @Dependent(name = IgniteSaResource.COMPONENT, type = IgniteSaResource.class),
    @Dependent(name = IgniteRoleResource.COMPONENT, type = IgniteRoleResource.class),
    @Dependent(name = IgniteRoleBindingResource.COMPONENT, type = IgniteRoleBindingResource.class,
            dependsOn = {IgniteSaResource.COMPONENT, IgniteRoleResource.COMPONENT}),
    @Dependent(name = IgniteConfigMapResource.COMPONENT, type = IgniteConfigMapResource.class),
    @Dependent(name = IgniteServiceResource.COMPONENT, type = IgniteServiceResource.class),
    @Dependent(name = IgniteStatefulSetResource.COMPONENT, type = IgniteStatefulSetResource.class,
        dependsOn = {IgniteRoleBindingResource.COMPONENT, IgniteServiceResource.COMPONENT, IgniteConfigMapResource.COMPONENT})
})
@Component
public class IgniteOperatorReconciler implements Reconciler<IgniteResource>, Cleaner<IgniteResource> {
  public static final String SELECTOR = "managed";

  @Override
  public UpdateControl<IgniteResource> reconcile(IgniteResource resource, Context<IgniteResource> context) throws Exception {
    return UpdateControl.patchStatus(resource);
  }

  @Override
  public DeleteControl cleanup(IgniteResource igniteResource, Context<IgniteResource> context) {
    return DeleteControl.defaultDelete();
  }
}
