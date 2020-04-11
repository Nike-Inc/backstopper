package com.nike.backstopper.springbootsample.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nike.backstopper.apierror.ApiErrorValue;
import com.nike.backstopper.springbootsample.model.SampleModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the functionality of {@link SampleController} using {@link ApiErrorValue} and constraint annotations.
 *
 * @author Andrey Tsarenko
 * @see SampleModel
 */
@RunWith(SpringRunner.class)
@WebMvcTest(SampleController.class)
@ComponentScan(basePackages = "com.nike")
public class SampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void postSampleModel() throws Exception {
        SampleModel sampleModel = new SampleModel("foo", "bar");

        this.mockMvc.perform(post("/sample/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleModel)))
                .andExpect(status().isOk());
    }

    @Test
    public void postSampleModelWithBlankFoo() throws Exception {
        SampleModel sampleModel = new SampleModel(null, "bar");

        this.mockMvc.perform(post("/sample/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleModel)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))

                .andExpect(jsonPath("$.errors[0].code", equalTo("INVALID_VALUE")))
                .andExpect(jsonPath("$.errors[0].message", equalTo("may not be empty")))
                .andExpect(jsonPath("$.errors[0].metadata.field", equalTo("foo")));
    }

    @Test
    public void postSampleModelWithBlankBar() throws Exception {
        SampleModel sampleModel = new SampleModel("foo", null);

        this.mockMvc.perform(post("/sample/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleModel)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))

                .andExpect(jsonPath("$.errors[0].code", equalTo("BLANK_BAR")))
                .andExpect(jsonPath("$.errors[0].message", equalTo("bar should not be blank")))
                .andExpect(jsonPath("$.errors[0].metadata.field", equalTo("bar")));
    }

    @Test
    public void postSampleModelWithBlankFooAndBar() throws Exception {
        SampleModel sampleModel = new SampleModel(null, null);

        this.mockMvc.perform(post("/sample/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleModel)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))

                .andExpect(jsonPath("$.errors[0].code", equalTo("BLANK_BAR")))
                .andExpect(jsonPath("$.errors[0].message", equalTo("bar should not be blank")))
                .andExpect(jsonPath("$.errors[0].metadata.field", equalTo("bar")))

                .andExpect(jsonPath("$.errors[1].code", equalTo("INVALID_VALUE")))
                .andExpect(jsonPath("$.errors[1].message", equalTo("may not be empty")))
                .andExpect(jsonPath("$.errors[1].metadata.field", equalTo("foo")));
    }

}
