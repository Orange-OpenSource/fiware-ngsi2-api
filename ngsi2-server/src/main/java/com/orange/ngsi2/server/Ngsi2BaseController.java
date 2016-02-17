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

import com.orange.ngsi2.exception.ConflictingEntitiesException;
import com.orange.ngsi2.exception.IncompatibleParameterException;
import com.orange.ngsi2.exception.InvalidatedSyntaxException;
import com.orange.ngsi2.exception.UnsupportedOperationException;
import com.orange.ngsi2.model.Attribute;
import com.orange.ngsi2.model.Entity;
import com.orange.ngsi2.model.Error;
import com.orange.ngsi2.model.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Controller for the NGSI v2 requests
 */
@RequestMapping(value = {"/v2"})
public abstract class Ngsi2BaseController {

    private static Logger logger = LoggerFactory.getLogger(Ngsi2BaseController.class);

    private static Pattern fieldPattern = Pattern.compile("[a-zA-Z0-9_,-]*");

    @RequestMapping(method = RequestMethod.GET,
            value = {"/"})
    final public ResponseEntity<Map<String,String>> getListResources() throws Exception {
        return new ResponseEntity<Map<String,String>>(getResources(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities"})
    final public ResponseEntity<List<Entity>> listEntitiesEndpoint(@RequestParam Optional<String> id, @RequestParam Optional<String> type, @RequestParam Optional<String> idPattern, @RequestParam Optional<Integer> limit, @RequestParam Optional<Integer> offset, @RequestParam Optional<String> attrs) throws Exception {

        if (id.isPresent() && idPattern.isPresent()) {
            throw new IncompatibleParameterException(id.get(), idPattern.get(), "List entities");
        }
        validateSyntax(id, type, attrs);
        return new ResponseEntity<List<Entity>>(listEntities(id, type, idPattern, limit, offset, attrs), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/entities", consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity createEntityEndpoint(@RequestBody Entity entity) {

        validateSyntax(entity);
        createEntity(entity);
        return new ResponseEntity(locationHeader(entity.getId()), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities/{entityId}"})
    final public ResponseEntity<Entity> retrieveEntityEndpoint(@PathVariable String entityId, @RequestParam Optional<String> attrs) throws Exception {

        validateSyntax(Optional.of(entityId), Optional.empty(), attrs);
        return new ResponseEntity<Entity>(retrieveEntity(entityId, attrs), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST,
            value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateOrAppendEntityEndpoint(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes) throws Exception {

        validateSyntax(entityId, attributes);
        updateOrAppendEntity(entityId, attributes);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(method = RequestMethod.PATCH, value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateExistingEntityAttributesEndpoint(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes) throws Exception {

        validateSyntax(entityId, attributes);
        updateExistingEntityAttributes(entityId, attributes);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(method = RequestMethod.PUT, value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity replaceAllEntityAttributesEndpoint(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes) throws Exception {

        validateSyntax(entityId, attributes);
        replaceAllEntityAttributes(entityId, attributes);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = {"/entities/{entityId}"})
    final public ResponseEntity removeEntityEndpoint(@PathVariable String entityId) throws Exception {

        validateSyntax(entityId);
        removeEntity(entityId);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }


    /*
     * Exception handling
     */

    @ExceptionHandler({UnsupportedOperationException.class})
    public ResponseEntity<Object> unsupportedOperation(UnsupportedOperationException exception) {
        logger.error("Unsupported operation: {}", exception.getMessage());
        Error error = new Error(exception.getError());
        error.setDescription(Optional.of(exception.getDescription()));
        return new ResponseEntity<Object>(error, HttpStatus.NOT_IMPLEMENTED);
    }

    @ExceptionHandler({IncompatibleParameterException.class})
    public ResponseEntity<Object> incompatibleParameter(IncompatibleParameterException exception) {
        logger.error("Incompatible parameter: {}", exception.getMessage());
        Error error = new Error(exception.getError());
        error.setDescription(Optional.of(exception.getDescription()));
        return new ResponseEntity<Object>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({InvalidatedSyntaxException.class})
    public ResponseEntity<Object> invalidSyntax(InvalidatedSyntaxException exception) {
        logger.error("Invalid syntax: {}", exception.getMessage());
        Error error = new Error(exception.getError());
        error.setDescription(Optional.of(exception.getDescription()));
        return new ResponseEntity<Object>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ConflictingEntitiesException.class})
    public ResponseEntity<Object> conflictingEntities(ConflictingEntitiesException exception) {
        logger.error("ConflictingEntities: {}", exception.getMessage());
        Error error = new Error(exception.getError());
        error.setDescription(Optional.of(exception.getDescription()));
        return new ResponseEntity<Object>(error, HttpStatus.CONFLICT);
    }

    /*
     * Methods overridden by child classes to handle the NGSI v2 requests
     */

    protected List<Entity> listEntities(Optional<String> ids, Optional<String> types, Optional<String> idPattern, Optional<Integer> limit, Optional<Integer> offset, Optional<String> attrs) throws Exception {
         throw new UnsupportedOperationException("List Entities");
    }

    protected Map<String,String> getResources() throws Exception {
        throw new UnsupportedOperationException("Retrieve API Resources");
    }

    protected void createEntity(Entity entity){
        throw new UnsupportedOperationException("Create Entity");
    }

    protected Entity retrieveEntity(String entityId, Optional<String> attrs) throws ConflictingEntitiesException {
        throw new UnsupportedOperationException("Retrieve Entity");
    }

    protected void updateOrAppendEntity(String entityId, Map<String, Attribute> attributes){
        throw new UnsupportedOperationException("Update Or Append Entity");
    }

    protected void updateExistingEntityAttributes(String entityId, Map<String, Attribute> attributes){
        throw new UnsupportedOperationException("Update Existing Entity Attributes");
    }

    protected void replaceAllEntityAttributes(String entityId, Map<String, Attribute> attributes){
        throw new UnsupportedOperationException("Replace All Entity Attributes");
    }

    protected void removeEntity(String entityId){
        throw new UnsupportedOperationException("Remove Entity");
    }

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

    private void validateSyntax(Map<String, Attribute> attributes) {
        //check attribute name
        attributes.keySet().forEach(s -> validateSyntax(s));
        attributes.values().forEach(attribute -> {
            //check attribute type
            if (attribute.getType() != null) {
                validateSyntax(attribute.getType().get());
            }
            Map<String, Metadata> metadatas = attribute.getMetadata();
            if (metadatas != null) {
                //check metadata name
                metadatas.keySet().forEach(s -> validateSyntax(s));
                //check metadata type
                metadatas.values().forEach(metadata -> {
                    if (metadata.getType() != null) {
                        validateSyntax(metadata.getType());
                    }
                });
            }

        });
    }

    private void validateSyntax(String entityId, Map<String, Attribute> attributes) {
        validateSyntax(entityId);
        if (attributes != null) {
            validateSyntax(attributes);
        }
    }

    private HttpHeaders locationHeader(String entityId) {
        StringBuilder location = new StringBuilder("/v2/entities/");
        location.append(entityId);
        HttpHeaders headers = new HttpHeaders();
        headers.put("Location", Collections.singletonList(location.toString()));
        return headers;
    }
}
