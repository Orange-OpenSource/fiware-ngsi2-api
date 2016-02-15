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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by pascale on 10/02/2016.
 */
public class IncompatibleParameterExceptionTest {

    @Test
    public void checkProperties() {
        IncompatibleParameterException exception = new IncompatibleParameterException("id", "idPattern", "List entities");
        assertEquals("error: 400 | description: The incoming request is invalid in this context. The parameter id is incompatible with idPattern in List entities operation. | affectedItems: null", exception.getMessage());
    }
}
