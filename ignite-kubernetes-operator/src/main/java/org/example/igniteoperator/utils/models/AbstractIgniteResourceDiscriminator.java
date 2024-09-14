package org.example.igniteoperator.utils.models;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import org.example.igniteoperator.customresource.IgniteResource;

import static org.example.igniteoperator.utils.DependentResourceUtils.buildDependentResourceName;

public abstract class AbstractIgniteResourceDiscriminator<R extends HasMetadata, P extends IgniteResource> extends ResourceIDMatcherDiscriminator<R, P> {
    public AbstractIgniteResourceDiscriminator(String component) {
        super(component, (p) -> new ResourceID(buildDependentResourceName(p, component), p.getMetadata().getNamespace()));
    }
}
