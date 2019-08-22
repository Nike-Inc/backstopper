package com.nike.backstopper.handler.spring.webflux.listener.impl;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.ApiErrorWithMetadata;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.internal.util.Pair;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.MediaTypeNotSupportedStatusException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link OneOffSpringWebFluxFrameworkExceptionHandlerListener}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class OneOffSpringWebFluxFrameworkExceptionHandlerListenerTest {
    
    private static final ProjectApiErrors testProjectApiErrors =
        ProjectApiErrorsForTesting.withProjectSpecificData(null, null);

    private OneOffSpringWebFluxFrameworkExceptionHandlerListener listener =
        new OneOffSpringWebFluxFrameworkExceptionHandlerListener(
            testProjectApiErrors, ApiExceptionHandlerUtils.DEFAULT_IMPL
        );

    @Test
    public void constructor_sets_projectApiErrors_and_utils_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        OneOffSpringWebFluxFrameworkExceptionHandlerListener
            impl = new OneOffSpringWebFluxFrameworkExceptionHandlerListener(projectErrorsMock, utilsMock);

        // then
        assertThat(Whitebox.getInternalState(impl, "projectApiErrors")).isSameAs(projectErrorsMock);
        assertThat(Whitebox.getInternalState(impl, "utils")).isSameAs(utilsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_projectApiErrors() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffSpringWebFluxFrameworkExceptionHandlerListener(null, ApiExceptionHandlerUtils.DEFAULT_IMPL)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_utils() {
        // when
        Throwable ex = Assertions.catchThrowable(
            () -> new OneOffSpringWebFluxFrameworkExceptionHandlerListener(mock(ProjectApiErrors.class), null)
        );

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    private void validateResponse(
        ApiExceptionHandlerListenerResult result,
        boolean expectedShouldHandle,
        Collection<? extends ApiError> expectedErrors,
        Pair<String, String> ... expectedExtraDetailsForLogging
    ) {
        List<Pair<String, String>> loggingDetailsList = (expectedExtraDetailsForLogging == null)
                                                        ? Collections.emptyList()
                                                        : Arrays.asList(expectedExtraDetailsForLogging);
        validateResponse(
            result, expectedShouldHandle, expectedErrors, loggingDetailsList
        );
    }

    private void validateResponse(
        ApiExceptionHandlerListenerResult result,
        boolean expectedShouldHandle,
        Collection<? extends ApiError> expectedErrors,
        List<Pair<String, String>> expectedExtraDetailsForLogging
    ) {
        if (!expectedShouldHandle) {
            assertThat(result.shouldHandleResponse).isFalse();
            return;
        }

        assertThat(result.errors).containsExactlyInAnyOrderElementsOf(expectedErrors);
        assertThat(result.extraDetailsForLogging).containsExactlyInAnyOrderElementsOf(expectedExtraDetailsForLogging);
    }

    @Test
    public void handleFluxExceptions_returns_ignoreResponse_if_passed_exception_it_does_not_want_to_handle() {
        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(
            new ApiException(testProjectApiErrors.getGenericServiceError())
        );

        // then
        validateResponse(result, false, null);
    }

    private enum TypeMismatchExceptionScenario {
        CONVERSION_NOT_SUPPORTED_500(
            HttpStatus.resolve(500),
            new ConversionNotSupportedException(
                new PropertyChangeEvent("doesNotMatter", "somePropertyName", "oldValue", "newValue"),
                Integer.class,
                null
            ),
            testProjectApiErrors.getGenericServiceError(),
            Arrays.asList(
                Pair.of("bad_property_name", "somePropertyName"),
                Pair.of("bad_property_value", "newValue"),
                Pair.of("required_type", Integer.class.toString())
            )
        ),
        GENERIC_TYPE_MISMATCH_EXCEPTION_400(
            HttpStatus.resolve(400),
            new TypeMismatchException(
                new PropertyChangeEvent("doesNotMatter", "somePropertyName", "oldValue", "newValue"),
                Integer.class
            ),
            new ApiErrorWithMetadata(
                testProjectApiErrors.getTypeConversionApiError(),
                Pair.of("bad_property_name", "somePropertyName"),
                Pair.of("bad_property_value", "newValue"),
                Pair.of("required_type", "int")
            ),
            Arrays.asList(
                Pair.of("bad_property_name", "somePropertyName"),
                Pair.of("bad_property_value", "newValue"),
                Pair.of("required_type", Integer.class.toString())
            )
        ),
        UNEXPECTED_4XX_STATUS_CODE(
            HttpStatus.resolve(403),
            new TypeMismatchException("doesNotMatter", Integer.class),
            testProjectApiErrors.getForbiddenApiError(),
            Collections.emptyList()
        ),
        UNEXPECTED_5XX_STATUS_CODE(
            HttpStatus.resolve(503),
            new TypeMismatchException("doesNotMatter", Integer.class),
            testProjectApiErrors.getTemporaryServiceProblemApiError(),
            Collections.emptyList()
        ),
        UNKNOWN_4XX_STATUS_CODE(
            HttpStatus.resolve(418),
            new TypeMismatchException("doesNotMatter", Integer.class),
            new ApiErrorBase(
                "GENERIC_API_ERROR_FOR_RESPONSE_STATUS_CODE_418",
                testProjectApiErrors.getGenericBadRequestApiError().getErrorCode(),
                "An error occurred that resulted in response status code 418",
                418
            ),
            Collections.emptyList()
        ),
        UNKNOWN_5XX_STATUS_CODE(
            HttpStatus.resolve(509),
            new TypeMismatchException("doesNotMatter", Integer.class),
            new ApiErrorBase(
                "GENERIC_API_ERROR_FOR_RESPONSE_STATUS_CODE_509",
                testProjectApiErrors.getGenericServiceError().getErrorCode(),
                "An error occurred that resulted in response status code 509",
                509
            ),
            Collections.emptyList()
        );

        public final HttpStatus status;
        public final TypeMismatchException tmeCause;
        public final ApiError expectedApiError;
        public final List<Pair<String, String>> expectedExtraDetailsForLogging;

        TypeMismatchExceptionScenario(
            HttpStatus status, TypeMismatchException tmeCause, ApiError expectedApiError,
            List<Pair<String, String>> expectedExtraDetailsForLogging
        ) {
            this.status = status;
            this.tmeCause = tmeCause;
            this.expectedApiError = expectedApiError;
            this.expectedExtraDetailsForLogging = expectedExtraDetailsForLogging;
        }
    }

    @DataProvider
    public static List<List<TypeMismatchExceptionScenario>> typeMismatchExceptionScenarioDataProvider() {
        return Stream.of(TypeMismatchExceptionScenario.values())
                     .map(Collections::singletonList)
                     .collect(Collectors.toList());
    }

    @UseDataProvider("typeMismatchExceptionScenarioDataProvider")
    @Test
    public void handleFluxExceptions_handles_ResponseStatusException_with_TypeMismatchException_cause_as_expected(
        TypeMismatchExceptionScenario scenario
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(
            scenario.status, "Some ResponseStatusException reason", scenario.tmeCause
        );
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );
        expectedExtraDetailsForLogging.addAll(scenario.expectedExtraDetailsForLogging);

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(scenario.expectedApiError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "400    |   MALFORMED_REQUEST",
        "401    |   UNAUTHORIZED"
    }, splitBy = "\\|")
    @Test
    public void handleFluxExceptions_returns_MALFORMED_REQUEST_for_ResponseStatusException_with_DecodingException_cause_only_if_status_is_400(
        int statusCode, BarebonesCoreApiErrorForTesting expectedError
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.resolve(statusCode),
            "Some ResponseStatusException reason",
            new DecodingException("Some decoding ex")
        );
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "400    |   Required foo parameter 'bar' is not present |   foo     |   bar     |   MALFORMED_REQUEST",
        "401    |   Required foo parameter 'bar' is not present |   null    |   null    |   UNAUTHORIZED",
        "400    |   Required parameter 'bar' is not present     |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Required foo parameter is not present       |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Blah foo parameter 'bar' is not present     |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Required foo blah 'bar' is not present      |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Required foo parameter 'bar' is not blah    |   null    |   null    |   GENERIC_BAD_REQUEST",
        "400    |   Some random reason                          |   null    |   null    |   GENERIC_BAD_REQUEST",
    }, splitBy = "\\|")
    @Test
    public void handleFluxExceptions_returns_MALFORMED_REQUEST_for_ResponseStatusException_with_special_required_param_reason_string(
        int statusCode,
        String exReasonString,
        String expectedMissingParamType,
        String expectedMissingParamName,
        BarebonesCoreApiErrorForTesting expectedBaseError
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.resolve(statusCode), exReasonString);
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        ApiError expectedError = expectedBaseError;
        if (expectedMissingParamName != null && expectedMissingParamType != null) {
            expectedError = new ApiErrorWithMetadata(
                expectedBaseError,
                Pair.of("missing_param_name", expectedMissingParamName),
                Pair.of("missing_param_type", expectedMissingParamType)
            );
        }

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "400    |   Request body is missing             |   MISSING_EXPECTED_CONTENT",
        "400    |   Request body is missing blahblah    |   MISSING_EXPECTED_CONTENT",
        "401    |   Request body is missing             |   UNAUTHORIZED",
        "400    |   Request body is                     |   GENERIC_BAD_REQUEST",
        "400    |   Some random reason                  |   GENERIC_BAD_REQUEST",
    }, splitBy = "\\|")
    @Test
    public void handleFluxExceptions_returns_MISSING_EXPECTED_CONTENT_for_ResponseStatusException_with_special_reason_string_beginning(
        int statusCode, String exReasonString, BarebonesCoreApiErrorForTesting expectedError
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.resolve(statusCode), exReasonString);
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "400    |   GENERIC_BAD_REQUEST",
        "401    |   UNAUTHORIZED",
        "403    |   FORBIDDEN",
        "404    |   NOT_FOUND",
        "405    |   METHOD_NOT_ALLOWED",
        "406    |   NO_ACCEPTABLE_REPRESENTATION",
        "415    |   UNSUPPORTED_MEDIA_TYPE",
        "429    |   TOO_MANY_REQUESTS",
        "500    |   GENERIC_SERVICE_ERROR",
        "503    |   TEMPORARY_SERVICE_PROBLEM",
    }, splitBy = "\\|")
    @Test
    public void handleFluxExceptions_handles_generic_ResponseStatusException_by_returning_ApiError_from_project_if_status_code_is_known(
        int desiredStatusCode, BarebonesCoreApiErrorForTesting expectedError
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.resolve(desiredStatusCode), "Some ResponseStatusException reason"
        );
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "418",
        "509"
    })
    @Test
    public void handleFluxExceptions_handles_generic_ResponseStatusException_by_returning_synthetic_ApiError_if_status_code_is_unknown(
        int desiredStatusCode
    ) {
        // given
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.resolve(desiredStatusCode), "Some ResponseStatusException reason"
        );
        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        String expectedErrorCode = (desiredStatusCode >= 500)
                                   ? testProjectApiErrors.getGenericServiceError().getErrorCode()
                                   : testProjectApiErrors.getGenericBadRequestApiError().getErrorCode();

        ApiError expectedError = new ApiErrorBase(
            "GENERIC_API_ERROR_FOR_RESPONSE_STATUS_CODE_" + desiredStatusCode,
            expectedErrorCode,
            "An error occurred that resulted in response status code " + desiredStatusCode,
            desiredStatusCode
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(expectedError),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void handleFluxExceptions_handles_MediaTypeNotSupportedStatusException_as_expected(
        boolean includesSupportedMediaTypes
    ) {
        // given
        List<MediaType> supportedMediaTypes = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.IMAGE_JPEG
        );
        MediaTypeNotSupportedStatusException ex =
            (includesSupportedMediaTypes)
            ? new MediaTypeNotSupportedStatusException(supportedMediaTypes)
            : new MediaTypeNotSupportedStatusException("Some reason");

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        String expectedSupportedMediaTypesValueStr =
            (includesSupportedMediaTypes)
            ? supportedMediaTypes.stream().map(Object::toString).collect(Collectors.joining(","))
            : "";

        expectedExtraDetailsForLogging.add(Pair.of("supported_media_types", expectedSupportedMediaTypesValueStr));

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getUnsupportedMediaTypeApiError()),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void handleFluxExceptions_handles_MethodNotAllowedException_as_expected(
        boolean supportedMethodsIsEmpty
    ) {
        // given
        String actualMethod = UUID.randomUUID().toString();
        List<HttpMethod> supportedMethods =
            (supportedMethodsIsEmpty)
            ? Collections.emptyList()
            : Arrays.asList(
                HttpMethod.GET,
                HttpMethod.POST
            );

        MethodNotAllowedException ex = new MethodNotAllowedException(actualMethod, supportedMethods);

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        // They throw the supported methods into a plain HashSet so we can't rely on the ordering.
        //      Verify it another way.
        Optional<String> supportedMethodsLoggingDetailsValue = result.extraDetailsForLogging
            .stream()
            .filter(p -> p.getKey().equals("supported_methods"))
            .map(Pair::getValue)
            .findAny();
        assertThat(supportedMethodsLoggingDetailsValue).isPresent();
        List<HttpMethod> actualLoggingDetailsMethods = supportedMethodsLoggingDetailsValue
            .map(s -> {
                if (s.equals("")) {
                    return Collections.<HttpMethod>emptyList();
                }
                return Arrays.stream(s.split(",")).map(HttpMethod::valueOf).collect(Collectors.toList());
            })
            .orElse(Collections.emptyList());

        assertThat(actualLoggingDetailsMethods).containsExactlyInAnyOrderElementsOf(supportedMethods);

        expectedExtraDetailsForLogging.add(Pair.of("supported_methods", supportedMethodsLoggingDetailsValue.get()));

        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getMethodNotAllowedApiError()),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void handleFluxExceptions_handles_NotAcceptableStatusException_as_expected(
        boolean includesSupportedMediaTypes
    ) {
        // given
        List<MediaType> supportedMediaTypes = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.IMAGE_JPEG
        );
        NotAcceptableStatusException ex =
            (includesSupportedMediaTypes)
            ? new NotAcceptableStatusException(supportedMediaTypes)
            : new NotAcceptableStatusException("Some reason");

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        String expectedSupportedMediaTypesValueStr =
            (includesSupportedMediaTypes)
            ? supportedMediaTypes.stream().map(Object::toString).collect(Collectors.joining(","))
            : "";

        expectedExtraDetailsForLogging.add(Pair.of("supported_media_types", expectedSupportedMediaTypesValueStr));

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getNoAcceptableRepresentationApiError()),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void handleFluxExceptions_handles_ServerErrorException_as_expected(
        boolean nullDetails
    ) throws NoSuchMethodException {
        // given
        MethodParameter details = new MethodParameter(String.class.getDeclaredMethod("length"), -1);

        ServerErrorException ex =
            (nullDetails)
            ? new ServerErrorException("Some reason", (Throwable) null)
            : new ServerErrorException("Some reason", details, null);

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        expectedExtraDetailsForLogging.add(
            Pair.of("method_parameter", String.valueOf(ex.getMethodParameter()))
        );
        expectedExtraDetailsForLogging.add(
            Pair.of("handler_method", String.valueOf(ex.getHandlerMethod()))
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getGenericServiceError()),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void handleFluxExceptions_handles_ServerWebInputException_as_expected(
        boolean nullDetails
    ) throws NoSuchMethodException {
        // given
        MethodParameter details = new MethodParameter(String.class.getDeclaredMethod("length"), -1);

        ServerWebInputException ex =
            (nullDetails)
            ? new ServerWebInputException("Some reason")
            : new ServerWebInputException("Some reason", details);

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        expectedExtraDetailsForLogging.add(
            Pair.of("method_parameter", String.valueOf(ex.getMethodParameter()))
        );

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getGenericBadRequestApiError()),
            expectedExtraDetailsForLogging
        );
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void handleFluxExceptions_handles_UnsupportedMediaTypeStatusException_as_expected(
        boolean includeDetails
    ) {
        // given
        MediaType actualMediaType = MediaType.TEXT_PLAIN;
        List<MediaType> supportedMediaTypes = Arrays.asList(
            MediaType.APPLICATION_JSON,
            MediaType.IMAGE_JPEG
        );
        ResolvableType javaBodyType = ResolvableType.forClass(Integer.class);
        UnsupportedMediaTypeStatusException ex =
            (includeDetails)
            ? new UnsupportedMediaTypeStatusException(actualMediaType, supportedMediaTypes, javaBodyType)
            : new UnsupportedMediaTypeStatusException("Some reason");

        List<Pair<String, String>> expectedExtraDetailsForLogging = new ArrayList<>();
        ApiExceptionHandlerUtils.DEFAULT_IMPL.addBaseExceptionMessageToExtraDetailsForLogging(
            ex, expectedExtraDetailsForLogging
        );

        String expectedSupportedMediaTypesValueStr =
            (includeDetails)
            ? supportedMediaTypes.stream().map(Object::toString).collect(Collectors.joining(","))
            : "";
        String expectedJavaBodyTypeValueStr =
            (includeDetails)
            ? javaBodyType.toString()
            : "null";

        expectedExtraDetailsForLogging.add(Pair.of("supported_media_types", expectedSupportedMediaTypesValueStr));
        expectedExtraDetailsForLogging.add(Pair.of("java_body_type", expectedJavaBodyTypeValueStr));

        // when
        ApiExceptionHandlerListenerResult result = listener.handleSpringMvcOrWebfluxSpecificFrameworkExceptions(ex);

        // then
        validateResponse(
            result,
            true,
            singleton(testProjectApiErrors.getUnsupportedMediaTypeApiError()),
            expectedExtraDetailsForLogging
        );
    }

    private enum ConcatenateCollectionToStringScenario {
        NULL_COLLECTION(null, ""),
        EMPTY_COLLECTION(Collections.emptyList(), ""),
        SINGLE_ITEM(Collections.singleton("foo"), "foo"),
        MULTIPLE_ITEMS(Arrays.asList("foo", "bar"), "foo,bar");

        public final Collection<String> collection;
        public final String expectedResult;

        ConcatenateCollectionToStringScenario(Collection<String> collection, String expectedResult) {
            this.collection = collection;
            this.expectedResult = expectedResult;
        }
    }

    @DataProvider
    public static List<List<ConcatenateCollectionToStringScenario>> concatenateCollectionToStringScenarioDataProvider() {
        return Stream.of(ConcatenateCollectionToStringScenario.values())
                     .map(Collections::singletonList)
                     .collect(Collectors.toList());
    }

    @UseDataProvider("concatenateCollectionToStringScenarioDataProvider")
    @Test
    public void concatenateCollectionToString_works_as_expected(ConcatenateCollectionToStringScenario scenario) {
        // when
        String result = listener.concatenateCollectionToString(scenario.collection);

        // then
        assertThat(result).isEqualTo(scenario.expectedResult);
    }
}