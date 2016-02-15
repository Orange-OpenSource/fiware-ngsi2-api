package com.orange.ngsi2.exception;

import java.util.Collection;

/**
 * Created by pascale on 10/02/2016.
 */
public class Ngsi2Exception extends Exception {

    private String error;

    private String description;

    private Collection<String> affectedItems;

    public Ngsi2Exception(String error, String description, Collection<String> affectedItems) {
        super("");
        this.error = error;
        this.description = description;
        this.affectedItems = affectedItems;
    }

    @Override
    public String getMessage() {
        return "error: " + error + " | description: " + description + " | affectedItems: " + affectedItems;
    }

}
