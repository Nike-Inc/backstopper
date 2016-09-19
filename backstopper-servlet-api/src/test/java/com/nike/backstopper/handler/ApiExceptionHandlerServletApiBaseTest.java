package com.nike.backstopper.handler;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.internal.util.MapBuilder;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit test for {@link com.nike.backstopper.handler.ApiExceptionHandlerServletApiBase}
 */
public class ApiExceptionHandlerServletApiBaseTest {

    private ApiExceptionHandlerServletApiBase instanceSpy;
    private HttpServletRequest servletRequestMock;
    private HttpServletResponse servletResponseMock;

    @Before
    public void beforeMethod() {
          instanceSpy = spy(new ApiExceptionHandlerServletApiBase<Object>(mock(ProjectApiErrors.class),
                                                                          Collections.<ApiExceptionHandlerListener>emptyList(),
                                                                          ApiExceptionHandlerUtils.DEFAULT_IMPL) {
              @Override
              protected Object prepareFrameworkRepresentation(DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> filteredClientErrors,
                                                              Throwable originalException, RequestInfoForLogging request) {
                  return null;
              }
          });
        servletRequestMock = mock(HttpServletRequest.class);
        servletResponseMock = mock(HttpServletResponse.class);
    }

    @Test
    public void maybeHandleExceptionReturnsSuperValue() throws UnexpectedMajorExceptionHandlingError {
        ErrorResponseInfo expectedResponseInfo = new ErrorResponseInfo(42, null, null);
        doReturn(expectedResponseInfo).when(instanceSpy).maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));
        ErrorResponseInfo actualResponseInfo = instanceSpy.maybeHandleException(new Exception(), servletRequestMock, servletResponseMock);
        assertThat(actualResponseInfo, sameInstance(expectedResponseInfo));
    }

    @Test
    public void maybeHandleExceptionSetsHeadersAndStatusCodeOnServletResponse() throws UnexpectedMajorExceptionHandlingError {
        ErrorResponseInfo<?> expectedResponseInfo = new ErrorResponseInfo(42, null, MapBuilder.<String, List<String>>builder().put("header1", Arrays.asList("h1val1")).put("header2", Arrays.asList("h2val1", "h2val2")).build());
        doReturn(expectedResponseInfo).when(instanceSpy).maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));
        instanceSpy.maybeHandleException(new Exception(), servletRequestMock, servletResponseMock);

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

    @Test
    public void maybeHandleExceptionDoesNothingToServletResponseIfErrorResponseInfoIsNull() throws UnexpectedMajorExceptionHandlingError {
        doReturn(null).when(instanceSpy).maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));
        instanceSpy.maybeHandleException(new Exception(), servletRequestMock, servletResponseMock);
        verifyNoMoreInteractions(servletResponseMock);
    }
}