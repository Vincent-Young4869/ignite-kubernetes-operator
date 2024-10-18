package org.yyc.ignite.operator.api.spec;

import lombok.Data;
import org.yyc.ignite.operator.api.type.K8sServiceTypeEnum;

import java.util.HashMap;
import java.util.Map;

@Data
public class K8sServiceSpec {
    private K8sServiceTypeEnum type =  K8sServiceTypeEnum.ClusterIP;
    private String ip = "";
    private Map<String, String> annotations = new HashMap<>();
}
