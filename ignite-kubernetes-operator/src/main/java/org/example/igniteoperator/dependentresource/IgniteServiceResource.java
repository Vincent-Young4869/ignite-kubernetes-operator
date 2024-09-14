package org.example.igniteoperator.dependentresource;

import static org.example.igniteoperator.utils.DependentResourceUtils.fromPrimary;
import static org.example.igniteoperator.utils.type.K8sServiceType.ClusterIP;
import static org.example.igniteoperator.utils.type.K8sServiceType.LoadBalancer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.HashMap;
import java.util.Map;
import org.example.igniteoperator.customresource.IgniteResource;
import org.example.igniteoperator.utils.TemplateLoadUtils;
import org.example.igniteoperator.utils.models.AbstractIgniteResourceDiscriminator;

@KubernetesDependent(resourceDiscriminator = IgniteServiceResource.Discriminator.class)
public class IgniteServiceResource extends CRUDKubernetesDependentResource<Service, IgniteResource> {

    public static final String COMPONENT = "ignite-service";
    private static final String RESOURCE_TEMPLATE_PATH = "templates/ignite-service.yaml";
    
    private Service template;
    public IgniteServiceResource() {
        super(Service.class);
        this.template = TemplateLoadUtils.loadYamlTemplate(Service.class, RESOURCE_TEMPLATE_PATH);
    }

    @Override
    protected Service desired(IgniteResource primary, Context<IgniteResource> context) {

        ObjectMeta meta = fromPrimary(primary,COMPONENT)
                .withAnnotations(primary.getSpec().getK8sServiceSpec().getAnnotations())
                .build();

        Map<String, String> selector = new HashMap<>(meta.getLabels());
        selector.put("component", IgniteStatefulSetResource.COMPONENT);
        
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
