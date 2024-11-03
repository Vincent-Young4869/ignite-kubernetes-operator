package org.yyc.ignite.operator.api.utils;

import lombok.extern.slf4j.Slf4j;
import org.yyc.ignite.operator.api.customresource.IgniteResource;
import org.yyc.ignite.operator.api.type.lifecycle.IgniteClusterLifecycleStateEnum;

import java.util.EnumMap;
import java.util.Map;

@Slf4j
public class LifecycleManageUtils {
    
    private static final Map<IgniteClusterLifecycleStateEnum, IgniteClusterLifecycleStateEnum[]> VALID_TRANSITIONS = new EnumMap<>(IgniteClusterLifecycleStateEnum.class);
    
    static {
        VALID_TRANSITIONS.put(IgniteClusterLifecycleStateEnum.CREATED,
                new IgniteClusterLifecycleStateEnum[]{IgniteClusterLifecycleStateEnum.INITIALIZING, IgniteClusterLifecycleStateEnum.TERMINATING});
        VALID_TRANSITIONS.put(IgniteClusterLifecycleStateEnum.INITIALIZING,
                new IgniteClusterLifecycleStateEnum[]{IgniteClusterLifecycleStateEnum.DEPLOYING, IgniteClusterLifecycleStateEnum.TERMINATING});
        VALID_TRANSITIONS.put(IgniteClusterLifecycleStateEnum.DEPLOYING,
                new IgniteClusterLifecycleStateEnum[]{IgniteClusterLifecycleStateEnum.INACTIVE_RUNNING, IgniteClusterLifecycleStateEnum.ACTIVE_RUNNING, IgniteClusterLifecycleStateEnum.FAILED, IgniteClusterLifecycleStateEnum.TERMINATING});
        VALID_TRANSITIONS.put(IgniteClusterLifecycleStateEnum.ACTIVE_RUNNING,
                new IgniteClusterLifecycleStateEnum[]{IgniteClusterLifecycleStateEnum.DEPLOYING, IgniteClusterLifecycleStateEnum.RECOVERING, IgniteClusterLifecycleStateEnum.TERMINATING});
        VALID_TRANSITIONS.put(IgniteClusterLifecycleStateEnum.INACTIVE_RUNNING,
                new IgniteClusterLifecycleStateEnum[]{IgniteClusterLifecycleStateEnum.ACTIVE_RUNNING, IgniteClusterLifecycleStateEnum.DEPLOYING, IgniteClusterLifecycleStateEnum.TERMINATING});
        VALID_TRANSITIONS.put(IgniteClusterLifecycleStateEnum.RECOVERING,
                new IgniteClusterLifecycleStateEnum[]{IgniteClusterLifecycleStateEnum.ACTIVE_RUNNING, IgniteClusterLifecycleStateEnum.FAILED, IgniteClusterLifecycleStateEnum.TERMINATING});
        VALID_TRANSITIONS.put(IgniteClusterLifecycleStateEnum.TERMINATING,
                new IgniteClusterLifecycleStateEnum[]{});
        VALID_TRANSITIONS.put(IgniteClusterLifecycleStateEnum.FAILED,
                new IgniteClusterLifecycleStateEnum[]{IgniteClusterLifecycleStateEnum.TERMINATING});
    }
    
    public static void statusTransitTo(IgniteResource igniteResource, IgniteClusterLifecycleStateEnum newState) {
        IgniteClusterLifecycleStateEnum currentState = igniteResource.getStatus().getIgniteClusterLifecycleState();
        
        if (isTransitionAllowed(currentState, newState)) {
            log.info("[ignite resource {}]: {} -> {}", igniteResource.getMetadata().getName(), currentState, newState);
            igniteResource.getStatus().updateLifecycleState(newState);
        } else {
            log.warn("Invalid transition for [ignite resource {}]: {} -> {}",
                    igniteResource.getMetadata().getName(), currentState, newState);
        }
    }
    
    private static boolean isTransitionAllowed(IgniteClusterLifecycleStateEnum fromState, IgniteClusterLifecycleStateEnum toState) {
        IgniteClusterLifecycleStateEnum[] allowedTransitions = VALID_TRANSITIONS.getOrDefault(fromState, new IgniteClusterLifecycleStateEnum[]{});
        for (IgniteClusterLifecycleStateEnum state : allowedTransitions) {
            if (state.equals(toState)) {
                return true;
            }
        }
        return false;
    }
}
