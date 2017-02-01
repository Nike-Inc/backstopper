package com.nike.backstopper.handler.spring.listener.impl;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListenerResult;
import com.nike.backstopper.handler.listener.impl.ListenerTestBase;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentConversionNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link OneOffSpringFrameworkExceptionHandlerListener}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class OneOffSpringFrameworkExceptionHandlerListenerTest extends ListenerTestBase {

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(null, null);
    private OneOffSpringFrameworkExceptionHandlerListener listener = new OneOffSpringFrameworkExceptionHandlerListener(testProjectApiErrors,
                                                                                                                       ApiExceptionHandlerUtils.DEFAULT_IMPL);

    @Test
    public void constructor_sets_projectApiErrors_and_utils_to_passed_in_args() {
        // given
        ProjectApiErrors projectErrorsMock = mock(ProjectApiErrors.class);
        ApiExceptionHandlerUtils utilsMock = mock(ApiExceptionHandlerUtils.class);

        // when
        OneOffSpringFrameworkExceptionHandlerListener impl = new OneOffSpringFrameworkExceptionHandlerListener(projectErrorsMock, utilsMock);

        // then
        Assertions.assertThat(impl.projectApiErrors).isSameAs(projectErrorsMock);
        Assertions.assertThat(impl.utils).isSameAs(utilsMock);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_projectApiErrors() {
        // when
        Throwable ex = Assertions.catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new OneOffSpringFrameworkExceptionHandlerListener(null, ApiExceptionHandlerUtils.DEFAULT_IMPL);
            }
        });

        // then
        Assertions.assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_utils() {
        // when
        Throwable ex = Assertions.catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new OneOffSpringFrameworkExceptionHandlerListener(mock(ProjectApiErrors.class), null);
            }
        });

        // then
        Assertions.assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }
    
    @Test
    public void shouldIgnoreExceptionThatItDoesNotWantToHandle() {
        validateResponse(listener.shouldHandleException(new ApiException(testProjectApiErrors.getGenericServiceError())), false, null);
    }

    @Test
    public void shouldReturnTYPE_CONVERSION_ERRORForTypeMismatchException() {
        TypeMismatchException ex = new TypeMismatchException("blah", Integer.class);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getTypeConversionApiError()));
    }

    @Test
    public void shouldReturnMALFORMED_REQUESTForMissingServletRequestParameterException() {
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("someParam", "someParamType");
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getMalformedRequestApiError()));

        // Also should add base exception message to logging details.
        assertThat(result.extraDetailsForLogging.size(), is(1));
        assertThat(result.extraDetailsForLogging.get(0), is(Pair.of("exception_message", "Required someParamType parameter 'someParam' is not present")));
    }

    @Test
    public void shouldReturnMALFORMED_REQUESTForHttpMessageConversionException() {
        HttpMessageConversionException ex = new HttpMessageConversionException("foobar");
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getMalformedRequestApiError()));
    }

    @Test
    public void shouldReturnMISSING_EXPECTED_CONTENTForHttpMessageNotReadableExceptionThatStartsWithKnownMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Required request body is missing " + UUID.randomUUID().toString());
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getMissingExpectedContentApiError()));
    }

    @Test
    public void shouldReturnMISSING_EXPECTED_CONTENTForHttpMessageNotReadableExceptionWhenTheStarsAlign() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("foobar", new JsonMappingException("No content to map due to end-of-input"));
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getMissingExpectedContentApiError()));
    }

    @Test
    public void shouldReturnMALFORMED_REQUESTForHttpMessageNotReadableExceptionWhenCauseIsNull() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("foobar", null);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getMalformedRequestApiError()));
    }

    @Test
    public void shouldReturnMALFORMED_REQUESTForHttpMessageNotReadableExceptionWhenCauseIsNotJsonMappingException() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("foobar", new Exception("No content to map due to end-of-input"));
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getMalformedRequestApiError()));
    }

    @Test
    public void shouldReturnMALFORMED_REQUESTForHttpMessageNotReadableExceptionWhenCauseIsJsonMappingExceptionButMessageIsNull() {
        JsonMappingException jme = mock(JsonMappingException.class);
        doReturn(null).when(jme).getMessage();
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("foobar", jme);
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getMalformedRequestApiError()));
    }

    @Test
    public void shouldReturnMALFORMED_REQUESTForHttpMessageNotReadableExceptionWhenCauseIsJsonMappingExceptionButMessageIsNotTheRightMessage() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("foobar", new JsonMappingException("garbagio"));
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getMalformedRequestApiError()));
    }

    @Test
    public void shouldReturnNO_ACCEPTABLE_REPRESENTATIONForHttpMediaTypeNotAcceptableException() {
        HttpMediaTypeNotAcceptableException ex = new HttpMediaTypeNotAcceptableException("asplode");
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getNoAcceptableRepresentationApiError()));
    }

    @Test
    public void shouldReturnUNSUPPORTED_MEDIA_TYPEForHttpMediaTypeNotSupportedException() {
        HttpMediaTypeNotSupportedException ex = new HttpMediaTypeNotSupportedException("asplode");
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getUnsupportedMediaTypeApiError()));
    }

    @Test
    public void shouldReturnMETHOD_NOT_ALLOWEDForHttpRequestMethodNotSupportedException() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("asplode");
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getMethodNotAllowedApiError()));
    }

    @Test
    public void shouldHandleException_should_return_not_found_error_when_passed_NoHandlerFoundException() {
        // given
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/some/url", mock(HttpHeaders.class));

        // when
        ApiExceptionHandlerListenerResult result = listener.shouldHandleException(ex);

        // then
        validateResponse(result, true, Collections.singletonList(testProjectApiErrors.getNotFoundApiError()));
    }

    @DataProvider
    public static Object[][] dataProviderForExtractRequiredTypeNoInfoLeak() {
        return new Object[][] {
            { null, null },
            { Byte.class, "byte" },
            { byte.class, "byte" },
            { Short.class, "short" },
            { short.class, "short" },
            { Integer.class, "int" },
            { int.class, "int" },
            { Long.class, "long" },
            { long.class, "long" },
            { Float.class, "float" },
            { float.class, "float" },
            { Double.class, "double" },
            { double.class, "double" },
            { Boolean.class, "boolean" },
            { boolean.class, "boolean" },
            { Character.class, "char" },
            { char.class, "char" },
            { CharSequence.class, "string" },
            { Object.class, "[complex type]"}
        };
    }

    @UseDataProvider("dataProviderForExtractRequiredTypeNoInfoLeak")
    @Test
    public void extractRequiredTypeNoInfoLeak_works_as_expected(Class<?> requiredType, String expectedResult) {
        // given
        TypeMismatchException typeMismatchExceptionMock = mock(TypeMismatchException.class);
        doReturn(requiredType).when(typeMismatchExceptionMock).getRequiredType();

        // when
        String result = listener.extractRequiredTypeNoInfoLeak(typeMismatchExceptionMock);

        // then
        Assertions.assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void extractPropertyName_works_for_MethodArgumentTypeMismatchException() {
        // given
        MethodArgumentTypeMismatchException exMock = mock(MethodArgumentTypeMismatchException.class);
        String name = UUID.randomUUID().toString();
        doReturn(name).when(exMock).getName();

        // when
        String result = listener.extractPropertyName(exMock);

        // then
        Assertions.assertThat(result).isEqualTo(name);
    }

    @Test
    public void extractPropertyName_works_for_MethodArgumentConversionNotSupportedException() {
        // given
        MethodArgumentConversionNotSupportedException exMock = mock(MethodArgumentConversionNotSupportedException.class);
        String name = UUID.randomUUID().toString();
        doReturn(name).when(exMock).getName();

        // when
        String result = listener.extractPropertyName(exMock);

        // then
        Assertions.assertThat(result).isEqualTo(name);
    }

    @Test
    public void extractPropertyName_returns_null_for_base_TypeMismatchException() {
        // given
        TypeMismatchException exMock = mock(TypeMismatchException.class);

        // when
        String result = listener.extractPropertyName(exMock);

        // then
        Assertions.assertThat(result).isNull();
    }
}
