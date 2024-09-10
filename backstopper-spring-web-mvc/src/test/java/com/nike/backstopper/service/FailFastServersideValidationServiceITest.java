package com.nike.backstopper.service;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.testing.base.BaseSpringEnabledValidationTestCase;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ServersideValidationError;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.Serializable;
import java.util.Arrays;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Component test for {@link com.nike.backstopper.service.FailFastServersideValidationService} that verifies it can be injected and called on arbitrary objects, and that the errors it throws are handled appropriately in the Spring MVC chain.
 *
 * @author Nic Munroe
 */
public class FailFastServersideValidationServiceITest extends BaseSpringEnabledValidationTestCase {

    @Inject
    private ObjectMapper objectMapper;

    private static final ApiError SOME_STRING_ERROR = new ApiErrorBase("SOME_STRING_ERROR", 99042, "some string error", 400, null);
    private static final ApiError SUB_OBJECT_ERROR = new ApiErrorBase("SUB_OBJECT_ERROR", 99043, "sub object error", 400, null);
    private static final ApiError SUBOBJECT_FIELD_ERROR = new ApiErrorBase("SUBOBJECT_FIELD_ERROR", 99044, "subobject field", 400, null);

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(
        Arrays.asList(SOME_STRING_ERROR, SUB_OBJECT_ERROR, SUBOBJECT_FIELD_ERROR),
        ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES
    );

    @Test
    public void shouldFailAppropriatelyWhenFailFastServersideValidationDoesNotPass() throws Exception {
        ValidateMe invalidObj = new ValidateMe(null, new ValidateMeSubobject("foo"));
        MvcResult result = this.mockMvc.perform(
                post("/ffsvsControllerDummy")
                        .content(objectMapper.writeValueAsString(invalidObj))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

        verifyErrorResponse(result, testProjectApiErrors, testProjectApiErrors.getServersideValidationApiError(), ServersideValidationError.class);
    }

    @Test
    public void shouldNotFailWhenFailFastServersideValidationPasses() throws Exception {
        ValidateMe validObj = new ValidateMe("foo", new ValidateMeSubobject("bar"));
        MvcResult result = this.mockMvc.perform(
                post("/ffsvsControllerDummy")
                        .content(objectMapper.writeValueAsString(validObj))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getContentAsString(), is("success"));
    }

    private static class ValidateMe implements Serializable {
        @NotNull(message = "SOME_STRING_ERROR")
        public final String someString;

        @Valid
        @NotNull(message = "SUB_OBJECT_ERROR")
        public final ValidateMeSubobject subObject;

        private ValidateMe() {
            this(null, null);
        }

        public ValidateMe(String someString, ValidateMeSubobject subObject) {
            this.someString = someString;
            this.subObject = subObject;
        }
    }

    private static class ValidateMeSubobject implements Serializable {
        @NotNull(message = "SUBOBJECT_FIELD_ERROR")
        public final String someSubObjectField;

        private ValidateMeSubobject() {
            this(null);
        }

        public ValidateMeSubobject(String someSubObjectField) {
            this.someSubObjectField = someSubObjectField;
        }
    }

    @Controller
    @RequestMapping("/ffsvsControllerDummy")
    public static class DummyController {

        @Inject
        private FailFastServersideValidationService validationService;

        @RequestMapping(method = RequestMethod.POST)
        @ResponseBody
        public String doServersideValidationStuff(@RequestBody ValidateMe validateMe) {
            validationService.validateObjectFailFast(validateMe);
            return "success";
        }
    }
}
