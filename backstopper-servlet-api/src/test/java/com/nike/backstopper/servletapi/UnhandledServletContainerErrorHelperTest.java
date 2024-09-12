package com.nike.backstopper.servletapi;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.sample.SampleProjectApiErrorsBase;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.exception.WrapperException;
import com.nike.internal.util.Pair;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link UnhandledServletContainerErrorHelper}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class UnhandledServletContainerErrorHelperTest {

    private UnhandledServletContainerErrorHelper helper;
    private ServletRequest requestMock;
    private ProjectApiErrors projectApiErrors;

    @Before
    public void beforeMethod() {
        helper = new UnhandledServletContainerErrorHelper();
        requestMock = mock(ServletRequest.class);
        projectApiErrors = new SampleProjectApiErrorsBase() {
            @Override
            protected List<ApiError> getProjectSpecificApiErrors() {
                return null;
            }

            @Override
            protected ProjectSpecificErrorCodeRange getProjectSpecificErrorCodeRange() {
                return null;
            }
        };
    }

    @DataProvider(value = {
        "true   |   true    |   true",
        "false  |   true    |   true",
        "false  |   false   |   true",
        "false  |   false   |   true",
        "false  |   false   |   false",
    }, splitBy = "\\|")
    @Test
    public void extractOrGenerateErrorForRequest_returns_wrapped_exception_from_request_attrs_if_available(
        boolean requestHasExInSpringboot3ReactiveAttr,
        boolean requestHasExInSpringboot3ServletAttr,
        boolean requestHasExInServletAttr
    ) {
        // given
        Throwable springboot3ReactiveAttrEx = new RuntimeException("some springboot 3 reactive request attr exception");
        Throwable springboot3ServletAttrEx = new RuntimeException("some springboot 3 servlet request attr exception");
        Throwable servletAttrEx = new RuntimeException("some servlet request attr exception");

        if (requestHasExInSpringboot3ReactiveAttr) {
            doReturn(springboot3ReactiveAttrEx)
                .when(requestMock)
                .getAttribute("org.springframework.boot.web.reactive.error.DefaultErrorAttributes.ERROR");
        }

        if (requestHasExInSpringboot3ServletAttr) {
            doReturn(springboot3ServletAttrEx)
                .when(requestMock)
                .getAttribute("org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR");
        }

        if (requestHasExInServletAttr) {
            doReturn(servletAttrEx).when(requestMock).getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        }

        // when
        Throwable result = helper.extractOrGenerateErrorForRequest(requestMock, projectApiErrors);

        // then
        if (requestHasExInSpringboot3ReactiveAttr) {
            assertThat(result)
                .isInstanceOf(WrapperException.class)
                .hasMessage("Caught a container exception.")
                .hasCause(springboot3ReactiveAttrEx);
        }
        else if (requestHasExInSpringboot3ServletAttr) {
            assertThat(result)
                .isInstanceOf(WrapperException.class)
                .hasMessage("Caught a container exception.")
                .hasCause(springboot3ServletAttrEx);
        }
        else if (requestHasExInServletAttr) {
            assertThat(result)
                .isInstanceOf(WrapperException.class)
                .hasMessage("Caught a container exception.")
                .hasCause(servletAttrEx);
        }
        else {
            assertThat(result)
                .isNotInstanceOf(WrapperException.class)
                .hasNoCause();
        }
    }

    private enum Synthetic404Scenario {
        INT_404(404),
        STRING_404("404");

        public final Object statusCodeAttr;

        Synthetic404Scenario(Object statusCodeAttr) {
            this.statusCodeAttr = statusCodeAttr;
        }
    }

    @DataProvider
    public static List<List<Synthetic404Scenario>> synthetic404DataProvider() {
        return Stream.of(Synthetic404Scenario.values())
                     .map(Collections::singletonList)
                     .collect(Collectors.toList());
    }

    @UseDataProvider("synthetic404DataProvider")
    @Test
    public void extractOrGenerateErrorForRequest_generates_synthetic_ApiException_for_404_when_no_exception_found_in_attrs_and_status_code_is_404(
        Synthetic404Scenario scenario
    ) {
        // given
        doReturn(scenario.statusCodeAttr).when(requestMock).getAttribute("jakarta.servlet.error.status_code");

        // when
        Throwable result = helper.extractOrGenerateErrorForRequest(requestMock, projectApiErrors);

        // then
        assertThat(result)
            .isInstanceOf(ApiException.class)
            .hasMessage("Synthetic exception for container 404.");

        ApiException apiEx = (ApiException)result;
        assertThat(apiEx.getApiErrors()).isEqualTo(singletonList(projectApiErrors.getNotFoundApiError()));
        assertThat(apiEx.getExtraDetailsForLogging()).isEqualTo(
            singletonList(Pair.of("synthetic_exception_for_container_404", "true"))
        );
    }

    private enum Synthetic500Scenario {
        NULL_STATUS_CODE(null, "null"),
        NOT_AN_INT_STATUS_CODE("not-an-int", "null"),
        NOT_A_404_INT_STATUS_CODE(400, "400"),
        NOT_A_404_STRING_STATUS_CODE("400", "400");

        public final Object statusCodeAttr;
        public final String expectedStatusCodeValueForExceptionDetails;

        Synthetic500Scenario(
            Object statusCodeAttr,
            @NotNull String expectedStatusCodeValueForExceptionDetails
        ) {
            this.statusCodeAttr = statusCodeAttr;
            this.expectedStatusCodeValueForExceptionDetails = expectedStatusCodeValueForExceptionDetails;
        }
    }

    @DataProvider
    public static List<List<Synthetic500Scenario>> synthetic500DataProvider() {
        return Stream.of(Synthetic500Scenario.values())
                     .map(Collections::singletonList)
                     .collect(Collectors.toList());
    }

    @UseDataProvider("synthetic500DataProvider")
    @Test
    public void extractOrGenerateErrorForRequest_generates_synthetic_ApiException_for_500_when_no_exception_found_in_attrs_and_not_404(
        Synthetic500Scenario scenario
    ) {
        // given
        doReturn(scenario.statusCodeAttr).when(requestMock).getAttribute("jakarta.servlet.error.status_code");

        // when
        Throwable result = helper.extractOrGenerateErrorForRequest(requestMock, projectApiErrors);

        // then
        assertThat(result)
            .isInstanceOf(ApiException.class)
            .hasMessage("Synthetic exception for unhandled container status code: " +
                        scenario.expectedStatusCodeValueForExceptionDetails
            );

        ApiException apiEx = (ApiException)result;
        assertThat(apiEx.getApiErrors()).isEqualTo(singletonList(projectApiErrors.getGenericServiceError()));
        assertThat(apiEx.getExtraDetailsForLogging()).isEqualTo(singletonList(
            Pair.of(
                "synthetic_exception_for_unhandled_status_code",
                scenario.expectedStatusCodeValueForExceptionDetails
            ))
        );
    }
}