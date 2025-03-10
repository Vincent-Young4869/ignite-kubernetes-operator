package org.yyc.ignite.operator.api.status;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.yyc.ignite.operator.api.type.lifecycle.IgniteClusterLifecycleStateEnum;

import java.util.ArrayList;
import java.util.List;

import static org.yyc.ignite.operator.api.utils.TimeUtils.currentTimestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class IgniteStatus extends ObservedGenerationAwareStatus {
    private IgniteClusterLifecycleStateEnum igniteClusterLifecycleState;
    private String lastLifecycleStateTimestamp;
    @Builder.Default
    private final List<IgniteClusterLifecycleStateEnum> historyStates = new ArrayList<>(List.of(IgniteClusterLifecycleStateEnum.CREATED));
    private String errorMessage;
    
    public void updateLifecycleState(IgniteClusterLifecycleStateEnum nextState) {
        lastLifecycleStateTimestamp = currentTimestamp();
        igniteClusterLifecycleState = nextState;
        synchronized (historyStates) {
            if (historyStates.isEmpty() || !historyStates.get(0).equals(nextState)) {
                historyStates.add(0, nextState);
            }
        }
    }
}
