package org.example.deptrackoperator.customresource;

import java.util.Map;
import lombok.Data;

@Data
public class DeptrackSpec {
  // Images
  private String apiServerImage = "registry.k8s.io/e2e-test-images/agnhost:2.39";
  private String apiServerVersion = "";

  private String frontendImage = "dependencytrack/frontend";
  private String frontendVersion = "";

  // PVC settings: NOT IMPLEMENTED
  private String pvcClass = ""; // Use default storage class
  private String pvcSize = "1Gi";


  // Database settings: NOT IMPLEMENTED
  private String dbUrl;
  private String dbDriver = "org.postgresql.Driver";
  private String dbSecret;


  // Ingress settings
  private String ingressHostname;
  private Map<String,String> ingressAnnotations;
}
