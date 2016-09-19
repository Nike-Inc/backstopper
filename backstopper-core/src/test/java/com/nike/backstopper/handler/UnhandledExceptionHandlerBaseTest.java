package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.DefaultErrorDTO;
import com.nike.internal.util.MapBuilder;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link com.nike.backstopper.handler.UnhandledExceptionHandlerBase}
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class UnhandledExceptionHandlerBaseTest {

    private UnhandledExceptionHandlerBase<TestDTO> exHandlerSpy;

    private RequestInfoForLogging reqMock;

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private List<ApiError> errorsExpectedToBeUsed = Collections.<ApiError>singletonList(BarebonesCoreApiErrorForTesting.GENERIC_SERVICE_ERROR);
    private int httpStatusCodeExpectedToBeUsed = testProjectApiErrors.determineHighestPriorityHttpStatusCode(errorsExpectedToBeUsed);
    private ApiExceptionHandlerUtils utilsSpy;

    @Before
    public void setupTest() {
        reqMock = mock(RequestInfoForLogging.class);
        utilsSpy = spy(ApiExceptionHandlerUtils.DEFAULT_IMPL);
        exHandlerSpy = spy(new TestUnhandledExceptionHandler(testProjectApiErrors, utilsSpy));
    }

    @Test
    public void handleException_should_delegate_to_ApiExceptionHandlerUtils_for_building_log_message_and_should_log_the_result() throws Exception {
        // given
        Exception exceptionToThrow = new Exception("kaboom");
        Logger loggerMock = mock(Logger.class);
        Whitebox.setInternalState(exHandlerSpy, "logger", loggerMock);
        final List<StringBuilder> sbHolder = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                StringBuilder sb = (StringBuilder) invocation.getArguments()[0];
                sb.append(UUID.randomUUID().toString());
                sbHolder.add(sb);
                return UUID.randomUUID().toString();
            }
        }).when(utilsSpy).buildErrorMessageForLogs(
            any(StringBuilder.class), eq(reqMock), eq(errorsExpectedToBeUsed), eq(httpStatusCodeExpectedToBeUsed),
            eq(exceptionToThrow), any(List.class)
        );

        // when
        exHandlerSpy.handleException(exceptionToThrow, reqMock);

        // then
        verify(utilsSpy).buildErrorMessageForLogs(
            any(StringBuilder.class), eq(reqMock), eq(errorsExpectedToBeUsed), eq(httpStatusCodeExpectedToBeUsed),
            eq(exceptionToThrow), any(List.class)
        );
        assertThat(sbHolder).hasSize(1);
        verify(loggerMock).error(sbHolder.get(0).toString(), exceptionToThrow);
    }

    @Test
    public void handleException_should_delegate_to_prepareFrameworkRepresentation_for_response() throws Exception {
        // given
        Exception exceptionToThrow = new Exception("kaboom");
        TestDTO frameworkRepresentationObj = mock(TestDTO.class);
        doReturn(frameworkRepresentationObj).when(exHandlerSpy).prepareFrameworkRepresentation(
            any(DefaultErrorContractDTO.class), anyInt(), any(Collection.class), any(Throwable.class), any(RequestInfoForLogging.class)
        );

        // when
        ErrorResponseInfo<TestDTO> returnValue = exHandlerSpy.handleException(exceptionToThrow, reqMock);

        // then
        assertThat(returnValue.frameworkRepresentationObj).isEqualTo(frameworkRepresentationObj);
    }

    @Test
    public void handleException_uses_getGenericServiceError_from_projectApiErrors() {
        // given
        ApiError expectedApiErrorMatch = testProjectApiErrors.getGenericServiceError();

        // when
        ErrorResponseInfo<TestDTO> result = exHandlerSpy.handleException(new Exception(), reqMock);

        // then
        DefaultErrorContractDTO errorContract = result.frameworkRepresentationObj.erv;
        assertThat(errorContract.errors).hasSize(1);
        DefaultErrorDTO error = errorContract.errors.get(0);
        assertThat(error.code).isEqualTo(expectedApiErrorMatch.getErrorCode());
        assertThat(error.message).isEqualTo(expectedApiErrorMatch.getMessage());
        assertThat(error.metadata).isEqualTo(expectedApiErrorMatch.getMetadata());
    }

    @Test
    public void handleException_should_add_error_id_to_response_header() {
        ErrorResponseInfo<TestDTO> result = exHandlerSpy.handleException(new Exception(), reqMock);

        assertThat(result.headersToAddToResponse.get("error_uid")).isEqualTo(singletonList(result.frameworkRepresentationObj.erv.error_id));
    }

    @Test
    public void logRequestBodyOnUnhandledExceptions_returns_false_by_default() {
        // expect
        assertThat(exHandlerSpy.logRequestBodyOnUnhandledExceptions(mock(Throwable.class), reqMock)).isFalse();
    }

    @Test
    public void handleException_should_add_headers_from_extraHeadersForResponse_to_ErrorResponseInfo() {
        // given
        final Map<String, List<String>> baseExtraHeaders = MapBuilder
            .builder("foo", singletonList(UUID.randomUUID().toString()))
            .put("bar", Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
            .build();
        UnhandledExceptionHandlerBase<TestDTO> handler = new TestUnhandledExceptionHandler(testProjectApiErrors, utilsSpy) {
            @Override
            protected Map<String, List<String>> extraHeadersForResponse(TestDTO frameworkRepresentation, DefaultErrorContractDTO errorContractDTO,
                                                                        int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                                                                        Throwable originalException,
                                                                        RequestInfoForLogging request) {
                return baseExtraHeaders;
            }
        };

        // when
        ErrorResponseInfo<TestDTO> result = handler.handleException(new Exception(), reqMock);

        // then
        Map<String, List<String>> expectedExtraHeaders = new HashMap<>(baseExtraHeaders);
        expectedExtraHeaders.put("error_uid", singletonList(result.frameworkRepresentationObj.erv.error_id));
        assertThat(result.headersToAddToResponse).isEqualTo(expectedExtraHeaders);
    }

    @Test
    public void handleException_should_not_allow_error_uid_from_extraHeadersForResponse_to_override_true_error_uid() {
        // given
        final Map<String, List<String>> baseExtraHeaders = MapBuilder
            .builder("error_uid", singletonList(UUID.randomUUID().toString()))
            .build();
        UnhandledExceptionHandlerBase<TestDTO> handler = new TestUnhandledExceptionHandler(testProjectApiErrors, utilsSpy) {
            @Override
            protected Map<String, List<String>> extraHeadersForResponse(TestDTO frameworkRepresentation, DefaultErrorContractDTO errorContractDTO,
                                                                        int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                                                                        Throwable originalException,
                                                                        RequestInfoForLogging request) {
                return baseExtraHeaders;
            }
        };

        // when
        ErrorResponseInfo<TestDTO> result = handler.handleException(new Exception(), reqMock);

        // then
        assertThat(result.headersToAddToResponse.get("error_uid"))
                  .isNotEqualTo(baseExtraHeaders.get("error_uid"))
                  .isEqualTo(singletonList(result.frameworkRepresentationObj.erv.error_id));
    }

    @DataProvider(value = {
        "true",
        "false"
    }, splitBy = "\\|")
    @Test
    public void handleException_delegates_to_generateLastDitchFallbackErrorResponseInfo_if_something_crazy_happens(boolean explodeBeforeUtilsErrorIdIsGenerated) {
        // given
        Exception origEx = new Exception("kaboom");
        RuntimeException ohWowThisIsBadException = new RuntimeException("yikes!");
        String utilsErrorIdToReturn = UUID.randomUUID().toString();
        if (explodeBeforeUtilsErrorIdIsGenerated) {
            doThrow(ohWowThisIsBadException).when(utilsSpy).buildErrorMessageForLogs(
                any(StringBuilder.class), eq(reqMock), eq(errorsExpectedToBeUsed), eq(httpStatusCodeExpectedToBeUsed),
                eq(origEx), any(List.class)
            );
        }
        else {
            doReturn(utilsErrorIdToReturn).when(utilsSpy).buildErrorMessageForLogs(
                any(StringBuilder.class), eq(reqMock), eq(errorsExpectedToBeUsed), eq(httpStatusCodeExpectedToBeUsed),
                eq(origEx), any(List.class)
            );
            doThrow(ohWowThisIsBadException).when(exHandlerSpy).prepareFrameworkRepresentation(
                any(DefaultErrorContractDTO.class), anyInt(), any(Collection.class), any(Throwable.class), any(RequestInfoForLogging.class)
            );
        }
        ErrorResponseInfo<TestDTO> lastDitchResponse = mock(ErrorResponseInfo.class);
        doReturn(lastDitchResponse).when(exHandlerSpy).generateLastDitchFallbackErrorResponseInfo(
            eq(origEx), eq(reqMock), anyString(), any(Map.class)
        );

        // when
        ErrorResponseInfo<TestDTO> result = exHandlerSpy.handleException(origEx, reqMock);

        // then
        if (explodeBeforeUtilsErrorIdIsGenerated) {
            verify(utilsSpy).buildErrorMessageForLogs(
                any(StringBuilder.class), eq(reqMock), eq(errorsExpectedToBeUsed), eq(httpStatusCodeExpectedToBeUsed),
                eq(origEx), any(List.class)
            );
        }
        else {
            verify(exHandlerSpy).prepareFrameworkRepresentation(
                any(DefaultErrorContractDTO.class), anyInt(), any(Collection.class), any(Throwable.class), any(RequestInfoForLogging.class)
            );
        }
        assertThat(result).isEqualTo(lastDitchResponse);
        ArgumentCaptor<String> lastDitchErrorIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> lastDitchHeadersCaptor = ArgumentCaptor.forClass(Map.class);
        verify(exHandlerSpy).generateLastDitchFallbackErrorResponseInfo(eq(origEx), eq(reqMock), lastDitchErrorIdCaptor.capture(), lastDitchHeadersCaptor.capture());
        String lastDitchErrorId = lastDitchErrorIdCaptor.getValue();
        Map<String, List<String>> lastDitchHeaders = lastDitchHeadersCaptor.getValue();
        if (!explodeBeforeUtilsErrorIdIsGenerated)
            assertThat(lastDitchErrorId).isEqualTo(utilsErrorIdToReturn);
        assertThat(lastDitchHeaders).isEqualTo(MapBuilder.builder("error_uid", singletonList(lastDitchErrorId)).build());
    }

    private Matcher<DefaultErrorContractDTO> errorResponseViewMatches(final DefaultErrorContractDTO expectedErrorContract) {
        return new CustomTypeSafeMatcher<DefaultErrorContractDTO>("a matching ErrorResponseView"){
            @Override
            protected boolean matchesSafely(DefaultErrorContractDTO item) {
                if (!(item.errors.size() == expectedErrorContract.errors.size()))
                    return false;

                for (int i = 0; i < item.errors.size(); i++) {
                    DefaultErrorDTO itemError = item.errors.get(i);
                    DefaultErrorDTO expectedError = item.errors.get(i);
                    if (!itemError.code.equals(expectedError.code))
                        return false;
                    if (!itemError.message.equals(expectedError.message))
                        return false;
                }

                return item.error_id.equals(expectedErrorContract.error_id);
            }
        };
    }

    private static class TestUnhandledExceptionHandler extends UnhandledExceptionHandlerBase<TestDTO> {

        public TestUnhandledExceptionHandler(ProjectApiErrors projectApiErrors, ApiExceptionHandlerUtils utils) {
            super(projectApiErrors, utils);
        }

        @Override
        protected TestDTO prepareFrameworkRepresentation(DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                                                         Throwable originalException, RequestInfoForLogging request) {
            return new TestDTO(errorContractDTO);
        }

        @Override
        protected ErrorResponseInfo<TestDTO> generateLastDitchFallbackErrorResponseInfo(
            Throwable ex, RequestInfoForLogging request, String errorUid,
            Map<String, List<String>> headersForResponseWithErrorUid) {

            return new ErrorResponseInfo<>(
                projectApiErrors.getGenericServiceError().getHttpStatusCode(),
                new TestDTO(new DefaultErrorContractDTO(UUID.randomUUID().toString(), singletonList(projectApiErrors.getGenericServiceError()))),
                Collections.<String, List<String>>emptyMap()
            );
        }
    }

    private static class TestDTO {
        public final DefaultErrorContractDTO erv;

        private TestDTO(DefaultErrorContractDTO erv) {
            this.erv = erv;
        }
    }
}
