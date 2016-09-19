package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testing.base.BaseSpringEnabledValidationTestCase;
import com.nike.backstopper.apierror.testing.base.TestCaseValidationSpringConfig;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.exception.ClientDataValidationError;
import com.nike.backstopper.exception.network.ServerHttpStatusCodeException;
import com.nike.backstopper.exception.network.ServerTimeoutException;
import com.nike.backstopper.exception.network.ServerUnknownHttpStatusCodeException;
import com.nike.backstopper.exception.network.ServerUnreachableException;
import com.nike.backstopper.service.ClientDataValidationService;

import org.junit.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the Spring MVC exception handling chain to verify that the various exception types are turned into the correct response JSON objects
 * with the correct clientfacing messages and returned with the correct http status codes.
 *
 * @author Nic Munroe
 */
public class ClientfacingErrorITest extends BaseSpringEnabledValidationTestCase {

    @Inject
    private ProjectApiErrors projectApiErrors;

    @Test
    public void shouldConvert5xxServerHttpStatusCodeExceptionToOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/clientFacingErrorTestDummy/throw5xxServerHttpStatusCodeException")).andReturn();
        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError(), ServerHttpStatusCodeException.class);
    }

    @Test
    public void shouldConvert4xxServerHttpStatusCodeExceptionsToOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/clientFacingErrorTestDummy/throw4xxServerHttpStatusCodeException")).andReturn();
        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError(), ServerHttpStatusCodeException.class);
    }

    @Test
    public void shouldConvertServerTimeoutExceptionToTEMPORARY_SERVICE_PROBLEM() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/clientFacingErrorTestDummy/throwServerTimeoutException")).andReturn();
        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getTemporaryServiceProblemApiError(), ServerTimeoutException.class);
    }

    @Test
    public void shouldConvertServerUnknownHttpStatusCodeExceptionToOUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/clientFacingErrorTestDummy/throwServerUnknownHttpStatusCodeException")).andReturn();
        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getOusideDependencyReturnedAnUnrecoverableErrorApiError(), ServerUnknownHttpStatusCodeException.class);
    }

    @Test
    public void shouldConvertServerUnreachableExceptionToTEMPORARY_SERVICE_PROBLEM() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/clientFacingErrorTestDummy/throwServerUnreachableException")).andReturn();
        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getTemporaryServiceProblemApiError(), ServerUnreachableException.class);
    }

    @Test
    public void shouldConvertValidationExceptionsAppropriatelyWithMixedErrorTypes() throws Exception {
        // The 4xx error should get overridden by the 5xx error
        List<? extends ApiError> errors = Arrays.asList(projectApiErrors.getMalformedRequestApiError(), projectApiErrors.getTemporaryServiceProblemApiError());
        MvcResult result = this.mockMvc.perform(
                get("/clientFacingErrorTestDummy/throwSpecificValidationExceptions")
                        .content(objectMapper.writeValueAsString(errors))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getTemporaryServiceProblemApiError(), ApiException.class);
    }

    @Test
    public void shouldConvertValidationExceptionsAppropriatelyWithSameErrorTypes() throws Exception {
        // Errors with the same error codes should all show up in the response.
        List<ApiError> errors = Arrays.asList(projectApiErrors.getMalformedRequestApiError(), projectApiErrors.getMissingExpectedContentApiError());
        MvcResult result = this.mockMvc.perform(
                get("/clientFacingErrorTestDummy/throwSpecificValidationExceptions")
                        .content(objectMapper.writeValueAsString(errors))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        verifyErrorResponse(result, projectApiErrors, errors, ApiException.class);
    }

    @Test
    public void shouldReturnWithoutErrorOnValidInputForDummyRequestObject() throws Exception {
        MvcResult result = this.mockMvc.perform(
                post("/clientFacingErrorTestDummy/validateDummyRequestObject")
                        .content(objectMapper.writeValueAsString(new DummyRequestObject("1", "2", "2013-01-01")))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();
        assertThat(result.getResponse().getContentAsString(), notNullValue());
        DummyResponseObject dro = objectMapper.readValue(result.getResponse()
                .getContentAsString(), DummyResponseObject.class);
        assertThat(dro.someField, is("2013-01-01"));
    }

    @Test
    public void shouldTranslateJsr303MethodArgumentNotValidExceptionErrorsOnInvalidInput() throws Exception {
        MvcResult result = this.mockMvc.perform(post("/clientFacingErrorTestDummy/validateDummyRequestObject")
                .content(objectMapper.writeValueAsString(new DummyRequestObject("foo", "bar", "baz")))
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

        verifyErrorResponse(result, projectApiErrors, Arrays.asList(TestCaseValidationSpringConfig.INVALID_COUNT_VALUE,
                                                                    TestCaseValidationSpringConfig.INVALID_OFFSET_VALUE,
                                                                    projectApiErrors.getTypeConversionApiError()),
                MethodArgumentNotValidException.class);
    }

    @Test
    public void shouldReturnWithoutErrorOnValidInputForIntParamsEndpoint() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/clientFacingErrorTestDummy/validateIntParams")
                .param("int1", "2")
                .param("int2", "4")
        ).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getContentAsString(), notNullValue());
        DummyResponseObject dro = objectMapper.readValue(result.getResponse()
                .getContentAsString(), DummyResponseObject.class);
        assertThat(dro.someField, is("2 4"));
    }

    @Test
    public void shouldTranslateJsr303BindExceptionErrorsOnInvalidInput() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/clientFacingErrorTestDummy/validateIntParams")
                .param("int1", "notanint")
                .param("int2", "alsonotanint")
        ).andExpect(status().isBadRequest()).andReturn();

        verifyErrorResponse(result, projectApiErrors, Arrays.asList(TestCaseValidationSpringConfig.INVALID_COUNT_VALUE, TestCaseValidationSpringConfig.INVALID_OFFSET_VALUE), BindException.class);
    }

    @Test
    public void shouldConvertTypeMismatchExceptionToTYPE_CONVERSION_ERROR() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/clientFacingErrorTestDummy/validateRequiredInteger")
                .param("someInt", "notaninteger")
        ).andReturn();

        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getTypeConversionApiError(), TypeMismatchException.class);
    }

    @Test
    public void shouldConvertMissingRequiredQueryParamToMALFORMED_REQUEST() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/clientFacingErrorTestDummy/validateRequiredInteger")).andReturn();

        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getMalformedRequestApiError(), MissingServletRequestParameterException.class);
    }

    @Test
    public void shouldConvertHttpMessageConversionExceptionToMALFORMED_REQUEST() throws Exception {
        MvcResult result = this.mockMvc.perform(post("/clientFacingErrorTestDummy/validateDummyRequestObject")
                .content("{broken json")
                .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

        // The handler does a instanceof HttpMessageConversionException, but this particular JSON error is a HttpMessageNotReadableException
        // (a subclass of HttpMessageConversionException), so that's what we need to expect. It should still get converted to MALFORMED_REQUEST.
        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getMalformedRequestApiError(), HttpMessageNotReadableException.class);
    }

    @Test
    public void shouldConvertBadAcceptHeaderToNO_ACCEPTABLE_REPRESENTATION() throws Exception {
        MvcResult result = this.mockMvc.perform(post("/clientFacingErrorTestDummy/validateDummyRequestObject")
                .content(objectMapper.writeValueAsString(new DummyRequestObject("2", "2", "2014-01-01")))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_XML)
        ).andReturn();

        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getNoAcceptableRepresentationApiError(),
                HttpMediaTypeNotAcceptableException.class);
    }

    @Test
    public void shouldConvertInvalidContentTypeHeaderToUNSUPPORTED_MEDIA_TYPE() throws Exception {
        MvcResult result = this.mockMvc.perform(post("/clientFacingErrorTestDummy/validateDummyRequestObject")
                .content(objectMapper.writeValueAsString(new DummyRequestObject("2", "2", "2014-01-01")))
                .contentType(MediaType.APPLICATION_XML)
        ).andReturn();

        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getUnsupportedMediaTypeApiError(),
                HttpMediaTypeNotSupportedException.class);
    }

    @Test
    public void shouldConvertMissingContentTypeHeaderToUNSUPPORTED_MEDIA_TYPE() throws Exception {
        MvcResult result = this.mockMvc.perform(post("/clientFacingErrorTestDummy/validateDummyRequestObject")
                .content(objectMapper.writeValueAsString(new DummyRequestObject("2", "2", "2014-01-01")))
        ).andReturn();

        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getUnsupportedMediaTypeApiError(),
                HttpMediaTypeNotSupportedException.class);
    }

    @Test
    public void shouldConvert429ServerHttpStatusCodeExceptionToTOO_MANY_REQUESTS() throws Exception {
        MvcResult result = this.mockMvc.perform(post("/clientFacingErrorTestDummy/throwTooManyRequestsException")).andReturn();

        verifyErrorResponse(result, projectApiErrors, projectApiErrors.getTooManyRequestsApiError(), ServerHttpStatusCodeException.class);
    }

    @Test
    public void shouldTranslateManualClientValidationErrorsOnInvalidInput() throws Exception {
        MvcResult result = this.mockMvc.perform(post("/clientFacingErrorTestDummy/validateDummyRequestObjectWithManualValidation")
                        .content(objectMapper.writeValueAsString(new DummyRequestObject("foo", "bar", "baz")))
                        .contentType(MediaType.APPLICATION_JSON)
        ).andReturn();

        verifyErrorResponse(result, projectApiErrors, Arrays.asList(TestCaseValidationSpringConfig.INVALID_COUNT_VALUE,
                                                                    TestCaseValidationSpringConfig.INVALID_OFFSET_VALUE,
                                                                    projectApiErrors.getTypeConversionApiError()),
                ClientDataValidationError.class);
    }

    private static class DummyRequestObject implements Serializable {
        @Min(value=1, message="INVALID_COUNT_VALUE")
        public String count;
        @Min(value=1, message="INVALID_OFFSET_VALUE")
        public String offset;
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message="TYPE_CONVERSION_ERROR")
        public String startDate;

        public DummyRequestObject() {}
        public DummyRequestObject(String count, String offset, String startDate) {
            this.count = count;
            this.offset = offset;
            this.startDate = startDate;
        }
    }

    private static class DummyResponseObject implements Serializable {
        public String someField = "foo";

        public DummyResponseObject() {}

        public DummyResponseObject(String someField) {
            this.someField = someField;
        }
    }

    @Controller
    @RequestMapping("/clientFacingErrorTestDummy")
    public static class DummyController {

        @Inject
        private ClientDataValidationService clientDataValidationService;

        private static class IntParams {
            @Min(value=1, message="INVALID_COUNT_VALUE")
            public String int1;
            @Min(value=1, message="INVALID_OFFSET_VALUE")
            public String int2;

            public IntParams() {}

            public IntParams(String int1, String int2) {
                this.int1 = int1;
                this.int2 = int2;
            }
        }

        @ModelAttribute("intParams")
        public IntParams resolveIntParameters(@RequestParam(value = "int1", required = false) String int1,
                                                    @RequestParam(value = "int2", required = false) String int2) {
            return new IntParams(int1, int2);
        }

        @RequestMapping("/throw5xxServerHttpStatusCodeException")
        public void throw5xxServerHttpStatusCodeException() {
            HttpServerErrorException serverResponseEx = new HttpServerErrorException(HttpStatus.NOT_IMPLEMENTED);
            throw new ServerHttpStatusCodeException(new Exception("Intentional test exception"), "FOO", serverResponseEx, serverResponseEx.getStatusCode().value(), serverResponseEx.getResponseHeaders(), serverResponseEx.getResponseBodyAsString());
        }

        @RequestMapping("/throw4xxServerHttpStatusCodeException")
        public void throw4xxServerHttpStatusCodeException() {
            HttpClientErrorException serverResponseEx = new HttpClientErrorException(HttpStatus.FAILED_DEPENDENCY, "ignoreme", responseBodyForDownstreamServiceError(), null);
            throw new ServerHttpStatusCodeException(new Exception("Intentional test exception"), "FOO", serverResponseEx, serverResponseEx.getStatusCode().value(), serverResponseEx.getResponseHeaders(), serverResponseEx.getResponseBodyAsString());
        }

        private byte[] responseBodyForDownstreamServiceError() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"result\":\"failure\",\"errorCode\":\"0x00000042\",\"errorMessage\":\"something bad happened\"}");

            return sb.toString().getBytes();
        }

        @RequestMapping("/throwServerTimeoutException")
        public void throwServerTimeoutException() {
            throw new ServerTimeoutException(new Exception("Intentional test exception"), "FOO");
        }

        @RequestMapping("/throwServerUnknownHttpStatusCodeException")
        public void throwServerUnknownHttpStatusCodeException() {
            UnknownHttpStatusCodeException serverResponseEx = new UnknownHttpStatusCodeException(42, null, null, null, null);
            throw new ServerUnknownHttpStatusCodeException(new Exception("Intentional test exception"), "FOO", serverResponseEx, serverResponseEx.getRawStatusCode(), serverResponseEx.getResponseHeaders(), serverResponseEx.getResponseBodyAsString());
        }

        @RequestMapping("/throwServerUnreachableException")
        public void throwServerUnreachableException() {
            throw new ServerUnreachableException(new Exception("Intentional test exception"), "FOO");
        }

        @RequestMapping("/throwSpecificValidationExceptions")
        public void throwSpecificValidationExceptions(@RequestBody List<BarebonesCoreApiErrorForTesting> errorsToThrow) {
            throw ApiException.newBuilder().withApiErrors(new ArrayList<ApiError>(errorsToThrow)).build();
        }

        @RequestMapping("/throwTooManyRequestsException")
        public void throwTooManyRequestsException() {
            HttpServerErrorException serverResponseEx = new HttpServerErrorException(HttpStatus.TOO_MANY_REQUESTS);
            throw new ServerHttpStatusCodeException(new Exception("Intentional test exception"), "FOO", serverResponseEx, serverResponseEx.getStatusCode().value(), serverResponseEx.getResponseHeaders(), serverResponseEx.getResponseBodyAsString());
        }

        @RequestMapping("/validateIntParams")
        @ResponseBody
        public DummyResponseObject validateListParams(@ModelAttribute @Valid IntParams intParams) {
            return new DummyResponseObject(intParams.int1 + " " + intParams.int2);
        }

        @RequestMapping(value = "/validateDummyRequestObject", method = RequestMethod.POST)
        @ResponseBody
        public DummyResponseObject validateDummyRequestObject(@RequestBody @Valid DummyRequestObject dummyRequestObject) {
            return new DummyResponseObject(dummyRequestObject.startDate);
        }

        @RequestMapping("/validateRequiredInteger")
        @ResponseBody
        public DummyResponseObject validateRequiredInteger(@RequestParam(required = true) Integer someInt) {
            return new DummyResponseObject(String.valueOf(someInt));
        }

        @RequestMapping(value = "/validateDummyRequestObjectWithManualValidation", method = RequestMethod.POST)
        @ResponseBody
        public DummyResponseObject validateDummyRequestObjectWithManualValidation(@RequestBody DummyRequestObject dummyRequestObject) {
            clientDataValidationService.validateObjectsFailFast(dummyRequestObject);
            return new DummyResponseObject(dummyRequestObject.startDate);
        }
    }
}
