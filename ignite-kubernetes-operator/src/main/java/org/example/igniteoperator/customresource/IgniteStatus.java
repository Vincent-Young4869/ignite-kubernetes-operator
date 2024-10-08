package org.example.igniteoperator.customresource;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.example.igniteoperator.utils.type.lifecycle.ResourceLifecycleState;

import java.util.ArrayList;
import java.util.List;

import static org.example.igniteoperator.utils.TimeUtils.currentTimestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class IgniteStatus extends ObservedGenerationAwareStatus {
    private ResourceLifecycleState resourceLifecycleState;
    private String lastLifecycleStateTimestamp;
    @Builder.Default
    private List<ResourceLifecycleState> historyStates = new ArrayList<>(List.of(ResourceLifecycleState.CREATED));
    private String errorMessage;
    
    public void updateLifecycleState(ResourceLifecycleState nextState) {
        if (!nextState.equals(resourceLifecycleState)) {
            lastLifecycleStateTimestamp = currentTimestamp();
            resourceLifecycleState = nextState;
            historyStates.add(0, nextState);
        }
    }
}
