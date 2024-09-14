package org.example.deptrackoperator.customresource;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeptrackStatus extends ObservedGenerationAwareStatus {
  private Boolean areWeGood;
  private String errorMessage;
}
