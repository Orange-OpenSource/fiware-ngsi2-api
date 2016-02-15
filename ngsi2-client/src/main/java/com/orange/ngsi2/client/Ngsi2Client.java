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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.orange.ngsi2.model.Entity;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureAdapter;
import org.springframework.web.client.AsyncRestTemplate;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * NGSIv2 API Client
 */
public class Ngsi2Client {

    /**
     *  When used, the total number of entities is returned in the response as a HTTP header named `X-Total-Count`.
     */
    public final static String OPTION_COUNT = "count";

    /**
     *  When used, the response payload uses `keyValues` simplified entity representation.
     */
    public final static String OPTION_KEYVALUES = "keyValues";

    /**
     * When used, the response payload uses `values` simplified entity representation.
     */
    public final static String OPTION_VALUES = "values";

    public class Ngsi2Exception extends Exception {

        private String error;
        private String description;
        private Collection<String> affectedItems;

    }

    private static final String basePath = "v2";
    private static final String entitiesPath = basePath + "/entities";
    private static final String typesPath = basePath + "/types";
    private static final String registrationsPath = basePath + "/subscriptions";
    private static final String baseSubscriptions = basePath + "/subscriptions";
    private static final String attributesPath = "/attrs/";
    private static final String valuePath = "/value";

    private AsyncRestTemplate asyncRestTemplate;

    private HttpHeaders defaultHttpHeaders;

    private String baseURL;

    private Ngsi2Client() {
        // set default headers for Content-Type and Accept to application/JSON
        defaultHttpHeaders = new HttpHeaders();
        defaultHttpHeaders.setContentType(MediaType.APPLICATION_JSON);
        defaultHttpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    /**
     * Default constructor
     * @param asyncRestTemplate AsyncRestTemplate to handle requests
     * @param baseURL base URL for the NGSIv2 service
     */
    public Ngsi2Client(AsyncRestTemplate asyncRestTemplate, String baseURL) {
        this();
        this.asyncRestTemplate = asyncRestTemplate;
        this.baseURL = baseURL.endsWith("/") ? baseURL : baseURL + "/";

        injectJava8ObjectMapper();
    }

    public ListenableFuture<Map<String, String>> getV2() {

        ListenableFuture<ResponseEntity<JsonNode>> responseFuture = request(HttpMethod.GET, baseURL + basePath, null, null,JsonNode.class, Collections.emptyMap());
        return new ListenableFutureAdapter<Map<String, String>, ResponseEntity<JsonNode>>(responseFuture) {
            @Override
            protected Map<String, String> adapt(ResponseEntity<JsonNode> result) throws ExecutionException {
                if (result.getStatusCode() == HttpStatus.OK) {
                    Map<String, String> services = new HashMap<>();
                    result.getBody().fields().forEachRemaining(entry ->
                        services.put(entry.getKey(), entry.getValue().textValue()));
                    return services;
                }
                throw new RuntimeException("Error");
            }
        };
    }

    /**
     * Retrieve a list of Entities
     * @param ids
     * @param idPatterns
     * @param types
     * @param attrs
     * @param query
     * @param georel
     * @param geometry
     * @param coords
     * @param options
     * @param offset
     * @param limit
     * @return
     */
    public ListenableFuture<Entity[]> getEntities(Collection<String> ids, Collection<String> idPatterns,
            Collection<String> types, Collection<String> attrs,
            String query, String georel, String geometry, String coords,
            Collection<String> options, int offset, int limit) {
        Map<String, Object> params = new HashMap<>();
        addPaginationParams(params, offset, limit);
        addParam(params, "id", ids);
        addParam(params, "idPattern", idPatterns);
        addParam(params, "type", types);
        addParam(params, "attrs", attrs);
        addParam(params, "query", query);
        addParam(params, "georel", georel);
        addParam(params, "geometry", geometry);
        addParam(params, "coords", coords);
        addParam(params, "options", options);

        return expect(HttpStatus.OK, request(HttpMethod.GET, baseURL + entitiesPath, null, null, Entity[].class, params));
    }

    /**
     * Create a new Entity
     * @param entity
     * @return
     */
    public ListenableFuture<Void> addEntity(Entity entity) {
        return expect(HttpStatus.NO_CONTENT, request(HttpMethod.POST, baseURL + entitiesPath, null, entity, Void.class, Collections.emptyMap()));
    }

    /**
     * Default headers
     * @return the default headers
     */
    public HttpHeaders getDefaultHttpHeaders() {
        return defaultHttpHeaders;
    }

    /**
     * Make a simplified HTTP request
     */
    protected <T,U> ListenableFuture<T> request(HttpMethod method, String url, U body, Class<T> responseType) {
        return expect(HttpStatus.OK, request(method, url, null, body, responseType, Collections.emptyMap()));
    }

    /**
     * Make an HTTP request
     */
    protected <T,U> ListenableFuture<ResponseEntity<T>> request(HttpMethod method, String url, HttpHeaders httpHeaders, U body, Class<T> responseType, Map<String, ?> uriVariables) {
        if (httpHeaders == null) {
            httpHeaders = getDefaultHttpHeaders();
        }
        HttpEntity<U> requestEntity = new HttpEntity<>(body, httpHeaders);

        return asyncRestTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
    }

    private <T> ListenableFuture<T> expect(HttpStatus status, ListenableFuture<ResponseEntity<T>> responseEntityListenableFuture) {
        return new ListenableFutureAdapter<T, ResponseEntity<T>>(responseEntityListenableFuture) {
            @Override
            protected T adapt(ResponseEntity<T> result) throws ExecutionException {
                if (result.getStatusCode().equals(status)) {
                    return result.getBody();
                }
                throw new RuntimeException("Error");
            }
        };
    }

    private void addPaginationParams(Map<String, Object> params, int offset, int limit) {
        if (offset > 0) {
            params.put("offset", offset);
        }
        if (limit > 0) {
            params.put("limit", limit);
        }
    }

    private void addParam(Map<String, Object> params, String key, String value) {
        if (!nullOrEmpty(value)) {
            params.put(key, value);
        }
    }

    private void addParam(Map<String, Object> params, String key, Collection<? extends CharSequence> value) {
        if (!nullOrEmpty(value)) {
            params.put(key, String.join(",", value));
        }
    }

    private boolean nullOrEmpty(Collection i) {
        return i == null || i.isEmpty();
    }

    private boolean nullOrEmpty(String i) {
        return i == null || i.isEmpty();
    }

    /**
     * Inject an ObjectMapper supporting Java8 by default
     */
    private void injectJava8ObjectMapper() {

        for(HttpMessageConverter httpMessageConverter : asyncRestTemplate.getMessageConverters()) {
            if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
                MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter)httpMessageConverter;
                converter.getObjectMapper().registerModule(new Jdk8Module());
                //asyncRestTemplate.getMessageConverters().remove(httpMessageConverter);
                //break;
            }
        }

        //asyncRestTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter(new ObjectMapper().registerModule(new Jdk8Module())));
    }
}
