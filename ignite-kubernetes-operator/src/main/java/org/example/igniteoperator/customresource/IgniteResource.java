package org.example.igniteoperator.customresource;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("com.yyc")
@Version("v1")
public class IgniteResource extends CustomResource<IgniteSpec, IgniteStatus> implements Namespaced {
}
