package com.nike.backstopper.annotation.post.processor.extractor;

import com.nike.backstopper.annotation.post.processor.model.AnnotationDetails;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.nike.backstopper.annotation.post.processor.TestUtils.buildAnnotationDetailsByValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link ElementExtractor}.
 *
 * @author Andrey Tsarenko
 */
@RunWith(DataProviderRunner.class)
public class ElementExtractorTest {

    private ElementExtractor elementExtractor;
    private Elements elementUtils;

    @Before
    public void setUp() {
        elementUtils = mock(Elements.class);
        elementExtractor = new ElementExtractor(elementUtils);
    }

    @Test
    public void getExecutableAnnotationValues() {
        when(elementUtils.getElementValuesWithDefaults(any(AnnotationMirror.class)))
                .thenReturn(Collections.emptyMap());

        assertThat(elementExtractor.getExecutableAnnotationValues(null)).isEmpty();
    }

    @Test
    public void getExecutionName() {
        ExecutableElement executableElement = mock(ExecutableElement.class, RETURNS_DEEP_STUBS);

        when(executableElement.getSimpleName().toString())
                .thenReturn("executionName");

        assertThat(elementExtractor.getExecutionName(executableElement)).isEqualTo("executionName");
    }

    @Test
    public void getExecutionReturnType() {
        ExecutableElement executableElement = mock(ExecutableElement.class, RETURNS_DEEP_STUBS);

        when(executableElement.getReturnType().toString())
                .thenReturn("executionReturnType");

        assertThat(elementExtractor.getExecutionReturnType(executableElement)).isEqualTo("executionReturnType");
    }

    @Test
    public void getAnnotationName() {
        AnnotationMirror annotationMirror = mock(AnnotationMirror.class, RETURNS_DEEP_STUBS);

        when(annotationMirror.getAnnotationType().toString())
                .thenReturn("annotationName");

        assertThat(elementExtractor.getAnnotationName(annotationMirror)).isEqualTo("annotationName");
    }

    @Test
    public void getAnnotationStringValue() {
        AnnotationDetails annotationDetails = buildAnnotationDetailsByValue("value");

        assertThat(elementExtractor.getAnnotationStringValue(annotationDetails, "annotationMethodName"))
                .isEqualTo("value");
    }

    @Test
    public void getAnnotationIntValue() {
        AnnotationDetails annotationDetails = buildAnnotationDetailsByValue(400);

        assertThat(elementExtractor.getAnnotationIntValue(annotationDetails, "annotationMethodName"))
                .isEqualTo(400);
    }

    @Test
    @UseDataProvider("unacceptableAnnotationValues")
    public void getNullableAnnotationStringValue(AnnotationDetails annotationDetails) {
        assertThat(elementExtractor.getAnnotationStringValue(annotationDetails, "annotationMethodName"))
                .isNull();
    }

    @Test
    @UseDataProvider("unacceptableAnnotationValues")
    public void getNullableAnnotationIntValue(AnnotationDetails annotationDetails) {
        assertThat(elementExtractor.getAnnotationIntValue(annotationDetails, "annotationMethodName"))
                .isNull();
    }

    @DataProvider
    public static List<List<AnnotationDetails>> unacceptableAnnotationValues() {
        return Stream.of(
                buildAnnotationDetailsByValue(null),
                buildAnnotationDetailsByValue(new Object()),
                buildAnnotationDetailsByValue(""),
                buildAnnotationDetailsByValue(" "))
                .map(Collections::singletonList)
                .collect(Collectors.toList());
    }

}
