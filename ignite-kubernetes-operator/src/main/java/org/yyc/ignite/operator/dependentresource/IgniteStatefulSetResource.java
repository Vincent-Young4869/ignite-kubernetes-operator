package org.yyc.ignite.operator.dependentresource;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDNoGCKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.springframework.util.StringUtils;
import org.yyc.ignite.operator.api.AbstractIgniteResourceDiscriminator;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.spec.PersistenceSpec;
import org.yyc.ignite.operator.api.spec.VolumeSpec;
import org.yyc.ignite.operator.api.utils.Constants;
import org.yyc.ignite.operator.api.utils.TemplateFileLoadUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.yyc.ignite.operator.api.type.IgniteEnvVarEnum.*;
import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.*;
import static org.yyc.ignite.operator.dependentresource.IgniteConfigMapResource.NODE_CONFIG_FILE_NAME;

@KubernetesDependent(resourceDiscriminator = IgniteStatefulSetResource.Discriminator.class)
public class IgniteStatefulSetResource extends CRUDNoGCKubernetesDependentResource<StatefulSet, IgniteResource> {
    public static final String COMPONENT = "ignite-cluster";
    public static final String IGNITE_NODE_CONTAINER_NAME = "ignite-node";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-cluster-statefulset.yaml";
    private static final StatefulSet RESOURCE_TEMPLATE = TemplateFileLoadUtils.loadYamlTemplate(StatefulSet.class, RESOURCE_TEMPLATE_PATH);
    
    public IgniteStatefulSetResource() {
        super(StatefulSet.class);
    }
    
    @Override
    protected StatefulSet desired(IgniteResource primary, Context<IgniteResource> context) {
        Map<String, String> annotations = new HashMap<>();
        annotations.put("ConfigMapDataHash", sha256Hex(primary.getSpec().getIgniteConfigMapSpec().toString()));
        annotations.put("podConfigHash", sha256Hex(primary.getSpec().getIgniteNodeSpec().toString()));
        ObjectMeta metadata = newK8sMetadataBuilder(primary, COMPONENT)
                .withAnnotations(annotations)
                .build();
        
        return new StatefulSetBuilder(RESOURCE_TEMPLATE)
                .withMetadata(metadata)
                .withSpec(buildSpec(primary, metadata))
                .build();
    }
    
    private StatefulSetSpec buildSpec(IgniteResource primary, ObjectMeta primaryMeta) {
        if (primary.getSpec().getPersistenceSpec().isPersistenceEnabled()) {
            return new StatefulSetSpecBuilder()
                    .withSelector(buildSelector(primaryMeta.getLabels()))
                    .withReplicas(primary.getSpec().getReplicas())
                    .withTemplate(buildPodTemplate(primary, primaryMeta))
                    .withVolumeClaimTemplates(buildPvcTemplates(primary.getSpec().getPersistenceSpec()))
                    .build();
        } else {
            return new StatefulSetSpecBuilder()
                    .withSelector(buildSelector(primaryMeta.getLabels()))
                    .withReplicas(primary.getSpec().getReplicas())
                    .withTemplate(buildPodTemplate(primary, primaryMeta))
                    .build();
        }
    }
    
    private LabelSelector buildSelector(Map<String, String> labels) {
        return new LabelSelectorBuilder()
                .addToMatchLabels(labels)
                .build();
    }
    
    private PodTemplateSpec buildPodTemplate(IgniteResource primary, ObjectMeta primaryMeta) {
        return new PodTemplateSpecBuilder()
                .withMetadata(primaryMeta)
                .withSpec(buildPodSpec(primary))
                .build();
    }
    
    private PodSpec buildPodSpec(IgniteResource primary) {
        PodSpec podSpec = new PodSpecBuilder(RESOURCE_TEMPLATE.getSpec().getTemplate().getSpec())
                .editMatchingContainer(isIgniteNodeContainer())// Assumes we have a single container
                .withImage(parseDockerImageReference(primary))
                .withEnv(buildEnvVarList(primary))
                .withNewResources()
                .addToRequests(buildResourceRequestQuantityMap(primary))
                .addToLimits(buildResourceLimitMap(primary))
                .endResources()
                .and()
                .editMatchingVolume(isConfigMapVolume())
                .editConfigMap()
                .withName(buildDependentResourceName(primary, IgniteConfigMapResource.COMPONENT))
                .endConfigMap()
                .endVolume()
                .withServiceAccountName(buildDependentResourceName(primary, IgniteSaResource.COMPONENT))
                .build();
        
        if (primary.getSpec().getPersistenceSpec().isPersistenceEnabled()) {
            podSpec = podSpec.edit()
                    .editMatchingContainer(isIgniteNodeContainer())
                    .addAllToVolumeMounts(buildPersistenceVolumeMounts(primary))
                    .and()
                    .build();
        }
        return podSpec;
    }
    
    private Predicate<ContainerBuilder> isIgniteNodeContainer() {
        return c -> c.getName().equals(IGNITE_NODE_CONTAINER_NAME);
    }
    
    private Predicate<VolumeBuilder> isConfigMapVolume() {
        return v -> v.getName().equals("config-vol");
    }
    
    private String parseDockerImageReference(IgniteResource primary) {
        String imageVersion = StringUtils.hasText(primary.getSpec().getIgniteNodeSpec().getIgniteVersion())
                ? ":" + primary.getSpec().getIgniteNodeSpec().getIgniteVersion().trim()
                : "";
        String imageName = StringUtils.hasText(primary.getSpec().getIgniteNodeSpec().getIgniteImage())
                ? primary.getSpec().getIgniteNodeSpec().getIgniteImage().trim()
                : Constants.DEFAULT_GRIDGAIN_IMAGE;
        return imageName + imageVersion;
    }
    
    private List<EnvVar> buildEnvVarList(IgniteResource primary) {
        List<EnvVar> envVars = new ArrayList<>();
        envVars.add(new EnvVar(OPTION_LIBS.name(), primary.getSpec().getIgniteNodeSpec().getIgniteOptionalLibs(), null));
        envVars.add(new EnvVar(JVM_OPTS.name(), primary.getSpec().getIgniteNodeSpec().getJvmOpts(), null));
        envVars.add(new EnvVar(CONFIG_URI.name(), "file:///opt/ignite/config/" + NODE_CONFIG_FILE_NAME, null));
        return envVars;
    }
    
    private Map<String, Quantity> buildResourceRequestQuantityMap(IgniteResource primary) {
        Map<String, Quantity> resourceRequest = new HashMap<>();
        resourceRequest.put("cpu", new Quantity(primary.getSpec().getIgniteNodeSpec().getIgniteNodeCpu()));
        resourceRequest.put("memory", new Quantity(primary.getSpec().getIgniteNodeSpec().getIgniteNodeMemory()));
        return resourceRequest;
    }
    
    private Map<String, Quantity> buildResourceLimitMap(IgniteResource primary) {
        Map<String, Quantity> resourceLimit = new HashMap<>();
        resourceLimit.put("cpu", new Quantity(primary.getSpec().getIgniteNodeSpec().getIgniteNodeCpu()));
        resourceLimit.put("memory", new Quantity(primary.getSpec().getIgniteNodeSpec().getIgniteNodeMemory()));
        return resourceLimit;
    }
    
    private List<VolumeMount> buildPersistenceVolumeMounts(IgniteResource primary) {
        VolumeMount dataVol = new VolumeMountBuilder()
                .withName(primary.getSpec().getPersistenceSpec().getDataVolumeSpec().getName())
                .withMountPath(primary.getSpec().getPersistenceSpec().getDataVolumeSpec().getMountPath())
                .build();
        VolumeMount walVol = new VolumeMountBuilder()
                .withName(primary.getSpec().getPersistenceSpec().getWalVolumeSpec().getName())
                .withMountPath(primary.getSpec().getPersistenceSpec().getWalVolumeSpec().getMountPath())
                .build();
        VolumeMount walArchiveVol = new VolumeMountBuilder()
                .withName(primary.getSpec().getPersistenceSpec().getWalArchiveVolumeSpec().getName())
                .withMountPath(primary.getSpec().getPersistenceSpec().getWalArchiveVolumeSpec().getMountPath())
                .build();
        List<VolumeMount> volumes = List.of(dataVol, walVol, walArchiveVol);
        return volumes;
    }
    
    private List<PersistentVolumeClaim> buildPvcTemplates(PersistenceSpec persistenceSpec) {
        return List.of(
                buildPvc(persistenceSpec.getDataVolumeSpec()),
                buildPvc(persistenceSpec.getWalVolumeSpec()),
                buildPvc(persistenceSpec.getWalArchiveVolumeSpec()));
    }
    
    private PersistentVolumeClaim buildPvc(VolumeSpec volumeSpec) {
        return new PersistentVolumeClaimBuilder()
                .withNewMetadata().withName(volumeSpec.getName()).endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .addToRequests("storage", new Quantity(volumeSpec.getStorage()))
                .endResources()
                .endSpec()
                .build();
    }
    
    static class Discriminator extends AbstractIgniteResourceDiscriminator<StatefulSet, IgniteResource> {
        public Discriminator() {
            super(COMPONENT);
        }
    }
}
