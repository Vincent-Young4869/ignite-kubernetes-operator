package org.yyc.ignite.operator.dependentresource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.jetbrains.annotations.NotNull;
import org.yyc.ignite.operator.api.AbstractIgniteResourceDiscriminator;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.utils.TemplateFileLoadUtils;

import java.util.HashMap;
import java.util.Map;

import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.buildMetadataTemplate;

@KubernetesDependent(resourceDiscriminator = IgniteSaResource.Discriminator.class)
public class IgniteSaResource extends CRUDKubernetesDependentResource<ServiceAccount, IgniteResource> {
    
    public static final String COMPONENT = "ignite-sa";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-sa.yaml";
    
    private ServiceAccount template;
    
    public IgniteSaResource() {
        super(ServiceAccount.class);
        this.template = TemplateFileLoadUtils.loadYamlTemplate(ServiceAccount.class, RESOURCE_TEMPLATE_PATH);
    }
    
    @NotNull
    private static Map<String, String> buildAnnotationsMap(IgniteResource primary) {
        Map<String, String> annotations = new HashMap<>();
        annotations.put("iam.gke.io/gcp-service-account", primary.getSpec().getIgniteSaSpec().getGoogleServiceAccount());
        return annotations;
    }
    
    @Override
    protected ServiceAccount desired(IgniteResource primary, Context<IgniteResource> context) {
        ObjectMetaBuilder metaBuilder = buildMetadataTemplate(primary, COMPONENT);
        ObjectMeta objectMeta = primary.getSpec().getIgniteSaSpec().isBindToGoogleSa()
                ? metaBuilder.withAnnotations(buildAnnotationsMap(primary)).build()
                : metaBuilder.build();
        
        return new ServiceAccountBuilder(template)
                .withMetadata(objectMeta)
                .build();
    }
    
    static class Discriminator extends AbstractIgniteResourceDiscriminator<ServiceAccount, IgniteResource> {
        public Discriminator() {
            super(COMPONENT);
        }
    }
    
}
