package org.example.igniteoperator.utils.models;

import lombok.Data;
import org.example.igniteoperator.utils.type.K8sServiceType;

import java.util.HashMap;
import java.util.Map;

@Data
public class K8sServiceSpec {
    private K8sServiceType type =  K8sServiceType.ClusterIP;
    private String ip = "";
    private Map<String, String> annotations = new HashMap<>();
}
