package org.example.igniteoperator.dependentresource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.utils.TemplateLoadUtils;
import org.example.igniteoperator.utils.models.AbstractIgniteResourceDiscriminator;

import static org.example.igniteoperator.utils.DependentResourceUtils.fromPrimary;

@KubernetesDependent(resourceDiscriminator = IgniteRoleResource.Discriminator.class)
public class IgniteRoleResource extends CRUDKubernetesDependentResource<Role, IgniteResource> {

    public static final String COMPONENT = "ignite-role";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-role.yaml";
    
    private Role template;
    public IgniteRoleResource() {
        super(Role.class);
        this.template = TemplateLoadUtils.loadYamlTemplate(Role.class, RESOURCE_TEMPLATE_PATH);
    }

    @Override
    protected Role desired(IgniteResource primary, Context<IgniteResource> context) {
        ObjectMeta metaData = fromPrimary(primary,COMPONENT).build();
        
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
