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
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.orange.ngsi2.Utils;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the Entity Type
 */
public class EntityTypeTest {

    private String jsonString = "{\n" +
            "  \"attrs\" : {\n" +
            "    \"temperature\" : {\n" +
            "      \"type\" : \"urn:phenomenum:temperature\"\n" +
            "    },\n" +
            "    \"humidity\" : {\n" +
            "      \"type\" : \"percentage\"\n" +
            "    },\n" +
            "    \"pressure\" : {\n" +
            "      \"type\" : \"null\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"count\" : 7\n" +
            "}";

    @Test
    public void serializationEntityTypeTest() throws JsonProcessingException {
        ObjectWriter writer = Utils.objectMapper.writer(new DefaultPrettyPrinter());

        EntityType entityType = new EntityType();
        Map<String,AttributeType> attrs = new HashMap<>();
        AttributeType tempAttribute = new AttributeType("urn:phenomenum:temperature");
        attrs.put("temperature", tempAttribute);
        AttributeType humidityAttribute = new AttributeType("percentage");
        attrs.put("humidity", humidityAttribute);
        AttributeType pressureAttribute = new AttributeType("null");
        attrs.put("pressure", pressureAttribute);
        entityType.setAttrs(attrs);
        entityType.setCount(7);
        String json = writer.writeValueAsString(entityType);

        assertEquals(jsonString, json);
    }

    @Test
    public void deserializationEntityTypeTest() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        EntityType entityType = objectMapper.readValue(jsonString, EntityType.class);
        assertEquals(7, entityType.getCount());
        assertEquals(3, entityType.getAttrs().size());
        assertTrue(entityType.getAttrs().containsKey("temperature"));
        assertTrue(entityType.getAttrs().containsKey("humidity"));
        assertTrue(entityType.getAttrs().containsKey("pressure"));
        assertEquals("urn:phenomenum:temperature", entityType.getAttrs().get("temperature").getType());
        assertEquals("percentage", entityType.getAttrs().get("humidity").getType());
        assertEquals("null", entityType.getAttrs().get("pressure").getType());
    }
}
