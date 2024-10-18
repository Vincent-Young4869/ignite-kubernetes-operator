package org.yyc.ignite.operator.utils.models;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import org.yyc.ignite.operator.customresource.IgniteResource;

import static org.yyc.ignite.operator.utils.DependentResourceUtils.buildDependentResourceName;

public abstract class AbstractIgniteResourceDiscriminator<R extends HasMetadata, P extends IgniteResource> extends ResourceIDMatcherDiscriminator<R, P> {
    public AbstractIgniteResourceDiscriminator(String component) {
        super(component, (p) -> new ResourceID(buildDependentResourceName(p, component), p.getMetadata().getNamespace()));
    }
}
