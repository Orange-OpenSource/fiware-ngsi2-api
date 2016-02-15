package com.orange.ngsi2.exception;

import com.orange.ngsi2.model.Error;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

/**
 * Root exception for all NGSIv2 errors
 */
public class Ngsi2Exception extends RuntimeException {

    private String error;

    private String description;

    private Collection<String> affectedItems;

    public Ngsi2Exception(Error error) {
        this(error.getError(), error.getDescription().orElse(""), error.getAffectedItems().orElse(Collections.emptyList()));
    }

    public Ngsi2Exception(String error, String description, Collection<String> affectedItems) {
        super(String.format("error: %s | description: %s | affectedItems: %s", error, description, affectedItems));
        this.error = error;
        this.description = description;
        this.affectedItems = affectedItems;
    }

    public String getError() {
        return error;
    }

    public String getDescription() {
        return description;
    }

    public Collection<String> getAffectedItems() {
        return affectedItems;
    }
}
