package com.orange.ngsi2.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Error model class
 */
public class Error {

    private String error;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Optional<String> description;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Optional<Collection<String>> affectedItems;

    public Error() {
    }

    public Error(String error) {
        this.error = error;
    }

    public Error(String error, Optional<String> description, Optional<Collection<String>> affectedItems) {
        this.error = error;
        this.description = description;
        this.affectedItems = affectedItems;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(Optional<String> description) {
        this.description = description;
    }

    public Optional<Collection<String>> getAffectedItems() {
        return affectedItems;
    }

    public void setAffectedItems(Optional<Collection<String>> affectedItems) {
        this.affectedItems = affectedItems;
    }

    @Override
    public String toString() {
        return String.format("error: %s | description: %s | affectedItems: %s", error, description.orElse(""), affectedItems.orElse(Collections.emptyList()));
    }
}
