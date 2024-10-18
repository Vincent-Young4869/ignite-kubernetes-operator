package org.yyc.ignite.operator.api.type;


public enum XmlTagEnum {
    BEAN,
    CLASS,
    NAME,
    VALUE,
    PROPERTY;
    
    public String tagValue() {
        return this.name().toLowerCase();
    }
}
