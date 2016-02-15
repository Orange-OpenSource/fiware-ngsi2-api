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
 * Created by pascale on 10/02/2016.
 */

public class IncompatibleParameterException extends Exception {

    private String error = "400";

    private String description = null;

    public IncompatibleParameterException(String param1, String param2, String operationName) {
        super("");
        description = String.format("The incoming request is invalid in this context. The parameter %s is incompatible with %s in %s operation.", param1, param2, operationName);
    }

    @Override
    public String getMessage() {
        return description;
    }

    public String getError() {
        return error;
    }

    public String getDescription() {
        return description;
    }
}
