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
import com.orange.ngsi2.exception.Ngsi2Exception;
import com.orange.ngsi2.model.Attribute;
import com.orange.ngsi2.model.Entity;
import com.orange.ngsi2.model.Paginated;
import org.junit.*;

import org.junit.rules.ExpectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Arrays;
import java.util.Collection;
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public Ngsi2ClientTest() {
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();
        //asyncRestTemplate.setMessageConverters(Collections.singletonList(new MappingJackson2HttpMessageConverter(Utils.objectMapper)));
        ngsiClient = new Ngsi2Client(asyncRestTemplate, baseURL);
        mockServer = MockRestServiceServer.createServer(asyncRestTemplate);
    }

    @Test
    public void testGetV2_ServerError() throws Exception {
        thrown.expect(Ngsi2Exception.class);
        thrown.expectMessage("error: 500 | description: Internal Server Error | affectedItems: [item1, item2, item3]");

        mockServer.expect(requestTo(baseURL + "/v2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError().body(Utils.loadResource("json/error500Response.json")));

        ngsiClient.getV2().get();
    }

    @Test
    public void testGetV2_ClientError() throws Exception {
        thrown.expect(Ngsi2Exception.class);
        thrown.expectMessage("error: 400 | description: Bad Request | affectedItems: null");

        mockServer.expect(requestTo(baseURL + "/v2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest().body(Utils.loadResource("json/error400Response.json")));

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
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getV2Response.json"), MediaType.APPLICATION_JSON));

        Map<String, String> endpoints = ngsiClient.getV2().get();
        assertEquals(4, endpoints.size());
        assertNotNull("/v2/entities", endpoints.get("entities_url"));
    }

    /*
     * Entities requests
     */

    @Test
    public void testGetEntities_Defaults() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getEntitiesResponse.json"), MediaType.APPLICATION_JSON));

        Paginated<Entity> entities = ngsiClient.getEntities(null, null, null, null, 0, 0, false).get();
        assertEquals(3, entities.getItems().size());
        assertNotNull(entities.getItems().get(0));
        assertNotNull(entities.getItems().get(1));
        assertNotNull(entities.getItems().get(2));
        assertEquals("DC_S1-D41", entities.getItems().get(0).getId());
        assertEquals("Room", entities.getItems().get(0).getType());
        assertEquals(35.6, entities.getItems().get(0).getAttributes().get("temperature").getValue());
        assertEquals(0, entities.getOffset());
        assertEquals(0, entities.getLimit());
        assertEquals(0, entities.getTotal());
    }

    @Test
    public void testGetEntities_Paginated() throws Exception {

        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.add("X-Total-Count", "12");

        mockServer.expect(requestTo(baseURL + "/v2/entities?offset=2&limit=10&options=count"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getEntitiesResponse.json"), MediaType.APPLICATION_JSON)
                        .headers(responseHeader));

        Paginated<Entity> entities = ngsiClient.getEntities(null, null, null, null, 2, 10, true).get();
        assertEquals(2, entities.getOffset());
        assertEquals(10, entities.getLimit());
        assertEquals(12, entities.getTotal());
    }

    @Test
    public void testGetEntities_AllParams() throws Exception {

        Collection<String> ids = Arrays.asList("room1", "house1");
        String idPattern = "room.*";
        Collection<String> types = Arrays.asList("Room", "House");
        Collection<String> params = Arrays.asList("temp", "pressure", "humidity");
        String query = "temp>10";
        String georel = "GEOREL";
        String geometry = "GEOMETRY";
        String coords = "COORDS";
        Collection<String> orderBy = Arrays.asList("temp", "!humidity");

        mockServer.expect(requestTo(baseURL + "/v2/entities?id=room1,house1&idPattern=room.*&" +
                "type=Room,House&attrs=temp,pressure,humidity&query=temp%253E10&georel=GEOREL&geometry=GEOMETRY&coords=COORDS&" +
                "orderBy=temp,!humidity&" +
                "offset=2&limit=10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getEntitiesResponse.json"), MediaType.APPLICATION_JSON));

        ngsiClient.getEntities(ids, idPattern, types, params, query, georel, geometry, coords, orderBy, 2, 10, false).get();
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

    @Test
    public void testGetEntity_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/DC_S1-D41?type=Room&attrs=temperature,humidity"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getEntityResponse.json"), MediaType.APPLICATION_JSON));

        ngsiClient.getEntity("DC_S1-D41", "Room", Arrays.asList("temperature", "humidity")).get();
    }

    @Test
    public void testUpdateEntity_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1?type=Room"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.temperature.value").value(35.6))
                .andRespond(withNoContent());

        ngsiClient.updateEntity("room1", "Room", Collections.singletonMap("temperature", new Attribute(35.6)), false).get();
    }

    @Test
    public void testUpdateEntity_Append() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1?type=Room&options=append"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.temperature.value").value(35.6))
                .andRespond(withNoContent());

        ngsiClient.updateEntity("room1", "Room", Collections.singletonMap("temperature", new Attribute(35.6)), true).get();
    }

    @Test
    public void testReplaceEntity_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1?type=Room"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.temperature.value").value(35.6))
                .andRespond(withNoContent());

        Entity e = new Entity("DC_S1-D41", "Room", Collections.singletonMap("temperature", new Attribute(35.6)));

        ngsiClient.replaceEntity("room1", "Room", Collections.singletonMap("temperature", new Attribute(35.6))).get();
    }

    @Test
    public void testDeleteEntity_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1?type=Room"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        ngsiClient.deleteEntity("room1", "Room").get();
    }

}
