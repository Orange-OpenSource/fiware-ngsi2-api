package com.orange.ngsi2.model;

import java.util.List;

/**
 * Request for bulk registering operation
 */
public class BulkRegisterRequest {

    public enum ActionType {
        CREATE, UPDATE, DELETE
    }

    private ActionType actionType;

    List<Registration> registrations;

    public BulkRegisterRequest() {
    }

    public BulkRegisterRequest(ActionType actionType, List<Registration> registrations) {
        this.actionType = actionType;
        this.registrations = registrations;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public List<Registration> getRegistrations() {
        return registrations;
    }

    public void setRegistrations(List<Registration> registrations) {
        this.registrations = registrations;
    }
}
