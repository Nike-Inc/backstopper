package com.nike.backstopper.handler.spring;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport;
import com.nike.internal.util.MapBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link SpringUnhandledExceptionHandler}.
 *
 * @author Nic Munroe
 */
public class SpringUnhandledExceptionHandlerTest {

    private SpringUnhandledExceptionHandler handlerSpy;
    private ProjectApiErrors testProjectApiErrors;
    private ApiExceptionHandlerUtils generalUtils;
    private SpringApiExceptionHandlerUtils springUtilsSpy;

    @Before
    public void beforeMethod() {
        springUtilsSpy = spy(SpringApiExceptionHandlerUtils.DEFAULT_IMPL);
        generalUtils = ApiExceptionHandlerUtils.DEFAULT_IMPL;
        testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
        handlerSpy = spy(new SpringUnhandledExceptionHandler(testProjectApiErrors, generalUtils, springUtilsSpy));
    }

    @Test
    public void generateLastDitchFallbackErrorResponseInfo_returns_expected_value() throws JsonProcessingException {
        // given
        Exception ex = new Exception("kaboom");
        RequestInfoForLogging reqMock = mock(RequestInfoForLogging.class);
        String errorId = UUID.randomUUID().toString();
        Map<String, List<String>> headersMap = MapBuilder.builder("error_uid", singletonList(errorId)).build();

        ApiError expectedGenericError = testProjectApiErrors.getGenericServiceError();
        int expectedHttpStatusCode = expectedGenericError.getHttpStatusCode();
        Map<String, List<String>> expectedHeadersMap = new HashMap<>(headersMap);
        String expectedBodyPayload = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(
            new DefaultErrorContractDTO(errorId, singletonList(expectedGenericError))
        );

        // when
        ErrorResponseInfo<ModelAndView> response = handlerSpy
            .generateLastDitchFallbackErrorResponseInfo(ex, reqMock, errorId, headersMap);

        // then
        assertThat(response.httpStatusCode).isEqualTo(expectedHttpStatusCode);
        assertThat(response.headersToAddToResponse).isEqualTo(expectedHeadersMap);
        assertThat(response.frameworkRepresentationObj.getView()).isInstanceOf(MappingJackson2JsonView.class);
        ObjectMapper objectMapperUsed = ((MappingJackson2JsonView)response.frameworkRepresentationObj.getView()).getObjectMapper();
        assertThat(objectMapperUsed).isSameAs(JsonUtilWithDefaultErrorContractDTOSupport.DEFAULT_SMART_MAPPER);
        assertThat(response.frameworkRepresentationObj.getModel()).hasSize(1);
        Object modelObj = response.frameworkRepresentationObj.getModel().values().iterator().next();
        assertThat(modelObj).isInstanceOf(DefaultErrorContractDTO.class);
        assertThat(objectMapperUsed.writeValueAsString(modelObj)).isEqualTo(expectedBodyPayload);
    }

    @Test
    public void prepareFrameworkRepresentation_delegates_to_springUtils() {
        // given
        DefaultErrorContractDTO errorContractMock = mock(DefaultErrorContractDTO.class);
        int statusCode = 424;
        Collection<ApiError> apiErrors = Collections.singleton(mock(ApiError.class));
        Throwable originalEx = new RuntimeException("kaboom");
        RequestInfoForLogging reqMock = mock(RequestInfoForLogging.class);
        ModelAndView modelAndViewMock = mock(ModelAndView.class);
        doReturn(modelAndViewMock).when(springUtilsSpy).generateModelAndViewForErrorResponse(
            errorContractMock, statusCode, apiErrors, originalEx, reqMock
        );

        // when
        ModelAndView result = handlerSpy
            .prepareFrameworkRepresentation(errorContractMock, statusCode, apiErrors, originalEx, reqMock);

        // then
        verify(springUtilsSpy).generateModelAndViewForErrorResponse(errorContractMock, statusCode, apiErrors, originalEx, reqMock);
        assertThat(result).isSameAs(modelAndViewMock);
    }

    @Test
    public void resolveException_delegates_to_handleException() {
        // given
        HttpServletRequest reqMock = mock(HttpServletRequest.class);
        HttpServletResponse responseMock = mock(HttpServletResponse.class);
        Exception originalEx = new RuntimeException("kaboom");
        ModelAndView modelAndViewMock = mock(ModelAndView.class);
        ErrorResponseInfo<ModelAndView> handleExceptionResult =
            new ErrorResponseInfo<>(424, modelAndViewMock, Collections.emptyMap());
        doReturn(handleExceptionResult).when(handlerSpy).handleException(originalEx, reqMock, responseMock);

        // when
        ModelAndView result = handlerSpy.resolveException(reqMock, responseMock, null, originalEx);

        // then
        verify(handlerSpy).handleException(originalEx, reqMock, responseMock);
        assertThat(result).isSameAs(handleExceptionResult.frameworkRepresentationObj);
    }

    @Test
    public void order_getters_and_setters_work() {
        // given
        int newOrder = handlerSpy.getOrder() + 42;

        // when
        handlerSpy.setOrder(newOrder);

        // then
        assertThat(handlerSpy.getOrder()).isEqualTo(newOrder);
    }

}