package org.yyc.ignite.operator.dependentresource;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import org.yyc.ignite.operator.api.AbstractIgniteResourceDiscriminator;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.type.K8sMetadataLabelEnum;
import org.yyc.ignite.operator.api.utils.TemplateFileLoadUtils;

import java.util.HashMap;
import java.util.Map;

import static org.yyc.ignite.operator.api.type.K8sServiceTypeEnum.ClusterIP;
import static org.yyc.ignite.operator.api.type.K8sServiceTypeEnum.LoadBalancer;
import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.buildMetadataTemplate;

@KubernetesDependent(resourceDiscriminator = IgniteServiceResource.Discriminator.class)
public class IgniteServiceResource extends CRUDKubernetesDependentResource<Service, IgniteResource> {
    
    public static final String COMPONENT = "ignite-service";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-service.yaml";
    
    private Service template;
    
    public IgniteServiceResource() {
        super(Service.class);
        this.template = TemplateFileLoadUtils.loadYamlTemplate(Service.class, RESOURCE_TEMPLATE_PATH);
    }
    
    @Override
    protected Service desired(IgniteResource primary, Context<IgniteResource> context) {
        
        ObjectMeta meta = buildMetadataTemplate(primary, COMPONENT)
                .withAnnotations(primary.getSpec().getK8sServiceSpec().getAnnotations())
                .build();
        
        Map<String, String> selector = new HashMap<>(meta.getLabels());
        selector.put(K8sMetadataLabelEnum.COMPONENT.labelName(), IgniteStatefulSetResource.COMPONENT);
        
        return switch (primary.getSpec().getK8sServiceSpec().getType()) {
            case ClusterIP -> buildClusterIpService(primary, meta, selector);
            case LoadBalancer -> buildLoadBalancerService(primary, meta, selector);
        };
    }
    
    private Service buildClusterIpService(IgniteResource primary, ObjectMeta meta, Map<String, String> selector) {
        return primary.getSpec().getK8sServiceSpec().getIp().isBlank()
                ? new ServiceBuilder(template)
                .withMetadata(meta)
                .editSpec()
                .withType(ClusterIP.name())
                .withSelector(selector)
                .endSpec()
                .build()
                : new ServiceBuilder(template)
                .withMetadata(meta)
                .editSpec()
                .withType(ClusterIP.name())
                .withClusterIP(primary.getSpec().getK8sServiceSpec().getIp())
                .withSelector(selector)
                .endSpec()
                .build();
    }
    
    private Service buildLoadBalancerService(IgniteResource primary, ObjectMeta meta, Map<String, String> selector) {
        return primary.getSpec().getK8sServiceSpec().getIp().isBlank()
                ? new ServiceBuilder(template)
                .withMetadata(meta)
                .editSpec()
                .withType(LoadBalancer.name())
                .withSelector(selector)
                .endSpec()
                .build()
                : new ServiceBuilder(template)
                .withMetadata(meta)
                .editSpec()
                .withType(LoadBalancer.name())
                .withLoadBalancerIP(primary.getSpec().getK8sServiceSpec().getIp())
                .withSelector(selector)
                .endSpec()
                .build();
    }
    
    static class Discriminator extends AbstractIgniteResourceDiscriminator<Service, IgniteResource> {
        public Discriminator() {
            super(COMPONENT);
        }
    }
    
}
