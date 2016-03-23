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

import com.orange.ngsi2.model.BulkUpdateRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.orange.ngsi2.utility.Utils.*;
import static com.orange.ngsi2.utility.Utils.updateReference;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.hamcrest.Matchers.hasSize;

/**
 * Tests for the NGSI v2 base controller.
 * This class uses the two FakeNgsi2ControllerHelper and NotImplementedNgsi2Controller classes
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestConfiguration.class)
@WebAppConfiguration
public class Ngsi2BaseControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private MappingJackson2HttpMessageConverter jsonV2Converter;

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void checkListResourcesNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Retrieve API Resources' is not implemented"))
                .andExpect(status().isNotImplemented());

    }

    @Test
    public void checkListEntitiesNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/entities").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'List Entities' is not implemented"))
                .andExpect(status().isNotImplemented());

    }

    @Test
    public void checkListEntitiesIncompatibleParameter() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("id", "Boe_Idearium,Bcn-Welt").param("idPattern", "Bode_.*").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. The parameter id is incompatible with idPattern in List entities operation."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesInvalidSyntax() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("id", "Boe_Idearium?").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Boe_Idearium? has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListTypeInvalidSyntax() throws Exception {
        String p257times = IntStream.range(0, 257)
                .mapToObj(x -> "p")
                .collect(Collectors.joining());
        String message = "The incoming request is invalid in this context. " + p257times + " has a bad syntax.";
        mockMvc.perform(
                get("/v2/i/entities").param("type", p257times).contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value(message))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListAttrsInvalidSyntax() throws Exception {
        String  invalidAttrs = IntStream.range(0, 257)
                .mapToObj(x -> "?")
                .collect(Collectors.joining());
        String message = "The incoming request is invalid in this context. " + invalidAttrs + " has a bad syntax.";
        mockMvc.perform(
                get("/v2/i/entities").param("attrs", invalidAttrs).contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value(message))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesWithCount() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("id", "Bcn-Welt").param("options","count").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("Bcn-Welt"))
                .andExpect(header().string("X-Total-Count","1"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkListEntitiesWithoutCount() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("id", "Bcn-Welt").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("Bcn-Welt"))
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkListEntitiesAllParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "near;maxDistance:1000").param("geometry", "point").param("coords", "-40.4,-3.5")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("Bcn-Welt"))
                .andExpect(header().string("X-Total-Count","1"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkListEntitiesUnsupportedKeyValuesOptions() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("id", "Bcn-Welt").param("options","keyValues").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues, values or unique"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkListEntitiesUnsupportedValuesOptions() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("id", "Bcn-Welt").param("options","values").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues, values or unique"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkListEntitiesUnsupportedUniqueOptions() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("id", "Bcn-Welt").param("options","unique").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues, values or unique"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkListEntitiesMissingGeorelParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("geometry", "point").param("coords", "-40.4,-3.5")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Bad request: Missing one argument of georel, geometry or coords"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesMissingGeometryParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "proximity").param("coords", "-40.4,-3.5")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Bad request: Missing one argument of georel, geometry or coords"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesMissingCoordsParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "proximity").param("geometry", "point")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Bad request: Missing one argument of georel, geometry or coords"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesWrongGeorelParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "proximity").param("geometry", "point").param("coords", "-40.4,-3.5")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. proximity has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesWrongModifierParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "near;distance:1000").param("geometry", "point").param("coords", "-40.4,-3.5")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. distance:1000 has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesEmptyDistanceParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "near;maxDistance").param("geometry", "point").param("coords", "-40.4,-3.5")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. maxDistance has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesWrongDistanceParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "near;maxDistance:meter").param("geometry", "point").param("coords", "-40.4,-3.5")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. maxDistance:meter has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesCoveredByGeorelParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "coveredBy").param("geometry", "polygon").param("coords", "25.774,-80.190;18.466,-66.118;32.321,-64.757;25.774,-80.190")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("Bcn-Welt"))
                .andExpect(header().string("X-Total-Count","1"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkListEntitiesIntersectsGeorelParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "intersects").param("geometry", "point").param("coords", "-40.4,-3.5")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("Bcn-Welt"))
                .andExpect(header().string("X-Total-Count","1"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkListEntitiesEqualsGeorelParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "equals").param("geometry", "point").param("coords", "-40.4,-3.51")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("Bcn-Welt"))
                .andExpect(header().string("X-Total-Count","1"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkListEntitiesDisjointGeorelParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "disjoint").param("geometry", "point").param("coords", "-40.4,-3.5")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("Bcn-Welt"))
                .andExpect(header().string("X-Total-Count","1"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkListEntitiesWrongGeometryParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "disjoint").param("geometry", "triangle").param("coords", "-40.4,-3.5")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. triangle has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesWrongCoordsParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "disjoint").param("geometry", "point").param("coords", "-40.4")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. coords has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesEmptyCoordsParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "disjoint").param("geometry", "point").param("coords", "")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. coords has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesMissingNumberCoordsParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "disjoint").param("geometry", "polygon").param("coords", "31.2,23.2;23.2")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. coords has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkListEntitiesBadNumberCoordsParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities").param("limit", "20").param("offset", "20").param("options","count")
                        .param("type", "Room").param("id", "Bcn-Welt").param("q", "temperature>40")
                        .param("georel", "disjoint").param("geometry", "polygon").param("coords", "31.2,23.2;23.2,BAD")
                        .param("attrs","seatNumber").param("orderBy", "temperature,!speed")
                        .contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. coords has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkCreateEntityNotImplemented() throws Exception {
        mockMvc.perform(
                post("/v2/ni/entities").content(json(jsonV2Converter, createEntityBcnWelt())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Create Entity' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkCreateEntityFake() throws Exception {
        mockMvc.perform(
                post("/v2/i/entities").content(json(jsonV2Converter, createEntityBcnWelt())).content(json(jsonV2Converter, createEntityBcnWelt())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Location","/v2/entities/Bcn-Welt"))
                .andExpect(status().isCreated());
    }

    @Test
    public void checkCreateEntityUnsupportedKeyValuesOptions() throws Exception {
        mockMvc.perform(
                post("/v2/i/entities").content(json(jsonV2Converter, createEntityBcnWelt())).param("options","keyValues").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveEntityNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/entities/Bcn-Welt").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Retrieve Entity' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveEntityConflictingEntities() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Boe-Idearium").param("attrs", "temperature").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("409"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Too many results. There are several results that match with the Boe-Idearium used in the request. Instead of, you can use GET /v2/entities?id=Boe-Idearium&attrs=temperature"))
                .andExpect(status().isConflict());
    }

    @Test
    public void checkRetrieveEntityInvalidSyntax() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn&Welt").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Bcn&Welt has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRetrieveEntityOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.type").value("Room"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("Bcn-Welt"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkRetrieveEntityAllParameters() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt").contentType(MediaType.APPLICATION_JSON)
                        .param("type", "Room")
                        .param("attrs","temperature,humidity")
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.type").value("Room"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("Bcn-Welt"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkRetrieveEntityUnsupportedKeyValuesOptions() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt").contentType(MediaType.APPLICATION_JSON)
                        .param("options","keyValues")
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveEntityUnsupportedValuesOptions() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt").contentType(MediaType.APPLICATION_JSON)
                        .param("options","values")
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: values"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveEntityUnsupportedUniqueOptions() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt").contentType(MediaType.APPLICATION_JSON)
                        .param("options","unique")
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: unique"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkUpdateOrAppendEntityIdNotImplemented() throws Exception {
        mockMvc.perform(
                post("/v2/ni/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Update Or Append Entity' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkUpdateOrAppendEntityIdInvalidSyntax() throws Exception {
        mockMvc.perform(
                post("/v2/i/entities/Bcn&Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Bcn&Welt has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkUpdateOrAppendEntityIdOK() throws Exception {
        mockMvc.perform(
                post("/v2/i/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkUpdateOrAppendEntityIdWithAppend() throws Exception {
        mockMvc.perform(
                post("/v2/i/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .param("options","append")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkUpdateOrAppendEntityIdUnsupportedKeyValuesOptions() throws Exception {
        mockMvc.perform(
                post("/v2/i/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .param("options","keyValues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkUpdateExistingEntityAttributesNotImplemented() throws Exception {
        mockMvc.perform(
                patch("/v2/ni/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Update Existing Entity Attributes' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkUpdateExistingEntityAttributesInvalidSyntax() throws Exception {
        mockMvc.perform(
                patch("/v2/i/entities/Bcn&Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Bcn&Welt has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkUpdateExistingEntityAttributesOK() throws Exception {
        mockMvc.perform(
                patch("/v2/i/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkUpdateExistingEntityAttributesUnsupportedKeyValuesOptions() throws Exception {
        mockMvc.perform(
                patch("/v2/i/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .param("options","keyValues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkReplaceAllEntityAttributesNotImplemented() throws Exception {
        mockMvc.perform(
                put("/v2/ni/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Replace All Entity Attributes' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkReplaceAllEntityAttributesInvalidSyntax() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesWithBadSyntax()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. ambient&Noise has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkReplaceAllEntityAttributesOK() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkReplaceAllEntityAttributesUnsupportedKeyValuesOptions() throws Exception {
        mockMvc.perform(
                put("/v2/ni/entities/Bcn-Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .param("options","keyValues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRemoveEntityNotImplemented() throws Exception {
        mockMvc.perform(
                delete("/v2/ni/entities/Bcn-Welt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Remove Entity' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRemoveEntityInvalidSyntax() throws Exception {
        mockMvc.perform(
                delete("/v2/i/entities/Bcn&Welt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Bcn&Welt has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRemoveEntityOK() throws Exception {
        mockMvc.perform(
                delete("/v2/i/entities/Bcn-Welt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkRetrieveEntityTypesNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/types").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Retrieve Entity Types' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveEntityTypesOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/types").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].type").value("Room"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].attrs[*]", hasSize(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].count").value(7))
                .andExpect(status().isOk());
    }

    @Test
    public void checkRetrieveEntityTypesUnsupportedValuesOptions() throws Exception {
        mockMvc.perform(
                get("/v2/i/types").contentType(MediaType.APPLICATION_JSON)
                        .param("options","values")
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: values"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveEntityTypeNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/types/Room").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Retrieve Entity Type' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveEntityTypeInvalidSyntax() throws Exception {
        mockMvc.perform(
                get("/v2/i/types/Room&").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Room& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRetrieveEntityTypeOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/types/Room").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.attrs[*]", hasSize(3)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.count").value(7))
                .andExpect(status().isOk());
    }

    @Test
    public void checkRetrieveAttributeByEntityIdNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/entities/Bcn-Welt/attrs/temperature").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Retrieve Attribute by Entity ID' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveAttributeByEntityIdConflictingEntities() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Boe-Idearium/attrs/temperature").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("409"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Too many results. There are several results that match with the Boe-Idearium used in the request. Instead of, you can use GET /v2/entities/Boe-Idearium/attrs/temperature?type="))
                .andExpect(status().isConflict());
    }

    @Test
    public void checkRetrieveAttributeByEntityIdInvalidSyntax() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt/attrs/temperature&").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. temperature& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRetrieveAttributeByEntityIdOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt/attrs/temperature").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.value").value(21.7))
                .andExpect(status().isOk());
    }

    @Test
    public void checkUpdateAttributeByEntityIdNotImplemented() throws Exception {
        mockMvc.perform(
                put("/v2/ni/entities/Bcn-Welt/attrs/temperature").content(json(jsonV2Converter, createUpdateTemperatureAttributeReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Update Attribute by Entity ID' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkUpdateAttributeByEntityIdInvalidSyntax() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature").content(json(jsonV2Converter, createUpdateTemperatureAttributeReferenceWithBadSyntax()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. unit?Code has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkUpdateAttributeByEntityIdConflictingEntities() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Boe-Idearium/attrs/temperature").content(json(jsonV2Converter, createUpdateTemperatureAttributeReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("409"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Too many results. There are several results that match with the Boe-Idearium used in the request. Instead of, you can use PUT /v2/entities/Boe-Idearium/attrs/temperature?type="))
                .andExpect(status().isConflict());
    }

    @Test
    public void checkUpdateAttributeByEntityIdOK() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature").content(json(jsonV2Converter, createUpdateTemperatureAttributeReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkRemoveAttributeByEntityIdNotImplemented() throws Exception {
        mockMvc.perform(
                delete("/v2/ni/entities/Bcn-Welt/attrs/temperature").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Remove Attribute' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRemoveAttributeByEntityIdInvalidSyntax() throws Exception {
        mockMvc.perform(
                delete("/v2/i/entities/Bcn-Welt/attrs/temperature&").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. temperature& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRemoveAttributeByEntityIdConflictingEntities() throws Exception {
        mockMvc.perform(
                delete("/v2/i/entities/Boe-Idearium/attrs/temperature").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("409"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Too many results. There are several results that match with the Boe-Idearium used in the request. Instead of, you can use DELETE /v2/entities/Boe-Idearium/attrs/temperature?type="))
                .andExpect(status().isConflict());
    }

    @Test
    public void checkRemoveAttributeByEntityIdOK() throws Exception {
        mockMvc.perform(
                delete("/v2/i/entities/Bcn-Welt/attrs/temperature").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkRetrieveAttributeValueNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/entities/Bcn-Welt/attrs/temperature/value").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Retrieve Attribute Value' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveTextPlainAttributeValueNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/entities/Bcn-Welt/attrs/temperature/value").contentType(MediaType.TEXT_PLAIN)
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string("error: 501 | description: this operation 'Retrieve Attribute Value' is not implemented | affectedItems: []"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveAttributeValueInvalidSyntax() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn&Welt/attrs/temperature/value").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Bcn&Welt has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRetrieveTextPlainAttributeValueInvalidSyntax() throws Exception {
        mockMvc.perform(
                get("/v2/ni/entities/Bcn&Welt/attrs/temperature/value").contentType(MediaType.TEXT_PLAIN)
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string("error: 400 | description: The incoming request is invalid in this context. Bcn&Welt has a bad syntax. | affectedItems: []"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRetrieveAttributeValueNotAcceptable() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt/attrs/temperature/value").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("406"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Not Acceptable: Accepted MIME types: text/plain."))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    public void checkRetrieveAttributeValueOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt/attrs/pressure/value")
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.city").value("Madrid"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkTextPlainRetrieveAttributeValueOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt/attrs/temperature/value")
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string("25.0"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkTextPlainRetrieveAttributeValueStringOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt/attrs/hello/value")
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string("\"hello, world\""))
                .andExpect(status().isOk());
    }

    @Test
    public void checkTextPlainRetrieveAttributeValueJsonObjectOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt/attrs/pressure/value")
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string("{\"address\":\"Ronda de la Comunicacions\",\"zipCode\":28050,\"city\":\"Madrid\",\"country\":\"Spain\"}"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkTextPlainRetrieveAttributeValueBooleanOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt/attrs/on/value")
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string("true"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkTextPlainRetrieveAttributeValueNullOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/entities/Bcn-Welt/attrs/color/value")
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string("null"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkUpdateAttributeValueNotImplemented() throws Exception {
        mockMvc.perform(
                put("/v2/ni/entities/Bcn-Welt/attrs/temperature/value").content(json(jsonV2Converter, createValueReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Update Attribute Value' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkUpdateAttributeValueInvalidSyntax() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature&/value").content(json(jsonV2Converter, createValueReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. temperature& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkUpdateAttributeValueConflictingEntities() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Boe-Idearium/attrs/temperature/value").content(json(jsonV2Converter, createValueReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("409"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Too many results. There are several results that match with the Boe-Idearium used in the request. Instead of, you can use PUT /v2/entities/Boe-Idearium/attrs/temperature/value?type="))
                .andExpect(status().isConflict());
    }

    @Test
    public void checkUpdateAttributeValueOK() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature/value").content(json(jsonV2Converter, createValueReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkTextPlainUpdateAttributeValueFloatOK() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature/value").content("22.5")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkTextPlainUpdateAttributeValueStringOK() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature/value").content("\"green\"")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkTextPlainUpdateAttributeValueBooleanFalseOK() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature/value").content("False")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string(""));
    }

    @Test
    public void checkTextPlainUpdateAttributeValueBooleanTrueOK() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature/value").content("True")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string(""));
    }

    @Test
    public void checkTextPlainUpdateAttributeValueNullOK() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature/value").content("null")
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string(""));
    }

    @Test
    public void checkTextPlainUpdateAttributeValueNotAcceptable() throws Exception {
        mockMvc.perform(
                put("/v2/i/entities/Bcn-Welt/attrs/temperature/value").content(json(jsonV2Converter, createValueReference()))
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Host", "localhost").accept(MediaType.TEXT_PLAIN))
                .andExpect(content().string("error: 406 | description: Not Acceptable: Accepted MIME types: text/plain. | affectedItems: []"))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    public void checkListRegistrationsNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/registrations").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Retrieve Registrations' is not implemented"))
                .andExpect(status().isNotImplemented());

    }

    @Test
    public void checkListRegistrationsOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/registrations").header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value("abcdefg"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkCreateRegistrationNotImplemented() throws Exception {
        mockMvc.perform(
                post("/v2/ni/registrations").content(json(jsonV2Converter, createRegistrationReference())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Create Registration' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkCreateRegistrationOK() throws Exception {
        mockMvc.perform(
                post("/v2/i/registrations").content(json(jsonV2Converter, createRegistrationReference())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    public void checkCreateRegistrationWithBasSyntax() throws Exception {
        mockMvc.perform(
                post("/v2/i/registrations").content(json(jsonV2Converter, createRegistrationReferenceWithBadSyntax())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. example& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRetrieveRegistrationNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/registrations/abcde").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Retrieve Registration' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveRegistrationInvalidSyntax() throws Exception {
        mockMvc.perform(
                get("/v2/i/registrations/abcde&").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. abcde& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRetrieveRegistrationOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/registrations/abcde").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.callback").value("http://localhost:1234"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("abcde"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkUpdateRegistrationNotImplemented() throws Exception {
        mockMvc.perform(
                patch("/v2/ni/registrations/abcde").content(json(jsonV2Converter, updateRegistrationReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Update Registration' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkUpdateRegistrationInvalidSyntax() throws Exception {
        mockMvc.perform(
                patch("/v2/i/registrations/abcde&").content(json(jsonV2Converter, updateRegistrationReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. abcde& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkUpdateRegistrationOK() throws Exception {
        mockMvc.perform(
                patch("/v2/i/registrations/abcde").content(json(jsonV2Converter, updateRegistrationReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkRemoveRegistrationNotImplemented() throws Exception {
        mockMvc.perform(
                delete("/v2/ni/registrations/abcde")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Remove Registration' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRemoveRegistrationInvalidSyntax() throws Exception {
        mockMvc.perform(
                delete("/v2/i/registrations/abcde&")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. abcde& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRemoveRegistrationOK() throws Exception {
        mockMvc.perform(
                delete("/v2/i/registrations/abcde")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkListSubscriptionsNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/subscriptions").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'List Subscriptions' is not implemented"))
                .andExpect(status().isNotImplemented());

    }

    @Test
    public void checkListSubscriptionsWithCount() throws Exception {
        mockMvc.perform(
                get("/v2/i/subscriptions").param("options","count").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("abcdefg"))
                .andExpect(header().string("X-Total-Count","1"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkListSubscriptionsWithoutCount() throws Exception {
        mockMvc.perform(
                get("/v2/i/subscriptions").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("abcdefg"))
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkCreateSubscriptionNotImplemented() throws Exception {
        mockMvc.perform(
                post("/v2/ni/subscriptions").content(json(jsonV2Converter, createSubscriptionReference())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Create Subscription' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkCreateSubscriptionOK() throws Exception {
        mockMvc.perform(
                post("/v2/i/subscriptions").content(json(jsonV2Converter, createSubscriptionReference())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    public void checkCreateSubscriptionWithBasSyntax() throws Exception {
        mockMvc.perform(
                post("/v2/i/subscriptions").content(json(jsonV2Converter, createSubscriptionReferenceWithBadSyntax())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. humidity# has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRetrieveSubscriptionNotImplemented() throws Exception {
        mockMvc.perform(
                get("/v2/ni/subscriptions/abcdef").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Retrieve Subscription' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRetrieveSubscriptionInvalidSyntax() throws Exception {
        mockMvc.perform(
                get("/v2/i/subscriptions/abcdef&").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. abcdef& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRetrieveSubscriptionOK() throws Exception {
        mockMvc.perform(
                get("/v2/i/subscriptions/abcdef").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("abcdef"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("active"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkUpdateSubscriptionNotImplemented() throws Exception {
        mockMvc.perform(
                patch("/v2/ni/subscriptions/abcdef").content(json(jsonV2Converter, updateSubscriptionReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Update Subscription' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkUpdateSubscriptionInvalidSyntax() throws Exception {
        mockMvc.perform(
                patch("/v2/i/subscriptions/abcdef&").content(json(jsonV2Converter, updateSubscriptionReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. abcdef& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkUpdateSubscriptionOK() throws Exception {
        mockMvc.perform(
                patch("/v2/i/subscriptions/abcdef").content(json(jsonV2Converter, updateSubscriptionReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkRemoveSubscriptionNotImplemented() throws Exception {
        mockMvc.perform(
                delete("/v2/ni/subscriptions/abcdef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Remove Subscription' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkRemoveSubscriptionInvalidSyntax() throws Exception {
        mockMvc.perform(
                delete("/v2/i/subscriptions/abcde&")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. abcde& has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkRemoveSubscriptionOK() throws Exception {
        mockMvc.perform(
                delete("/v2/i/subscriptions/abcdef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(""))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkBulkUpdateNotImplemented() throws Exception {
        mockMvc.perform(
                post("/v2/ni/op/update").content(json(jsonV2Converter, updateReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Update' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkBulkUpdateOK() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/update").content(json(jsonV2Converter, updateReference())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void checkBulkUpdateWrongSyntax() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/update").content(json(jsonV2Converter, updateWrongSyntax())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. DC_S1 D41 has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkBulkUpdateUnsupportedKeyValuesOption() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/update").content(json(jsonV2Converter, updateReference()))
                        .param("options","keyValues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkBulkQueryNotImplemented() throws Exception {
        mockMvc.perform(
                post("/v2/ni/op/query").content(json(jsonV2Converter, queryReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Query' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkBulkQueryOK() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/query").content(json(jsonV2Converter, queryReference())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("Bcn-Welt"))
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkBulkQueryOKWithCount() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/query").content(json(jsonV2Converter, queryReference())).contentType(MediaType.APPLICATION_JSON)
                        .param("options","count")
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("Bcn-Welt"))
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkBulkQueryWrongSyntax() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/query").content(json(jsonV2Converter, queryWrongSyntax())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. FIWARE::...? has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkBulkQueryUnsupportedKeyValuesOption() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/query").content(json(jsonV2Converter, queryReference()))
                        .param("options","keyValues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Unsupported option value: keyValues, values or unique"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkBulkRegisterNotImplemented() throws Exception {
        mockMvc.perform(
                post("/v2/ni/op/register").content(json(jsonV2Converter, registerReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Register' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkBulkRegisterOK() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/register").content(json(jsonV2Converter, registerReference())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0]").value("abcd"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[1]").value("efgh"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkBulkRegisterWrongSyntax() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/register").content(json(jsonV2Converter, registerWrongSyntax())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Room? has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkBulkDiscoverNotImplemented() throws Exception {
        mockMvc.perform(
                post("/v2/ni/op/discover").content(json(jsonV2Converter, queryReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("501"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("this operation 'Discover' is not implemented"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    public void checkBulkDiscoverOK() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/discover").content(json(jsonV2Converter, queryReference())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("abcdefg"))
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkBulkDiscoverOKWithCount() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/discover").content(json(jsonV2Converter, queryReference())).contentType(MediaType.APPLICATION_JSON)
                        .param("options","count")
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].id").value("abcdefg"))
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(status().isOk());
    }

    @Test
    public void checkBulkDiscoverWrongSyntax() throws Exception {
        mockMvc.perform(
                post("/v2/i/op/discover").content(json(jsonV2Converter, queryWrongSyntax())).contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. FIWARE::...? has a bad syntax."))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void checkPattern() {
        assertTrue(Pattern.matches("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*", "Bcn_Welt"));
        assertTrue(Pattern.matches("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*", "Bcn-Welt"));
        assertFalse(Pattern.matches("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*", "Bcn Welt"));
        assertFalse(Pattern.matches("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*", "Bcn&Welt"));
        assertFalse(Pattern.matches("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*", "Bcn?Welt"));
        assertFalse(Pattern.matches("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*", "Bcn/Welt"));
        assertFalse(Pattern.matches("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*", "Bcn#Welt"));

        String p257times = IntStream.range(0, 257)
                .mapToObj(x -> "p")
                .collect(Collectors.joining());
        assertTrue(Pattern.matches("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*", p257times));
        String invalid256times = IntStream.range(0, 256)
                .mapToObj(x -> "?")
                .collect(Collectors.joining());
        assertFalse(Pattern.matches("[\\x21\\x22\\x24\\x25\\x27-\\x2E\\x30-\\x3E\\x40-\\x7E]*", invalid256times));
    }

}
