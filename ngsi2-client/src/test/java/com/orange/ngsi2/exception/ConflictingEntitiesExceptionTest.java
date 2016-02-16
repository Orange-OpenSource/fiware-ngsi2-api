package com.orange.ngsi2.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by pascale on 16/02/2016.
 */
public class ConflictingEntitiesExceptionTest {

    @Test
    public void checkProperties() {
        ConflictingEntitiesException exception = new ConflictingEntitiesException("Boe-Idearium","GET /v2/entities?id=Boe-Idearium&attrs=temperature");
        assertEquals("error: 409 | description: Too many results. There are several results that match with the Boe-Idearium used in the request. Instead of, you can use GET /v2/entities?id=Boe-Idearium&attrs=temperature | affectedItems: null", exception.getMessage());
    }
}
