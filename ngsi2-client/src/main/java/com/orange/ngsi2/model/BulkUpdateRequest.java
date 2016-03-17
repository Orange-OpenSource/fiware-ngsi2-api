package com.orange.ngsi2.model;

import java.util.Collection;

/**
 * Request for bulk update operation
 */
public class BulkUpdateRequest {

    public enum Action {
        APPEND, APPEND_STRICT, UPDATE, DELETE
    }

    private Action actionType;

    private Collection<Entity> entities;

    public BulkUpdateRequest() {
    }

    public BulkUpdateRequest(Action actionType, Collection<Entity> entities) {
        this.actionType = actionType;
        this.entities = entities;
    }

    public Action getActionType() {
        return actionType;
    }

    public void setActionType(Action actionType) {
        this.actionType = actionType;
    }

    public Collection<Entity> getEntities() {
        return entities;
    }

    public void setEntities(Collection<Entity> entities) {
        this.entities = entities;
    }
}
