package com.orange.ngsi2.model;

/**
 * Scope model
 */
public class Scope {

    private String type;

    private String value;

    public Scope() {
    }

    public Scope(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
