package com.nike.backstopper.handler.spring;

import com.nike.backstopper.handler.spring.SpringContainerErrorController.SpringbootErrorControllerIsNotOnClasspath;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests the functionality of {@link SpringbootErrorControllerIsNotOnClasspath}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class SpringbootErrorControllerIsNotOnClasspathTest {

    private SpringbootErrorControllerIsNotOnClasspath impl;

    @Before
    public void beforeMethod() {
        impl = new SpringbootErrorControllerIsNotOnClasspath();
    }

    @Test
    public void getConfigurationPhase_returns_REGISTER_BEAN() {
        // expect
        assertThat(impl.getConfigurationPhase()).isEqualTo(ConfigurationPhase.REGISTER_BEAN);
    }

    @Test
    public void isClassAvailableOnClasspath_returns_true_for_class_on_classpath() {
        // expect
        assertThat(impl.isClassAvailableOnClasspath(String.class.getName())).isTrue();
    }

    @Test
    public void isClassAvailableOnClasspath_returns_false_for_class_not_on_classpath() {
        // expect
        assertThat(impl.isClassAvailableOnClasspath("foo.doesnotexist.Blah")).isFalse();
    }

    @DataProvider(value = {
        "true    |   false",
        "false   |   true",
    }, splitBy = "\\|")
    @Test
    public void matches_method_works_as_expected(
        boolean sb2IsOnClasspath, boolean expectedResult
    ) {
        // given
        SpringbootErrorControllerIsNotOnClasspath implSpy = spy(impl);
        ConditionContext contextMock = mock(ConditionContext.class);
        AnnotatedTypeMetadata metadataMock = mock(AnnotatedTypeMetadata.class);

        doReturn(sb2IsOnClasspath)
            .when(implSpy)
            .isClassAvailableOnClasspath("org.springframework.boot.web.servlet.error.ErrorController");

        // when
        boolean result = implSpy.matches(contextMock, metadataMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

}