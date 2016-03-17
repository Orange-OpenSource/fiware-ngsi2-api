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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Notification model
 */
public class Notification {

    /**
     * Format model
     */
    public enum Format {
        normalized, keyValues, values;
    }

    List<String> attributes;

    URL callback;

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<Map<String, String>> headers;

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<Map<String, String>> query;

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<Format> attrsFormat;

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<Long> throttling;

    long timesSent;

    Instant lastNotification;

    public Notification() {
    }

    public Notification(List<String> attributes, URL callback) {
        this.attributes = attributes;
        this.callback = callback;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public URL getCallback() {
        return callback;
    }

    public void setCallback(URL callback) {
        this.callback = callback;
    }

    public Optional<Long> getThrottling() {
        return throttling;
    }

    public void setThrottling(Optional<Long> throttling) {
        this.throttling = throttling;
    }

    public long getTimesSent() {
        return timesSent;
    }

    public void setTimesSent(long timesSent) {
        this.timesSent = timesSent;
    }

    public Instant getLastNotification() {
        return lastNotification;
    }

    public void setLastNotification(Instant lastNotification) {
        this.lastNotification = lastNotification;
    }

    public Optional<Map<String, String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Optional<Map<String, String>> headers) {
        this.headers = headers;
    }

    @JsonIgnore
    public void setHeader(String key, String value) {
        if (this.headers == null) {
            this.headers = Optional.of(new HashMap<>());
        }
        this.headers.get().put(key, value);
    }

    public Optional<Map<String, String>> getQuery() {
        return query;
    }

    public void setQuery(Optional<Map<String, String>> query) {
        this.query = query;
    }

    @JsonIgnore
    public void setQuery(String key, String value) {
        if (this.query == null) {
            this.query = Optional.of(new HashMap<>());
        }
        this.query.get().put(key, value);
    }

    public Optional<Format> getAttrsFormat() {
        return attrsFormat;
    }

    public void setAttrsFormat(Optional<Format> attrsFormat) {
        this.attrsFormat = attrsFormat;
    }
}
