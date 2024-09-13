package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.SortedApiErrorSet;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.exception.StackTraceLoggingBehavior;
import com.nike.backstopper.exception.WrapperException;
import com.nike.backstopper.exception.network.ServerTimeoutException;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.listener.impl.DownstreamNetworkExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.GenericApiExceptionHandlerListener;
import com.nike.backstopper.handler.listener.impl.ServersideValidationErrorHandlerListener;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.DefaultErrorDTO;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.nike.backstopper.apierror.SortedApiErrorSet.singletonSortedSetOf;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link com.nike.backstopper.handler.ApiExceptionHandlerBase}
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class ApiExceptionHandlerBaseTest {
    private ApiExceptionHandlerBase<TestDTO> handler;
    @Mock
    private RequestInfoForLogging reqMock;

    @Before
    public void setupMethod() {
        handler = new TestApiExceptionHandler();
        //noinspection resource
        MockitoAnnotations.openMocks(this);
    }

    private boolean containsApiError(Collection<DefaultErrorDTO> errorViews, ApiError error) {
        for (DefaultErrorDTO iev : errorViews) {
            if (iev.code.equals(error.getErrorCode()) && iev.message.equals(error.getMessage()))
                return true;
        }
        return false;
    }

    private void validateResponse(ErrorResponseInfo<TestDTO> result, Collection<ApiError> expectedErrors) {
        assertThat(result, notNullValue());

        int expectedHttpStatusCode = expectedErrors.iterator().next().getHttpStatusCode();
        for (ApiError error : expectedErrors) {
            assertThat(error.getHttpStatusCode(), is(expectedHttpStatusCode));
        }
        assertThat(result.httpStatusCode, is(expectedHttpStatusCode));

        assertNotNull(result.frameworkRepresentationObj);
        assertNotNull(result.frameworkRepresentationObj.erv);

        assertNotNull(result.frameworkRepresentationObj.erv.error_id);
        assertNotNull(result.frameworkRepresentationObj.erv.errors);

        assertThat(result.frameworkRepresentationObj.erv.errors.size(), is(expectedErrors.size()));
        for(ApiError error : expectedErrors) {
            assertTrue(containsApiError(result.frameworkRepresentationObj.erv.errors, error));
        }
    }

    private List<ApiError> findAllApiErrorsWithHttpStatusCode(int httpStatusCode) {
        List<ApiError> returnList = new ArrayList<>();
        for (ApiError error : testProjectApiErrors.getProjectApiErrors()) {
            if (error.getHttpStatusCode() == httpStatusCode)
                returnList.add(error);
        }
        return returnList;
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_throws_IllegalArgumentException_if_passed_null_projectApiErrors() {
        // expect
        new ApiExceptionHandlerBase<>(
            null, singletonList(new GenericApiExceptionHandlerListener()), ApiExceptionHandlerUtils.DEFAULT_IMPL
        ) {
            @Override
            protected Object prepareFrameworkRepresentation(
                DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                Throwable originalException, RequestInfoForLogging request
            ) {
                return null;
            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_throws_IllegalArgumentException_if_passed_null_listener_list() {
        // expect
        new ApiExceptionHandlerBase<>(
            mock(ProjectApiErrors.class), null, ApiExceptionHandlerUtils.DEFAULT_IMPL
        ) {
            @Override
            protected Object prepareFrameworkRepresentation(
                DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                Throwable originalException, RequestInfoForLogging request) {
                return null;
            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_throws_IllegalArgumentException_if_passed_null_apiExceptionHandlerUtils() {
        // expect
        new ApiExceptionHandlerBase<>(
            mock(ProjectApiErrors.class), singletonList(new GenericApiExceptionHandlerListener()), null
        ) {
            @Override
            protected Object prepareFrameworkRepresentation(
                DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                Throwable originalException, RequestInfoForLogging request) {
                return null;
            }
        };
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void maybeHandleException_should_call_doHandleApiException_with_results_of_shouldHandleApiException_and_return_result_of_doHandleApiException(
        boolean shouldHandle
    ) throws UnexpectedMajorExceptionHandlingError {
        // given
        Throwable ex = new RuntimeException("kaboom");
        ApiExceptionHandlerBase<?> handlerSpy = spy(new TestApiExceptionHandler());

        ApiExceptionHandlerListenerResult listenerResultMock =
            (shouldHandle)
            ? ApiExceptionHandlerListenerResult.handleResponse(
                singletonSortedSetOf(mock(ApiError.class)),
                singletonList(Pair.of("foo", UUID.randomUUID().toString())),
                singletonList(Pair.of("bar", singletonList(UUID.randomUUID().toString())))
              )
            : ApiExceptionHandlerListenerResult.ignoreResponse();
        doReturn(listenerResultMock).when(handlerSpy).shouldHandleApiException(ex);

        ErrorResponseInfo<?> errorResponseInfoMock = mock(ErrorResponseInfo.class);
        doReturn(errorResponseInfoMock).when(handlerSpy).doHandleApiException(
            any(SortedApiErrorSet.class), any(), any(), any(Throwable.class),
            any(RequestInfoForLogging.class)
        );

        // when
        ErrorResponseInfo<?> result = handlerSpy.maybeHandleException(ex, reqMock);

        // then
        if (shouldHandle) {
            verify(handlerSpy).doHandleApiException(listenerResultMock.errors, listenerResultMock.extraDetailsForLogging,
                                                    listenerResultMock.extraResponseHeaders, ex, reqMock);
            Assertions.assertThat(result).isSameAs(errorResponseInfoMock);
        }
        else {
            Assertions.assertThat(result).isNull();
        }
    }

    @Test
    public void verifyErrorIdIsValidUuid() throws UnexpectedMajorExceptionHandlingError {
        ErrorResponseInfo<TestDTO> result = handler.maybeHandleException(new ApiException(testProjectApiErrors.getGenericServiceError()), reqMock);
        assertNotNull(result.frameworkRepresentationObj.erv.error_id);
        UUID errorUuid = UUID.fromString(result.frameworkRepresentationObj.erv.error_id);
        assertThat(errorUuid, notNullValue());
    }

    @Test
    public void verifyErrorIdIsValidUuidEvenIfRequestContainsTraceIdHeader() throws UnexpectedMajorExceptionHandlingError {
        // This is a change to previous behavior - we used to use trace ID if available but based on API principles change we now do a UUID at all times
        when(reqMock.getHeader(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn("some_dtrace_id");
        ErrorResponseInfo<TestDTO> result = handler.maybeHandleException(new ApiException(testProjectApiErrors.getGenericServiceError()), reqMock);
        assertNotNull(result.frameworkRepresentationObj.erv.error_id);
        UUID errorUuid = UUID.fromString(result.frameworkRepresentationObj.erv.error_id);
        assertThat(errorUuid, notNullValue());
    }

    @Test
    public void verifyErrorIdIsValidUuidEvenIfRequestDoesNotContainTraceIdHeaderButDoesContainTraceIdAttribute() throws UnexpectedMajorExceptionHandlingError {
        // This is a change to previous behavior - we used to use trace ID if available but based on API principles change we now do a UUID at all times
        when(reqMock.getAttribute(ApiExceptionHandlerUtils.DEFAULT_DISTRIBUTED_TRACE_ID_HEADER_KEY)).thenReturn("some_dtrace_id");
        ErrorResponseInfo<TestDTO> result = handler.maybeHandleException(new ApiException(testProjectApiErrors.getGenericServiceError()), reqMock);
        assertNotNull(result.frameworkRepresentationObj.erv.error_id);
        UUID errorUuid = UUID.fromString(result.frameworkRepresentationObj.erv.error_id);
        assertThat(errorUuid, notNullValue());
    }

    @Test
    public void shouldReturnAllErrorsWhenAllErrorsAreSameHttpStatusCode() throws UnexpectedMajorExceptionHandlingError {
        for (Integer httpStatusCode : testProjectApiErrors.getStatusCodePriorityOrder()) {
            List<ApiError> allErrorsForThisHttpStatusCode = findAllApiErrorsWithHttpStatusCode(httpStatusCode);
            // Skip if we have no errors for a status code in the default order list of status codes:
            if (!allErrorsForThisHttpStatusCode.isEmpty()) {
                ErrorResponseInfo<TestDTO> result = handler.maybeHandleException(ApiException.newBuilder().withApiErrors(allErrorsForThisHttpStatusCode).build(), reqMock);
                validateResponse(result, allErrorsForThisHttpStatusCode);
            }
        }
    }

    @Test
    public void shouldFilterOutLowerPriorityErrorsWhenGivenErrorsWithMixedHttpStatusCodes() throws UnexpectedMajorExceptionHandlingError {
        for (int i = 0; i < testProjectApiErrors.getStatusCodePriorityOrder().size(); i++) {
            for (int z = 0; z < testProjectApiErrors.getStatusCodePriorityOrder().size(); z++) {
                if (i != z) {
                    int firstHttpStatusCode = testProjectApiErrors.getStatusCodePriorityOrder().get(i);
                    int secondHttpStatusCode = testProjectApiErrors.getStatusCodePriorityOrder().get(z);
                    List<ApiError> errorsWithFirstHttpStatusCode = findAllApiErrorsWithHttpStatusCode(firstHttpStatusCode);
                    List<ApiError> errorsWithSecondHttpStatusCode = findAllApiErrorsWithHttpStatusCode(secondHttpStatusCode);

                    List<ApiError> combinedErrors = new ArrayList<>(errorsWithFirstHttpStatusCode);
                    combinedErrors.addAll(errorsWithSecondHttpStatusCode);

                    ErrorResponseInfo<TestDTO> result = handler.maybeHandleException(ApiException.newBuilder().withApiErrors(combinedErrors).build(), reqMock);
                    int higherPriorityStatusCode = testProjectApiErrors.determineHighestPriorityHttpStatusCode(combinedErrors);
                    if (higherPriorityStatusCode == firstHttpStatusCode)
                        validateResponse(result, errorsWithFirstHttpStatusCode);
                    else
                        validateResponse(result, errorsWithSecondHttpStatusCode);
                }
            }
        }
    }

    @Test
    public void shouldDeduplicateRepeatedErrors() throws UnexpectedMajorExceptionHandlingError {
        List<ApiError> repeatedErrors = Arrays.asList(testProjectApiErrors.getGenericServiceError(), testProjectApiErrors.getGenericServiceError(), testProjectApiErrors.getGenericServiceError());
        ErrorResponseInfo<TestDTO> result = handler.maybeHandleException(ApiException.newBuilder().withApiErrors(repeatedErrors).build(), reqMock);
        validateResponse(result, singletonList(testProjectApiErrors.getGenericServiceError()));
    }

    @Test
    public void shouldIgnoreExceptionsNotCoveredByAnyHandlerListeners() throws UnexpectedMajorExceptionHandlingError {
        ErrorResponseInfo<TestDTO> result = handler.maybeHandleException(new Exception(), reqMock);
        assertThat(result, nullValue());
    }

    @Test(expected = UnexpectedMajorExceptionHandlingError.class)
    public void shouldThrowUnexpectedMajorExceptionHandlingErrorIfBizarroInnerExceptionOccurs() throws UnexpectedMajorExceptionHandlingError {
        handler.maybeHandleException(new ApiException(testProjectApiErrors.getGenericServiceError()) {
            @Override
            public List<ApiError> getApiErrors() {
                throw new RuntimeException("Bizarro inner exception");
            }
        }, reqMock);
    }

    @Test
    public void handleExceptionShouldAddConnectionTypeToLoggingDetailsWhenPassedANetworkException() {
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        handler.doHandleApiException(singletonSortedSetOf(CUSTOM_API_ERROR), extraDetailsForLogging,
                                     null, new ServerTimeoutException(null, "FOO"),
                                     reqMock);
        assertThat(extraDetailsForLogging.contains(Pair.of("connection_type", "FOO")), is(true));
    }

    @Test
    public void handleExceptionShouldGracefullyHandleNullConnectionTypeWhenPassedANetworkException() {
        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        handler.doHandleApiException(singletonSortedSetOf(CUSTOM_API_ERROR), extraDetailsForLogging,
                                     null, new ServerTimeoutException(null, null), reqMock);
        assertThat(extraDetailsForLogging.contains(Pair.of("connection_type", (String) null)), is(true));
    }

    @Test
    public void handleExceptionShouldUseGenericServiceErrorIfProjectApiErrorsDotGetSublistContainingOnlyHttpStatusCodeSomehowReturnsNull() {
        ProjectApiErrors mockProjectApiErrors = mock(ProjectApiErrors.class);
        doReturn(42).when(mockProjectApiErrors).determineHighestPriorityHttpStatusCode(anyCollection());
        doReturn(null).when(mockProjectApiErrors).getSublistContainingOnlyHttpStatusCode(anyCollection(), anyInt());
        doReturn(BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR).when(mockProjectApiErrors).getGenericServiceError();

        ApiExceptionHandlerBase<TestDTO> handler = spy(new TestApiExceptionHandler(mockProjectApiErrors));

        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        ErrorResponseInfo<TestDTO> testDto = handler.doHandleApiException(singletonSortedSetOf(CUSTOM_API_ERROR), extraDetailsForLogging,
                                                                          null, new Exception(), reqMock);
        assertThat(testDto.frameworkRepresentationObj.erv.errors.size(), is(1));
        assertThat(testDto.frameworkRepresentationObj.erv.errors.get(0).code, is(mockProjectApiErrors.getGenericServiceError().getErrorCode()));
    }

    @Test
    public void handleExceptionShouldUseGenericServiceErrorIfProjectApiErrorsDotGetSublistContainingOnlyHttpStatusCodeSomehowReturnsEmptyList() {
        ProjectApiErrors mockProjectApiErrors = mock(ProjectApiErrors.class);
        doReturn(42).when(mockProjectApiErrors).determineHighestPriorityHttpStatusCode(anyCollection());
        doReturn(Collections.emptyList()).when(mockProjectApiErrors).getSublistContainingOnlyHttpStatusCode(anyCollection(), anyInt());
        doReturn(BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR).when(mockProjectApiErrors).getGenericServiceError();

        ApiExceptionHandlerBase<TestDTO> handler = spy(new TestApiExceptionHandler(mockProjectApiErrors));

        List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        ErrorResponseInfo<TestDTO> testDto = handler.doHandleApiException(singletonSortedSetOf(CUSTOM_API_ERROR), extraDetailsForLogging,
                                                                          null, new Exception(), reqMock);
        assertThat(testDto.frameworkRepresentationObj.erv.errors.size(), is(1));
        assertThat(testDto.frameworkRepresentationObj.erv.errors.get(0).code, is(mockProjectApiErrors.getGenericServiceError().getErrorCode()));
    }

    @Test
    public void handleExceptionShouldAddErrorIdToResponseHeader() {
        ApiExceptionHandlerBase<TestDTO> handler = new TestApiExceptionHandler();
        ErrorResponseInfo<TestDTO> result = handler.doHandleApiException(singletonSortedSetOf(CUSTOM_API_ERROR),
                                                                         new ArrayList<>(),
                                                                         null, new Exception(), reqMock);
        assertThat(result.headersToAddToResponse.get("error_uid"), is(
            singletonList(result.frameworkRepresentationObj.erv.error_id)));
    }

    @Test
    public void doHandleApiException_should_add_headers_from_extraHeadersForResponse_to_ErrorResponseInfo() {
        // given
        final Map<String, List<String>> baseExtraHeaders = MapBuilder
            .builder("foo", singletonList(UUID.randomUUID().toString()))
            .put("bar", Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
            .build();
        ApiExceptionHandlerBase<TestDTO> handler = new TestApiExceptionHandler() {
            @Override
            protected Map<String, List<String>> extraHeadersForResponse(TestDTO frameworkRepresentation, DefaultErrorContractDTO errorContractDTO,
                                                                        int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                                                                        Throwable originalException,
                                                                        RequestInfoForLogging request) {
                return baseExtraHeaders;
            }
        };

        // when
        ErrorResponseInfo<TestDTO> result = handler.doHandleApiException(singletonSortedSetOf(CUSTOM_API_ERROR),
                                                                         new ArrayList<>(),
                                                                         null, new Exception(), reqMock);

        // then
        Map<String, List<String>> expectedExtraHeaders = new HashMap<>(baseExtraHeaders);
        expectedExtraHeaders.put("error_uid", singletonList(result.frameworkRepresentationObj.erv.error_id));
        Assertions.assertThat(result.headersToAddToResponse).isEqualTo(expectedExtraHeaders);
    }

    @Test
    public void doHandleApiException_should_add_headers_from_passed_in_extra_headers_to_ErrorResponseInfo() {
        // given
        final List<Pair<String, List<String>>> extraHeadersArg = Arrays.asList(
            Pair.of("foo", singletonList(UUID.randomUUID().toString())),
            Pair.of("bar", Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
        );
        ApiExceptionHandlerBase<TestDTO> handler = new TestApiExceptionHandler();

        // when
        ErrorResponseInfo<TestDTO> result = handler.doHandleApiException(
            singletonSortedSetOf(CUSTOM_API_ERROR), new ArrayList<>(), extraHeadersArg,
            new Exception(), reqMock
        );

        // then
        Map<String, List<String>> expectedExtraHeaders = new HashMap<>();
        for (Pair<String, List<String>> headerPair : extraHeadersArg) {
            expectedExtraHeaders.put(headerPair.getLeft(), headerPair.getRight());
        }
        expectedExtraHeaders.put("error_uid", singletonList(result.frameworkRepresentationObj.erv.error_id));
        Assertions.assertThat(result.headersToAddToResponse).isEqualTo(expectedExtraHeaders);
    }

    @Test
    public void doHandleApiException_should_let_headers_from_extraHeadersForResponse_method_override_passed_in_extra_headers_arg() {
        // given
        List<String> fooHeaderValueFromExtraHeadersMethod = singletonList(UUID.randomUUID().toString());
        List<String> barHeaderValueFromExtraHeadersArg = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        List<String> thirdThingHeaderValueFromExtraHeadersMethod = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        final List<Pair<String, List<String>>> extraHeadersArg = Arrays.asList(
            Pair.of("foo", singletonList(UUID.randomUUID().toString())),
            Pair.of("bar", barHeaderValueFromExtraHeadersArg)
        );
        final Map<String, List<String>> extraHeadersFromMethod = MapBuilder
            .builder("foo", fooHeaderValueFromExtraHeadersMethod)
            .put("thirdThing", thirdThingHeaderValueFromExtraHeadersMethod)
            .build();
        ApiExceptionHandlerBase<TestDTO> handler = new TestApiExceptionHandler() {
            @Override
            protected Map<String, List<String>> extraHeadersForResponse(TestDTO frameworkRepresentation, DefaultErrorContractDTO errorContractDTO,
                                                                        int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                                                                        Throwable originalException,
                                                                        RequestInfoForLogging request) {
                return extraHeadersFromMethod;
            }
        };

        // when
        ErrorResponseInfo<TestDTO> result = handler.doHandleApiException(
            singletonSortedSetOf(CUSTOM_API_ERROR), new ArrayList<>(), extraHeadersArg,
            new Exception(), reqMock
        );

        // then
        Map<String, List<String>> expectedExtraHeaders = new HashMap<>();
        expectedExtraHeaders.put("foo", fooHeaderValueFromExtraHeadersMethod);
        expectedExtraHeaders.put("bar", barHeaderValueFromExtraHeadersArg);
        expectedExtraHeaders.put("thirdThing", thirdThingHeaderValueFromExtraHeadersMethod);
        expectedExtraHeaders.put("error_uid", singletonList(result.frameworkRepresentationObj.erv.error_id));
        Assertions.assertThat(result.headersToAddToResponse).isEqualTo(expectedExtraHeaders);
    }

    @Test
    public void doHandleApiException_should_not_allow_error_uid_from_extraHeadersForResponse_to_override_true_error_uid() {
        // given
        final Map<String, List<String>> baseExtraHeaders = MapBuilder
            .builder("error_uid", singletonList(UUID.randomUUID().toString()))
            .build();
        ApiExceptionHandlerBase<TestDTO> handler = new TestApiExceptionHandler() {
            @Override
            protected Map<String, List<String>> extraHeadersForResponse(TestDTO frameworkRepresentation, DefaultErrorContractDTO errorContractDTO,
                                                                        int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                                                                        Throwable originalException,
                                                                        RequestInfoForLogging request) {
                return baseExtraHeaders;
            }
        };

        // when
        ErrorResponseInfo<TestDTO> result = handler.doHandleApiException(singletonSortedSetOf(CUSTOM_API_ERROR),
                                                                         new ArrayList<>(),
                                                                         null, new Exception(), reqMock);

        // then
        Assertions.assertThat(result.headersToAddToResponse.get("error_uid"))
                  .isNotEqualTo(baseExtraHeaders.get("error_uid"))
                  .isEqualTo(singletonList(result.frameworkRepresentationObj.erv.error_id));
    }

    @Test
    public void doHandleApiException_should_not_allow_error_uid_from_passed_in_extra_headers_to_override_true_error_uid() {
        // given
        final List<Pair<String, List<String>>> extraHeadersArg = singletonList(
            Pair.of("error_uid", singletonList(UUID.randomUUID().toString()))
        );
        ApiExceptionHandlerBase<TestDTO> handler = new TestApiExceptionHandler();

        // when
        ErrorResponseInfo<TestDTO> result = handler.doHandleApiException(
            singletonSortedSetOf(CUSTOM_API_ERROR), new ArrayList<>(), extraHeadersArg,
            new Exception(), reqMock
        );

        // then
        Assertions.assertThat(result.headersToAddToResponse.get("error_uid"))
                  .isNotEqualTo(extraHeadersArg.get(0).getRight())
                  .isEqualTo(singletonList(result.frameworkRepresentationObj.erv.error_id));
    }

    @DataProvider(value = {
        "399    |   true",
        "400    |   false",
        "499    |   false",
        "500    |   true",
        "600    |   true",
    }, splitBy = "\\|")
    @Test
    public void shouldLogStackTrace_has_expected_default_behavior(int statusCode, boolean expectedResult) {
        // given
        @SuppressWarnings("unchecked")
        Collection<ApiError> errorsCollectionMock = mock(Collection.class);
        Throwable originalExceptionMock = mock(Throwable.class);
        Throwable coreExceptionMock = mock(Throwable.class);

        // when
        boolean result = handler.shouldLogStackTrace(
            statusCode, errorsCollectionMock, originalExceptionMock, coreExceptionMock, reqMock
        );

        // then
        Assertions.assertThat(result).isEqualTo(expectedResult);
        verifyNoMoreInteractions(errorsCollectionMock, originalExceptionMock, coreExceptionMock, reqMock);
    }

    @DataProvider(value = {
        // DEFER_TO_DEFAULT_BEHAVIOR follows default behavior of using status code to determine the result.
        "399    |   DEFER_TO_DEFAULT_BEHAVIOR   |   true",
        "400    |   DEFER_TO_DEFAULT_BEHAVIOR   |   false",
        "499    |   DEFER_TO_DEFAULT_BEHAVIOR   |   false",
        "500    |   DEFER_TO_DEFAULT_BEHAVIOR   |   true",
        "600    |   DEFER_TO_DEFAULT_BEHAVIOR   |   true",

        // FORCE_STACK_TRACE forces a true result no matter what the status code is.
        "399    |   FORCE_STACK_TRACE           |   true",
        "400    |   FORCE_STACK_TRACE           |   true",
        "499    |   FORCE_STACK_TRACE           |   true",
        "500    |   FORCE_STACK_TRACE           |   true",
        "600    |   FORCE_STACK_TRACE           |   true",

        // FORCE_NO_STACK_TRACE forces a false result no matter what the status code is.
        "399    |   FORCE_NO_STACK_TRACE        |   false",
        "400    |   FORCE_NO_STACK_TRACE        |   false",
        "499    |   FORCE_NO_STACK_TRACE        |   false",
        "500    |   FORCE_NO_STACK_TRACE        |   false",
        "600    |   FORCE_NO_STACK_TRACE        |   false",
    }, splitBy = "\\|")
    @Test
    public void shouldLogStackTrace_honors_ApiException_with_StackTraceLoggingBehavior(
        int statusCode, StackTraceLoggingBehavior stackTraceLoggingBehavior, boolean expectedResult
    ) {
        // given
        @SuppressWarnings("unchecked")
        Collection<ApiError> errorsCollectionMock = mock(Collection.class);
        Throwable originalExceptionMock = mock(Throwable.class);
        ApiException coreException = ApiException
            .newBuilder()
            .withApiErrors(mock(ApiError.class))
            .withStackTraceLoggingBehavior(stackTraceLoggingBehavior)
            .build();

        // when
        boolean result = handler.shouldLogStackTrace(
            statusCode, errorsCollectionMock, originalExceptionMock, coreException, reqMock
        );

        // then
        Assertions.assertThat(result).isEqualTo(expectedResult);
        verifyNoMoreInteractions(errorsCollectionMock, originalExceptionMock, reqMock);
    }

    // DEFAULT_WRAPPER_EXCEPTION_CLASS_NAMES should contain at least WrapperException, ExecutionException, CompletionException
    @DataProvider(value = {
            "com.nike.backstopper.exception.WrapperException",
            "java.util.concurrent.ExecutionException",
            "java.util.concurrent.CompletionException"
    }, splitBy = "\\|")
    @Test
    public void DEFAULT_WRAPPER_EXCEPTION_CLASS_NAMES_contains_important_values(String expectedClassName) {
        // expect
        assertThat(handler.DEFAULT_WRAPPER_EXCEPTION_CLASS_NAMES.contains(expectedClassName), is(true));
    }

    @Test
    public void getWrapperExceptionClassNames_returns_DEFAULT_WRAPPER_EXCEPTION_CLASS_NAMES() {
        // expect
        assertThat(handler.getWrapperExceptionClassNames(), sameInstance(handler.DEFAULT_WRAPPER_EXCEPTION_CLASS_NAMES));
    }

    @Test
    public void unwrapAndFindCoreException_returns_null_if_passed_null() {
        // expect
        assertThat(handler.unwrapAndFindCoreException(null), nullValue());
    }

    @Test
    public void unwrapAndFindCoreException_returns_passed_in_arg_if_exception_has_no_cause() {
        // given
        Throwable noCause = new Exception("boom");

        // expect
        assertThat(handler.unwrapAndFindCoreException(noCause), sameInstance(noCause));
    }

    @Test
    public void unwrapAndFindCoreException_returns_passed_in_arg_if_cause_is_infinite_loop() {
        // given
        Throwable error = mock(WrapperException.class);
        doReturn(error).when(error).getCause();

        // expect
        assertThat(handler.unwrapAndFindCoreException(error), sameInstance(error));
    }

    @Test
    public void unwrapAndFindCoreException_unwraps_WrapperException() {
        // given
        Throwable underlyingError = new Exception("bang");
        WrapperException wrapperEx = new WrapperException(underlyingError);

        // expect
        assertThat(handler.unwrapAndFindCoreException(wrapperEx), sameInstance(underlyingError));
    }

    @Test
    public void unwrapAndFindCoreException_unwraps_ExecutionException() {
        // given
        Throwable underlyingError = new Exception("bang");
        ExecutionException wrapperEx = new ExecutionException(underlyingError);

        // expect
        assertThat(handler.unwrapAndFindCoreException(wrapperEx), sameInstance(underlyingError));
    }

    @Test
    public void unwrapAndFindCoreException_unwraps_recursively() {
        // given
        Throwable underlyingError = new Exception("bang");
        ExecutionException wrapperEx = new ExecutionException(underlyingError);
        WrapperException doubleWrapperEx = new WrapperException(wrapperEx);

        // expect
        assertThat(handler.unwrapAndFindCoreException(doubleWrapperEx), sameInstance(underlyingError));
    }

    @Test
    public void unwrapAndFindCoreException_returns_passed_in_arg_if_exception_has_cause_but_is_not_unwrappable() {
        // given
        Throwable wrapperEx = new Exception(new Exception("boom"));

        // expect
        assertThat(handler.unwrapAndFindCoreException(wrapperEx), sameInstance(wrapperEx));
    }

    private static class CustomExceptionOfDoom extends Exception { }
    private static final ApiError CUSTOM_API_ERROR = new ApiErrorBase("CUSTOM_API_ERROR", 99042, "some message", 400);
    private static class CustomExceptionOfDoomHandlerListener implements ApiExceptionHandlerListener {

        @Override
        public ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex) {
            if (ex instanceof CustomExceptionOfDoom)
                return ApiExceptionHandlerListenerResult.handleResponse(singletonSortedSetOf(CUSTOM_API_ERROR));

            return ApiExceptionHandlerListenerResult.ignoreResponse();
        }

    }

    private static class UnknownException extends Exception { }

    @Test
    public void shouldIgnoreUnknownExceptionTypes() throws UnexpectedMajorExceptionHandlingError {
        assertThat(handler.maybeHandleException(new UnknownException(), reqMock), nullValue());
    }

    @Test
    public void shouldUseCustomHandlerListenersIfSet() throws UnexpectedMajorExceptionHandlingError {
        ErrorResponseInfo<TestDTO> result = handler.maybeHandleException(new CustomExceptionOfDoom(), reqMock);
        validateResponse(result, singletonList(CUSTOM_API_ERROR));
    }

    private static class TestApiExceptionHandler extends ApiExceptionHandlerBase<TestDTO> {

        private TestApiExceptionHandler(ProjectApiErrors projectApiErrorsToUse) {
            super(projectApiErrorsToUse,
                Arrays.asList(
                    new GenericApiExceptionHandlerListener(),
                    new ServersideValidationErrorHandlerListener(testProjectApiErrors, ApiExceptionHandlerUtils.DEFAULT_IMPL),
                    new DownstreamNetworkExceptionHandlerListener(testProjectApiErrors),
                    new CustomExceptionOfDoomHandlerListener()),
                ApiExceptionHandlerUtils.DEFAULT_IMPL);
        }

        private TestApiExceptionHandler() {
            this(testProjectApiErrors);
        }

        @Override
        protected TestDTO prepareFrameworkRepresentation(DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                                                         Throwable originalException, RequestInfoForLogging request) {
            return new TestDTO(errorContractDTO);
        }
    }

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(
        singletonList(CUSTOM_API_ERROR),
        ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES
    );

    private record TestDTO(DefaultErrorContractDTO erv) {
    }

}
