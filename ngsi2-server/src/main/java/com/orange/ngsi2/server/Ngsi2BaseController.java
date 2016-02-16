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
public class Ngsi2BaseController {

    private static Logger logger = LoggerFactory.getLogger(Ngsi2BaseController.class);

    Pattern p = Pattern.compile("[a-zA-Z0-9_,-]*");

    @RequestMapping(method = RequestMethod.GET,
            value = {"/"})
    final public ResponseEntity<Map<String,String>> getListResources() throws Exception {
        return new ResponseEntity<Map<String,String>>(getResources(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities"})
    final public ResponseEntity<List<Entity>> getListEntities(@RequestParam Optional<String> id, @RequestParam Optional<String> type, @RequestParam Optional<String> idPattern, @RequestParam Optional<Integer> limit, @RequestParam Optional<Integer> offset, @RequestParam Optional<String> attrs) throws Exception {
        Collection<String> itemsToValidate = new ArrayList<>();
        if (id.isPresent() && idPattern.isPresent()) {
            throw new IncompatibleParameterException(id.get(), idPattern.get(), "List entities");
        }
        if (id.isPresent())
            itemsToValidate.add(id.get());
        if (type.isPresent())
            itemsToValidate.add(type.get());
        if (attrs.isPresent()) {
            itemsToValidate.add(attrs.get());
        }
        syntaxValidation(itemsToValidate);
        return new ResponseEntity<List<Entity>>(getEntities(id, type, idPattern, limit, offset, attrs), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/entities", consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity postEntity(@RequestBody Entity entity) {
        Collection<String> itemsToValidate = new ArrayList<>();
        if (entity.getId() != null) {
            itemsToValidate.add(entity.getId());
        }
        if (entity.getType() != null ) {
            itemsToValidate.add(entity.getType());
        }
        if (entity.getAttributes() != null) {
            itemsToValidate.addAll(entity.getAttributes().keySet());
        }
        syntaxValidation(itemsToValidate);
        StringBuilder location = new StringBuilder("/v2/entities/");
        location.append(createEntity(entity));
        HttpHeaders headers = new HttpHeaders();
        headers.put("Location", Collections.singletonList(location.toString()));
        return new ResponseEntity(headers, HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities/{entityId}"})
    final public ResponseEntity<Entity> getEntity(@PathVariable String entityId, @RequestParam Optional<String> attrs) throws Exception {
        Collection<String> itemsToValidate = new ArrayList<>();
        itemsToValidate.add(entityId);
        if (attrs.isPresent()) {
            itemsToValidate.add(attrs.get());
        }
        syntaxValidation(itemsToValidate);
        List<Entity> entities = getEntities(Optional.of(entityId), null, null, null, null, attrs);
        if (entities.size() > 1 ) {
            StringBuilder url = new StringBuilder("GET /v2/entities?id=");
            url.append(entityId);
            if (attrs.isPresent()) {
                url.append("&attrs=");
                url.append(attrs.get());
            }
            throw new ConflictingEntitiesException(entityId, url.toString());
        }
        return new ResponseEntity<Entity>(entities.get(0), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST,
            value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateOrAppendEntity(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes) throws Exception {
        Collection<String> itemsToValidate = new ArrayList<>();
        itemsToValidate.add(entityId);
        itemsToValidate.addAll(attributes.keySet());
        syntaxValidation(itemsToValidate);
        updateEntity(entityId, attributes);
        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(method = RequestMethod.PATCH, value = {"/entities/{entityId}"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    final public ResponseEntity updateExistingEntityAttributes(@PathVariable String entityId, @RequestBody HashMap<String, Attribute> attributes) throws Exception {
        Collection<String> itemsToValidate = new ArrayList<>();
        itemsToValidate.add(entityId);
        itemsToValidate.addAll(attributes.keySet());
        syntaxValidation(itemsToValidate);
        updateExistingAttributes(entityId, attributes);
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
        logger.error("Invalid syntax: {}", exception.getAffectedItems().toString());
        Error error = new Error(exception.getError());
        error.setDescription(Optional.of(exception.getDescription()));
        error.setAffectedItems(Optional.of(exception.getAffectedItems()));
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

    protected List<Entity> getEntities(Optional<String> id, Optional<String> type, Optional<String> idPattern, Optional<Integer> limit, Optional<Integer> offset, Optional<String> attrs) throws Exception {
         throw new UnsupportedOperationException("List Entities");
    }

    protected Map<String,String> getResources() throws Exception {
        throw new UnsupportedOperationException("Retrieve API Resources");
    }

    protected String createEntity(Entity entity){
        throw new UnsupportedOperationException("Create Entity");
    }

    protected void updateEntity(String entityId, HashMap<String, Attribute> attributes){
        throw new UnsupportedOperationException("Update Entity");
    }

    protected void updateExistingAttributes(String entityId, HashMap<String, Attribute> attributes){
        throw new UnsupportedOperationException("Update Existing Attributes");
    }

    private void syntaxValidation(Collection<String> items) throws InvalidatedSyntaxException {
        Collection<String> affectedItems = new ArrayList<>();

        items.forEach(s -> {
            if (( s.length() > 256) || (!p.matcher(s).matches()))  {
                affectedItems.add(s);
            }
        });
        if (!affectedItems.isEmpty()) {
            throw new InvalidatedSyntaxException(affectedItems);
        }
    }
}
