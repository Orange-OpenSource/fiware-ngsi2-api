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

import com.orange.ngsi2.exception.IncompatibleParameterException;
import com.orange.ngsi2.exception.UnsupportedOperationException;
import com.orange.ngsi2.model.Entity;
import com.orange.ngsi2.model.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * Controller for the NGSI v2 requests
 */
public class Ngsi2BaseController {

    private static Logger logger = LoggerFactory.getLogger(Ngsi2BaseController.class);

    @RequestMapping(method = RequestMethod.GET,
            value = {"/entities"})
    final public ResponseEntity<List<Entity>> getListEntities(@PathVariable Optional<String> id, @PathVariable Optional<String> type, @PathVariable Optional<String> idPattern, @PathVariable Optional<Integer> limit, @PathVariable Optional<Integer> offset, @PathVariable Optional<String> attrs) throws Exception {
        if (id.isPresent() && idPattern.isPresent()) {
            throw new IncompatibleParameterException(id.get(), idPattern.get(), "List entities");
        }
        return new ResponseEntity<List<Entity>>(getEntities(id, type, idPattern, limit, offset, attrs), HttpStatus.OK);
    }

    /*
     * Exception handling
     */

    @ExceptionHandler({UnsupportedOperationException.class})
    public ResponseEntity<Object> unsupportedOperation(UnsupportedOperationException exception) {
        logger.error("Unsupported operation: {}", exception.toString());
        Error error = new Error(exception.getError());
        error.setDescription(Optional.of(exception.getDescription()));
        return new ResponseEntity<Object>(error, HttpStatus.NOT_IMPLEMENTED);
    }

    /*
     * Methods overridden by child classes to handle the NGSI v2 requests
     */

    protected List<Entity> getEntities(Optional<String> id, Optional<String> type, Optional<String> idPattern, Optional<Integer> limit, Optional<Integer> offset, Optional<String> attrs) throws Exception {
         throw new UnsupportedOperationException("List Entities");
    }
}
