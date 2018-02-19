package com.nike.backstopper.handler.jaxrs;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.UnexpectedMajorExceptionHandlingError;
import com.nike.backstopper.handler.jaxrs.config.JaxRsApiExceptionHandlerListenerList;
import com.nike.backstopper.model.DefaultErrorContractDTO;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link JaxRsApiExceptionHandler }
 *
 * @author dsand7
 * @author Michael Irwin
 */
@RunWith(DataProviderRunner.class)
public class JaxRsApiExceptionHandlerTest {

    private JaxRsApiExceptionHandler handlerSpy;
    private JaxRsUnhandledExceptionHandler unhandledSpy;
    private JaxRsApiExceptionHandlerListenerList listenerList;
    private static final ApiError EXPECTED_ERROR = new ApiErrorBase("test", 99008, "test", 8);

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(
        singletonList(EXPECTED_ERROR), ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES
    );

    @Before
    public void beforeMethod() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn("/fake/path");
        when(mockRequest.getMethod()).thenReturn("GET");
        when(mockRequest.getQueryString()).thenReturn("queryString");

        listenerList = new JaxRsApiExceptionHandlerListenerList(testProjectApiErrors, ApiExceptionHandlerUtils.DEFAULT_IMPL);

        unhandledSpy = spy(new JaxRsUnhandledExceptionHandler(testProjectApiErrors, ApiExceptionHandlerUtils.DEFAULT_IMPL));

        handlerSpy = spy(new JaxRsApiExceptionHandler(
            testProjectApiErrors,
            listenerList,
            ApiExceptionHandlerUtils.DEFAULT_IMPL, unhandledSpy));
        Whitebox.setInternalState(handlerSpy, "request", mockRequest);
        Whitebox.setInternalState(handlerSpy, "response", mock(HttpServletResponse.class));
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_jaxRsUnhandledExceptionHandler_is_null() {
        // when
        Throwable ex = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new JaxRsApiExceptionHandler(testProjectApiErrors, listenerList, ApiExceptionHandlerUtils.DEFAULT_IMPL, null);
            }
        });

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void toResponseReturnsCorrectResponseForCoreApiErrorException() {

        ApiError expectedError = BarebonesCoreApiErrorForTesting.MALFORMED_REQUEST;
        ApiException.Builder exceptionBuilder = ApiException.Builder.newBuilder();
        exceptionBuilder.withExceptionMessage("test this");
        exceptionBuilder.withApiErrors(expectedError);
        Response actualResponse = handlerSpy.toResponse(exceptionBuilder.build());

        Assert.assertEquals(expectedError.getHttpStatusCode(), actualResponse.getStatus());
    }

    @Test
    public void toResponseReturnsCorrectResponseForJaxRsWebApplicationException() {

        NotFoundException exception = new NotFoundException("uri not found!");
        Response actualResponse = handlerSpy.toResponse(exception);

        Assert.assertEquals(HttpServletResponse.SC_NOT_FOUND, actualResponse.getStatus());
    }

    @Test
    public void prepareFrameworkRepresentationContainsAppropriateStatusCode() {

        Response.ResponseBuilder response = handlerSpy.prepareFrameworkRepresentation(new DefaultErrorContractDTO(null, null),
                                                                                      HttpServletResponse.SC_OK, null, null, null);
        Assert.assertEquals(HttpServletResponse.SC_OK, response.build().getStatus());
    }

    @Test
    public void prepareFrameworkRepresentationContainsEntity() {

        Response.ResponseBuilder response = handlerSpy.prepareFrameworkRepresentation(new DefaultErrorContractDTO(null, null),
                                                                                      HttpServletResponse.SC_OK, null, null, null);
        Assert.assertNotNull(response.build().getEntity());
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void toResponseContainsContentTypeHeaderForHandledException(boolean overrideDefaultContentType) {
        // given
        ApiException exception = ApiException.Builder.newBuilder()
                                                     .withExceptionMessage("test this")
                                                     .withApiErrors(BarebonesCoreApiErrorForTesting.MALFORMED_REQUEST)
                                                     .build();

        String expectedContentType = MediaType.APPLICATION_JSON;
        if (overrideDefaultContentType) {
            final String finalExpectedContentType = UUID.randomUUID().toString();
            expectedContentType = finalExpectedContentType;
            doAnswer(invocation -> {
                ErrorResponseInfo<Response.ResponseBuilder> errorResponseInfo =
                    (ErrorResponseInfo<Response.ResponseBuilder>) invocation.getArguments()[0];
                return errorResponseInfo.frameworkRepresentationObj.header("Content-Type", finalExpectedContentType);
            }).when(handlerSpy).setContentType(
                any(ErrorResponseInfo.class), any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(Throwable.class)
            );
        }

        // when
        Response response = handlerSpy.toResponse(exception);

        // then
        verify(handlerSpy).setContentType(
            any(ErrorResponseInfo.class), any(HttpServletRequest.class), any(HttpServletResponse.class),
            eq(exception)
        );
        assertThat(response.getMetadata()).isNotNull();
        assertThat(response.getMetadata().get("Content-Type")).isEqualTo(singletonList(expectedContentType));
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void toResponseContainsContentTypeHeaderForUnhandledException(boolean overrideDefaultContentType) {
        // given
        String expectedContentType = MediaType.APPLICATION_JSON;
        if (overrideDefaultContentType) {
            final String finalExpectedContentType = UUID.randomUUID().toString();
            expectedContentType = finalExpectedContentType;
            doAnswer(invocation -> {
                ErrorResponseInfo<Response.ResponseBuilder> errorResponseInfo =
                    (ErrorResponseInfo<Response.ResponseBuilder>) invocation.getArguments()[0];
                return errorResponseInfo.frameworkRepresentationObj.header("Content-Type", finalExpectedContentType);
            }).when(handlerSpy).setContentType(
                any(ErrorResponseInfo.class), any(HttpServletRequest.class), any(HttpServletResponse.class),
                any(Throwable.class)
            );
        }

        // when
        Response response = handlerSpy.toResponse(new Exception("not handled"));

        // then
        verify(handlerSpy).setContentType(
            any(ErrorResponseInfo.class), any(HttpServletRequest.class), any(HttpServletResponse.class),
            any(Throwable.class)
        );
        assertThat(response.getMetadata()).isNotNull();
        assertThat(response.getMetadata().get("Content-Type")).isEqualTo(singletonList(expectedContentType));
    }

    @Test
    public void toResponse_delegates_to_unhandled_exception_handler_if_maybeHandleException_throws_UnexpectedMajorExceptionHandlingError()
        throws UnexpectedMajorExceptionHandlingError {
        // given
        doThrow(new UnexpectedMajorExceptionHandlingError("foo", null))
            .when(handlerSpy).maybeHandleException(any(Throwable.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
        Exception ex = new Exception("kaboom");
        String uniqueHeader = UUID.randomUUID().toString();
        ErrorResponseInfo<Response.ResponseBuilder> unhandledHandlerResponse =
            new ErrorResponseInfo<>(500, Response.serverError().header("unique-header", uniqueHeader), null);
        doReturn(unhandledHandlerResponse)
            .when(unhandledSpy).handleException(any(Throwable.class), any(HttpServletRequest.class), any(HttpServletResponse.class));

        // when
        Response response = handlerSpy.toResponse(ex);

        // then
        verify(handlerSpy).maybeHandleException(any(Throwable.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
        verify(unhandledSpy).handleException(any(Throwable.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
        assertThat(response.getMetadata().get("unique-header")).isEqualTo(singletonList(uniqueHeader));
    }

}
