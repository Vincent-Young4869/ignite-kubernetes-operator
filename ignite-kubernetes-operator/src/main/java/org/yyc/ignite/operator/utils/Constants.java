package org.yyc.ignite.operator.utils;

import java.time.Duration;

public interface Constants {

    String OPERATOR_NAME = "ignite-operator";
    String DEFAULT_GRIDGAIN_IMAGE = "gridgain/community";
    
    Duration RECONCILE_MAX_RETRY_DURATION = Duration.ofMinutes(2);
    
}
