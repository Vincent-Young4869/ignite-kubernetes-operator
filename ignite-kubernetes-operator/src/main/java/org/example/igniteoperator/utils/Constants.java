package org.example.igniteoperator.utils;

import java.time.Duration;

public interface Constants {

    String OPERATOR_NAME = "ignite-operator";
    String DEFAULT_GRIDGAIN_IMAGE = "gridgain/community";
    String DEFAULT_FRONTEND_IMAGE = "dependencytrack/frontend";
    
    Duration RECONCILE_MAX_RETRY_DURATION = Duration.ofMinutes(2);
}
