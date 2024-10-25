package org.yyc.ignite.operator.api.type;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum K8sMetadataLabelEnum {
    NAME("name"),
    COMPONENT("component"),
    MANAGED_BY("ManagedBy");
    
    private final String labelName;
    
    public String labelName() {
        return this.labelName;
    }
}
