package org.example.igniteoperator.dependentresource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.utils.TemplateLoadUtils;
import org.example.igniteoperator.utils.models.AbstractIgniteResourceDiscriminator;

import static org.example.igniteoperator.utils.DependentResourceUtils.buildDependentResourceName;
import static org.example.igniteoperator.utils.DependentResourceUtils.fromPrimary;

@KubernetesDependent(resourceDiscriminator = IgniteRoleBindingResource.Discriminator.class)
public class IgniteRoleBindingResource extends CRUDKubernetesDependentResource<RoleBinding, IgniteResource> {

    public static final String COMPONENT = "ignite-role-binding";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-rolebinding.yaml";
    
    private RoleBinding template;
    public IgniteRoleBindingResource() {
        super(RoleBinding.class);
        this.template = TemplateLoadUtils.loadYamlTemplate(RoleBinding.class, RESOURCE_TEMPLATE_PATH);
    }

    @Override
    protected RoleBinding desired(IgniteResource primary, Context<IgniteResource> context) {
        ObjectMeta metaData = fromPrimary(primary,COMPONENT).build();
        
        return new RoleBindingBuilder(template)
                .withMetadata(metaData)
                .withRoleRef(new RoleRefBuilder()
                        .withKind("Role")
                        .withName(buildDependentResourceName(primary, IgniteRoleResource.COMPONENT))
                        .withApiGroup("rbac.authorization.k8s.io")
                        .build())
                .withSubjects(new SubjectBuilder()
                        .withKind("ServiceAccount")
                        .withName(buildDependentResourceName(primary, IgniteSaResource.COMPONENT))
                        .withNamespace(primary.getMetadata().getNamespace())
                        .build())
                .build();
    }
    
    static class Discriminator extends AbstractIgniteResourceDiscriminator<RoleBinding, IgniteResource> {
        public Discriminator() {
            super(COMPONENT);
        }
    }

}
