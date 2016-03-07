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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Condition model
 */
public class Condition {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<String> attributes;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    Map<String, String> expression;

    public Condition() {
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

    public Map<String, String> getExpression() {
        return expression;
    }

    public void setExpression(Map<String, String> expression) {
        this.expression = expression;
    }

    @JsonIgnore
    public void setExpression(String key, String value) {
        if (expression == null) {
            expression = new HashMap<String, String>();
        }
        expression.put(key, value);
    }
}
