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

package com.orange.ngsi2.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Optional;

/**
 * SubjectRegistration Entity model
 */
public class SubjectEntity {

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<String> id;

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<String> idPattern;

    @JsonInclude(JsonInclude.Include.NON_ABSENT)
    Optional<String> type;

    public SubjectEntity() {
    }

    public SubjectEntity(Optional<String> id) {
        this.id = id;
    }

    public Optional<String> getId() {
        return id;
    }

    public void setId(Optional<String> id) {
        this.id = id;
    }

    public Optional<String> getIdPattern() {
        return idPattern;
    }

    public void setIdPattern(Optional<String> idPattern) {
        this.idPattern = idPattern;
    }

    public Optional<String> getType() {
        return type;
    }

    public void setType(Optional<String> type) {
        this.type = type;
    }
}
