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
        InvalidatedSyntaxException exception = new InvalidatedSyntaxException(Collections.singleton("DC_S1-D41?"));
        assertEquals("error: 400 | description: Syntax invalid | affectedItems: [DC_S1-D41?]", exception.getMessage());
        assertEquals("400", exception.getError());
        assertEquals(1, exception.getAffectedItems().size());
        exception.getAffectedItems().forEach(s -> assertEquals("DC_S1-D41?", s));
    }

}
