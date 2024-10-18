package org.yyc.ignite.operator.dependentresource;

import io.fabric8.kubernetes.api.model.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.utils.TemplateFileLoadUtils;
import org.yyc.ignite.operator.api.AbstractIgniteResourceDiscriminator;
import org.jetbrains.annotations.NotNull;

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

    @Override
    protected ServiceAccount desired(IgniteResource primary, Context<IgniteResource> context) {
        ObjectMetaBuilder metaBuilder = buildMetadataTemplate(primary,COMPONENT);
        ObjectMeta objectMeta = primary.getSpec().getIgniteSaSpec().isBindToGoogleSa()
                ? metaBuilder.withAnnotations(buildAnnotationsMap(primary)).build()
                : metaBuilder.build();
        
        return new ServiceAccountBuilder(template)
                .withMetadata(objectMeta)
                .build();
    }
    
    @NotNull
    private static Map<String, String> buildAnnotationsMap(IgniteResource primary) {
        Map<String, String> annotations = new HashMap<>();
        annotations.put("iam.gke.io/gcp-service-account", primary.getSpec().getIgniteSaSpec().getGoogleServiceAccount());
        return annotations;
    }
    
    static class Discriminator extends AbstractIgniteResourceDiscriminator<ServiceAccount, IgniteResource> {
        public Discriminator() {
            super(COMPONENT);
        }
    }

}
