package org.yyc.ignite.operator.dependentresource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.utils.TemplateFileLoadUtils;
import org.yyc.ignite.operator.api.AbstractIgniteResourceDiscriminator;

import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.buildMetadataTemplate;

@KubernetesDependent(resourceDiscriminator = IgniteRoleResource.Discriminator.class)
public class IgniteRoleResource extends CRUDKubernetesDependentResource<Role, IgniteResource> {

    public static final String COMPONENT = "ignite-role";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-role.yaml";
    
    private Role template;
    public IgniteRoleResource() {
        super(Role.class);
        this.template = TemplateFileLoadUtils.loadYamlTemplate(Role.class, RESOURCE_TEMPLATE_PATH);
    }

    @Override
    protected Role desired(IgniteResource primary, Context<IgniteResource> context) {
        ObjectMeta metaData = buildMetadataTemplate(primary,COMPONENT).build();
        
        return new RoleBuilder(template)
                .withMetadata(metaData)
                .build();
    }
    
    static class Discriminator extends AbstractIgniteResourceDiscriminator<Role, IgniteResource> {
        public Discriminator() {
            super(COMPONENT);
        }
    }

}
