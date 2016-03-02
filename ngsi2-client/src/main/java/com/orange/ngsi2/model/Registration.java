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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Registration model
 */
public class Registration {

    String id;

    SubjectRegistration subject;

    URL callback;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Metadata> metadata;

    String duration;

    public Registration() {
    }

    public Registration(String id, URL callback) {
        this.id = id;
        this.callback = callback;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SubjectRegistration getSubject() {
        return subject;
    }

    public void setSubject(SubjectRegistration subject) {
        this.subject = subject;
    }

    public URL getCallback() {
        return callback;
    }

    public void setCallback(URL callback) {
        this.callback = callback;
    }

    public Map<String, Metadata> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Metadata> metadata) {
        this.metadata = metadata;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    @JsonIgnore
    public void addMetadata(String key, Metadata metadata) {
        if (this.metadata == null) {
            this.metadata = new HashMap<String, Metadata>();
        }
        this.metadata.put(key, metadata);
    }
}
