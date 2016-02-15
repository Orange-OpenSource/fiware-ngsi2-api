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
 * 501 Unsupported operation
 */
public class UnsupportedOperationException extends Ngsi2Exception {

    private final static String message = "this operation '%s' is not implemented";

    public UnsupportedOperationException(String operationName) {
        super("501", String.format(message, operationName), null);
    }
}
