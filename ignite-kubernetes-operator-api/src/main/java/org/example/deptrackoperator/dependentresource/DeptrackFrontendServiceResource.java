package org.example.deptrackoperator.dependentresource;

import static org.example.deptrackoperator.utils.BuilderHelper.fromPrimary;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import java.util.HashMap;
import java.util.Map;
import org.example.deptrackoperator.customresource.DeptrackResource;
import org.example.deptrackoperator.utils.BuilderHelper;

@KubernetesDependent(resourceDiscriminator = DeptrackFrontendServiceResource.Discriminator.class)
public class DeptrackFrontendServiceResource extends CRUDKubernetesDependentResource<Service, DeptrackResource> {

    public static final String COMPONENT = "frontend-service";

    private Service template;
    public DeptrackFrontendServiceResource() {
        super(Service.class);
        this.template = BuilderHelper.loadTemplate(Service.class, "templates/frontend-service.yaml");
    }

    @Override
    protected Service desired(DeptrackResource primary, Context<DeptrackResource> context) {

        ObjectMeta meta = fromPrimary(primary,COMPONENT)
          .build();

        Map<String, String> selector = new HashMap<>(meta.getLabels());
        selector.put("component", DeptrackFrontendDeploymentResource.COMPONENT);

        return new ServiceBuilder(template)
          .withMetadata(meta)
          .editSpec()
          .withSelector(selector)
          .endSpec()
          .build();
    }

//    static class Discriminator implements ResourceDiscriminator<Service,DeptrackResource> {
//        @Override
//        public Optional<Service> distinguish(Class<Service> resource, DeptrackResource primary, Context<DeptrackResource> context) {
//            var ies = context.eventSourceRetriever().getResourceEventSourceFor(Service.class,COMPONENT);
//            return ies.getSecondaryResource(primary);
//        }
//    }

    static class Discriminator extends ResourceIDMatcherDiscriminator<Service,DeptrackResource> {
        public Discriminator() {
            super(COMPONENT, (p) -> new ResourceID(p.getMetadata().getName() + "-" + COMPONENT,p.getMetadata().getNamespace()));
        }
    }

}
