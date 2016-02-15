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
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

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
                .andExpect(MockMvcResultMatchers.jsonPath("$.description").value("Syntax invalid"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.affectedItems").value("Boe_Idearium?"))
                .andExpect(status().isBadRequest());
    }

}