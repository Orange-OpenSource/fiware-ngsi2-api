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

/**
 * 400 Incompatible parameter
 */

public class IncompatibleParameterException extends Ngsi2Exception {

    private final static String message = "The incoming request is invalid in this context. The parameter %s is incompatible with %s in %s operation.";

    public IncompatibleParameterException(String param1, String param2, String operationName) {
        super("400", String.format(message, param1, param2, operationName), null);
    }

}
