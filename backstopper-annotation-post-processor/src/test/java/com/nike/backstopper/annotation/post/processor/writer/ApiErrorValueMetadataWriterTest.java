package com.nike.backstopper.annotation.post.processor.writer;

import com.nike.backstopper.annotation.post.processor.exception.ApiErrorValueMetadataWriterException;
import com.nike.backstopper.model.ApiErrorValueMetadata;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.processing.Filer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static com.nike.backstopper.annotation.post.processor.TestUtils.readApiErrorValuesMetadata;
import static javax.tools.StandardLocation.CLASS_OUTPUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the functionality of {@link ApiErrorValueMetadataWriter}.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueMetadataWriterTest {

    public static final ApiErrorValueMetadata API_ERROR_VALUE_METADATA = new ApiErrorValueMetadata(
            "errorCodeValue", 400, "messageValue");

    private Filer filer;
    private ApiErrorValueMetadataWriter apiErrorValueMetadataWriter;

    @Before
    public void setUp() {
        filer = mock(Filer.class, RETURNS_DEEP_STUBS);
        apiErrorValueMetadataWriter = new ApiErrorValueMetadataWriter(filer);
    }

    @Test
    public void write() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        when(filer.createResource(eq(CLASS_OUTPUT), anyString(), anyString()).openOutputStream())
                .thenReturn(byteArrayOutputStream);

        apiErrorValueMetadataWriter.write(Collections.singleton(API_ERROR_VALUE_METADATA));

        Set<ApiErrorValueMetadata> wroteApiErrorValuesMetadata = readApiErrorValuesMetadata(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

        assertThat(wroteApiErrorValuesMetadata).containsOnly(API_ERROR_VALUE_METADATA);
    }

    @Test
    public void writeEmptyApiErrorValuesMetadata() throws Exception {
        apiErrorValueMetadataWriter.write(Collections.emptySet());

        verifyZeroInteractions(filer.createResource(eq(CLASS_OUTPUT), anyString(), anyString()).openOutputStream());
    }

    @Test(expected = ApiErrorValueMetadataWriterException.class)
    public void writeFailed() throws Exception {
        when(filer.createResource(eq(CLASS_OUTPUT), anyString(), anyString()).openOutputStream())
                .thenThrow(new IOException("test exception"));

        apiErrorValueMetadataWriter.write(Collections.singleton(API_ERROR_VALUE_METADATA));
    }

}
