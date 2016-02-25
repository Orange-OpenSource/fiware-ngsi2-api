/*
 * Copyright (C) 2016 Orange
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.orange.ngsi2.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test class of Error
 */
public class ErrorTest {

    @Test
    public void checkSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

        Error parseError = new Error("400");
        String json = objectMapper.writeValueAsString(parseError);
        assertTrue(json.contains("error"));
        assertFalse(json.contains("description"));
        assertFalse(json.contains("affectedItems"));
    }

    @Test
    public void checkSerializationComplete() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());

        Error parseError = new Error("400");
        parseError.setDescription(Optional.of("The incoming JSON payload cannot be parsed"));
        parseError.setAffectedItems(Optional.of(Collections.singleton("entities")));
        String json = objectMapper.writeValueAsString(parseError);
        assertTrue(json.contains("error"));
        assertTrue(json.contains("description"));
        assertTrue(json.contains("affectedItems"));
    }

    @Test
    public void checkToString() {
        Error parseError = new Error("400", Optional.of("The incoming JSON payload cannot be parsed"), Optional.empty());
        assertEquals("error: 400 | description: The incoming JSON payload cannot be parsed | affectedItems: []", parseError.toString());
        parseError.setAffectedItems(Optional.of(Collections.singleton("entities")));
        assertEquals("error: 400 | description: The incoming JSON payload cannot be parsed | affectedItems: [entities]", parseError.toString());
    }
}
