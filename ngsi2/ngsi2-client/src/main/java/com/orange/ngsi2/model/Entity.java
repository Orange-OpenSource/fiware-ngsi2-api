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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity Model
 */
public class Entity {

    private String id;

    private String type;

    private Map<String, Attribute> attributes;

    public Entity() {
    }

    public Entity(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public Entity(String id, String type, Map<String, Attribute> attributes) {
        this(id, type);
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonAnyGetter
    public Map<String, Attribute> getAttributes() {
        return attributes;
    }

    @JsonAnySetter
    public void setAttributes(String key, Attribute attribute) {
        if (attributes == null) {
            attributes = new HashMap<String, Attribute>();
        }
        attributes.put(key, attribute);
    }

    @JsonIgnore
    public void setAttributes(Map<String, Attribute> attributes) {
        this.attributes = attributes;
    }

}
