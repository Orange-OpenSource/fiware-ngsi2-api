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

/**
 * Georel is used for entities querying
 */
public class Georel {

    /**
     * Georel Enum model
     */
    public enum Relation {
        near, coveredBy, intersects, equals, disjoint
    }

    /**
     * Modifier Enum model
     */
    public enum Modifier {
        maxDistance, minDistance
    }

    private final Relation relation;

    /**
     * Defined only for a near relation
     */
    private Modifier modifier;

    /**
     * Defined only for a near relation
     */
    private float distance;

    public Georel(Relation relation) {
        this.relation = relation;
    }

    /**
     * Defines a near relation with modifier and distance
     * @param modifier
     * @param distance
     */
    public Georel(Modifier modifier, float distance) {
        this.relation = Relation.near;
        this.modifier = modifier;
        this.distance = distance;
    }

    public Relation getRelation() {
        return relation;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public void setModifier(Modifier modifier) {
        this.modifier = modifier;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }
}
