package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.internal.util.MapBuilder;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link com.nike.backstopper.handler.UnhandledExceptionHandlerServletApiBase}
 */
public class UnhandledExceptionHandlerServletApiBaseTest {

    private UnhandledExceptionHandlerServletApiBase instanceSpy;
    private HttpServletRequest servletRequestMock;
    private HttpServletResponse servletResponseMock;

    @Before
    public void beforeMethod() {
        instanceSpy = spy(new UnhandledExceptionHandlerServletApiBase<Object>(mock(ProjectApiErrors.class), ApiExceptionHandlerUtils.DEFAULT_IMPL) {
            @Override
            protected Object prepareFrameworkRepresentation(DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
                                                            Throwable originalException, RequestInfoForLogging request) {
                return null;
            }

            @Override
            protected ErrorResponseInfo<Object> generateLastDitchFallbackErrorResponseInfo(Throwable ex,
                                                                                           RequestInfoForLogging request,
                                                                                           String errorUid,
                                                                                           Map<String, List<String>> headersForResponseWithErrorUid) {
                return null;
            }
        });
        servletRequestMock = mock(HttpServletRequest.class);
        servletResponseMock = mock(HttpServletResponse.class);
    }

    @Test
    public void handleExceptionReturnsSuperValue() throws UnexpectedMajorExceptionHandlingError {
        ErrorResponseInfo expectedResponseInfo = new ErrorResponseInfo(42, null, null);
        doReturn(expectedResponseInfo).when(instanceSpy).handleException(any(Throwable.class), any(RequestInfoForLogging.class));
        ErrorResponseInfo actualResponseInfo = instanceSpy.handleException(new Exception(), servletRequestMock, servletResponseMock);
        assertThat(actualResponseInfo, sameInstance(expectedResponseInfo));
    }

    @Test
    public void handleExceptionSetsHeadersAndStatusCodeOnServletResponse() throws UnexpectedMajorExceptionHandlingError {
        ErrorResponseInfo<?> expectedResponseInfo = new ErrorResponseInfo(
            42,
            null,
            MapBuilder.<String, List<String>>builder()
                      .put("header1", List.of("h1val1"))
                      .put("header2", Arrays.asList("h2val1", "h2val2"))
                      .build()
        );
        doReturn(expectedResponseInfo).when(instanceSpy).handleException(any(Throwable.class), any(RequestInfoForLogging.class));
        instanceSpy.handleException(new Exception(), servletRequestMock, servletResponseMock);

        verify(servletResponseMock).setStatus(expectedResponseInfo.httpStatusCode);
        int numHeadersChecked = 0;
        for (Map.Entry<String, List<String>> entry : expectedResponseInfo.headersToAddToResponse.entrySet()) {
            for (String headerValue : entry.getValue()) {
                verify(servletResponseMock).addHeader(entry.getKey(), headerValue);
                numHeadersChecked++;
            }
        }
        assertThat(numHeadersChecked > 0, is(true));
        assertThat(numHeadersChecked >= expectedResponseInfo.headersToAddToResponse.size(), is(true));
    }

}