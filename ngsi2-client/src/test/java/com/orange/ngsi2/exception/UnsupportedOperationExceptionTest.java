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

package com.orange.ngsi2.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by pascale on 10/02/2016.
 */
public class UnsupportedOperationExceptionTest {

    @Test
    public void checkProperties() {
        UnsupportedOperationException exception = new UnsupportedOperationException("List Entities");
        assertEquals("this operation 'List Entities' is not implemented", exception.getMessage());
    }

}
