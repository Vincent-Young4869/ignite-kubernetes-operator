package org.yyc.ignite.operator.dependentresource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.w3c.dom.Document;
import org.yyc.ignite.operator.api.AbstractIgniteResourceDiscriminator;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.utils.TemplateFileLoadUtils;

import java.util.HashMap;
import java.util.Map;

import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.buildMetadataTemplate;
import static org.yyc.ignite.operator.api.utils.XmlUpdateUtils.updateConfigMapXmlData;

@KubernetesDependent(resourceDiscriminator = IgniteConfigMapResource.Discriminator.class)
public class IgniteConfigMapResource extends CRUDKubernetesDependentResource<ConfigMap, IgniteResource> {
    
    public static final String COMPONENT = "ignite-configmap";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-node-configuration.xml";
    
    private Document configData;
    
    public IgniteConfigMapResource() {
        super(ConfigMap.class);
        this.configData = TemplateFileLoadUtils.loadXmlTemplate(RESOURCE_TEMPLATE_PATH);
    }
    
    @Override
    protected ConfigMap desired(IgniteResource primary, Context<IgniteResource> context) {
        String updatedConfigData = updateConfigMapXmlData(configData,
                primary.getSpec().getIgniteConfigMapSpec(),
                primary.getSpec().getPersistenceSpec(),
                primary.getMetadata().getName() + "-" + IgniteServiceResource.COMPONENT,
                primary.getMetadata().getNamespace());
        Map<String, String> data = new HashMap<>();
        data.put("node-configuration.xml", updatedConfigData);
        
        return new ConfigMapBuilder()
                .withMetadata(buildMetadataTemplate(primary, COMPONENT).build())
                .withData(data)
                .build();
    }
    
    static class Discriminator extends AbstractIgniteResourceDiscriminator<ConfigMap, IgniteResource> {
        public Discriminator() {
            super(COMPONENT);
        }
    }
}
