package org.example.igniteoperator.dependentresource;

import static org.example.igniteoperator.reconciler.IgniteOperatorReconciler.SELECTOR;
import static org.example.igniteoperator.utils.XmlUtils.updateConfigMapXmlData;
import static org.example.igniteoperator.utils.DependentResourceUtils.buildDependentResourceName;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.HashMap;
import java.util.Map;
import org.example.igniteoperator.customresource.IgniteResource;

import org.example.igniteoperator.utils.TemplateLoadUtils;
import org.example.igniteoperator.utils.models.AbstractIgniteResourceDiscriminator;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;

@KubernetesDependent(resourceDiscriminator = IgniteConfigMapResource.Discriminator.class)
public class IgniteConfigMapResource extends CRUDKubernetesDependentResource<ConfigMap, IgniteResource> {

    public static final String COMPONENT = "ignite-configmap";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-node-configuration.xml";
    
    private Document configData;
    public IgniteConfigMapResource() {
        super(ConfigMap.class);
        
        this.configData = TemplateLoadUtils.loadXmlTemplate(RESOURCE_TEMPLATE_PATH);
    }

    @Override
    protected ConfigMap desired(IgniteResource primary, Context<IgniteResource> context) {
        String updatedConfigData;
        try {
          updatedConfigData = updateConfigMapXmlData(configData,
                  primary.getSpec().getIgniteConfigMapSpec(),
                  primary.getSpec().getPersistenceSpec(),
                  primary.getMetadata().getName() + "-" + IgniteServiceResource.COMPONENT,
                  primary.getMetadata().getNamespace());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        Map<String, String> data = new HashMap<>();
        data.put("node-configuration.xml", updatedConfigData);
        return new ConfigMapBuilder()
            .withMetadata(
                new ObjectMetaBuilder()
                    .withName(buildDependentResourceName(primary, COMPONENT))
                    .withNamespace(primary.getMetadata().getNamespace())
                    .withLabels(getLabels())
                    .build())
            .withData(data)
            .build();
    }

    private static @NotNull Map<String, String> getLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put(SELECTOR, "true");
        return labels;
    }
    
    static class Discriminator extends AbstractIgniteResourceDiscriminator<ConfigMap, IgniteResource> {
        public Discriminator() {
            super(COMPONENT);
        }
    }

}
