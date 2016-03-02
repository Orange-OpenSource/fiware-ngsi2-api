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

import java.util.List;

/**
 * SubjectRegistration model
 */
public class SubjectRegistration {

    List<SubjectEntity> entities;

    List<String> attributes;

    public SubjectRegistration() {
    }

    public SubjectRegistration(List<SubjectEntity> entities, List<String> attributes) {
        this.entities = entities;
        this.attributes = attributes;
    }

    public List<SubjectEntity> getEntities() {
        return entities;
    }

    public void setEntities(List<SubjectEntity> entities) {
        this.entities = entities;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }
}
