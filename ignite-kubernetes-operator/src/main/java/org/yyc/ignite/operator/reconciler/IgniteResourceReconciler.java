package org.yyc.ignite.operator.reconciler;

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
import org.springframework.stereotype.Component;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.type.K8sMetadataLabelEnum;
import org.yyc.ignite.operator.api.type.lifecycle.IgniteClusterLifecycleStateEnum;
import org.yyc.ignite.operator.api.utils.Constants;
import org.yyc.ignite.operator.dependentresource.*;
import org.yyc.ignite.operator.hooks.InitializeHook;
import org.yyc.ignite.operator.hooks.PostDeleteHook;
import org.yyc.ignite.operator.hooks.PreInitializeHook;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.buildDependentResourceName;
import static org.yyc.ignite.operator.api.utils.TimeUtils.isReconcileDurationExceeded;


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
                        deletePostcondition = PostDeleteHook.class,
                        dependsOn = {IgniteRoleBindingResource.COMPONENT, IgniteServiceResource.COMPONENT, IgniteConfigMapResource.COMPONENT})
        }
)
@Component
public class IgniteResourceReconciler implements
        Reconciler<IgniteResource>,
        Cleaner<IgniteResource>,
        ErrorStatusHandler<IgniteResource>,
        EventSourceInitializer<IgniteResource> {
    
    @Override
    public UpdateControl<IgniteResource> reconcile(IgniteResource resource, Context<IgniteResource> context) throws Exception {
        IgniteClusterLifecycleStateEnum nextLifecycleState = getNextLifecycleState(resource, context);
        resource.getStatus().updateLifecycleState(nextLifecycleState);
        if (nextLifecycleState.equals(IgniteClusterLifecycleStateEnum.FAILED)) {
            resource.getStatus().setErrorMessage("the cluster fails to be created due to insufficient number of pods, " +
                    "please inspect pod events or logs for troubleshooting.");
        }
        if (!nextLifecycleState.isTerminal()) {
            return UpdateControl.patchStatus(resource).rescheduleAfter(10, TimeUnit.SECONDS);
        }
        return UpdateControl.patchStatus(resource);
    }
    
    private IgniteClusterLifecycleStateEnum getNextLifecycleState(IgniteResource igniteResource,
                                                                  Context<IgniteResource> context) {
        IgniteClusterLifecycleStateEnum currentLifecycleState = igniteResource.getStatus().getIgniteClusterLifecycleState();
        if (currentLifecycleState.equals(IgniteClusterLifecycleStateEnum.TERMINATING)) {
            return IgniteClusterLifecycleStateEnum.TERMINATING;
        }
        
        KubernetesClient client = context.getClient();
        
        String statefulSetName = buildDependentResourceName(igniteResource, IgniteStatefulSetResource.COMPONENT);
        String namespace = igniteResource.getMetadata().getNamespace();
        StatefulSet statefulSet = client.apps().statefulSets()
                .inNamespace(namespace)
                .withName(statefulSetName)
                .get();
        if (Objects.isNull(statefulSet)) {
            return IgniteClusterLifecycleStateEnum.DEPLOYING;
        }
        
        boolean isIgniteClusterReady = Objects.equals(statefulSet.getStatus().getReadyReplicas(), statefulSet.getSpec().getReplicas());
        if (isIgniteClusterReady) {
            return igniteResource.getSpec().getPersistenceSpec().isPersistenceEnabled()
                    ? IgniteClusterLifecycleStateEnum.INACTIVE_RUNNING
                    : IgniteClusterLifecycleStateEnum.ACTIVE_RUNNING;
        }
        
        String lastReconciledTimestamp = igniteResource.getStatus().getLastLifecycleStateTimestamp();
        if (isReconcileDurationExceeded(lastReconciledTimestamp, Constants.RECONCILE_MAX_RETRY_DURATION)) {
            return IgniteClusterLifecycleStateEnum.FAILED;
        } else if (currentLifecycleState.equals(IgniteClusterLifecycleStateEnum.RECOVERING)) {
            return IgniteClusterLifecycleStateEnum.RECOVERING;
        }
        
        return IgniteClusterLifecycleStateEnum.DEPLOYING;
    }
    
    @Override
    public DeleteControl cleanup(IgniteResource resource, Context<IgniteResource> context) {
        return DeleteControl.defaultDelete();
    }
    
    @Override
    public ErrorStatusUpdateControl<IgniteResource> updateErrorStatus(IgniteResource resource,
                                                                      Context<IgniteResource> context, Exception e) {
        resource.getStatus().updateLifecycleState(IgniteClusterLifecycleStateEnum.FAILED);
        resource.getStatus().setErrorMessage("Exception occurs during reconciliation, please contact ignite operator developer.");
        log.error("Exception occurs during reconciliation: {}", e.getMessage());
        return ErrorStatusUpdateControl.patchStatus(resource).withNoRetry();
    }
    
    /**
     * Register event source for pods within the ignite cluster, any change on pods (ignite nodes) will trigger a reconciliation
     * Note that event source is trivial (won't trigger reconciliation) when primary resource is under FAILED status
     *
     * @param eventSourceContext
     * @return
     */
    @Override
    public Map<String, EventSource> prepareEventSources(EventSourceContext<IgniteResource> eventSourceContext) {
        final SecondaryToPrimaryMapper<Pod> webappsMatchingTomcatName =
                (Pod p) -> eventSourceContext.getPrimaryCache()
                        .list(igniteResource -> String.format("%s-%s-",
                                        igniteResource.getMetadata().getName(),
                                        IgniteStatefulSetResource.COMPONENT)
                                .equals(p.getMetadata().getGenerateName())
                                && !igniteResource.getStatus().getIgniteClusterLifecycleState().equals(IgniteClusterLifecycleStateEnum.FAILED))
                        .map(ResourceID::fromResource)
                        .collect(Collectors.toSet());
        
        final PrimaryToSecondaryMapper<IgniteResource> igniteResourceToPods = (IgniteResource primary) -> {
            Set<ResourceID> podResources = new HashSet<>();
            if (primary.getStatus().getIgniteClusterLifecycleState().equals(IgniteClusterLifecycleStateEnum.FAILED)) {
                return podResources;
            }
            eventSourceContext.getClient().pods().inNamespace(primary.getMetadata().getNamespace())
                    .withLabel(K8sMetadataLabelEnum.NAME.labelName(), primary.getMetadata().getName())
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
