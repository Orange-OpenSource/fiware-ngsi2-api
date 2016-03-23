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

package com.orange.ngsi2.utility;

import com.orange.ngsi2.model.*;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.*;

/**
 * Helpers for tests
 */
public class Utils {

    static public Map<String, String> createListResourcesReference() {
        HashMap<String, String> resources = new HashMap<>();

        resources.put("entities_url", "/v2/entities");
        resources.put("types_url", "/v2/types");
        resources.put("subscriptions_url", "/v2/subscriptions");
        resources.put("registrations_url", "/v2/registrations");

        return resources;
    }

    static public List<Entity> createListEntitiesReference() {

        List<Entity> entities = new ArrayList<Entity>();

        Entity entityRoomDC = new Entity("DC_S1-D41", "Room");
        entityRoomDC.setAttributes("temperature", new Attribute(35.6));
        entities.add(entityRoomDC);

        Entity entityRoomBoe = new Entity("Boe-Idearium", "Room");
        entityRoomBoe.setAttributes("temperature", new Attribute(22.5));
        entities.add(entityRoomBoe);

        Entity entityCar = new Entity("P-9873-K", "Car");
        Attribute speedAttribute = new Attribute(100);
        speedAttribute.setType(Optional.of("number"));
        Metadata accuracyMetadata = new Metadata();
        accuracyMetadata.setValue(2);
        Metadata timestampMetadata = new Metadata();
        timestampMetadata.setValue("2015-06-04T07:20:27.378Z");
        timestampMetadata.setType("date");
        speedAttribute.addMetadata("accuracy", accuracyMetadata);
        speedAttribute.addMetadata("timestamp", timestampMetadata);
        entityCar.setAttributes("speed", speedAttribute);
        entities.add(entityCar);

        return entities;
    }

    static public List<Entity> createListEntitiesWrongSyntax() {

        List<Entity> entities = new ArrayList<Entity>();

        Entity entityRoomDC = new Entity("DC_S1 D41", "Room");
        entityRoomDC.setAttributes("temperature", new Attribute(35.6));
        entities.add(entityRoomDC);

        Entity entityRoomBoe = new Entity("Boe Idearium", "Room");
        entityRoomBoe.setAttributes("temperature", new Attribute(22.5));
        entities.add(entityRoomBoe);

        Entity entityCar = new Entity("P-9873 K", "Car");
        Attribute speedAttribute = new Attribute(100);
        speedAttribute.setType(Optional.of("number"));
        Metadata accuracyMetadata = new Metadata();
        accuracyMetadata.setValue(2);
        Metadata timestampMetadata = new Metadata();
        timestampMetadata.setValue("2015-06-04T07:20:27.378Z");
        timestampMetadata.setType("date");
        speedAttribute.addMetadata("accuracy", accuracyMetadata);
        speedAttribute.addMetadata("timestamp", timestampMetadata);
        entityCar.setAttributes("speed", speedAttribute);
        entities.add(entityCar);

        return entities;
    }

    static public Entity createEntityBcnWelt() {

        Entity EntityBcnWelt = new Entity("Bcn-Welt", "Room");
        EntityBcnWelt.setAttributes("temperature", new Attribute(21.7));
        EntityBcnWelt.setAttributes("humidity", new Attribute(60));
        return EntityBcnWelt;
    }

    static public List<Entity> createListEntitiesConflictingReference() {

        List<Entity> entities = new ArrayList<Entity>();

        Entity entityRoomA = new Entity("Boe-Idearium", "RoomA");
        entityRoomA.setAttributes("temperature", new Attribute(35.6));
        entities.add(entityRoomA);

        Entity entityRoomB = new Entity("Boe-Idearium", "RoomB");
        entityRoomB.setAttributes("temperature", new Attribute(22.5));
        entities.add(entityRoomB);

        return entities;
    }

    static public HashMap<String, Attribute> createUpdateAttributesReference() {

        HashMap<String, Attribute> attributes = new HashMap<>();
        Attribute noiseAttribut = new Attribute(31.5);
        noiseAttribut.setType(Optional.of("float"));
        Metadata metadata = new Metadata("decibel");
        metadata.setType("string");
        noiseAttribut.addMetadata("metric", metadata);
        attributes.put("ambientNoise", noiseAttribut);
        return attributes;
    }

    static public HashMap<String, Attribute> createUpdateAttributesWithBadSyntax() {

        HashMap<String, Attribute> attributes = new HashMap<>();
        Attribute noiseAttribut = new Attribute(31.5);
        attributes.put("ambient&Noise", noiseAttribut);
        return attributes;
    }

    static public Paginated<EntityType> createEntityTypesRoom() {
        EntityType entityTypeRoom = new EntityType();
        entityTypeRoom.setType("Room");
        entityTypeRoom.setAttrs("temperature", new AttributeType("urn:phenomenum:temperature"));
        entityTypeRoom.setAttrs("humidity", new AttributeType("percentage"));
        entityTypeRoom.setAttrs("pressure", new AttributeType("null"));
        entityTypeRoom.setCount(7);

        return new Paginated<>(Collections.singletonList(entityTypeRoom), 0, 1, 100);
    }

    static public EntityType createEntityTypeRoom() {

        EntityType entityTypeRoom = new EntityType();
        entityTypeRoom.setAttrs("temperature", new AttributeType("urn:phenomenum:temperature"));
        entityTypeRoom.setAttrs("humidity", new AttributeType("percentage"));
        entityTypeRoom.setAttrs("pressure", new AttributeType("null"));
        entityTypeRoom.setCount(7);
        return entityTypeRoom;
    }

    static public Attribute createTemperatureEntityBcnWelt() {

        return new Attribute(21.7);
    }

    static public Attribute createUpdateTemperatureAttributeReference() {

        Attribute temperatureAttribute = new Attribute(25);
        Metadata metadata = new Metadata("CEL");
        temperatureAttribute.addMetadata("unitCode", metadata);
        return temperatureAttribute;
    }

    static public Attribute createUpdateTemperatureAttributeReferenceWithBadSyntax() {

        Attribute temperatureAttribute = new Attribute(25);
        Metadata metadata = new Metadata("CEL");
        temperatureAttribute.addMetadata("unit?Code", metadata);
        return temperatureAttribute;
    }

    static public Object createValueReference() {
        class ValueReference {
            String address = "Ronda de la Comunicacions";
            int zipCode = 28050;
            String city = "Madrid";
            String country = "Spain";

            public ValueReference() {
            }

            public String getAddress() {
                return address;
            }

            public void setAddress(String address) {
                this.address = address;
            }

            public int getZipCode() {
                return zipCode;
            }

            public void setZipCode(int zipCode) {
                this.zipCode = zipCode;
            }

            public String getCity() {
                return city;
            }

            public void setCity(String city) {
                this.city = city;
            }

            public String getCountry() {
                return country;
            }

            public void setCountry(String country) {
                this.country = country;
            }
        }
        return new ValueReference();
    }

    static public List<Registration> createListRegistrationsReference() throws MalformedURLException {

        Registration registration = new Registration("abcdefg", new URL("http://weather.example.com/ngsi"));
        registration.setDuration("PT1M");
        SubjectEntity subjectEntity = new SubjectEntity(Optional.of("Bcn_Welt"));
        subjectEntity.setType(Optional.of("Room"));
        SubjectRegistration subjectRegistration = new SubjectRegistration( Collections.singletonList(subjectEntity), Collections.singletonList("temperature"));
        registration.setSubject(subjectRegistration);
        Metadata metadataService = new Metadata("weather.example.com", "none");
        registration.addMetadata("providingService", metadataService);
        Metadata metadataAuthority = new Metadata("AEMET - Spain", "none");
        registration.addMetadata("providingAuthority", metadataAuthority);
        return Collections.singletonList(registration);
    }

    static public Registration createRegistrationReference() throws MalformedURLException {

        Registration registration = new Registration();
        registration.setCallback(new URL("http://localhost:1234"));
        registration.setDuration("PT1M");
        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setType(Optional.of("Room"));
        SubjectRegistration subjectRegistration = new SubjectRegistration( Collections.singletonList(subjectEntity), Collections.singletonList("humidity"));
        registration.setSubject(subjectRegistration);
        Metadata metadataProvider = new Metadata("example", null);
        registration.addMetadata("provider", metadataProvider);
        return registration;
    }

    static public Registration retrieveRegistrationReference() throws MalformedURLException {

        Registration registration = createRegistrationReference();
        registration.setId("abcde");
        return registration;
    }

    static public Registration createRegistrationReferenceWithBadSyntax() throws MalformedURLException {

        Registration registration = new Registration();
        registration.setCallback(new URL("http://localhost:1234"));
        registration.setDuration("PT1M");
        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setType(Optional.of("Room"));
        SubjectRegistration subjectRegistration = new SubjectRegistration( Collections.singletonList(subjectEntity), Collections.singletonList("humidity"));
        registration.setSubject(subjectRegistration);
        Metadata metadataProvider = new Metadata("example&", null);
        registration.addMetadata("provider", metadataProvider);
        return registration;
    }

    static public Registration updateRegistrationReference() {

        Registration registration = new Registration();
        registration.setDuration("PT1M");
        return registration;
    }

    static public List<Subscription> createListSubscriptionsReference() throws MalformedURLException {

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
        notification.setHeader("X-MyHeader", "foo");
        notification.setQuery("authToken", "bar");
        notification.setAttrsFormat(Optional.of(Notification.Format.keyValues));
        notification.setThrottling(Optional.of(new Long(5)));
        notification.setTimesSent(12);
        notification.setLastNotification(Instant.parse("2015-10-05T16:00:00.10Z"));
        Subscription subscription = new Subscription("abcdefg", subjectSubscription, notification, Instant.parse("2016-04-05T14:00:00.20Z"), Subscription.Status.active);
        return Collections.singletonList(subscription);
    }

    static public Subscription createSubscriptionReference() throws MalformedURLException {
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
        return subscription;
    }

    static public Subscription createSubscriptionReferenceWithBadSyntax() throws MalformedURLException {
        SubjectEntity subjectEntity = new SubjectEntity();
        subjectEntity.setType(Optional.of("Room"));
        Condition condition = new Condition();
        condition.setAttributes(Collections.singletonList("temperature"));
        condition.setExpression("q", "temperature>40");
        SubjectSubscription subjectSubscription = new SubjectSubscription(Collections.singletonList(subjectEntity), condition);
        List<String> attributes = new ArrayList<>();
        attributes.add("temperature");
        attributes.add("humidity#");
        Notification notification = new Notification(attributes, new URL("http://localhost:1234"));
        notification.setThrottling(Optional.of(new Long(5)));
        Subscription subscription = new Subscription();
        subscription.setSubject(subjectSubscription);
        subscription.setNotification(notification);
        subscription.setExpires(Instant.parse("2016-04-05T14:00:00.20Z"));
        return subscription;
    }

    static public Subscription retrieveSubscriptionReference() throws MalformedURLException {

        Subscription subscription = createSubscriptionReference();
        subscription.setId("abcdef");
        subscription.getNotification().setTimesSent(12);
        subscription.getNotification().setLastNotification(Instant.parse("2015-10-05T16:00:00.10Z"));
        subscription.setStatus(Subscription.Status.active);
        return subscription;
    }

    static public Subscription updateSubscriptionReference() {

        Subscription subscription = new Subscription();
        subscription.setExpires(Instant.parse("2016-04-05T14:00:00.20Z"));
        return subscription;
    }

    static public BulkUpdateRequest updateReference() {
        return new BulkUpdateRequest(BulkUpdateRequest.Action.APPEND, createListEntitiesReference());
    }

    static public BulkUpdateRequest updateWrongSyntax() {
        return new BulkUpdateRequest(BulkUpdateRequest.Action.APPEND, createListEntitiesWrongSyntax());
    }

    static public BulkQueryRequest queryReference() {
        List<SubjectEntity> entities = new ArrayList<>();
        SubjectEntity subjectEntity1 = new SubjectEntity();
        subjectEntity1.setIdPattern(Optional.of(".*"));
        subjectEntity1.setType(Optional.of("myFooType"));
        entities.add(subjectEntity1);
        SubjectEntity subjectEntity2 = new SubjectEntity();
        subjectEntity2.setId(Optional.of("myBar"));
        subjectEntity2.setType(Optional.of("myBarType"));
        entities.add(subjectEntity2);
        List<String> attributes = new ArrayList<>();
        attributes.add("temperature");
        attributes.add("humidity");
        List<Scope> scopes = new ArrayList<>();
        Scope scope = new Scope("FIWARE::...", "...");
        scopes.add(scope);
        return new BulkQueryRequest(entities, attributes, scopes);
    }

    static public BulkQueryRequest queryWrongSyntax() {
        BulkQueryRequest bulkQueryRequest = queryReference();
        bulkQueryRequest.getScopes().get(0).setType(bulkQueryRequest.getScopes().get(0).getType() + "?");
        return bulkQueryRequest;
    }

    static public BulkRegisterRequest registerReference() throws MalformedURLException {
        List<Registration> registrations = new ArrayList<>();
        Registration registration1 = new Registration();
        registration1.setCallback(new URL("http://localhost:1234"));
        registration1.setDuration("PT1M");
        SubjectRegistration subjectRegistration1 = new SubjectRegistration();
        SubjectEntity subjectEntity1 = new SubjectEntity();
        subjectEntity1.setType(Optional.of("Room"));
        subjectRegistration1.setEntities(Collections.singletonList(subjectEntity1));
        subjectRegistration1.setAttributes(Collections.singletonList("humidity"));
        registration1.setSubject(subjectRegistration1);
        registrations.add(registration1);
        Registration registration2 = new Registration();
        registration1.setCallback(new URL("http://localhost:5678"));
        registration1.setDuration("PT1M");
        SubjectRegistration subjectRegistration2 = new SubjectRegistration();
        SubjectEntity subjectEntity2 = new SubjectEntity();
        subjectEntity2.setType(Optional.of("Car"));
        subjectRegistration2.setEntities(Collections.singletonList(subjectEntity2));
        subjectRegistration2.setAttributes(Collections.singletonList("speed"));
        registration2.setSubject(subjectRegistration2);
        registrations.add(registration2);
        return new BulkRegisterRequest(BulkRegisterRequest.ActionType.CREATE, registrations);
    }

    static public BulkRegisterRequest registerWrongSyntax() throws MalformedURLException {
        BulkRegisterRequest bulkRegisterRequest = registerReference();
        bulkRegisterRequest.getRegistrations().get(0).getSubject().getEntities().forEach(subjectEntity -> {
            if (subjectEntity.getType().isPresent()) {
                subjectEntity.setType(Optional.of(subjectEntity.getType().get()+"?"));
            }
        });
        return bulkRegisterRequest;
    }

    static public String json(MappingJackson2HttpMessageConverter mapping, Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mapping.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}
