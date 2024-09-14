package org.example.igniteoperator.customresource;

import lombok.Data;
import org.example.igniteoperator.utils.models.IgniteConfigMapSpec;
import org.example.igniteoperator.utils.models.IgniteSaSpec;
import org.example.igniteoperator.utils.models.K8sServiceSpec;
import org.example.igniteoperator.utils.models.PersistenceSpec;

@Data
public class IgniteSpec {
  // service account
  private IgniteSaSpec igniteSaSpec = new IgniteSaSpec();
  
  // Images
  private String igniteImage = "";
  private String igniteVersion = "8.8.42-openjdk17";

  // StatefulSet settings
  private Integer replicas = 1;
  private String igniteOptionalLibs = "";
  private String jvmOpts = "-Xms1g -Xmx1g";
  private String igniteNodeCpu = "1";
  private String igniteNodeMemory = "3Gi";  // request and limit share the same memory size for now
  
  private K8sServiceSpec k8sServiceSpec = new K8sServiceSpec();
  private IgniteConfigMapSpec igniteConfigMapSpec;
  private PersistenceSpec persistenceSpec;

}
