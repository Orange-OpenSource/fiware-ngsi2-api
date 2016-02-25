package com.orange.ngsi2.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by pascale on 24/02/2016.
 */
public class NotAcceptableExceptionTest {

    @Test
    public void checkProperties() {
        NotAcceptableException exception = new NotAcceptableException();
        assertEquals("error: 406 | description: Not Acceptable: Accepted MIME types: text/plain. | affectedItems: []", exception.getMessage());
    }
}
