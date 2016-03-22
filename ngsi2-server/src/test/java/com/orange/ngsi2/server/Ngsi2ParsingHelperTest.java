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
import com.orange.ngsi2.model.GeoQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.*;

/**
 * Test class for Ngsi2ParsingHelper
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestConfiguration.class)
@WebAppConfiguration
public class Ngsi2ParsingHelperTest {

    @Test
    public void testParseTextValue() {
        assertEquals("hello world", Ngsi2ParsingHelper.parseTextValue("\"hello world\""));
        assertEquals(true, Ngsi2ParsingHelper.parseTextValue("true"));
        assertEquals(false, Ngsi2ParsingHelper.parseTextValue("false"));
        assertEquals(null, Ngsi2ParsingHelper.parseTextValue("null"));
        assertEquals(1, Ngsi2ParsingHelper.parseTextValue("1"));
        assertEquals(1.2f, Ngsi2ParsingHelper.parseTextValue("1.2"));
    }

    @Test
    public void testParseGeoQuery() {
        GeoQuery geoQuery = Ngsi2ParsingHelper.parseGeoQuery("near;minDistance:23.3", "point", "12.3,14.2");
        assertEquals(GeoQuery.Relation.near, geoQuery.getRelation());
        assertEquals(GeoQuery.Modifier.minDistance, geoQuery.getModifier());
        assertEquals(23.3, geoQuery.getDistance(), 0.1);
        assertEquals(GeoQuery.Geometry.point, geoQuery.getGeometry());
        assertEquals(1, geoQuery.getCoordinates().size());
        assertEquals(12.3, geoQuery.getCoordinates().get(0).getLatitude(), 0.1);
        assertEquals(14.2, geoQuery.getCoordinates().get(0).getLongitude(), 0.1);
    }

    @Test
    public void testParseGeometryRelations() {
        assertEquals(GeoQuery.Relation.disjoint, Ngsi2ParsingHelper.parseGeoQuery("disjoint", "point", "12.3,14.2").getRelation());
        assertEquals(GeoQuery.Relation.coveredBy, Ngsi2ParsingHelper.parseGeoQuery("coveredBy", "point", "12.3,14.2").getRelation());
        assertEquals(GeoQuery.Relation.intersects, Ngsi2ParsingHelper.parseGeoQuery("intersects", "point", "12.3,14.2").getRelation());
        assertEquals(GeoQuery.Relation.equals, Ngsi2ParsingHelper.parseGeoQuery("equals", "point", "12.3,14.2").getRelation());
    }

    @Test
    public void testParseGeometryModifiers() {
        assertEquals(GeoQuery.Modifier.maxDistance, Ngsi2ParsingHelper.parseGeoQuery("near;maxDistance:23.3", "point", "12.3,14.2").getModifier());
        assertEquals(GeoQuery.Modifier.minDistance, Ngsi2ParsingHelper.parseGeoQuery("near;minDistance:23.3", "point", "12.3,14.2").getModifier());
    }

    @Test(expected = InvalidatedSyntaxException.class)
    public void testParseGeometryBadModifier() {
        Ngsi2ParsingHelper.parseGeoQuery("near;badModifier:23.3", "point", "12.3,14.2");
    }

    @Test
    public void testParseGeometry() {
        assertEquals(GeoQuery.Geometry.box, Ngsi2ParsingHelper.parseGeometry("box"));
        assertEquals(GeoQuery.Geometry.line, Ngsi2ParsingHelper.parseGeometry("line"));
        assertEquals(GeoQuery.Geometry.point, Ngsi2ParsingHelper.parseGeometry("point"));
        assertEquals(GeoQuery.Geometry.polygon, Ngsi2ParsingHelper.parseGeometry("polygon"));
    }

    @Test(expected = InvalidatedSyntaxException.class)
    public void testParseGeometryError() {
        Ngsi2ParsingHelper.parseGeometry("blah");
    }

    @Test
    public void testParseCoords() {
        assertEquals(12.3, Ngsi2ParsingHelper.parseCoordinates("12.3,14.2").get(0).getLatitude(), 0.1);
        assertEquals(14.2, Ngsi2ParsingHelper.parseCoordinates("12.3,14.2").get(0).getLongitude(), 0.1);
        assertEquals(2.1, Ngsi2ParsingHelper.parseCoordinates("12.3,14.2;2.1,2.3").get(1).getLatitude(), 0.1);
        assertEquals(2.3, Ngsi2ParsingHelper.parseCoordinates("12.3,14.2;2.1,2.3").get(1).getLongitude(), 0.1);
    }

    @Test(expected = InvalidatedSyntaxException.class)
    public void testParseCoordsEmptyError() {
        Ngsi2ParsingHelper.parseCoordinates("");
    }

    @Test(expected = InvalidatedSyntaxException.class)
    public void testParseCoordsIncomplete() {
        Ngsi2ParsingHelper.parseCoordinates("12.3");
    }

    @Test(expected = InvalidatedSyntaxException.class)
    public void testParseCoordsIncomplete2() {
        Ngsi2ParsingHelper.parseCoordinates("12.3,1.2;23.3");
    }

    @Test(expected = InvalidatedSyntaxException.class)
    public void testParseCoordsBadNumber() {
        Ngsi2ParsingHelper.parseCoordinates("12.3,BAD");
    }
}
