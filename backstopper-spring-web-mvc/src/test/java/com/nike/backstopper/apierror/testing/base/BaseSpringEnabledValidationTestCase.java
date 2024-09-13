package com.nike.backstopper.apierror.testing.base;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.DefaultErrorDTO;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Base JUnit+Spring unit test class that should be extended by all unit tests that need a functioning spring context
 * with mock MVC testing available.
 *
 * @author Nic Munroe
 */
@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class, // Needed for dependency injection of the test classes to happen.
    BaseSpringEnabledValidationTestCase.LogBeforeClass.class,
    BaseSpringEnabledValidationTestCase.LogAfterClass.class
})
@ContextConfiguration(
    classes = {TestCaseValidationSpringConfig.class}
)
@WebAppConfiguration
public abstract class BaseSpringEnabledValidationTestCase extends AbstractJUnit4SpringContextTests {

    public BaseSpringEnabledValidationTestCase() {
        super();
    }

    public static class LogBeforeClass extends AbstractTestExecutionListener {

        @Override
        public void beforeTestClass(TestContext testContext) throws Exception {
            Class<?> testClass = testContext.getTestClass();
            Logger logger = LoggerFactory.getLogger(testClass);
            logger.info("******** Starting test_class={}", testClass.getName());
            super.beforeTestClass(testContext);
        }
    }

    public static class LogAfterClass extends AbstractTestExecutionListener {

        @Override
        public void afterTestClass(TestContext testContext) throws Exception {
            Class<?> testClass = testContext.getTestClass();
            Logger logger = LoggerFactory.getLogger(testClass);
            logger.info("******** Shutting down test_class={}", testClass.getName());
            super.afterTestClass(testContext);
        }
    }

    @Inject
    protected WebApplicationContext wac;

    protected MockMvc mockMvc;

    @Inject
    protected ObjectMapper objectMapper;

    @Before
    public void setupMethodBase() {
        this.mockMvc = webAppContextSetup(wac).build();
    }

    private List<Pair<String, String>> convertToCodeAndMessagePairs(Collection<ApiError> errors) {
        List<Pair<String, String>> pairs = new ArrayList<>();
        for (ApiError error : errors) {
            pairs.add(Pair.of(error.getErrorCode(), error.getMessage()));
        }

        return pairs;
    }

    /**
     * Helper for {@link #verifyErrorResponse(MvcResult, ProjectApiErrors, List, Class)}  that converts the given
     * ApiError to a singleton list. See that method's javadocs for more info.
     */
    protected void verifyErrorResponse(MvcResult result, ProjectApiErrors projectApiErrors, ApiError expectedError,
                                       Class<? extends Exception> expectedExceptionType) throws IOException {
        verifyErrorResponse(result, projectApiErrors, Collections.singletonList(expectedError), expectedExceptionType);
    }

    /**
     * Verifies that the given MvcResult's {@link org.springframework.test.web.servlet.MvcResult#getResponse()} has the
     * expected HTTP status code, that its contents can be converted to the appropriate {@link DefaultErrorContractDTO} with the
     * expected errors (as per the default error handling contract), and that the MvcResult's {@link
     * org.springframework.test.web.servlet.MvcResult#getResolvedException()} matches the given expectedExceptionType.
     */
    protected void verifyErrorResponse(MvcResult result, ProjectApiErrors projectApiErrors,
                                       List<ApiError> expectedErrors, Class<? extends Exception> expectedExceptionType)
        throws IOException {
        Integer expectedStatusCode = projectApiErrors.determineHighestPriorityHttpStatusCode(expectedErrors);
        expectedErrors = projectApiErrors.getSublistContainingOnlyHttpStatusCode(expectedErrors, expectedStatusCode);
        MockHttpServletResponse response = result.getResponse();
        assertThat(response.getStatus(), is(expectedStatusCode));
        DefaultErrorContractDTO details =
            objectMapper.readValue(response.getContentAsString(), DefaultErrorContractDTO.class);
        assertNotNull(details);
        assertNotNull(details.error_id);
        assertNotNull(details.errors);
        assertThat(details.errors.size(), is(expectedErrors.size()));
        List<Pair<String, String>> expectedErrorsAsPairs = convertToCodeAndMessagePairs(expectedErrors);
        for (DefaultErrorDTO errorView : details.errors) {
            assertTrue(expectedErrorsAsPairs.contains(Pair.of(errorView.code, errorView.message)));
        }
        assertNotNull(result.getResolvedException());
        Assertions.assertThat(result.getResolvedException()).isInstanceOf(expectedExceptionType);
    }

}
