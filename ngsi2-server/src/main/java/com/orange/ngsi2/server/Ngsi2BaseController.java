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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.ngsi2.exception.*;
import com.orange.ngsi2.exception.UnsupportedOperationException;
import com.orange.ngsi2.model.*;
import com.orange.ngsi2.model.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Controller for the NGSI v2 requests
 */
@RequestMapping(value = {"/v2"})
public abstract class Ngsi2BaseController {

    private static Logger logger = LoggerFactory.getLogger(Ngsi2BaseController.class);

    private static Pattern fieldPattern = Pattern.compile("[a-zA-Z0-9_-]*");

    @Autowired
    ObjectMapper objectMapper;

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
     * @param options an optional list of options separated by comma. Possible values for option: count,keyValues,values.
     *        If count is present then the total number of entities is returned in the response as a HTTP header named `X-Total-Count`.
     * @return a list of Entities http status 200 (ok)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities"})
    final public ResponseEntity<List<Entity>> listEntitiesEndpoint(@RequestParam Optional<String> id, @RequestParam Optional<String> type, @RequestParam Optional<String> idPattern, @RequestParam Optional<Integer> limit, @RequestParam Optional<Integer> offset, @RequestParam Optional<String> attrs, @RequestParam Optional<String> options) throws Exception {

        if (id.isPresent() && idPattern.isPresent()) {
            throw new IncompatibleParameterException(id.get(), idPattern.get(), "List entities");
        }
        validateSyntax(id, type, attrs);
        Paginated<Entity> paginatedEntity = listEntities(id, type, idPattern, limit, offset, attrs);
        List<Entity> entityList = paginatedEntity.getItems();
        if (options.isPresent() && (options.get().contains("count"))) {
            return new ResponseEntity<>(entityList , xTotalCountHeader(paginatedEntity.getTotal()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(entityList, HttpStatus.OK);
        }
    }

    /**
     * Endpoint post /v2/entities
     * @param entity
     * @return http status 201 (created) and location header /v2/entities/{entityId}
     */
    @RequestMapping(method = RequestMethod.POST, value = "/entities", consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity createEntityEndpoint(@RequestBody Entity entity) {

        validateSyntax(entity);
        createEntity(entity);
        return new ResponseEntity(locationHeader(entity.getId()), HttpStatus.CREATED);
    }

    /**
     * Endpoint get /v2/entities/{entityId}
     * @param entityId the entity ID
     * @param attrs an optional list of attributes to return for the entity
     * @return the entity and http status 200 (ok) or 409 (conflict)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities/{entityId}"})
    final public ResponseEntity<Entity> retrieveEntityEndpoint(@PathVariable String entityId, @RequestParam Optional<String> attrs) throws Exception {

        validateSyntax(Optional.of(entityId), Optional.empty(), attrs);
        return new ResponseEntity<>(retrieveEntity(entityId, attrs), HttpStatus.OK);
    }

    /**
     * Endpoint post /v2/entities/{entityId}
     * @param entityId the entity ID
     * @param attributes the attributes to update or to append
     * @return http status 201 (created)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.POST,
            value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateOrAppendEntityEndpoint(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes) throws Exception {

        validateSyntax(entityId, attributes);
        updateOrAppendEntity(entityId, attributes);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint patch /v2/entities/{entityId}
     * @param entityId the entity ID
     * @param attributes the attributes to update
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.PATCH, value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateExistingEntityAttributesEndpoint(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes) throws Exception {

        validateSyntax(entityId, attributes);
        updateExistingEntityAttributes(entityId, attributes);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint put /v2/entities/{entityId}
     * @param entityId the entity ID
     * @param attributes the new set of attributes
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.PUT, value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity replaceAllEntityAttributesEndpoint(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes) throws Exception {

        validateSyntax(entityId, attributes);
        replaceAllEntityAttributes(entityId, attributes);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint delete /v2/entities/{entityId}
     * @param entityId the entity ID
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.DELETE, value = {"/entities/{entityId}"})
    final public ResponseEntity removeEntityEndpoint(@PathVariable String entityId) throws Exception {

        validateSyntax(entityId);
        removeEntity(entityId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint get /v2/types/{entityType}
     * @param entityType the type of entity
     * @return the entity type json object and http status 200 (ok)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.GET,
            value = {"/types/{entityType}"})
    final public ResponseEntity<EntityType> retrieveEntityTypeEndpoint(@PathVariable String entityType) throws Exception {

        validateSyntax(entityType);
        return new ResponseEntity<>(retrieveEntityType(entityType), HttpStatus.OK);
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

        validateSyntax(Optional.of(entityId), type, Optional.of(attrName));
        return new ResponseEntity<>(retrieveAttributeByEntityId(entityId, attrName, type), HttpStatus.OK);
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

        validateSyntax(Optional.of(entityId), type, Optional.of(attrName));
        validateSyntax(attribute);
        updateAttributeByEntityId(entityId, attrName, type, attribute);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    /**
     * Endpoint delete /v2/entities/{entityId}/attrs/{attrName}
     * @param entityId the entity ID
     * @return http status 204 (no content)
     * @throws Exception
     */
    @RequestMapping(method = RequestMethod.DELETE, value = {"/entities/{entityId}/attrs/{attrName}"})
    final public ResponseEntity removeAttributeByEntityIdEndpoint(@PathVariable String entityId, @PathVariable String attrName, @RequestParam Optional<String> type) throws Exception {

        validateSyntax(Optional.of(entityId), type, Optional.of(attrName));
        removeAttributeByEntityId(entityId, attrName, type);
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

        validateSyntax(Optional.of(entityId), type, Optional.of(attrName));
        Object value = retrieveAttributeValue(entityId, attrName, type);
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

        validateSyntax(Optional.of(entityId), type, Optional.of(attrName));
        Object value = retrieveAttributeValue(entityId, attrName, type);
        return new ResponseEntity<>(valueToString(value), HttpStatus.OK);
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

        validateSyntax(Optional.of(entityId), type, Optional.of(attrName));
        updateAttributeValue(entityId, attrName, type, value);
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

        validateSyntax(Optional.of(entityId), type, Optional.of(attrName));
        updateAttributeValue(entityId, attrName, type, stringToValue(value));
        return new ResponseEntity(HttpStatus.NO_CONTENT);
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
     * @param registration
     * @return http status 201 (created)
     */
    @RequestMapping(method = RequestMethod.POST, value = "/registrations", consumes = MediaType.APPLICATION_JSON_VALUE)
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

    /*
     * Methods overridden by child classes to handle the NGSI v2 requests
     */

    /**
     * Retrieve a list of Entities which match different criteria
     * @param ids an optional list of entity IDs (cannot be used with idPatterns)
     * @param types an optional list of types of entity
     * @param idPattern a optional pattern of entity IDs (cannot be used with ids)
     * @param limit an optional limit (0 for none)
     * @param offset an optional offset (0 for none)
     * @param attrs an optional list of attributes to return for all entities
     * @return a paginated of list of Entities
     * @throws Exception
     */
    protected Paginated<Entity> listEntities(Optional<String> ids, Optional<String> types, Optional<String> idPattern, Optional<Integer> limit, Optional<Integer> offset, Optional<String> attrs) throws Exception {
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
     * @param attrs an optional list of attributes to return for the entity
     * @return the Entity
     * @throws ConflictingEntitiesException
     */
    protected Entity retrieveEntity(String entityId, Optional<String> attrs) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Retrieve Entity");
    }

    /**
     * Update existing or append some attributes to an entity
     * @param entityId the entity ID
     * @param attributes the attributes to update or to append
     */
    protected void updateOrAppendEntity(String entityId, Map<String, Attribute> attributes){
        throw new UnsupportedOperationException("Update Or Append Entity");
    }

    /**
     * Update existing attributes to an entity. The entity attributes are updated with the ones in the attributes.
     * If one or more attributes in the payload doesn't exist in the entity, an error if returned
     * @param entityId the entity ID
     * @param attributes the attributes to update
     */
    protected void updateExistingEntityAttributes(String entityId, Map<String, Attribute> attributes){
        throw new UnsupportedOperationException("Update Existing Entity Attributes");
    }

    /**
     * Replace all the existing attributes of an entity with a new set of attributes
     * @param entityId the entity ID
     * @param attributes the new set of attributes
     */
    protected void replaceAllEntityAttributes(String entityId, Map<String, Attribute> attributes){
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
     * @return the Attribute
     * @throws ConflictingEntitiesException
     */
    protected Attribute retrieveAttributeByEntityId(String entityId, String attrName, Optional<String> type) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Retrieve Attribute by Entity ID");
    }

    /**
     * Update an Attribute by the entity ID
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type to avoid ambiguity in the case there are several entities with the same entity id
     * @param attribute the new attributes data
     * @throws ConflictingEntitiesException
     */
    protected void updateAttributeByEntityId(String entityId, String attrName, Optional<String> type, Attribute attribute) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Update Attribute by Entity ID");
    }

    /**
     * Delete an attribute
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type to avoid ambiguity in the case there are several entities with the same entity id
     * @throws ConflictingEntitiesException
     */
    protected void removeAttributeByEntityId(String entityId, String attrName, Optional<String> type) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Remove Attribute");
    }

    /**
     * Delete an attribute
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type to avoid ambiguity in the case there are several entities with the same entity id
     */
    protected Object retrieveAttributeValue(String entityId, String attrName, Optional<String> type) {
        throw new UnsupportedOperationException("Retrieve Attribute Value");
    }

    /**
     * Update an Attribute Value
     * @param entityId the entity ID
     * @param attrName the attribute name
     * @param type an optional type to avoid ambiguity in the case there are several entities with the same entity id
     * @param value the new value
     * @throws ConflictingEntitiesException
     */
    protected void updateAttributeValue(String entityId, String attrName, Optional<String> type, Object value) throws ConflictingEntitiesException {
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

    /*
     * Private Methods 
     */

    private void validateSyntax(String field) throws InvalidatedSyntaxException {
        if (( field.length() > 256) || (!fieldPattern.matcher(field).matches())) {
            throw new InvalidatedSyntaxException(field);
        }
    }

    private void validateSyntax(String[] stringTab) throws InvalidatedSyntaxException {
        for (int i = 0; i < stringTab.length; i++) {
            validateSyntax(stringTab[i]);
        }
    }

    private void validateSyntax(Optional<String> ids, Optional<String> types, Optional<String> attrs) {
        if (ids.isPresent()) {
            validateSyntax(ids.get().split(","));
        }
        if (types.isPresent()) {
            validateSyntax(types.get().split(","));
        }
        if (attrs.isPresent()) {
            validateSyntax(attrs.get().split(","));
        }
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
            validateSyntax(attribute.getType().get());
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

    private void validateSyntax(String entityId, Map<String, Attribute> attributes) {
        validateSyntax(entityId);
        if (attributes != null) {
            validateSyntax(attributes);
        }
    }

    private void validateSyntax(Registration registration) {
        registration.getSubject().getEntities().forEach(subjectEntity -> {
            if (subjectEntity.getId() != null) {
                validateSyntax(subjectEntity.getId().get());
            }
            if (subjectEntity.getType() != null) {
                validateSyntax(subjectEntity.getType().get());
            }
        });
        registration.getSubject().getAttributes().forEach(this::validateSyntax);
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

    private HttpHeaders locationHeader(String entityId) {
        StringBuilder location = new StringBuilder("/v2/entities/");
        location.append(entityId);
        HttpHeaders headers = new HttpHeaders();
        headers.put("Location", Collections.singletonList(location.toString()));
        return headers;
    }

    private HttpHeaders xTotalCountHeader(int countNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.put("X-Total-Count", Collections.singletonList(Integer.toString(countNumber)));
        return headers;
    }

    private String valueToString(Object value) throws JsonProcessingException {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return (String)value;
        } else if (value instanceof Boolean) {
            return ((Boolean)value).toString();
        } else if (value instanceof Number) {
            return String.valueOf((Number)value);
        } else {
            return objectMapper.writeValueAsString(value);
        }
    }

    private Object stringToValue(String value) {
        if  (value.equalsIgnoreCase("true")) {
            return new Boolean(true);
        } else if (value.equalsIgnoreCase("false")) {
            return new Boolean(false);
        } else if (value.equalsIgnoreCase("null")) {
            return null;
        } else if ((value.startsWith("\"")) && (value.endsWith("\""))) {
            return value;
        } else {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e1) {
                try {
                    return Float.parseFloat(value);
                } catch (NumberFormatException e2) {
                    try {
                        return Double.parseDouble(value);
                    } catch (NumberFormatException e3) {
                        throw new NotAcceptableException();
                    }
                }
            }
        }
    }

}
