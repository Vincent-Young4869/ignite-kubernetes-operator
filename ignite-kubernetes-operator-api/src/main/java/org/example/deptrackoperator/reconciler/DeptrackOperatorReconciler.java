package org.example.deptrackoperator.reconciler;

import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import org.example.deptrackoperator.customresource.DeptrackResource;
import org.example.deptrackoperator.dependentresource.DeptrackApiServerDeploymentResource;
import org.example.deptrackoperator.dependentresource.DeptrackApiServerServiceResource;
import org.example.deptrackoperator.dependentresource.DeptrackFrontendDeploymentResource;
import org.example.deptrackoperator.dependentresource.DeptrackFrontendServiceResource;
import org.example.deptrackoperator.dependentresource.DeptrackIngressResource;
import org.springframework.stereotype.Component;

@ControllerConfiguration(dependents = {
    @Dependent(name = DeptrackApiServerDeploymentResource.COMPONENT, type = DeptrackApiServerDeploymentResource.class),
    @Dependent(name = DeptrackFrontendDeploymentResource.COMPONENT, type = DeptrackFrontendDeploymentResource.class),
    @Dependent(name = DeptrackApiServerServiceResource.COMPONENT, type = DeptrackApiServerServiceResource.class),
    @Dependent(name = DeptrackFrontendServiceResource.COMPONENT, type = DeptrackFrontendServiceResource.class),
    @Dependent(type = DeptrackIngressResource.class)
})
@Component
public class DeptrackOperatorReconciler implements Reconciler<DeptrackResource>, Cleaner<DeptrackResource> {


  @Override
  public UpdateControl<DeptrackResource> reconcile(DeptrackResource resource, Context<DeptrackResource> context) {
    // return UpdateControl.noUpdate();
    // resource.getStatus()
    //     .setAreWeGood(
    //         context.managedDependentResourceContext()  // accessing workflow reconciliation results
    //             .getWorkflowReconcileResult().orElseThrow()
    //             .allDependentResourcesReady());
    return UpdateControl.patchStatus(resource);
  }

  @Override
  public DeleteControl cleanup(DeptrackResource deptrackResource, Context<DeptrackResource> context) {
    return DeleteControl.defaultDelete();
  }
}
