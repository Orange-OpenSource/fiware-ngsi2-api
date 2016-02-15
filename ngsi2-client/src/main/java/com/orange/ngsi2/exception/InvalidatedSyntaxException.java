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

import java.util.Collection;

/**
 * Created by pascale on 11/02/2016.
 */
public class InvalidatedSyntaxException extends Exception {

    private String error = "400";

    private String description = null;

    private Collection<String> affectedItems;

    public InvalidatedSyntaxException(Collection<String> affectedItems) {
        super("");
        description = String.format("Syntax invalid");
        this.affectedItems = affectedItems;
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

    public Collection<String> getAffectedItems() {
        return affectedItems;
    }
}
