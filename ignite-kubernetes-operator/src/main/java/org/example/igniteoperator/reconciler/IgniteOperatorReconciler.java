package org.example.igniteoperator.reconciler;

import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import lombok.extern.slf4j.Slf4j;
import org.example.igniteoperator.conditions.PostDeploymentHook;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.dependentresource.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static org.example.igniteoperator.utils.TimeUtils.currentTimestamp;

@Slf4j
@ControllerConfiguration(
        dependents = {
                @Dependent(name = IgniteSaResource.COMPONENT, type = IgniteSaResource.class),
                @Dependent(name = IgniteRoleResource.COMPONENT, type = IgniteRoleResource.class),
                @Dependent(name = IgniteRoleBindingResource.COMPONENT, type = IgniteRoleBindingResource.class,
                        dependsOn = {IgniteSaResource.COMPONENT, IgniteRoleResource.COMPONENT}),
                @Dependent(name = IgniteConfigMapResource.COMPONENT, type = IgniteConfigMapResource.class),
                @Dependent(name = IgniteServiceResource.COMPONENT, type = IgniteServiceResource.class),
                @Dependent(name = IgniteStatefulSetResource.COMPONENT, type = IgniteStatefulSetResource.class,
                        readyPostcondition = PostDeploymentHook.class,
                        dependsOn = {IgniteRoleBindingResource.COMPONENT, IgniteServiceResource.COMPONENT, IgniteConfigMapResource.COMPONENT})
        }
)
@Component
public class IgniteOperatorReconciler implements Reconciler<IgniteResource>, Cleaner<IgniteResource> {
  public static final String SELECTOR = "managed";

  @Override
  public UpdateControl<IgniteResource> reconcile(IgniteResource resource, Context<IgniteResource> context) throws Exception {
    log.info("current status is {}", resource.getStatus());
    if (!resource.getStatus().getResourceLifecycleState().isTerminal()) {
      return UpdateControl.patchStatus(resource).rescheduleAfter(30, TimeUnit.SECONDS);
    }
    return UpdateControl.patchStatus(resource);
  }

  @Override
  public DeleteControl cleanup(IgniteResource igniteResource, Context<IgniteResource> context) {
    return DeleteControl.defaultDelete();
  }
}
