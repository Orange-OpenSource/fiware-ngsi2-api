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
import com.orange.ngsi2.Utils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the Attribute
 */
public class AttributeTest {

    @Test
    public void serializationAttributeWithNullMetadataTest() throws JsonProcessingException {
        Attribute attribute = new Attribute(23.5);
        attribute.setType(Optional.of("float"));
        String json = Utils.objectMapper.writeValueAsString(attribute);
        assertTrue(json.contains("value"));
        assertTrue(json.contains("type"));
        assertTrue(json.contains("metadata"));
    }

    @Test
    public void serializationAttributeWithMetadataTest() throws JsonProcessingException {
        Attribute attribute = new Attribute(23.5);
        attribute.setType(Optional.of("float"));
        Metadata metadata = new Metadata();
        metadata.setType("mesure");
        metadata.setValue("celsius");
        HashMap<String,Metadata> metadatas = new HashMap<String, Metadata>();
        metadatas.put("metadata1", metadata);
        attribute.setMetadata(metadatas);
        String json = Utils.objectMapper.writeValueAsString(attribute);
        String jsonString = "{\"value\":23.5,\"type\":\"float\",\"metadata\":{\"metadata1\":{\"value\":\"celsius\",\"type\":\"mesure\"}}}";
        assertEquals(jsonString, json);
    }

}
