package com.orange.ngsi2.exception;

import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * Created by pascale on 11/02/2016.
 */
public class InvalidatedSyntaxExceptionTest {

    @Test
    public void checkProperties() {
        InvalidatedSyntaxException exception = new InvalidatedSyntaxException("DC_S1-D41?");
        assertEquals("error: 400 | description: The incoming request is invalid in this context. DC_S1-D41? has a bad syntax. | affectedItems: null", exception.getMessage());
        assertEquals("400", exception.getError());
        assertEquals(null, exception.getAffectedItems());
    }

}
