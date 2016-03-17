package com.orange.ngsi2.model;

import java.util.List;

/**
 * Request for bulk query operation
 */
public class BulkQueryRequest {

    private List<SubjectEntity> entities;

    private List<String> attributes;

    private List<Scope> scopes;

    public BulkQueryRequest() {
    }

    public BulkQueryRequest(List<SubjectEntity> entities, List<String> attributes, List<Scope> scopes) {
        this.entities = entities;
        this.attributes = attributes;
        this.scopes = scopes;
    }

    public List<SubjectEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<SubjectEntity> entities) {
        this.entities = entities;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }
}
