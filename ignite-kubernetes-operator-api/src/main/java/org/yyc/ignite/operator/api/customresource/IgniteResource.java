package org.yyc.ignite.operator.api.customresource;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import org.yyc.ignite.operator.api.spec.IgniteSpec;
import org.yyc.ignite.operator.api.status.IgniteStatus;

@Group("com.yyc")
@Version("v1")
public class IgniteResource extends CustomResource<IgniteSpec, IgniteStatus> implements Namespaced {
}
