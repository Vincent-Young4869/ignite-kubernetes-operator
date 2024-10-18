package org.yyc.ignite.operator.utils.type;


public enum XmlTags {
    BEAN,
    CLASS,
    NAME,
    VALUE,
    PROPERTY;
    
    public String tagValue() {
        return this.name().toLowerCase();
    }
}
