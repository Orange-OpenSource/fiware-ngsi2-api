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
                get("/v2/i/entities").param("id", "Boe_Idearium").param("idPattern", "Bode_.*").contentType(MediaType.APPLICATION_JSON).header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. The parameter Boe_Idearium is incompatible with Bode_.* in List entities operation."))
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
                get("/v2/i/entities/Bcn%Welt").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Bcn%Welt has a bad syntax."))
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
                post("/v2/i/entities/Bcn%Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Bcn%Welt has a bad syntax."))
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
                patch("/v2/i/entities/Bcn%Welt").content(json(jsonV2Converter, createUpdateAttributesReference()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Bcn%Welt has a bad syntax."))
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. ambient%Noise has a bad syntax."))
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
                delete("/v2/i/entities/Bcn%Welt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Bcn%Welt has a bad syntax."))
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
                get("/v2/i/types/Room%").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. Room% has a bad syntax."))
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
                get("/v2/i/entities/Bcn-Welt/attrs/temperature%").contentType(MediaType.APPLICATION_JSON)
                        .header("Host", "localhost").accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. temperature% has a bad syntax."))
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
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("The incoming request is invalid in this context. unit%Code has a bad syntax."))
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
    public void checkPattern() {
        assertTrue(Pattern.matches("[a-zA-Z0-9_,-]*", "Bcn_Welt"));
        assertTrue(Pattern.matches("[a-zA-Z0-9_,-]*", "Bcn-Welt"));
        String p257times = IntStream.range(0, 257)
                .mapToObj(x -> "p")
                .collect(Collectors.joining());
        assertTrue(Pattern.matches("[a-zA-Z0-9_,-]*", p257times));
        String invalid256times = IntStream.range(0, 256)
                .mapToObj(x -> "?")
                .collect(Collectors.joining());
        assertFalse(Pattern.matches("[a-zA-Z0-9_,-]*", invalid256times));
    }

}
