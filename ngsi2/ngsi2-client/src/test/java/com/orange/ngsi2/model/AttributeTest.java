package com.orange.ngsi2.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.Test;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by pascale on 08/02/2016.
 */
public class AttributeTest {

    ObjectMapper objectMapper;

    @Test
    public void serializationAttributeWithNullMetadataTest() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());;
        Attribute attribute = new Attribute(23.5);
        attribute.setType(Optional.of("float"));
        String json = objectMapper.writeValueAsString(attribute);
        assertTrue(json.contains("value"));
        assertTrue(json.contains("type"));
        assertTrue(json.contains("metadata"));
    }

    @Test
    public void serializationAttributeWithMetadataTest() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new Jdk8Module());;
        Attribute attribute = new Attribute(23.5);
        attribute.setType(Optional.of("float"));
        Metadata metadata = new Metadata();
        metadata.setType("mesure");
        metadata.setValue("celsius");
        HashMap<String,Metadata> metadatas = new HashMap<String, Metadata>();
        metadatas.put("metadata1", metadata);
        attribute.setMetadata(metadatas);
        String json = objectMapper.writeValueAsString(attribute);
        String jsonString = "{\"value\":23.5,\"type\":\"float\",\"metadata\":{\"metadata1\":{\"value\":\"celsius\",\"type\":\"mesure\"}}}";
        assertEquals(jsonString, json);
    }

}
