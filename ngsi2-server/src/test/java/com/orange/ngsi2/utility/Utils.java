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

import com.orange.ngsi2.model.Attribute;
import com.orange.ngsi2.model.Entity;
import com.orange.ngsi2.model.Metadata;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

    static public Entity createEntityBcnWelt() {

        Entity EntityBcnWelt = new Entity("Bcn-Welt", "Room");
        EntityBcnWelt.setAttributes("temperature", new Attribute(21.7));
        EntityBcnWelt.setAttributes("humidity", new Attribute(60));
        return EntityBcnWelt;
    }

    static public String json(MappingJackson2HttpMessageConverter mapping, Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mapping.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}
