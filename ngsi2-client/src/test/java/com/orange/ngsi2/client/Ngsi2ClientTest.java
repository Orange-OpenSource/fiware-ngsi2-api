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
import com.orange.ngsi2.model.*;
import org.junit.*;

import org.junit.rules.ExpectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;

import java.net.URL;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

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
        thrown.expectMessage("error: 400 | description: Bad Request | affectedItems: []");

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
        Georel georel = new Georel(GeorelEnum.near, Optional.of(ModifierEnum.maxDistance), Optional.of(Float.parseFloat("1000")));
        GeometryEnum geometry = GeometryEnum.point;
        List<Coordinate> coords = Arrays.asList(new Coordinate(Double.parseDouble("-10.5"),Double.parseDouble("30.5")), new Coordinate(Double.parseDouble("-15.5"),Double.parseDouble("35.5")));
        Collection<String> orderBy = Arrays.asList("temp", "!humidity");

        mockServer.expect(requestTo(baseURL + "/v2/entities?id=room1,house1&idPattern=room.*&" +
                "type=Room,House&attrs=temp,pressure,humidity&query=temp%253E10&georel=near;maxDistance:1000.0&geometry=point&coords=-10.5,30.5;-15.5,35.5&" +
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

        ngsiClient.replaceEntity("room1", "Room", Collections.singletonMap("temperature", new Attribute(35.6))).get();
    }

    @Test
    public void testDeleteEntity_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1?type=Room"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        ngsiClient.deleteEntity("room1", "Room").get();
    }

    /*
     * Attributes requests
     */

    @Test
    public void testGetAttribute_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/DC_S1-D41/attrs/temperature?type=Room"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getAttributeResponse.json"), MediaType.APPLICATION_JSON));

        Attribute attribute = ngsiClient.getAttribute("DC_S1-D41", "Room", "temperature").get();

        assertNotNull(attribute);
        assertEquals(35.6, attribute.getValue());
        assertEquals("urn:phenomenum:temperature", attribute.getType().get());
        assertNotNull(attribute.getMetadata());
        assertEquals("2015-06-04T07:20:27.378Z", attribute.getMetadata().get("timestamp").getValue());
        assertEquals("date", attribute.getMetadata().get("timestamp").getType());
    }

    @Test
    public void testUpdateAttribute_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1/attrs/temperature?type=Room"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.value").value(35.6))
                .andExpect(jsonPath("$.metadata.timestamp.type").value("date"))
                .andExpect(jsonPath("$.metadata.timestamp.value").value("2015-06-04T07:20:27.378Z"))
                .andRespond(withNoContent());

        Attribute attribute = new Attribute(35.6);
        attribute.addMetadata("timestamp", new Metadata("date", "2015-06-04T07:20:27.378Z"));
        ngsiClient.updateAttribute("room1", "Room", "temperature", attribute).get();
    }

    @Test
    public void testDeleteAttibute_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1/attrs/temperature?type=Room"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        ngsiClient.deleteAttribute("room1", "Room", "temperature").get();
    }

    @Test
    public void testGetAttributeValue_Object() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1/attrs/object/value?type=Room"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Utils.loadResource("json/getAttributeValueObjectResponse.json"), MediaType.APPLICATION_JSON));

        Map<String, Object> e = (Map<String, Object>) ngsiClient.getAttributeValue("room1", "Room", "object").get();

        assertEquals(42, e.get("int"));
        assertEquals(3.1415, e.get("float"));
        assertEquals("hello world !", e.get("string"));
        assertEquals(null, e.get("null"));
        assertEquals(Arrays.asList(0, 1,2,3,4,5,6), e.get("array"));
        assertEquals(Collections.singletonMap("hello", "world"), e.get("object"));
    }

    @Test
    public void testGetAttributeValue_Array() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1/attrs/array/value?type=Room"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(Utils.loadResource("json/getAttributeValueArrayResponse.json"), MediaType.APPLICATION_JSON));

        Object e = ngsiClient.getAttributeValue("room1", "Room", "array").get();

        assertEquals(Arrays.asList(0, 1,2,3,4,5,6), e);
    }

    @Test
    public void testGetAttributeValueAsString_Number() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/entities/room1/attrs/text/value?type=Room"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN_VALUE))
                .andRespond(withSuccess("some random text", MediaType.TEXT_PLAIN));

        String result = ngsiClient.getAttributeValueAsString("room1", "Room", "text").get();

        assertEquals("some random text", result);
    }

    /*
     * Entity types requests
     */

    @Test
    public void testGetEntityTypes_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/types"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getEntityTypesResponse.json"), MediaType.APPLICATION_JSON));

        Paginated<EntityType> result = ngsiClient.getEntityTypes(0, 0, false).get();

        assertNotNull(result.getItems());
        assertEquals(result.getItems().size(), 2);
        assertEquals(result.getItems().get(0).getType(), "Car");
        assertNotNull(result.getItems().get(0).getAttrs());
        assertEquals(result.getItems().get(0).getAttrs().size(), 3);
        assertEquals(result.getItems().get(0).getAttrs().get("speed").getType(), "none");
    }

    @Test
    public void testGetEntityType_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/types/Car"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getEntityTypeResponse.json"), MediaType.APPLICATION_JSON));

        EntityType entityType = ngsiClient.getEntityType("Car").get();

        assertNotNull(entityType);
        assertNotNull(entityType.getAttrs());
        assertEquals(entityType.getAttrs().size(), 3);
        assertEquals(entityType.getAttrs().get("pressure").getType(), "none");
    }

    /*
     * Registrations requests
     */

    @Test
    public void testGetRegistrations_Defaults() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/registrations"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getRegistrationsResponse.json"), MediaType.APPLICATION_JSON));

        List<Registration> registrations = ngsiClient.getRegistrations().get();
        assertEquals(1, registrations.size());
        assertEquals("abcdefg", registrations.get(0).getId());
        assertEquals("http://weather.example.com/ngsi", registrations.get(0).getCallback().toString());
        assertEquals("PT1M", registrations.get(0).getDuration());
        assertEquals(1, registrations.get(0).getSubject().getEntities().size());
        assertEquals("Bcn_Welt", registrations.get(0).getSubject().getEntities().get(0).getId().get());
        assertEquals(1, registrations.get(0).getSubject().getAttributes().size());
        assertEquals("temperature", registrations.get(0).getSubject().getAttributes().get(0));
        assertEquals(2, registrations.get(0).getMetadata().size());
        assertTrue(registrations.get(0).getMetadata().containsKey("providingService"));
        assertTrue(registrations.get(0).getMetadata().containsKey("providingAuthority"));
    }

    @Test
    public void testAddRegistration_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/registrations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.duration").value("PT1M"))
                .andRespond(withNoContent());

        Registration registration = new Registration();
        registration.setDuration("PT1M");
        registration.setCallback(new URL("http://localhost:1234"));
        Metadata metadata = new Metadata("example");
        registration.addMetadata("provider", metadata);
        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setType(Optional.of("Room"));
        SubjectRegistration subjectRegistration = new SubjectRegistration(Collections.singletonList(subjectEntity), Collections.singletonList("humidity"));
        registration.setSubject(subjectRegistration);
        ngsiClient.addRegistration(registration).get();
    }

    @Test
    public void testGetRegistration_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/registrations/abcdef"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getRegistrationResponse.json"), MediaType.APPLICATION_JSON));

        Registration registration = ngsiClient.getRegistration("abcdef").get();

        assertNotNull(registration);
        assertEquals("abcdef", registration.getId());
        assertEquals("http://localhost:1234", registration.getCallback().toString());
        assertEquals("PT1M", registration.getDuration());
        assertNotNull(registration.getSubject().getEntities());
        assertEquals(1,registration.getSubject().getEntities().size());
        assertEquals("Room",registration.getSubject().getEntities().get(0).getType().get());
        assertEquals(1,registration.getSubject().getAttributes().size());
        assertEquals("humidity",registration.getSubject().getAttributes().get(0));
    }

    @Test
    public void testUpdateRegistration_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/registrations/abcdef"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.duration").value("PT1M"))
                .andRespond(withNoContent());

        Registration registration = new Registration();
        registration.setDuration("PT1M");
        ngsiClient.updateRegistration("abcdef", registration).get();
    }

    @Test
    public void testDeleteRegistration_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/registrations/abcdef"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withNoContent());

        ngsiClient.deleteRegistration("abcdef").get();
    }

    /*
     * Subscriptions requests
     */

    @Test
    public void testGetSubscriptions_Defaults() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/subscriptions"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getSubscriptionsResponse.json"), MediaType.APPLICATION_JSON));

        Paginated<Subscription> subscriptions = ngsiClient.getSubscriptions(0,0,false).get();
        assertEquals(1, subscriptions.getItems().size());
        assertNotNull(subscriptions.getItems().get(0));
        assertEquals("abcdefg", subscriptions.getItems().get(0).getId());
        assertEquals("active", subscriptions.getItems().get(0).getStatus().toString());
        assertEquals("Bcn_Welt", subscriptions.getItems().get(0).getSubject().getEntities().get(0).getId().get());
        assertTrue(subscriptions.getItems().get(0).getSubject().getCondition().getAttributes().contains("temperature"));
        assertEquals("temperature>40", subscriptions.getItems().get(0).getSubject().getCondition().getExpression().get("q"));
        assertEquals("http://localhost:1234", subscriptions.getItems().get(0).getNotification().getCallback().toString());
        assertTrue(subscriptions.getItems().get(0).getNotification().getHeaders().isPresent());
        assertEquals(1, subscriptions.getItems().get(0).getNotification().getHeaders().get().size());
        assertTrue(subscriptions.getItems().get(0).getNotification().getHeaders().get().containsKey("X-MyHeader"));
        assertEquals("foo", subscriptions.getItems().get(0).getNotification().getHeaders().get().get("X-MyHeader"));
        assertTrue(subscriptions.getItems().get(0).getNotification().getQuery().isPresent());
        assertEquals(1, subscriptions.getItems().get(0).getNotification().getQuery().get().size());
        assertTrue(subscriptions.getItems().get(0).getNotification().getQuery().get().containsKey("authToken"));
        assertEquals("bar", subscriptions.getItems().get(0).getNotification().getQuery().get().get("authToken"));
        assertTrue(subscriptions.getItems().get(0).getNotification().getAttrsFormat().isPresent());
        assertEquals(FormatEnum.keyValues, subscriptions.getItems().get(0).getNotification().getAttrsFormat().get());
        assertEquals(2, subscriptions.getItems().get(0).getNotification().getAttributes().size());
        assertTrue(subscriptions.getItems().get(0).getNotification().getAttributes().contains("temperature"));
        assertTrue(subscriptions.getItems().get(0).getNotification().getAttributes().contains("humidity"));
        assertEquals(new Long(5), subscriptions.getItems().get(0).getNotification().getThrottling().get());
        assertEquals(12, subscriptions.getItems().get(0).getNotification().getTimesSent());

        assertEquals(0, subscriptions.getOffset());
        assertEquals(0, subscriptions.getLimit());
        assertEquals(0, subscriptions.getTotal());
    }

    @Test
    public void testGetSubscriptions_Paginated() throws Exception {

        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.add("X-Total-Count", "1");

        mockServer.expect(requestTo(baseURL + "/v2/subscriptions?offset=2&limit=10&options=count"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getSubscriptionsResponse.json"), MediaType.APPLICATION_JSON)
                        .headers(responseHeader));

        Paginated<Subscription> subscriptions = ngsiClient.getSubscriptions(2,10,true).get();

        assertEquals(2, subscriptions.getOffset());
        assertEquals(10, subscriptions.getLimit());
        assertEquals(1, subscriptions.getTotal());
    }

    @Test
    public void testAddSubscription_OK() throws Exception {

        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.add("Location", "/v2/subscriptions/abcde98765");

        mockServer.expect(requestTo(baseURL + "/v2/subscriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.subject.entities[0].type").value("Room"))
                .andExpect(jsonPath("$.subject.condition.attributes[0]").value("temperature"))
                .andExpect(jsonPath("$.subject.condition.expression.q").value("temperature>40"))
                .andRespond(withNoContent().headers(responseHeader));

        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setType(Optional.of("Room"));
        Condition condition = new Condition();
        condition.setAttributes(Collections.singletonList("temperature"));
        condition.setExpression("q", "temperature>40");
        SubjectSubscription subjectSubscription = new SubjectSubscription(Collections.singletonList(subjectEntity), condition);
        List<String> attributes = new ArrayList<>();
        attributes.add("temperature");
        attributes.add("humidity");
        Notification notification = new Notification(attributes, new URL("http://localhost:1234"));
        notification.setThrottling(Optional.of(new Long(5)));
        Subscription subscription = new Subscription();
        subscription.setSubject(subjectSubscription);
        subscription.setNotification(notification);
        subscription.setExpires(Instant.parse("2016-04-05T14:00:00.20Z"));

        assertEquals("abcde98765", ngsiClient.addSubscription(subscription).get());
    }

    @Test
    public void testAddSubscription_returnEmpty() throws Exception {

        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.add("Location", "");

        mockServer.expect(requestTo(baseURL + "/v2/subscriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.subject.entities[0].type").value("Room"))
                .andExpect(jsonPath("$.subject.condition.attributes[0]").value("temperature"))
                .andExpect(jsonPath("$.subject.condition.expression.q").value("temperature>40"))
                .andRespond(withNoContent().headers(responseHeader));

        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setType(Optional.of("Room"));
        Condition condition = new Condition();
        condition.setAttributes(Collections.singletonList("temperature"));
        condition.setExpression("q", "temperature>40");
        SubjectSubscription subjectSubscription = new SubjectSubscription(Collections.singletonList(subjectEntity), condition);
        List<String> attributes = new ArrayList<>();
        attributes.add("temperature");
        attributes.add("humidity");
        Notification notification = new Notification(attributes, new URL("http://localhost:1234"));
        notification.setThrottling(Optional.of(new Long(5)));
        Subscription subscription = new Subscription();
        subscription.setSubject(subjectSubscription);
        subscription.setNotification(notification);
        subscription.setExpires(Instant.parse("2016-04-05T14:00:00.20Z"));

        assertEquals("", ngsiClient.addSubscription(subscription).get());
    }

    @Test
    public void testGetSubscription_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/subscriptions/abcdef"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(Utils.loadResource("json/getSubscriptionResponse.json"), MediaType.APPLICATION_JSON));

        Subscription subscription = ngsiClient.getSubscription("abcdef").get();
        assertEquals("abcdef", subscription.getId());
        assertEquals("active", subscription.getStatus().toString());
    }

    @Test
    public void testUpdateSubscription_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/subscriptions/abcdef"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.expires").value("2017-04-05T14:00:00.200Z"))
                .andRespond(withNoContent());

        Subscription subscription = new Subscription();
        subscription.setExpires(Instant.parse("2017-04-05T14:00:00.200Z"));
        ngsiClient.updateSubscription("abcdef", subscription);
    }

    @Test
    public void testDeleteSubscription_OK() throws Exception {

        mockServer.expect(requestTo(baseURL + "/v2/subscriptions/abcdef"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withNoContent());

        ngsiClient.deleteSubscription("abcdef");
    }

}
