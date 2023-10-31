package com.nike.backstopper.annotation.post.processor;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.nike.backstopper.apierror.ApiErrorValue;
import com.nike.backstopper.model.ApiErrorValueMetadata;
import org.junit.Test;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static com.nike.backstopper.annotation.post.processor.TestUtils.readApiErrorValuesMetadata;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.JavaFileObject.Kind.OTHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link ApiErrorValueProcessor} using {@link Compiler#javac()}.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueProcessorTest {

    private static final String GENERATED_API_ERROR_METADATA_PATH = "/CLASS_OUTPUT/META-INF/api-error-value-metadata";
    private static final String EXPECTED_API_ERROR_METADATA_PATH = "META-INF/test/expected-api-error-value-metadata";

    private static final String TEST_API_ERROR_VALUE_MODEL_CLASS_PATH = "test/ApiErrorValueModelCompileTimeTest.java";
    private static final String TEST_API_ERROR_VALUE_WRONG_MODEL_CLASS_PATH = "test/ApiErrorValueWrongModelCompileTimeTest.java";

    @Test
    public void process() {
        ApiErrorValueProcessor apiErrorValueProcessor = new ApiErrorValueProcessor();

        Compilation compilationResult = executeAnnotationPostProcessor(
                apiErrorValueProcessor, TEST_API_ERROR_VALUE_MODEL_CLASS_PATH);
        Set<ApiErrorValueMetadata> generatedApiErrorValuesMetadata = findGeneratedApiErrorMetadata(compilationResult)
                .map(TestUtils::readApiErrorValuesMetadata)
                .orElseThrow(() -> new IllegalStateException("ApiErrorMetadata not found"));

        Set<ApiErrorValueMetadata> expectedApiErrorValuesMetadata = readApiErrorValuesMetadata(
                JavaFileObjects.forResource(EXPECTED_API_ERROR_METADATA_PATH));

        assertThat(generatedApiErrorValuesMetadata).isEqualTo(expectedApiErrorValuesMetadata);

        // ApiErrorValueProcessor should be processed once in the first round.
        RoundEnvironment roundEnvironment = mock(RoundEnvironment.class);
        apiErrorValueProcessor.process(Collections.emptySet(), roundEnvironment);

        verify(roundEnvironment, never()).getElementsAnnotatedWith(eq(ApiErrorValue.class));
    }

    @Test
    public void processInvalidApiErrorValues() {
        ApiErrorValueProcessor apiErrorValueProcessor = new ApiErrorValueProcessor();

        Compilation compilationResult = executeAnnotationPostProcessor(
                apiErrorValueProcessor, TEST_API_ERROR_VALUE_WRONG_MODEL_CLASS_PATH);

        assertThat(compilationResult.errors()).hasSize(14);
    }

    @Test
    public void processFailed() {
        ProcessingEnvironment processingEnvironment = mock(ProcessingEnvironment.class);
        RoundEnvironment roundEnvironment = mock(RoundEnvironment.class);
        Messager messager = mock(Messager.class);
        Element element = mock(Element.class);

        when(processingEnvironment.getMessager())
                .thenReturn(messager);
        when(roundEnvironment.getElementsAnnotatedWith(eq(ApiErrorValue.class)))
                .thenAnswer(invocation -> Collections.singleton(element));
        when(element.getAnnotationMirrors())
                .thenThrow(new RuntimeException("test exception"));

        ApiErrorValueProcessor apiErrorValueProcessor = new ApiErrorValueProcessor();
        apiErrorValueProcessor.init(processingEnvironment);
        apiErrorValueProcessor.process(Collections.emptySet(), roundEnvironment);

        verify(messager).printMessage(eq(ERROR), anyString());
    }

    private Compilation executeAnnotationPostProcessor(Processor annotationPostProcessor, String classToCompile) {
        return Compiler.javac()
                .withProcessors(annotationPostProcessor)
                .compile(JavaFileObjects.forResource(classToCompile));
    }

    private Optional<JavaFileObject> findGeneratedApiErrorMetadata(Compilation compilationResult) {
        return compilationResult.generatedFiles().stream()
                .filter(javaFileObject -> OTHER == javaFileObject.getKind())
                .filter(javaFileObject -> GENERATED_API_ERROR_METADATA_PATH.equals(javaFileObject.getName()))
                .reduce((javaFileObject, nextJavaFileObject) -> {
                    // one metadata file should be generated.
                    throw new IllegalStateException(String.format("Multiple ApiErrorMetadata files generated: %s, %s",
                            javaFileObject, nextJavaFileObject));
                });
    }

}
