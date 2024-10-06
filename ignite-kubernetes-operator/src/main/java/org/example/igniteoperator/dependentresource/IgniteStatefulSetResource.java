package org.example.igniteoperator.dependentresource;

import static org.example.igniteoperator.utils.DependentResourceUtils.buildDependentResourceName;
import static org.example.igniteoperator.utils.DependentResourceUtils.fromPrimary;
import static org.example.igniteoperator.utils.type.IgniteEnvVar.*;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpecBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.utils.Constants;
import org.example.igniteoperator.utils.TemplateLoadUtils;
import org.example.igniteoperator.utils.models.AbstractIgniteResourceDiscriminator;
import org.example.igniteoperator.utils.models.PersistenceSpec;
import org.example.igniteoperator.utils.models.VolumeSpec;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

@KubernetesDependent( resourceDiscriminator = IgniteStatefulSetResource.Discriminator.class)
public class IgniteStatefulSetResource extends CRUDKubernetesDependentResource<StatefulSet, IgniteResource> {
  public static final String COMPONENT = "ignite-cluster";
  private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-cluster-statefulset.yaml";
  private StatefulSet template;

  public IgniteStatefulSetResource() {
    super(StatefulSet.class);
    this.template = TemplateLoadUtils.loadYamlTemplate(StatefulSet.class, RESOURCE_TEMPLATE_PATH);
  }

  @Override
  protected StatefulSet desired(IgniteResource primary, Context<IgniteResource> context) {
    Map<String, String> annotations = new HashMap<>();
    annotations.put("ConfigMapMetadata", primary.getSpec().getIgniteConfigMapSpec().toString());
    annotations.put("podConfig", primary.getSpec().getIgniteNodeSpec().toString());
    ObjectMeta meta = fromPrimary(primary,COMPONENT)
            .withAnnotations(annotations)
            .build();

    return new StatefulSetBuilder(template)
        .withMetadata(meta)
        .withSpec(buildSpec(primary, meta))
        .build();
  }

  private StatefulSetSpec buildSpec(IgniteResource primary, ObjectMeta primaryMeta) {
    if (primary.getSpec().getPersistenceSpec().isPersistenceEnabled()) {
      return new StatefulSetSpecBuilder()
              .withSelector(buildSelector(primaryMeta.getLabels()))
              .withReplicas(primary.getSpec().getReplicas())
              .withTemplate(buildPodTemplate(primary,primaryMeta))
              .withVolumeClaimTemplates(buildPvcTemplates(primary.getSpec().getPersistenceSpec()))
              .build();
    } else {
      return new StatefulSetSpecBuilder()
              .withSelector(buildSelector(primaryMeta.getLabels()))
              .withReplicas(primary.getSpec().getReplicas())
              .withTemplate(buildPodTemplate(primary,primaryMeta))
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
    PodSpec podSpec = new PodSpecBuilder(template.getSpec().getTemplate().getSpec())
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
  
  @NotNull
  private static String parseDockerImageReference(IgniteResource primary) {
    String imageVersion = StringUtils.hasText(primary.getSpec().getIgniteNodeSpec().getIgniteVersion())
            ? ":" + primary.getSpec().getIgniteNodeSpec().getIgniteVersion().trim()
            : "";
    String imageName = StringUtils.hasText(primary.getSpec().getIgniteNodeSpec().getIgniteImage())
            ? primary.getSpec().getIgniteNodeSpec().getIgniteImage().trim()
            : Constants.DEFAULT_GRIDGAIN_IMAGE;
    return imageName + imageVersion;
  }
  
  @NotNull private static Predicate<ContainerBuilder> isIgniteNodeContainer() {
    return c -> c.getName().equals("ignite-node");
  }
  
  @NotNull
  private static Map<String,Quantity> buildResourceRequestQuantityMap(IgniteResource primary) {
    Map<String, Quantity> resourceRequest = new HashMap<>();
    resourceRequest.put("cpu", new Quantity(primary.getSpec().getIgniteNodeSpec().getIgniteNodeCpu()));
    resourceRequest.put("memory", new Quantity(primary.getSpec().getIgniteNodeSpec().getIgniteNodeMemory()));return resourceRequest;
  }
  
  @NotNull
  private static Map<String,Quantity> buildResourceLimitMap(IgniteResource primary) {
    Map<String, Quantity> resourceLimit = new HashMap<>();
    resourceLimit.put("cpu", new Quantity(primary.getSpec().getIgniteNodeSpec().getIgniteNodeCpu()));
    resourceLimit.put("memory", new Quantity(primary.getSpec().getIgniteNodeSpec().getIgniteNodeMemory()));return resourceLimit;
  }
  
  @NotNull
  private static Predicate<VolumeBuilder> isConfigMapVolume() {
    return v -> v.getName().equals("config-vol");
  }

  private List<EnvVar> buildEnvVarList(IgniteResource primary) {
      List<EnvVar> envVars = new ArrayList<>();
      envVars.add(new EnvVar(OPTION_LIBS.name(), primary.getSpec().getIgniteNodeSpec().getIgniteOptionalLibs(), null));
      envVars.add(new EnvVar(JVM_OPTS.name(), primary.getSpec().getIgniteNodeSpec().getJvmOpts(), null));
      envVars.add(new EnvVar(CONFIG_URI.name(), "file:///opt/ignite/config/node-configuration.xml", null));
      return envVars;
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
  
  private static List<VolumeMount> buildPersistenceVolumeMounts(IgniteResource primary) {
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
  
  static class Discriminator extends AbstractIgniteResourceDiscriminator<StatefulSet, IgniteResource> {
    public Discriminator() {
      super(COMPONENT);
    }
  }
}
