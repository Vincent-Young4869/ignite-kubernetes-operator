package org.example.igniteoperator.reconciler;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import lombok.extern.slf4j.Slf4j;
import org.example.igniteoperator.conditions.InitializeHook;
import org.example.igniteoperator.conditions.PostDeleteCondition;
import org.example.igniteoperator.conditions.PreInitializeHook;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.dependentresource.*;
import org.example.igniteoperator.utils.Constants;
import org.example.igniteoperator.utils.type.lifecycle.ResourceLifecycleState;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
public class IgniteOperatorReconciler implements
        Reconciler<IgniteResource>, Cleaner<IgniteResource>, ErrorStatusHandler<IgniteResource>, EventSourceInitializer<IgniteResource> {
    public static final String SELECTOR = "managed";
    
    @Override
    public UpdateControl<IgniteResource> reconcile(IgniteResource resource, Context<IgniteResource> context) throws Exception {
        ResourceLifecycleState nextLifecycleState = getNextLifecycleState(resource, context);
        resource.getStatus().updateLifecycleState(nextLifecycleState);
        if (nextLifecycleState.equals(ResourceLifecycleState.FAILED)) {
            resource.getStatus().setErrorMessage("the cluster fails to be created due to insufficient number of pods, " +
                    "please inspect pod events or logs for troubleshooting.");
        }
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
        return DeleteControl.defaultDelete();
    }
    
    @Override
    public ErrorStatusUpdateControl<IgniteResource> updateErrorStatus(IgniteResource resource, Context<IgniteResource> context, Exception e) {
        resource.getStatus().updateLifecycleState(ResourceLifecycleState.FAILED);
        resource.getStatus().setErrorMessage("Exception occurs during reconciliation, please contact ignite operator developer.");
        log.error("Exception occurs during reconciliation: {}", e.getMessage());
        return ErrorStatusUpdateControl.patchStatus(resource).withNoRetry();
    }
    
    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<IgniteResource> eventSourceContext) {
        final SecondaryToPrimaryMapper<Pod> webappsMatchingTomcatName =
                (Pod p) -> eventSourceContext.getPrimaryCache()
                        .list(igniteResource -> String.format("%s-%s-", igniteResource.getMetadata().getName(), IgniteStatefulSetResource.COMPONENT).equals(p.getMetadata().getGenerateName()))
                        .map(ResourceID::fromResource)
                        .collect(Collectors.toSet());
        
        final PrimaryToSecondaryMapper<IgniteResource> igniteResourceToPods = (IgniteResource primary) -> {
            Set<ResourceID> podResources = new HashSet<>();
            eventSourceContext.getClient().pods().inNamespace(primary.getMetadata().getNamespace())
                    .withLabel("name", primary.getMetadata().getName())
                    .list().getItems()
                    .forEach(pod -> podResources.add(ResourceID.fromResource(pod)));
            return podResources;
        };
        
        InformerConfiguration<Pod> configuration =
                InformerConfiguration.from(Pod.class, eventSourceContext)
                        .withSecondaryToPrimaryMapper(webappsMatchingTomcatName)
                        .withPrimaryToSecondaryMapper(igniteResourceToPods)
                        .build();
        return EventSourceInitializer
                .nameEventSources(new InformerEventSource<>(configuration, eventSourceContext));
    }
}
