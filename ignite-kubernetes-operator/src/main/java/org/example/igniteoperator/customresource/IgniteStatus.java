package org.example.igniteoperator.customresource;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Data;

@Data
public class IgniteStatus extends ObservedGenerationAwareStatus {
}
