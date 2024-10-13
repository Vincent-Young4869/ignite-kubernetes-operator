package org.example.igniteoperator.customresource;

import lombok.Data;
import org.example.igniteoperator.utils.models.*;

@Data
public class IgniteSpec {
  // service account
  private IgniteSaSpec igniteSaSpec = new IgniteSaSpec();
  
  private Integer replicas = 1;
  private IgniteNodeSpec igniteNodeSpec;
  
  private K8sServiceSpec k8sServiceSpec = new K8sServiceSpec();
  private IgniteConfigMapSpec igniteConfigMapSpec = new IgniteConfigMapSpec();
  private PersistenceSpec persistenceSpec;
}
