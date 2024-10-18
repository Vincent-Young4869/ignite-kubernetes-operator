package org.yyc.ignite.operator.api;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import org.yyc.ignite.operator.api.customresource.IgniteResource;

import static org.yyc.ignite.operator.api.utils.DependentResourceUtils.buildDependentResourceName;

public abstract class AbstractIgniteResourceDiscriminator<R extends HasMetadata, P extends IgniteResource> extends ResourceIDMatcherDiscriminator<R, P> {
    public AbstractIgniteResourceDiscriminator(String component) {
        super(component, (p) -> new ResourceID(buildDependentResourceName(p, component), p.getMetadata().getNamespace()));
    }
}
