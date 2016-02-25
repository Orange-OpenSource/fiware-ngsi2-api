package com.orange.ngsi2.exception;

import com.orange.ngsi2.model.Error;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Root exception for all NGSIv2 errors
 */
public class Ngsi2Exception extends RuntimeException {

    private Error error = new Error();

    /**
     * Return specialized exception based on the HTTP status code and error
     * @param statusCode the response code
     * @param error the error
     * @return the corresponding Ngsi2Exception
     */
    public static Ngsi2Exception fromError(int statusCode, Error error) {
        switch (statusCode) {
            case 409: return new ConflictingEntitiesException(error);
            case 400: return new InvalidatedSyntaxException(error);
            default: return new Ngsi2Exception(error);
        }
    }

    public Ngsi2Exception(Error error) {
        this(error.getError(), error.getDescription().orElse(""), error.getAffectedItems().orElse(Collections.emptyList()));
    }

    public Ngsi2Exception(String error, String description, Collection<String> affectedItems) {
        this.error.setError(error);
        if (description != null) {
            this.error.setDescription(Optional.of(description));
        } else {
            this.error.setDescription(Optional.empty());
        }
        if (affectedItems != null) {
            this.error.setAffectedItems(Optional.of(affectedItems));
        } else {
            this.error.setAffectedItems(Optional.empty());
        }
    }

    public Error getError() {
        return error;
    }

    public String getMessage() {
        return error.toString();
    }
}
