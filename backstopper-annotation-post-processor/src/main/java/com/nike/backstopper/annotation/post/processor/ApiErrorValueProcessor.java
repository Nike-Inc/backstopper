package com.nike.backstopper.annotation.post.processor;

import com.nike.backstopper.annotation.post.processor.evaluator.AnnotationEvaluator;
import com.nike.backstopper.annotation.post.processor.extractor.ElementExtractor;
import com.nike.backstopper.annotation.post.processor.model.ElementDetails;
import com.nike.backstopper.annotation.post.processor.resolver.ApiErrorValueMetadataResolver;
import com.nike.backstopper.annotation.post.processor.scanner.ElementDetailsScanner;
import com.nike.backstopper.annotation.post.processor.writer.ApiErrorValueMetadataWriter;
import com.nike.backstopper.apierror.ApiErrorValue;
import com.nike.backstopper.model.ApiErrorValueMetadata;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * The annotation {@link Processor} that writes metadata file for {@link ApiErrorValue} annotation
 * including enclosed JSR 303 constraint annotations or a valid constraint annotation such as Hibernate/custom.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueProcessor extends AbstractProcessor {

    /**
     * The full name of {@link ApiErrorValue} annotation.
     */
    public static final String API_ERROR_VALUE_ANNOTATION_NAME = ApiErrorValue.class.getName();
    private final AtomicBoolean roundProcessed = new AtomicBoolean(false);

    private Messager messager;
    private ElementDetailsScanner elementDetailsScanner;
    private ApiErrorValueMetadataResolver apiErrorValueMetadataResolver;
    private ApiErrorValueMetadataWriter apiErrorValueMetadataWriter;

    /**
     * Initialize the annotation {@link Processor}, with:
     * {@link ElementExtractor}
     * {@link AnnotationEvaluator}
     * {@link ElementDetailsScanner}
     * {@link ApiErrorValueMetadataResolver}
     * {@link ApiErrorValueMetadataWriter}
     *
     * @param processingEnv a environment to get {@link Elements}, {@link Messager}, {@link Filer}.
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        ElementExtractor elementExtractor = new ElementExtractor(processingEnv.getElementUtils());
        AnnotationEvaluator annotationEvaluator = new AnnotationEvaluator(elementExtractor);

        messager = processingEnv.getMessager();
        elementDetailsScanner = new ElementDetailsScanner(elementExtractor);
        apiErrorValueMetadataResolver = new ApiErrorValueMetadataResolver(
                processingEnv.getMessager(), elementExtractor, annotationEvaluator);
        apiErrorValueMetadataWriter = new ApiErrorValueMetadataWriter(processingEnv.getFiler());
    }

    /**
     * Supports only {@link ApiErrorValue}.
     *
     * @return full name of {@link ApiErrorValue}.
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(API_ERROR_VALUE_ANNOTATION_NAME);
    }

    /**
     * Supports the latest versions of JDK, implemented without using deprecated/restricted API
     * such as in JDK 9 and later, the target build version is JDK 7.
     *
     * @return latest version of JDK.
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * Processes {@link ApiErrorValue} annotation including enclosed JSR 303 constraint annotations
     * or a valid constraint annotations such as Hibernate/custom and writes {@link ApiErrorValueMetadata}s
     * to {@code META-INF/api-error-value-metadata} file if valid metadata is collected.
     *
     * @param annotations      the supported {@link ApiErrorValue} annotation.
     * @param roundEnvironment the target round environment to get elements annotated with {@link ApiErrorValue}.
     * @return always {@code true}, will be processed only in the first round, to avoid recreating the metadata file.
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (!roundProcessed.getAndSet(true)) {

            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(ApiErrorValue.class);
            try {
                Set<ElementDetails> elementDetails = elementDetailsScanner.scan(elements);
                Set<ApiErrorValueMetadata> apiErrorValuesMetadata = apiErrorValueMetadataResolver.resolve(elementDetails);

                apiErrorValueMetadataWriter.write(apiErrorValuesMetadata);
            } catch (Exception e) {
                messager.printMessage(ERROR, String.format("Unable to process %s: %s, 'elements': %s",
                        API_ERROR_VALUE_ANNOTATION_NAME, e.getMessage(), elements));
            }
        }
        return true;
    }

}
