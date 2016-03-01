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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the Registration
 */
public class RegistrationTest {

    String jsonString = "{\n" +
            "    \"id\": \"abcdefg\",\n" +
            "    \"subject\": {\n" +
            "      \"entities\": [\n" +
            "        {\n" +
            "          \"id\": \"Bcn_Welt\",\n" +
            "          \"type\": \"Room\"\n" +
            "        }\n" +
            "      ],\n" +
            "      \"attributes\": [\n" +
            "        \"temperature\"\n" +
            "      ]\n" +
            "    },\n" +
            "    \"callback\": \"http://weather.example.com/ngsi\",\n" +
            "    \"metadata\": {\n" +
            "      \"providingService\": {\n" +
            "        \"value\": \"weather.example.com\",\n" +
            "        \"type\": \"none\"\n" +
            "      },\n" +
            "      \"providingAuthority\": {\n" +
            "        \"value\": \"AEMET - Spain\",\n" +
            "        \"type\": \"none\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"duration\": \"PT1M\"\n" +
            "  }";

    @Test
    public void serializationRegistrationTest() throws JsonProcessingException, MalformedURLException {
        ObjectWriter writer = Utils.objectMapper.writer(new DefaultPrettyPrinter());

        Registration registration = new Registration("abcde", new URL("http://localhost:1234"));
        registration.setDuration("PT1M");
        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setType(Optional.of("Room"));
        Subject subject = new Subject(Collections.singletonList(subjectEntity), Collections.singletonList("humidity"));
        registration.setSubject(subject);
        String json = writer.writeValueAsString(registration);
        String jsonString ="{\n" +
                "  \"id\" : \"abcde\",\n" +
                "  \"subject\" : {\n" +
                "    \"entities\" : [ {\n" +
                "      \"type\" : \"Room\"\n" +
                "    } ],\n" +
                "    \"attributes\" : [ \"humidity\" ]\n" +
                "  },\n" +
                "  \"callback\" : \"http://localhost:1234\",\n" +
                "  \"duration\" : \"PT1M\"\n" +
                "}";
        assertEquals(jsonString, json);
    }

    @Test
    public void deserializationEntityTypeTest() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());
        Registration registration = objectMapper.readValue(jsonString, Registration.class);
        assertEquals("abcdefg", registration.getId());
        assertEquals(new URL("http://weather.example.com/ngsi"), registration.getCallback());
        assertEquals("PT1M", registration.getDuration());
        assertEquals(1, registration.getSubject().getEntities().size());
        assertEquals("Bcn_Welt", registration.getSubject().getEntities().get(0).getId().get());
        assertEquals("Room", registration.getSubject().getEntities().get(0).getType().get());
        assertEquals(1, registration.getSubject().getAttributes().size());
        assertEquals("temperature", registration.getSubject().getAttributes().get(0));
        assertEquals(2, registration.getMetadata().size());
        assertTrue(registration.getMetadata().containsKey("providingService"));
        assertTrue(registration.getMetadata().containsKey("providingAuthority"));
    }
}
