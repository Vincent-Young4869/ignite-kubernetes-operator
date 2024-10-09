package org.example.igniteoperator.reconciler;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import lombok.extern.slf4j.Slf4j;
import org.example.igniteoperator.conditions.InitializeHook;
import org.example.igniteoperator.conditions.PostDeleteCondition;
import org.example.igniteoperator.conditions.PreInitializeHook;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.dependentresource.*;
import org.example.igniteoperator.utils.Constants;
import org.example.igniteoperator.utils.type.lifecycle.ResourceLifecycleState;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.example.igniteoperator.utils.DependentResourceUtils.buildDependentResourceName;
import static org.example.igniteoperator.utils.TimeUtils.isReconcileDurationExceeded;


@Slf4j
@ControllerConfiguration(
        dependents = {
                @Dependent(name = IgniteSaResource.COMPONENT, type = IgniteSaResource.class,
                        reconcilePrecondition = PreInitializeHook.class,
                        readyPostcondition = InitializeHook.class),
                @Dependent(name = IgniteRoleResource.COMPONENT, type = IgniteRoleResource.class,
                        dependsOn = {IgniteSaResource.COMPONENT}),
                @Dependent(name = IgniteRoleBindingResource.COMPONENT, type = IgniteRoleBindingResource.class,
                        dependsOn = {IgniteSaResource.COMPONENT, IgniteRoleResource.COMPONENT}),
                @Dependent(name = IgniteConfigMapResource.COMPONENT, type = IgniteConfigMapResource.class,
                        dependsOn = {IgniteRoleResource.COMPONENT}),
                @Dependent(name = IgniteServiceResource.COMPONENT, type = IgniteServiceResource.class,
                        dependsOn = {IgniteRoleResource.COMPONENT}),
                @Dependent(name = IgniteStatefulSetResource.COMPONENT, type = IgniteStatefulSetResource.class,
                        deletePostcondition = PostDeleteCondition.class,
                        dependsOn = {IgniteRoleBindingResource.COMPONENT, IgniteServiceResource.COMPONENT, IgniteConfigMapResource.COMPONENT})
        }
)
@Component
public class IgniteOperatorReconciler implements Reconciler<IgniteResource>, Cleaner<IgniteResource>, ErrorStatusHandler<IgniteResource> {
    public static final String SELECTOR = "managed";
    
    @Override
    public UpdateControl<IgniteResource> reconcile(IgniteResource resource, Context<IgniteResource> context) throws Exception {
        ResourceLifecycleState nextLifecycleState = getNextLifecycleState(resource, context);
        resource.getStatus().updateLifecycleState(nextLifecycleState);
        if (nextLifecycleState.equals(ResourceLifecycleState.FAILED)) {
            resource.getStatus().setErrorMessage("the cluster fails to be created due to insufficient number of pods, " +
                    "please inspect pod events or logs for troubleshooting.");
        }
        log.info("{}...", nextLifecycleState);
        if (!nextLifecycleState.isTerminal()) {
            return UpdateControl.patchStatus(resource).rescheduleAfter(10, TimeUnit.SECONDS);
        }
        return UpdateControl.patchStatus(resource);
    }
    
    private ResourceLifecycleState getNextLifecycleState(IgniteResource igniteResource, Context<IgniteResource> context) {
        ResourceLifecycleState currentLifecycleState = igniteResource.getStatus().getResourceLifecycleState();
        if (currentLifecycleState.equals(ResourceLifecycleState.TERMINATING)) {
            return ResourceLifecycleState.TERMINATING;
        }
        
        KubernetesClient client = context.getClient();
        
        String statefulSetName = buildDependentResourceName(igniteResource, IgniteStatefulSetResource.COMPONENT);
        String namespace = igniteResource.getMetadata().getNamespace();
        StatefulSet statefulSet = client.apps().statefulSets()
                .inNamespace(namespace)
                .withName(statefulSetName)
                .get();
        if (Objects.isNull(statefulSet)) {
            return ResourceLifecycleState.DEPLOYING;
        }
        
        boolean isIgniteClusterReady = Objects.equals(statefulSet.getStatus().getReadyReplicas(), statefulSet.getSpec().getReplicas());
        if (isIgniteClusterReady) {
            return igniteResource.getSpec().getPersistenceSpec().isPersistenceEnabled()
                    ? ResourceLifecycleState.INACTIVE_RUNNING
                    : ResourceLifecycleState.ACTIVE_RUNNING;
        }
        
        String lastReconciledTimestamp = igniteResource.getStatus().getLastLifecycleStateTimestamp();
        if (isReconcileDurationExceeded(lastReconciledTimestamp, Constants.RECONCILE_MAX_RETRY_DURATION)) {
            return ResourceLifecycleState.FAILED;
        } else if (currentLifecycleState.equals(ResourceLifecycleState.RECOVERING)) {
            return ResourceLifecycleState.RECOVERING;
        }
        
        return ResourceLifecycleState.DEPLOYING;
    }
    
    @Override
    public DeleteControl cleanup(IgniteResource resource, Context<IgniteResource> context) {
        // KubernetesClient client = context.getClient();
        // List<Pod> pods = client.pods().inNamespace(resource.getMetadata().getNamespace())
        //         .withLabel("name", resource.getMetadata().getName())
        //         .list()
        //         .getItems();
        // if (!pods.isEmpty()) {
        //     IgniteResource latestResource = client.resource(resource).get();
        //     latestResource.getStatus().updateLifecycleState(ResourceLifecycleState.TERMINATING);
        //     client.resource(latestResource).updateStatus();
        // }
        
        return DeleteControl.defaultDelete();
    }
    
    @Override
    public ErrorStatusUpdateControl<IgniteResource> updateErrorStatus(IgniteResource resource, Context<IgniteResource> context, Exception e) {
        resource.getStatus().updateLifecycleState(ResourceLifecycleState.FAILED);
        resource.getStatus().setErrorMessage("Exception occurs during reconciliation, please contact ignite operator developer.");
        log.error("Exception occurs during reconciliation: {}", e.getMessage());
        return ErrorStatusUpdateControl.patchStatus(resource).withNoRetry();
    }
}
