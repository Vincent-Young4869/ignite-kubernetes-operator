package org.example.igniteoperator.utils.models;

import lombok.Data;

@Data
public class IgniteNodeSpec {
    private String igniteImage = "";
    private String igniteVersion = "8.8.42-openjdk17";
    
    // StatefulSet settings
    private Integer replicas = 1;
    private String igniteOptionalLibs = "";
    private String jvmOpts = "-Xms1g -Xmx1g";
    private String igniteNodeCpu = "1";
    private String igniteNodeMemory = "3Gi";  // request and limit share the same memory size for now
}
