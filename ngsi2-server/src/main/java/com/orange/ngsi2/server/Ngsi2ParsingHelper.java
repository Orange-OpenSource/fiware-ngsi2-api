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

package com.orange.ngsi2.server;

import com.orange.ngsi2.exception.InvalidatedSyntaxException;
import com.orange.ngsi2.exception.NotAcceptableException;
import com.orange.ngsi2.model.Coordinate;
import com.orange.ngsi2.model.GeoQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to parse some NGSIv2 specific payload or parameters
 */
public class Ngsi2ParsingHelper {

    /**
     * Attempt to parse a text value as boolean, double quoted string, or number
     * @param value the text value to parse
     * @return the value
     * @throws NotAcceptableException if text cannot be parsed
     */
    public static Object parseTextValue(String value) {
        if  (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        } else if (value.equalsIgnoreCase("null")) {
            return null;
        } else if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            return value.substring(1, value.length()-1);
        }
        // Attempt to parse as the simplest number format possible...
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {}
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {}
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}
        throw new NotAcceptableException();
    }

    /**
     * Parse a GeoQuery request parameters
     * @param georel the georel parameter
     * @param geometry the geometry parameter
     * @param coords the coords parameter
     * @return a GeoQuery
     * @throws InvalidatedSyntaxException on error
     */
    public static GeoQuery parseGeoQuery(String georel, String geometry, String coords) {
        String[] georelFields = georel.split(";");
        GeoQuery.Relation relation;
        try {
            relation = GeoQuery.Relation.valueOf(georelFields[0]);
        } catch (IllegalArgumentException e) {
            throw new InvalidatedSyntaxException(georelFields[0]);
        }
        if (relation == GeoQuery.Relation.near && georelFields.length > 1) {
            String[] modifierFields = georelFields[1].split(":");
            if (modifierFields.length != 2) {
                throw new InvalidatedSyntaxException(georelFields[1]);
            }
            try {
                return new GeoQuery(GeoQuery.Modifier.valueOf(modifierFields[0]), Float.parseFloat(modifierFields[1]),
                        parseGeometry(geometry), parseCoordinates(coords));
            } catch (IllegalArgumentException e) {
                throw new InvalidatedSyntaxException(georelFields[1]);
            }
        }
        return new GeoQuery(relation, parseGeometry(geometry), parseCoordinates(coords));

    }

    /**
     * Parse the geometry parameter
     * @param geometry the geometry parameter
     * @return a GeoQuery.Geometry
     * @throws InvalidatedSyntaxException on error
     */
    public static GeoQuery.Geometry parseGeometry(String geometry) {
        try {
            return GeoQuery.Geometry.valueOf(geometry);
        } catch (IllegalArgumentException e) {
            throw new InvalidatedSyntaxException(geometry);
        }
    }

    /**
     * Parse the coords parameter
     * @param stringCoord the coord parameter
     * @return a List of Coordinate
     * @throws InvalidatedSyntaxException on error
     */
    public static List<Coordinate> parseCoordinates(String stringCoord) {
        String[] coords = stringCoord.split("\\s*(;|,)\\s*");
        if (coords.length == 0 || coords.length % 2 != 0) {
            throw new InvalidatedSyntaxException("coords");
        }
        List<Coordinate> coordinates = new ArrayList<>();
        try {
            for (int i = 0; i < coords.length; i += 2) {
                coordinates.add(new Coordinate(Double.parseDouble(coords[i]), Double.parseDouble(coords[i + 1])));
            }
        } catch (NumberFormatException e) {
            throw new InvalidatedSyntaxException("coords");
        }
        return coordinates;
    }
}
