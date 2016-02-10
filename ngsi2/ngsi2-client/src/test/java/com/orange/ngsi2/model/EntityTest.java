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
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by pascale on 09/02/2016.
 */
public class EntityTest {

    ObjectMapper objectMapper;

    @Test
    public void serializationEntityTest() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        ObjectWriter writer = objectMapper.writer(new DefaultPrettyPrinter());

        Entity entity = new Entity("Bcn-Welt", "Room");
        HashMap<String,Attribute> attributes = new HashMap<String, Attribute>();
        Attribute tempAttribute = new Attribute(21.7);
        attributes.put("temperature", tempAttribute);
        Attribute humidityAttribute = new Attribute(60);
        attributes.put("humidity", humidityAttribute);
        entity.setAttributes(attributes);
        String json = writer.writeValueAsString(entity);
        String jsonString = "{\n" +
                "  \"id\" : \"Bcn-Welt\",\n" +
                "  \"type\" : \"Room\",\n" +
                "  \"temperature\" : {\n" +
                "    \"value\" : 21.7,\n" +
                "    \"metadata\" : { }\n" +
                "  },\n" +
                "  \"humidity\" : {\n" +
                "    \"value\" : 60,\n" +
                "    \"metadata\" : { }\n" +
                "  }\n" +
                "}";
        assertEquals(jsonString, json);
    }

    @Test
    public void deserializationEntityTest() throws IOException {
        String jsonString = "{\n" +
                "  \"id\" : \"Bcn-Welt\",\n" +
                "  \"type\" : \"Room\",\n" +
                "  \"temperature\" : {\n" +
                "    \"value\" : 21.7\n" +
                "  },\n" +
                "  \"humidity\" : {\n" +
                "    \"value\" : 60\n" +
                "  }\n" +
                "}";
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        Entity entity = objectMapper.readValue(jsonString, Entity.class);
        Attribute temp = entity.getAttributes().get("temperature");
        assertEquals(0, temp.getMetadata().size());
    }

}
