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

package com.orange.ngsi2.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.ngsi2.exception.*;
import com.orange.ngsi2.exception.UnsupportedOperationException;
import com.orange.ngsi2.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Controller for the NGSI v2 requests
 */
@RequestMapping(value = {"/v2"})
public abstract class Ngsi2BaseController {

    private static Logger logger = LoggerFactory.getLogger(Ngsi2BaseController.class);

    /* Field allowed characters are the ones in the plain ASCII set except the following ones: control characters,
       whitespace, &, ?, / and #.
     */
    //private static Pattern fieldPattern = Pattern.compile("[a-zA-Z0-9_-]*");
    private static Pattern fieldPattern = Pattern.compile("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*");

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Endpoint get /v2
     * @return the list of supported operations under /v2 and http status 200 (ok)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/"})
    final public ResponseEntity<Map<String,String>> listResourcesEndpoint() throws Exception {
        return new ResponseEntity<>(listResources(), HttpStatus.OK);
    }

    /**
     * Endpoint get /v2/entities
     * @param id an optional list of entity IDs separated by comma (cannot be used with idPatterns)
     * @param type an optional list of types of entity separated by comma
     * @param idPattern a optional pattern of entity IDs (cannot be used with ids)
     * @param limit an optional limit (0 for none)
     * @param offset an optional offset (0 for none)
     * @param attrs an optional list of attributes separated by comma to return for all entities
     * @param query an optional Simple Query Language query
     * @param georel an optional Geo query. Possible values: near, coveredBy, intersects, equals, disjoint.
     * @param geometry an optional geometry. Possible values: point, line, polygon, box.
     * @param coords an optional coordinate
     * @param orderBy an option list of attributes to difine the order of entities
     * @param options an optional list of options separated by comma. Possible value for option: count.
     *        Theses keyValues,values and unique options are not supported.
     *        If count is present then the total number of entities is returned in the response as a HTTP header named `X-Total-Count`.
     * @return a list of Entities http status 200 (ok)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities"})
    final public ResponseEntity<List<Entity>> listEntitiesEndpoint(@RequestParam Optional<Set<String>> id, @RequestParam Optional<Set<String>> type,
                                                                   @RequestParam Optional<String> idPattern, @RequestParam Optional<Integer> limit,
                                                                   @RequestParam Optional<Integer> offset, @RequestParam Optional<List<String>> attrs,
                                                                   @RequestParam Optional<String> query, @RequestParam Optional<String> georel,
                                                                   @RequestParam Optional<String> geometry, @RequestParam Optional<String> coords,
                                                                   @RequestParam Optional<List<String>> orderBy,
                                                                   @RequestParam Optional<Set<String>> options) throws Exception {

        if (id.isPresent() && idPattern.isPresent()) {
            throw new IncompatibleParameterException("id", "idPattern", "List entities");
        }

        validateSyntax(id.orElse(null), type.orElse(null), attrs.orElse(null));

        Optional<GeoQuery> geoQuery = Optional.empty();
        // If one of them is present, all are mandatory
        if (georel.isPresent() || geometry.isPresent() || coords.isPresent()) {
            if (!(georel.isPresent() && geometry.isPresent() && coords.isPresent())) {
                throw new BadRequestException("Missing one argument of georel, geometry or coords");
            }
            geoQuery = Optional.of(Ngsi2ParsingHelper.parseGeoQuery(georel.get(), geometry.get(), coords.get()));
        }

        boolean count = false;
        if (options.isPresent()) {
            Set<String> optionsSet = options.get();
            //TODO: to support keyValues, values and unique as options
            if (optionsSet.contains("keyValues") || optionsSet.contains("values") || optionsSet.contains("unique")) {
                throw new UnsupportedOptionException("keyValues, values or unique");
            }
            count = optionsSet.contains("count");
        }

        Paginated<Entity> paginatedEntity = listEntities(id.orElse(null), type.orElse(null), idPattern.orElse(null), limit.orElse(0), offset.orElse(0), attrs.orElse(new ArrayList<>()), query.orElse(null), geoQuery.orElse(null), orderBy.orElse(new ArrayList<>()));
        if (count) {
            return new ResponseEntity<>(paginatedEntity.getItems(), xTotalCountHeader(paginatedEntity.getTotal()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(paginatedEntity.getItems(), HttpStatus.OK);
        }
    }

    /**
     * Endpoint post /v2/entities
     * @param entity
     * @param options keyValues is not supported.
     * @return http status 201 (created) and location header /v2/entities/{entityId}
     */
    @RequestMapping(method = RequestMethod.POST, value = "/entities", consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity createEntityEndpoint(@RequestBody Entity entity, @RequestParam Optional<String> options) {

        validateSyntax(entity);
        //TODO: to support keyValues as options
        if (options.isPresent())  {
            throw new UnsupportedOptionException(options.get());
        }
        createEntity(entity);
        return new ResponseEntity(locationHeader(entity.getId()), HttpStatus.CREATED);
    }

    /**
     * Endpoint get /v2/entities/{entityId}
     * @param entityId the entity ID
     * @param type an optional type of entity
     * @param attrs an optional list of attributes to return for the entity
     * @param options an optional list of options separated by comma.
     *        Theses keyValues,values and unique options are not supported.
     * @return the entity and http status 200 (ok) or 409 (conflict)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities/{entityId}"})
    final public ResponseEntity<Entity> retrieveEntityEndpoint(@PathVariable String entityId, @RequestParam Optional<String> type, @RequestParam Optional<List<String>> attrs,
                                                               @RequestParam Optional<String> options) throws Exception {

        validateSyntax(entityId, type.orElse(null), attrs.orElse(null));
        //TODO: to support keyValues, values and unique as options
        if (options.isPresent()) {
            throw new UnsupportedOptionException(options.get());
        }
        return new ResponseEntity<>(retrieveEntity(entityId, type.orElse(null), attrs.orElse(new ArrayList<>())), HttpStatus.OK);
    }

    /**
     * Endpoint post /v2/entities/{entityId}
     * @param entityId the entity ID
     * @param attributes the attributes to update or to append
     * @param type an optional type of entity
     * @param options an optional list of options separated by comma. Possible value for option: append.
     *        keyValues options is not supported.
     *        If append is present then the operation is an append operation
     * @return http status 201 (created)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST,
            value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateOrAppendEntityEndpoint(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes,
                                                             @RequestParam Optional<String> type, @RequestParam Optional<Set<String>> options) throws Exception {
        validateSyntax(entityId, type.orElse(null), attributes);

        boolean append = false;
        if (options.isPresent()) {
            //TODO: to support keyValues as options
            if (options.get().contains("keyValues")) {
                throw new UnsupportedOptionException("keyValues");
            }
            append = options.get().contains("append");
        }
        updateOrAppendEntity(entityId, type.orElse(null), attributes, append);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint patch /v2/entities/{entityId}
     * @param entityId the entity ID
     * @param attributes the attributes to update
     * @param type an optional type of entity
     * @param options keyValues is not supported.
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.PATCH, value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateExistingEntityAttributesEndpoint(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes,
                                                                       @RequestParam Optional<String> type, @RequestParam Optional<String> options) throws Exception {

        validateSyntax(entityId, type.orElse(null), attributes);
        //TODO: to support keyValues as options
        if (options.isPresent())  {
            throw new UnsupportedOptionException(options.get());
        }
        updateExistingEntityAttributes(entityId, type.orElse(null), attributes);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint put /v2/entities/{entityId}
     * @param entityId the entity ID
     * @param attributes the new set of attributes
     * @param type an optional type of entity
     * @param options keyValues is not supported.
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.PUT, value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity replaceAllEntityAttributesEndpoint(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes,
                                                                   @RequestParam Optional<String> type, @RequestParam Optional<String> options) throws Exception {

        validateSyntax(entityId, type.orElse(null), attributes);
        //TODO: to support keyValues as options
        if (options.isPresent())  {
            throw new UnsupportedOptionException(options.get());
        }
        replaceAllEntityAttributes(entityId, type.orElse(null), attributes);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint delete /v2/entities/{entityId}
     * @param entityId the entity ID
     * @param type an optional type of entity
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.DELETE, value = {"/entities/{entityId}"})
    final public ResponseEntity removeEntityEndpoint(@PathVariable String entityId, @RequestParam Optional<String> type) throws Exception {

        validateSyntax(entityId);
        type.ifPresent(this::validateSyntax);
        removeEntity(entityId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint get /v2/entities/{entityId}/attrs/{attrName}
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type of entity
     * @return the attribute and http status 200 (ok) or 409 (conflict)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities/{entityId}/attrs/{attrName}"})
    final public ResponseEntity<Attribute> retrieveAttributeByEntityIdEndpoint(@PathVariable String entityId, @PathVariable String attrName, @RequestParam Optional<String> type) throws Exception {

        validateSyntax(entityId, type.orElse(null), attrName);
        return new ResponseEntity<>(retrieveAttributeByEntityId(entityId, attrName, type.orElse(null)), HttpStatus.OK);
    }

    /**
     * Endpoint put /v2/entities/{entityId}/attrs/{attrName}
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type of entity
     * @return http status 204 (no content) or 409 (conflict)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.PUT,
            value = {"/entities/{entityId}/attrs/{attrName}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateAttributeByEntityIdEndpoint(@PathVariable String entityId, @PathVariable String attrName, @RequestParam Optional<String> type, @RequestBody Attribute attribute) throws Exception {

        validateSyntax(entityId, type.orElse(null), attrName);
        validateSyntax(attribute);
        updateAttributeByEntityId(entityId, attrName, type.orElse(null), attribute);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint delete /v2/entities/{entityId}/attrs/{attrName}
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type of entity
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.DELETE, value = {"/entities/{entityId}/attrs/{attrName}"})
    final public ResponseEntity removeAttributeByEntityIdEndpoint(@PathVariable String entityId, @PathVariable String attrName, @RequestParam Optional<String> type) throws Exception {

        validateSyntax(entityId, type.orElse(null), attrName);
        removeAttributeByEntityId(entityId, attrName, type.orElse(null));
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint get /v2/entities/{entityId}/attrs/{attrName}/value
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type of entity
     * @return the value and http status 200 (ok) or 409 (conflict)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities/{entityId}/attrs/{attrName}/value"}, produces = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity<Object> retrieveAttributeValueEndpoint(@PathVariable String entityId, @PathVariable String attrName, @RequestParam Optional<String> type) throws Exception {

        validateSyntax(entityId, type.orElse(null), attrName);
        Object value = retrieveAttributeValue(entityId, attrName, type.orElse(null));
        if ((value == null) || (value instanceof String) || (value instanceof Number) || (value instanceof Boolean)) {
            throw new NotAcceptableException();
        }
        return new ResponseEntity<>(value, HttpStatus.OK);
    }

    /**
     * Endpoint get /v2/entities/{entityId}/attrs/{attrName}/value
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type of entity
     * @return the value and http status 200 (ok) or 409 (conflict)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities/{entityId}/attrs/{attrName}/value"}, produces = MediaType.TEXT_PLAIN_VALUE)
    final public ResponseEntity<String> retrievePlainTextAttributeValueEndpoint(@PathVariable String entityId, @PathVariable String attrName, @RequestParam Optional<String> type) throws Exception {

        validateSyntax(entityId, type.orElse(null), attrName);
        Object value = retrieveAttributeValue(entityId, attrName, type.orElse(null));
        return new ResponseEntity<>(objectMapper.writeValueAsString(value), HttpStatus.OK);
    }

    /**
     * Endpoint put /v2/entities/{entityId}/attrs/{attrName}/value
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type of entity
     * @return http status 204 (No Content) or 409 (conflict)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.PUT,
            value = {"/entities/{entityId}/attrs/{attrName}/value"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateAttributeValueEndpoint(@PathVariable String entityId, @PathVariable String attrName, @RequestParam Optional<String> type, @RequestBody Object value) throws Exception {

        validateSyntax(entityId, type.orElse(null), attrName);
        updateAttributeValue(entityId, attrName, type.orElse(null), value);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint put /v2/entities/{entityId}/attrs/{attrName}/value
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type of entity
     * @return http status 204 (No Content) or 409 (conflict)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.PUT,
            value = {"/entities/{entityId}/attrs/{attrName}/value"}, consumes = MediaType.TEXT_PLAIN_VALUE)
    final public ResponseEntity updatePlainTextAttributeValueEndpoint(@PathVariable String entityId, @PathVariable String attrName, @RequestParam Optional<String> type, @RequestBody String value) throws Exception {

        validateSyntax(entityId, type.orElse(null), attrName);
        updateAttributeValue(entityId, attrName, type.orElse(null), Ngsi2ParsingHelper.parseTextValue(value));
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint get /v2/types
     * @param limit an optional limit (0 for none)
     * @param offset an optional offset (0 for none)
     * @param options an optional list of options separated by comma. Possible value for option: count.
     *        values option is not supported.
     *        If count is present then the total number of entities is returned in the response as a HTTP header named `X-Total-Count`.
     * @return the entity type json object and http status 200 (ok)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET, value = {"/types"})
    final public ResponseEntity<List<EntityType>> retrieveEntityTypesEndpoint(@RequestParam Optional<Integer> limit,
            @RequestParam Optional<Integer> offset,
            @RequestParam Optional<Set<String>> options) throws Exception {

        boolean count = false;
        if (options.isPresent()) {
            //TODO: to support values as options
            if (options.get().contains("values")) {
                throw new UnsupportedOptionException("values");
            }
            count = options.get().contains("count");
        }
        Paginated<EntityType> entityTypes = retrieveEntityTypes(limit.orElse(0), offset.orElse(0), count);
        if (count) {
            return new ResponseEntity<>(entityTypes.getItems() , xTotalCountHeader(entityTypes.getTotal()), HttpStatus.OK);
        }
        return new ResponseEntity<>(entityTypes.getItems(), HttpStatus.OK);
    }

    /**
     * Endpoint get /v2/types/{entityType}
     * @param entityType the type of entity
     * @return the entity type json object and http status 200 (ok)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET, value = {"/types/{entityType}"})
    final public ResponseEntity<EntityType> retrieveEntityTypeEndpoint(@PathVariable String entityType) throws Exception {

        validateSyntax(entityType);
        return new ResponseEntity<>(retrieveEntityType(entityType), HttpStatus.OK);
    }

    /**
     * Endpoint get /v2/registrations
     * @return a list of Registrations http status 200 (ok)
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/registrations"})
    final public ResponseEntity<List<Registration>> listRegistrationsEndpoint() {

        return new ResponseEntity<>(listRegistrations(), HttpStatus.OK);
    }

    /**
     * Endpoint post /v2/registrations
     * @param registration a registration to create
     * @return http status 201 (created)
     */
    @RequestMapping(method = RequestMethod.POST,
            value = "/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity createRegistrationEndpoint(@RequestBody Registration registration) {

        validateSyntax(registration);
        createRegistration(registration);
        return new ResponseEntity(HttpStatus.CREATED);
    }

    /**
     * Endpoint get /v2/registrations/{registrationId}
     * @param registrationId the registration ID
     * @return the entity and http status 200 (ok)
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/registrations/{registrationId}"})
    final public ResponseEntity<Registration> retrieveRegistrationEndpoint(@PathVariable String registrationId) throws Exception {

        validateSyntax(registrationId);
        return new ResponseEntity<>(retrieveRegistration(registrationId), HttpStatus.OK);
    }

    /**
     * Endpoint patch /v2/registrations/{registrationId}
     * @param registrationId the registration ID
     * @return http status 204 (No Content)
     */
    @RequestMapping(method = RequestMethod.PATCH,
            value = {"/registrations/{registrationId}"})
    final public ResponseEntity updateRegistrationEndpoint(@PathVariable String registrationId, @RequestBody Registration registration) throws Exception {

        validateSyntax(registrationId);
        validateSyntax(registration);
        updateRegistration(registrationId, registration);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint delete /v2/registrations/{registrationId}
     * @param registrationId the registration ID
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.DELETE, value = {"/registrations/{registrationId}"})
    final public ResponseEntity removeRegistrationEndpoint(@PathVariable String registrationId) throws Exception {

        validateSyntax(registrationId);
        removeRegistration(registrationId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint get /v2/subscriptions
     * @param limit an optional limit (0 for none)
     * @param offset an optional offset (0 for none)
     * @param options an optional list of options separated by comma. Possible values for option: count.
     *        If count is present then the total number of entities is returned in the response as a HTTP header named `X-Total-Count`.
     * @return a list of Entities http status 200 (ok)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/subscriptions"})
    final public ResponseEntity<List<Subscription>> listSubscriptionsEndpoint(@RequestParam Optional<Integer> limit, @RequestParam Optional<Integer> offset, @RequestParam Optional<String> options) throws Exception {

        Paginated<Subscription> paginatedSubscription = listSubscriptions(limit.orElse(0), offset.orElse(0));
        List<Subscription> subscriptionList = paginatedSubscription.getItems();
        if (options.isPresent() && (options.get().contains("count"))) {
            return new ResponseEntity<>(subscriptionList , xTotalCountHeader(paginatedSubscription.getTotal()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(subscriptionList, HttpStatus.OK);
        }
    }

    /**
     * Endpoint post /v2/subscriptions
     * @param subscription a subscription to create
     * @return http status 201 (created)
     */
    @RequestMapping(method = RequestMethod.POST, value = "/subscriptions", consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity createSubscriptionEndpoint(@RequestBody Subscription subscription) {

        validateSyntax(subscription);
        createSubscription(subscription);
        return new ResponseEntity(HttpStatus.CREATED);
    }

    /**
     * Endpoint get /v2/subscriptions/{subscriptionId}
     * @param subscriptionId the subscription ID
     * @return the subscription and http status 200 (ok)
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/subscriptions/{subscriptionId}"})
    final public ResponseEntity<Subscription> retrieveSubscriptionEndpoint(@PathVariable String subscriptionId) throws Exception {

        validateSyntax(subscriptionId);
        return new ResponseEntity<>(retrieveSubscription(subscriptionId), HttpStatus.OK);
    }

    /**
     * Endpoint patch /v2/subscriptions/{subscriptionId}
     * @param subscriptionId the subscription ID
     * @return http status 204 (No Content)
     */
    @RequestMapping(method = RequestMethod.PATCH,
            value = {"/subscriptions/{subscriptionId}"})
    final public ResponseEntity updateSubscriptionEndpoint(@PathVariable String subscriptionId, @RequestBody Subscription subscription) throws Exception {

        validateSyntax(subscriptionId);
        validateSyntax(subscription);
        updateSubscription(subscriptionId, subscription);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint delete /v2/subscriptions/{subscriptionId}
     * @param subscriptionId the subscription ID
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.DELETE, value = {"/subscriptions/{subscriptionId}"})
    final public ResponseEntity removeSubscriptionEndpoint(@PathVariable String subscriptionId) throws Exception {

        validateSyntax(subscriptionId);
        removeSubscription(subscriptionId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /*
     * POJ RPC "bulk" Operations
     */

    /**
     * Update, append or delete multiple entities in a single operation
     * @param bulkUpdateRequest a BulkUpdateRequest with an actionType and a list of entities to update
     * @param options an optional list of options separated by comma. keyValues option is not supported.
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST, value = {"/op/update"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity bulkUpdateEndpoint(@RequestBody BulkUpdateRequest bulkUpdateRequest, @RequestParam Optional<String> options) throws Exception {

        bulkUpdateRequest.getEntities().forEach(this::validateSyntax);
        //TODO: to support keyValues as options
        if (options.isPresent())  {
            throw new UnsupportedOptionException(options.get());
        }
        bulkUpdate(bulkUpdateRequest);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Query multiple entities in a single operation
     * @param bulkQueryRequest defines the list of entities, attributes and scopes to match entities
     * @param limit an optional limit
     * @param offset an optional offset
     * @param orderBy an optional list of attributes to order the entities
     * @param options an optional list of options separated by comma. Possible value for option: count.
     *        Theses keyValues,values and unique options are not supported.
     *        If count is present then the total number of entities is returned in the response as a HTTP header named `X-Total-Count`.
     * @return a list of Entities http status 200 (ok)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST, value = {"/op/query"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity<List<Entity>> bulkQueryEndpoint(@RequestBody BulkQueryRequest bulkQueryRequest, @RequestParam Optional<Integer> limit,
                                                  @RequestParam Optional<Integer> offset, @RequestParam Optional<List<String>> orderBy,
                                                  @RequestParam Optional<Set<String>> options) throws Exception {

        validateSyntax(bulkQueryRequest);
        boolean count = false;
        if (options.isPresent()) {
            Set<String> optionsSet = options.get();
            //TODO: to support keyValues, values and unique as options
            if (optionsSet.contains("keyValues") || optionsSet.contains("values") || optionsSet.contains("unique")) {
                throw new UnsupportedOptionException("keyValues, values or unique");
            }
            count = optionsSet.contains("count");
        }
        Paginated<Entity> paginatedEntity = bulkQuery(bulkQueryRequest, limit.orElse(0), offset.orElse(0), orderBy.orElse(new ArrayList<>()), count);
        if (count) {
            return new ResponseEntity<>(paginatedEntity.getItems(), xTotalCountHeader(paginatedEntity.getTotal()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(paginatedEntity.getItems(), HttpStatus.OK);
        }
    }

    /**
     * Create, update or delete registrations to multiple entities in a single operation
     * @param bulkRegisterRequest defines the list of entities to register
     * @return a list of registration ids
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST, value = {"/op/register"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity<List<String>> bulkRegisterEndpoint(@RequestBody BulkRegisterRequest bulkRegisterRequest) throws Exception {

        bulkRegisterRequest.getRegistrations().forEach(this::validateSyntax);
        return new ResponseEntity<>(bulkRegister(bulkRegisterRequest), HttpStatus.OK);
    }

    /**
     * Discover registration matching entities and their attributes
     * @param bulkQueryRequest defines the list of entities, attributes and scopes to match registrations
     * @param offset an optional offset (0 for none)
     * @param limit an optional limit (0 for none)
     * @param options an optional list of options separated by comma. Possible value for option: count.
     *        If count is present then the total number of registrations is returned in the response as a HTTP header named `X-Total-Count`.
     * @return a paginated list of registration
     */
    @RequestMapping(method = RequestMethod.POST, value = {"/op/discover"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity<List<Registration>> bulkDiscoverEndpoint(@RequestBody BulkQueryRequest bulkQueryRequest, @RequestParam Optional<Integer> limit,
                                                                @RequestParam Optional<Integer> offset,
                                                                @RequestParam Optional<Set<String>> options) {

        validateSyntax(bulkQueryRequest);
        boolean count = false;
        if (options.isPresent()) {
            Set<String> optionsSet = options.get();
            count = optionsSet.contains("count");
        }
        Paginated<Registration> paginatedRegistration = bulkDiscover(bulkQueryRequest, limit.orElse(0), offset.orElse(0), count);
        if (count) {
            return new ResponseEntity<>(paginatedRegistration.getItems(), xTotalCountHeader(paginatedRegistration.getTotal()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(paginatedRegistration.getItems(), HttpStatus.OK);
        }
    }

    /*
     * Exception handling
     */

    @ExceptionHandler({UnsupportedOperationException.class})
    public ResponseEntity<Object> unsupportedOperation(UnsupportedOperationException exception, HttpServletRequest request) {
        logger.error("Unsupported operation: {}", exception.getMessage());
        HttpStatus httpStatus = HttpStatus.NOT_IMPLEMENTED;
        if (request.getHeader("Accept").contains(MediaType.TEXT_PLAIN_VALUE)) {
            return new ResponseEntity<>(exception.getError().toString(), httpStatus);
        }
        return new ResponseEntity<>(exception.getError(), httpStatus);
    }

    @ExceptionHandler({UnsupportedOptionException.class})
    public ResponseEntity<Object> unsupportedOption(UnsupportedOptionException exception, HttpServletRequest request) {
        logger.error("Unsupported option: {}", exception.getMessage());
        HttpStatus httpStatus = HttpStatus.NOT_IMPLEMENTED;
        if (request.getHeader("Accept").contains(MediaType.TEXT_PLAIN_VALUE)) {
            return new ResponseEntity<>(exception.getError().toString(), httpStatus);
        }
        return new ResponseEntity<>(exception.getError(), httpStatus);
    }

    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<Object> incompatibleParameter(BadRequestException exception, HttpServletRequest request) {
        logger.error("Bad request: {}", exception.getMessage());
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        if (request.getHeader("Accept").contains(MediaType.TEXT_PLAIN_VALUE)) {
            return new ResponseEntity<>(exception.getError().toString(), httpStatus);
        }
        return new ResponseEntity<>(exception.getError(), httpStatus);
    }

    @ExceptionHandler({IncompatibleParameterException.class})
    public ResponseEntity<Object> incompatibleParameter(IncompatibleParameterException exception, HttpServletRequest request) {
        logger.error("Incompatible parameter: {}", exception.getMessage());
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        if (request.getHeader("Accept").contains(MediaType.TEXT_PLAIN_VALUE)) {
            return new ResponseEntity<>(exception.getError().toString(), httpStatus);
        }
        return new ResponseEntity<>(exception.getError(), httpStatus);
    }

    @ExceptionHandler({InvalidatedSyntaxException.class})
    public ResponseEntity<Object> invalidSyntax(InvalidatedSyntaxException exception, HttpServletRequest request) {
        logger.error("Invalid syntax: {}", exception.getMessage());
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        if (request.getHeader("Accept").contains(MediaType.TEXT_PLAIN_VALUE)) {
            return new ResponseEntity<>(exception.getError().toString(), httpStatus);
        }
        return new ResponseEntity<>(exception.getError(), httpStatus);
    }

    @ExceptionHandler({ConflictingEntitiesException.class})
    public ResponseEntity<Object> conflictingEntities(ConflictingEntitiesException exception, HttpServletRequest request) {
        logger.error("ConflictingEntities: {}", exception.getMessage());
        HttpStatus httpStatus = HttpStatus.CONFLICT;
        if (request.getHeader("Accept").contains(MediaType.TEXT_PLAIN_VALUE)) {
            return new ResponseEntity<>(exception.getError().toString(), httpStatus);
        }
        return new ResponseEntity<>(exception.getError(), httpStatus);
    }

    @ExceptionHandler({NotAcceptableException.class})
    public ResponseEntity<Object> notAcceptable(NotAcceptableException exception, HttpServletRequest request) {
        logger.error("Not Acceptable: {}", exception.getMessage());
        HttpStatus httpStatus = HttpStatus.NOT_ACCEPTABLE;
        if (request.getHeader("Accept").contains(MediaType.TEXT_PLAIN_VALUE)) {
            return new ResponseEntity<>(exception.getError().toString(), httpStatus);
        }
        return new ResponseEntity<>(exception.getError(), httpStatus);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<Object> illegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        logger.error("Illegal Argument: {}", exception.getMessage());
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        if (request.getHeader("Accept").contains(MediaType.TEXT_PLAIN_VALUE)) {
            return new ResponseEntity<>(exception.getMessage(), httpStatus);
        }
        return new ResponseEntity<>(exception.getMessage(), httpStatus);
    }

    /*
     * Methods overridden by child classes to handle the NGSI v2 requests
     */

    /**
     * Retrieve a list of Entities which match different criteria
     * @param ids an optional list of entity IDs (cannot be used with idPatterns) (null for none)
     * @param types an optional list of types of entity (null for none)
     * @param idPattern a optional pattern of entity IDs (cannot be used with ids) (null for none)
     * @param limit an optional limit (0 for none)
     * @param offset an optional offset (0 for none)
     * @param attrs an optional list of attributes to return for all entities (null or empty for none)
     * @param query an optional Simple Query Language query (null for none)
     * @param geoQuery an optional Geo query (null for none)
     * @param orderBy an option list of attributes to define the order of entities (null or empty for none)
     * @return a paginated of list of Entities
     * @throws Exception
     */
    protected Paginated<Entity> listEntities(Set<String> ids, Set<String> types, String idPattern,
                                             int limit, int offset, List<String> attrs,
                                             String query, GeoQuery geoQuery, List<String> orderBy) throws Exception {
         throw new UnsupportedOperationException("List Entities");
    }

    /**
     * Retrieve the list of supported operations under /v2
     * @return the list of supported operations under /v2
     * @throws Exception
     */
    protected Map<String,String> listResources() throws Exception {
        throw new UnsupportedOperationException("Retrieve API Resources");
    }

    /**
     * Create a new entity
     * @param entity the entity to create
     */
    protected void createEntity(Entity entity){
        throw new UnsupportedOperationException("Create Entity");
    }

    /**
     * Retrieve an Entity by the entity ID
     * @param entityId the entity ID
     * @param type an optional type of entity (null for none)
     * @param attrs an optional list of attributes to return for the entity (null or empty for none)
     * @return the Entity
     * @throws ConflictingEntitiesException
     */
    protected Entity retrieveEntity(String entityId, String type, List<String> attrs) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Retrieve Entity");
    }

    /**
     * Update existing or append some attributes to an entity
     * @param entityId the entity ID
     * @param type an optional type of entity (null for none)
     * @param attributes the attributes to update or to append
     * @param append boolean true if the operation is an append operation
     */
    protected void updateOrAppendEntity(String entityId, String type, Map<String, Attribute> attributes, Boolean append){
        throw new UnsupportedOperationException("Update Or Append Entity");
    }

    /**
     * Update existing attributes to an entity. The entity attributes are updated with the ones in the attributes.
     * If one or more attributes in the payload doesn't exist in the entity, an error if returned
     * @param entityId the entity ID
     * @param type an optional type of entity (null for none)
     * @param attributes the attributes to update
     */
    protected void updateExistingEntityAttributes(String entityId, String type, Map<String, Attribute> attributes){
        throw new UnsupportedOperationException("Update Existing Entity Attributes");
    }

    /**
     * Replace all the existing attributes of an entity with a new set of attributes
     * @param entityId the entity ID
     * @param type an optional type of entity (null for none)
     * @param attributes the new set of attributes
     */
    protected void replaceAllEntityAttributes(String entityId, String type, Map<String, Attribute> attributes){
        throw new UnsupportedOperationException("Replace All Entity Attributes");
    }

    /**
     * Delete an entity
     * @param entityId the entity ID
     */
    protected void removeEntity(String entityId){
        throw new UnsupportedOperationException("Remove Entity");
    }

    /**
     * Retrieve a list of entity types
     * @param limit an optional limit (0 for none)
     * @param offset an optional offset (0 for none)
     * @param count whether or not to count the total number of entity types
     * @return the list of entity types
     */
    protected Paginated<EntityType> retrieveEntityTypes(int limit, int offset, boolean count) {
        throw new UnsupportedOperationException("Retrieve Entity Types");
    }

    /**
     * Retrieve an Entity Type by the type with the union set of attribute name and attribute type and with the count
     * of entities belonging to that type
     * @param entityType the type of entity
     * @return the EntityType
     */
    protected EntityType retrieveEntityType(String entityType) {
        throw new UnsupportedOperationException("Retrieve Entity Type");
    }

    /**
     * Retrieve an Attribute by the entity ID
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type to avoid ambiguity in the case there are several entities with the same entity id
     *             null for none
     * @return the Attribute
     * @throws ConflictingEntitiesException
     */
    protected Attribute retrieveAttributeByEntityId(String entityId, String attrName, String type) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Retrieve Attribute by Entity ID");
    }

    /**
     * Update an Attribute by the entity ID
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type to avoid ambiguity in the case there are several entities with the same entity id
     *             null for none
     * @param attribute the new attributes data
     * @throws ConflictingEntitiesException
     */
    protected void updateAttributeByEntityId(String entityId, String attrName, String type, Attribute attribute) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Update Attribute by Entity ID");
    }

    /**
     * Delete an attribute
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type to avoid ambiguity in the case there are several entities with the same entity id
     *             null for none
     * @throws ConflictingEntitiesException
     */
    protected void removeAttributeByEntityId(String entityId, String attrName, String type) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Remove Attribute");
    }

    /**
     * Delete an attribute
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type to avoid ambiguity in the case there are several entities with the same entity id
     *             null for none
     * @return value
     */
    protected Object retrieveAttributeValue(String entityId, String attrName, String type) {
        throw new UnsupportedOperationException("Retrieve Attribute Value");
    }

    /**
     * Update an Attribute Value
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type to avoid ambiguity in the case there are several entities with the same entity id.
     *             null for none
     * @param value the new value
     * @throws ConflictingEntitiesException
     */
    protected void updateAttributeValue(String entityId, String attrName, String type, Object value) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Update Attribute Value");
    }

    /**
     * Retrieve the list of all Registrations presents in the system
     * @return list of Registrations
     */
    protected List<Registration> listRegistrations() {
        throw new UnsupportedOperationException("Retrieve Registrations");
    }

    /**
     * Create a new registration
     * @param registration the registration to create
     */
    protected void createRegistration(Registration registration){
        throw new UnsupportedOperationException("Create Registration");
    }

    /**
     * Retrieve a Registration by the registration ID
     * @param registrationId the registration ID
     * @return the registration
     */
    protected Registration retrieveRegistration(String registrationId) {
        throw new UnsupportedOperationException("Retrieve Registration");
    }

    /**
     * Update some fields to a registration
     * @param registrationId the registration ID
     * @param registration the some fields of the registration to update
     */
    protected void updateRegistration(String registrationId, Registration registration){
        throw new UnsupportedOperationException("Update Registration");
    }

    /**
     * Delete a registration
     * @param registrationId the registration ID
     */
    protected void removeRegistration(String registrationId){
        throw new UnsupportedOperationException("Remove Registration");
    }

    /**
     * Retrieve the list of all Subscriptions present in the system
     * @param limit an optional limit (0 for none)
     * @param offset an optional offset (0 for none)
     * @return a paginated of list of Subscriptions
     * @throws Exception
     */
    protected Paginated<Subscription> listSubscriptions( int limit, int offset) throws Exception {
        throw new UnsupportedOperationException("List Subscriptions");
    }

    /**
     * Create a new subscription
     * @param subscription the subscription to create
     */
    protected void createSubscription(Subscription subscription){
        throw new UnsupportedOperationException("Create Subscription");
    }

    /**
     * Retrieve a subscription by the subscription ID
     * @param subscriptionId the registration ID
     * @return the registration
     */
    protected Subscription retrieveSubscription(String subscriptionId) {
        throw new UnsupportedOperationException("Retrieve Subscription");
    }

    /**
     * Update some fields to a subscription
     * @param subscriptionId the subscription ID
     * @param subscription the some fields of the subscription to update
     */
    protected void updateSubscription(String subscriptionId, Subscription subscription){
        throw new UnsupportedOperationException("Update Subscription");
    }

    /**
     * Delete a subscription
     * @param subscriptionId the subscription ID
     */
    protected void removeSubscription(String subscriptionId){
        throw new UnsupportedOperationException("Remove Subscription");
    }

    /**
     * Update, append or delete multiple entities in a single operation
     * @param bulkUpdateRequest a BulkUpdateRequest with an actionType and a list of entities to update
     */
    protected void bulkUpdate(BulkUpdateRequest bulkUpdateRequest){
        throw new UnsupportedOperationException("Update");
    }

    /**
     * Query multiple entities in a single operation
     * @param bulkQueryRequest an optional list of entity IDs (cannot be used with idPatterns)
     * @param limit an optional limit (0 for none)
     * @param offset an optional offset (0 for none)
     * @param orderBy an option list of attributes to define the order of entities (empty for none)
     * @param count is true if the count is required
     * @return a paginated of list of Entities
     */
    protected Paginated<Entity> bulkQuery(BulkQueryRequest bulkQueryRequest, int limit, int offset, List<String> orderBy, Boolean count){
        throw new UnsupportedOperationException("Query");
    }

    /**
     * Create, update or delete registrations to multiple entities in a single operation
     * @param bulkRegisterRequest defines the list of entities to register
     * @return a list of registration ids
     */
    protected List<String> bulkRegister(BulkRegisterRequest bulkRegisterRequest) {
        throw new UnsupportedOperationException("Register");
    }

    /**
     * Discover registration matching entities and their attributes
     * @param bulkQueryRequest defines the list of entities, attributes and scopes to match registrations
     * @param offset an optional offset (0 for none)
     * @param limit an optional limit (0 for none)
     * @param count is true if the count is required
     * @return a paginated list of registration
     */
    protected Paginated<Registration> bulkDiscover(BulkQueryRequest bulkQueryRequest, int limit, int offset, Boolean count) {
        throw new UnsupportedOperationException("Discover");
    }

    /*
     * Private Methods 
     */

    private void validateSyntax(String field) throws InvalidatedSyntaxException {
        if (( field.length() > 256) || (!fieldPattern.matcher(field).matches())) {
            throw new InvalidatedSyntaxException(field);
        }
    }

    private void validateSyntax(Collection<String> strings) {
        if (strings != null) {
            strings.forEach(this::validateSyntax);
        }
    }

    private void validateSyntax(Set<String> ids, Set<String> types, List<String> attrs) {
        validateSyntax(ids);
        validateSyntax(types);
        validateSyntax(attrs);
    }

    private void validateSyntax(String id, String type, List<String> attrs) {
        if (id != null) validateSyntax(id);
        if (type != null) validateSyntax(type);
        validateSyntax(attrs);
    }

    private void validateSyntax(String id, String type, String attributeName) {
        if (id != null) validateSyntax(id);
        if (type != null) validateSyntax(type);
        if (attributeName != null) validateSyntax(attributeName);
    }

    private void validateSyntax(Entity entity) {
        if (entity.getId() != null) {
            validateSyntax(entity.getId());
        }
        if (entity.getType() != null ) {
            validateSyntax(entity.getType());
        }
        if (entity.getAttributes() != null) {
            validateSyntax(entity.getAttributes());
        }
    }

    private void validateSyntax(Attribute attribute) {
        //check attribute type
        if (attribute.getType() != null) {
            attribute.getType().ifPresent(this::validateSyntax);
        }
        Map<String, Metadata> metadatas = attribute.getMetadata();
        if (metadatas != null) {
            //check metadata name
            metadatas.keySet().forEach(this::validateSyntax);
            //check metadata type
            metadatas.values().forEach(metadata -> {
                if (metadata.getType() != null) {
                    validateSyntax(metadata.getType());
                }
            });
        }
    }

    private void validateSyntax(Map<String, Attribute> attributes) {
        //check attribute name
        attributes.keySet().forEach(this::validateSyntax);
        attributes.values().forEach(this::validateSyntax);
    }

    private void validateSyntax(String entityId, String type, Map<String, Attribute> attributes) {
        if (entityId != null) {
            validateSyntax(entityId);
        }
        if (type != null) {
            validateSyntax(type);
        }
        if (attributes != null) {
            validateSyntax(attributes);
        }
    }

    private void validateSyntax(List<SubjectEntity> subjectEntities) {
        subjectEntities.forEach(subjectEntity -> {
            if (subjectEntity.getId() != null) {
                subjectEntity.getId().ifPresent(this::validateSyntax);
            }
            if (subjectEntity.getType()!= null) {
                subjectEntity.getType().ifPresent(this::validateSyntax);
            }
        });
    }

    private void validateSyntax(Registration registration) {
        if (registration.getSubject() != null) {
            if (registration.getSubject().getEntities() != null) {
                validateSyntax(registration.getSubject().getEntities());
            }
            if (registration.getSubject().getAttributes() != null) {
                registration.getSubject().getAttributes().forEach(this::validateSyntax);
            }
        }
        Map<String, Metadata> metadatas = registration.getMetadata();
        if (metadatas != null) {
            //check metadata name
            metadatas.keySet().forEach(this::validateSyntax);
            //check metadata type
            metadatas.values().forEach(metadata -> {
                if (metadata.getType() != null) {
                    validateSyntax(metadata.getType());
                }
            });
        }
    }

    private void validateSyntax(Subscription subscription) {
        if (subscription.getSubject() != null) {
            if (subscription.getSubject().getEntities() != null) {
                validateSyntax(subscription.getSubject().getEntities());
            }
            if ((subscription.getSubject().getCondition() != null) && (subscription.getSubject().getCondition().getAttributes() != null)) {
                subscription.getSubject().getCondition().getAttributes().forEach(this::validateSyntax);
            }
        }
        if ((subscription.getNotification() != null) && (subscription.getNotification().getAttributes() != null)) {
            subscription.getNotification().getAttributes().forEach(this::validateSyntax);
        }
    }

    private void validateSyntax(BulkQueryRequest bulkQueryRequest) {
        validateSyntax(bulkQueryRequest.getEntities());
        validateSyntax(bulkQueryRequest.getAttributes());
        bulkQueryRequest.getScopes().forEach(scope -> {
            if (scope.getType() != null) {
                validateSyntax(scope.getType());
            }
        });
    }

    private HttpHeaders locationHeader(String entityId) {
        HttpHeaders headers = new HttpHeaders();
        headers.put("Location", Collections.singletonList("/v2/entities/" + entityId));
        return headers;
    }

    private HttpHeaders xTotalCountHeader(int countNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.put("X-Total-Count", Collections.singletonList(Integer.toString(countNumber)));
        return headers;
    }
}
