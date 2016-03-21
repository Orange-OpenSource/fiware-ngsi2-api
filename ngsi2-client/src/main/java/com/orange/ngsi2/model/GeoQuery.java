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
 * GeoQuery is used for entities querying
 */
public class GeoQuery {

    /**
     * GeoQuery Enum model
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

    /**
     * Geometry enum used for entities querying
     */
    public enum Geometry {
        point, line, polygon, box
    }

    private final Relation relation;

    private final Geometry geometry;

    private final List<Coordinate> coordinates;

    /**
     * Defined only for a near relation
     */
    private final Modifier modifier;

    /**
     * Defined only for a near relation
     */
    private final float distance;

    /**
     * Default constructor
     * @param relation relation to the geometry
     * @param geometry geometry to match
     * @param coordinates coordinates for the geometry
     */
    public GeoQuery(Relation relation, Geometry geometry, List<Coordinate> coordinates) {
        this.relation = relation;
        this.geometry = geometry;
        this.coordinates = coordinates;
        this.modifier = null;
        this.distance = 0;
    }

    /**
     * Defines a near relation with modifier and distance
     * @param modifier
     * @param distance
     */
    public GeoQuery(Modifier modifier, float distance, Geometry geometry, List<Coordinate> coordinates) {
        this.relation = Relation.near;
        this.modifier = modifier;
        this.distance = distance;
        this.geometry = geometry;
        this.coordinates = coordinates;
    }

    public Relation getRelation() {
        return relation;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public float getDistance() {
        return distance;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }
}
