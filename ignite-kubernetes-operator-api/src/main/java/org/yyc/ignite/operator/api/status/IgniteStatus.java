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
    private List<IgniteClusterLifecycleStateEnum> historyStates = new ArrayList<>(List.of(IgniteClusterLifecycleStateEnum.CREATED));
    private String errorMessage;
    
    // TODO: include timestamp into historyStates, consider use Queue/Stack
    public synchronized void updateLifecycleState(IgniteClusterLifecycleStateEnum nextState) {
        lastLifecycleStateTimestamp = currentTimestamp();
        igniteClusterLifecycleState = nextState;
        historyStates.add(0, nextState);
    }
}
