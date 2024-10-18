package org.yyc.ignite.operator.api.utils;

import java.time.Duration;
import java.time.Instant;

public class TimeUtils {
    
    public static String currentTimestamp() {
        return Instant.now().toString();
    }
    
    public static boolean isReconcileDurationExceeded(String lastReconciledTime, Duration maxTimeDuration) {
        Instant instant = Instant.parse(lastReconciledTime);
        Duration reconciliationDelay = Duration.between(instant, Instant.now());
        return reconciliationDelay.compareTo(maxTimeDuration) > 0;
    }
}
