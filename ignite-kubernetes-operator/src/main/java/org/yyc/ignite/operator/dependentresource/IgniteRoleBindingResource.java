package org.yyc.ignite.operator.dependentresource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.yyc.ignite.operator.api.AbstractIgniteResourceDiscriminator;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.utils.TemplateFileLoadUtils;

import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.buildDependentResourceName;
import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.newK8sMetadataBuilder;

@KubernetesDependent(resourceDiscriminator = IgniteRoleBindingResource.Discriminator.class)
public class IgniteRoleBindingResource extends CRUDKubernetesDependentResource<RoleBinding, IgniteResource> {
    
    public static final String COMPONENT = "ignite-role-binding";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-rolebinding.yaml";
    
    private RoleBinding template;
    
    public IgniteRoleBindingResource() {
        super(RoleBinding.class);
        this.template = TemplateFileLoadUtils.loadYamlTemplate(RoleBinding.class, RESOURCE_TEMPLATE_PATH);
    }
    
    @Override
    protected RoleBinding desired(IgniteResource primary, Context<IgniteResource> context) {
        ObjectMeta metaData = newK8sMetadataBuilder(primary, COMPONENT).build();
        
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
