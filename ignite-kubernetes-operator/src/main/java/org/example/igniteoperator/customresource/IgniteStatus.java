package org.example.igniteoperator.customresource;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.*;
import org.example.igniteoperator.utils.type.lifecycle.ResourceLifecycleState;

import static org.example.igniteoperator.utils.TimeUtils.currentTimestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IgniteStatus extends ObservedGenerationAwareStatus {
    private ResourceLifecycleState resourceLifecycleState;
    private String lastLifecycleStateTimestamp;
    
    public void updateLifecycleState(ResourceLifecycleState nextState) {
        if (!nextState.equals(resourceLifecycleState)) {
            lastLifecycleStateTimestamp = currentTimestamp();
            resourceLifecycleState = nextState;
        }
    }
}
