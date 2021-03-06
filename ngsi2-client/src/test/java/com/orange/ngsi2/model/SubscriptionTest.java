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
import com.fasterxml.jackson.databind.ObjectWriter;
import com.orange.ngsi2.Utils;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the Subscription
 */
public class SubscriptionTest {

    String jsonString="{\n" +
            "  \"id\" : \"abcdefg\",\n" +
            "  \"subject\" : {\n" +
            "    \"entities\" : [ {\n" +
            "      \"id\" : \"Bcn_Welt\",\n" +
            "      \"type\" : \"Room\"\n" +
            "    } ],\n" +
            "    \"condition\" : {\n" +
            "      \"attributes\" : [ \"temperature\" ],\n" +
            "      \"expression\" : {\n" +
            "        \"q\" : \"temperature>40\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"notification\" : {\n" +
            "    \"attributes\" : [ \"temperature\", \"humidity\" ],\n" +
            "    \"callback\" : \"http://localhost:1234\",\n" +
            "    \"headers\" : {\n" +
            "      \"X-MyHeader\" : \"foo\"\n" +
            "    },\n" +
            "    \"query\" : {\n" +
            "      \"authToken\" : \"bar\"\n" +
            "    },\n" +
            "    \"attrsFormat\" : \"keyValues\",\n" +
            "    \"throttling\" : 5,\n" +
            "    \"timesSent\" : 12,\n" +
            "    \"lastNotification\" : \"2015-10-05T16:00:00.100Z\"\n" +
            "  },\n" +
            "  \"expires\" : \"2016-04-05T14:00:00.200Z\",\n" +
            "  \"status\" : \"active\"\n" +
            "}";

    @Test
    public void serializationSubscriptionTest() throws JsonProcessingException, MalformedURLException {
        ObjectWriter writer = Utils.objectMapper.writer(new DefaultPrettyPrinter());

        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setId(Optional.of("Bcn_Welt"));
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
        notification.setTimesSent(12);
        notification.setLastNotification(Instant.parse("2015-10-05T16:00:00.10Z"));
        notification.setHeader("X-MyHeader", "foo");
        notification.setQuery("authToken", "bar");
        notification.setAttrsFormat(Optional.of(Notification.Format.keyValues));
        String json = writer.writeValueAsString(new Subscription("abcdefg", subjectSubscription, notification, Instant.parse("2016-04-05T14:00:00.20Z"), Subscription.Status.active));

        assertEquals(jsonString, json);
    }

    @Test
    public void deserializationSubscriptionTest() throws IOException {

        Subscription subscription = Utils.objectMapper.readValue(jsonString, Subscription.class);
        assertEquals("abcdefg", subscription.getId());
        assertEquals("2016-04-05T14:00:00.200Z", subscription.getExpires().toString());
        assertEquals(Subscription.Status.active, subscription.getStatus());
        assertEquals(1, subscription.getSubject().getEntities().size());
        assertEquals(1, subscription.getSubject().getCondition().getAttributes().size());
        assertTrue(subscription.getSubject().getCondition().getAttributes().contains("temperature"));
        assertEquals(1, subscription.getSubject().getCondition().getExpression().size());
        assertTrue(subscription.getSubject().getCondition().getExpression().containsKey("q"));
        assertEquals("temperature>40", subscription.getSubject().getCondition().getExpression().get("q"));
        assertEquals(2, subscription.getNotification().getAttributes().size());
        assertTrue(subscription.getNotification().getAttributes().contains("temperature"));
        assertTrue(subscription.getNotification().getAttributes().contains("humidity"));
        assertEquals(new URL("http://localhost:1234"), subscription.getNotification().getCallback());
        assertEquals(Optional.of(new Long(5)), subscription.getNotification().getThrottling());
        assertEquals(12, subscription.getNotification().getTimesSent());
        assertTrue(subscription.getNotification().getHeaders().isPresent());
        assertEquals(1, subscription.getNotification().getHeaders().get().size());
        assertTrue(subscription.getNotification().getHeaders().get().containsKey("X-MyHeader"));
        assertEquals("foo", subscription.getNotification().getHeaders().get().get("X-MyHeader"));
        assertTrue(subscription.getNotification().getQuery().isPresent());
        assertEquals(1, subscription.getNotification().getQuery().get().size());
        assertTrue(subscription.getNotification().getQuery().get().containsKey("authToken"));
        assertEquals("bar", subscription.getNotification().getQuery().get().get("authToken"));
        assertTrue(subscription.getNotification().getAttrsFormat().isPresent());
        assertEquals(Notification.Format.keyValues, subscription.getNotification().getAttrsFormat().get());
        assertEquals("2015-10-05T16:00:00.100Z", subscription.getNotification().getLastNotification().toString());
    }
}
