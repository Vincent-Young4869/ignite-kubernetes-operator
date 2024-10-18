package org.yyc.ignite.operator.api.type.lifecycle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

public enum IgniteClusterLifecycleStateEnum {
    CREATED(false,
            "The resource was created in Kubernetes but not yet handled by the operator"),
    INITIALIZING(false,
            "The resource is handled by the operator, and is being initialized for the deployment preparation"),
    DEPLOYING(false,
            "The resource is being deployed, but it’s not yet considered to be stable."),
    INACTIVE_RUNNING(true,
            "The ignite cluster is considered to be stable but is inactive (extra trigger is needed to activate cluster, this happens when cluster enables persistence)"),
    ACTIVE_RUNNING(true,
            "The ignite cluster is considered to be stable and is activated"),
    RECOVERING(false,
            "The ignite cluster is self-healing due to one or more ignite node crushes (e.g. due to OOM, network interrupted)"),
    FAILED(true,
            "The ignite cluster fails the deployment, typically no running pods"),
    TERMINATING(false,
            "The ignite cluster is terminating.");
    
    @JsonIgnore
    @Getter
    private final boolean terminal;
    @JsonIgnore
    @Getter
    private final String description;
    
    IgniteClusterLifecycleStateEnum(boolean terminal, String description) {
        this.terminal = terminal;
        this.description = description;
    }
}
