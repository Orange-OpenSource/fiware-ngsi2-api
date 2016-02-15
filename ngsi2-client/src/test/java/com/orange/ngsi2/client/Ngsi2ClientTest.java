/*
 * Copyright (C) 2016 Orange
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.orange.ngsi2.client;

import com.orange.ngsi2.Utils;
import com.orange.ngsi2.model.Attribute;
import com.orange.ngsi2.model.Entity;
import org.junit.*;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;


/**
 * Tests for Ngsi2Client
 */
public class Ngsi2ClientTest {

    private final static String baseURL = "http://localhost:8080";

    private MockRestServiceServer mockServer;

    private Ngsi2Client ngsiClient;

    public Ngsi2ClientTest() {
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();
        //asyncRestTemplate.setMessageConverters(Collections.singletonList(new MappingJackson2HttpMessageConverter(Utils.objectMapper)));
        ngsiClient = new Ngsi2Client(asyncRestTemplate, baseURL);
        mockServer = MockRestServiceServer.createServer(asyncRestTemplate);
    }

    @Test(expected = HttpServerErrorException.class)
    public void testGetV2_ServerError() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        ngsiClient.getV2().get();
    }

    @Test(expected = HttpClientErrorException.class)
    public void testGetV2_ClientError() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest());

        ngsiClient.getV2().get();
    }

    @Test(expected = HttpMessageNotReadableException.class)
    public void testGetV2_SyntaxError() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{/", MediaType.APPLICATION_JSON));

        ngsiClient.getV2().get();
    }

    @Test
    public void testGetV2_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Utils.loadResource("json/getV2Response.json"), MediaType.APPLICATION_JSON));

        Map<String, String> endpoints = ngsiClient.getV2().get();
        assertEquals(4, endpoints.size());
        assertNotNull("/v2/entities", endpoints.get("entities_url"));
    }

    @Test
    public void testGetEntities_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Utils.loadResource("json/getEntitiesResponse.json"), MediaType.APPLICATION_JSON));

        Entity[] entities = ngsiClient.getEntities(null, null, null, null, null, null, null, null, null, 0, 0).get();

        assertEquals(3, entities.length);
        assertNotNull(entities[0]);
        assertNotNull(entities[1]);
        assertNotNull(entities[2]);
        assertEquals("DC_S1-D41", entities[0].getId());
        assertEquals("Room", entities[0].getType());
        assertEquals(35.6, entities[0].getAttributes().get("temperature").getValue());
    }


    @Test
    public void testAddEntity_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value("DC_S1-D41"))
                .andExpect(jsonPath("$.type").value("Room"))
                .andExpect(jsonPath("$.temperature.value").value(35.6))
                .andRespond(withNoContent());

        Entity e = new Entity("DC_S1-D41", "Room", Collections.singletonMap("temperature", new Attribute(35.6)));

        ngsiClient.addEntity(e).get();
    }

}
