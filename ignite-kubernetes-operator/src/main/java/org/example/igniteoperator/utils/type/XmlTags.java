package org.example.igniteoperator.utils.type;


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
