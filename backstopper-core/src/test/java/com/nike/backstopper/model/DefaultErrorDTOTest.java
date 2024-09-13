package com.nike.backstopper.model;

import com.nike.backstopper.apierror.ApiError;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link DefaultErrorDTO}. Since there isn't really much functionality,
 * this just verifies that the constructors/etc work without blowing up.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
@SuppressWarnings("ClassEscapesDefinedScope")
public class DefaultErrorDTOTest {

    @Test
    public void default_constructor_should_not_blow_up() {
        // when
        DefaultErrorDTO error = new DefaultErrorDTO();

        // then
        assertThat(error.code).isNull();
        assertThat(error.message).isNull();
        assertThat(error.metadata).isNotNull().isEmpty();
    }

    private enum MetadataArgOption {
        NULL, EMPTY, NOT_EMPTY
    }

    private Map<String, Object> generateMetadata(MetadataArgOption metadataArgOption) {
        // We need to return a modifiable map, because Map.copyOf() (which DefaultErrorDTO uses to copy metadata passed
        //      in) is smart enough to return the original map if it's already unmodifiable, and our tests want to prove
        //      that it will do a deep copy if necessary.
        Map<String, Object> modifiableMetadataMap = new HashMap<>();
        modifiableMetadataMap.put("foo", UUID.randomUUID().toString());
        modifiableMetadataMap.put("bar", 42);

        return switch (metadataArgOption) {
            case NULL -> null;
            case EMPTY -> new HashMap<>();
            case NOT_EMPTY -> modifiableMetadataMap;
        };
    }

    private void verifyMetadata(final DefaultErrorDTO error, MetadataArgOption metadataArgOption, Map<String, Object> expectedMetadata) {
        switch(metadataArgOption) {
            case NULL:
            case EMPTY: // intentional fall-through
                assertThat(error.metadata)
                    .isNotNull()
                    .isEmpty();

                break;
            case NOT_EMPTY:
                assertThat(error.metadata).isEqualTo(expectedMetadata);

                if (isUnmodifiableMap(expectedMetadata)) {
                    // DefaultErrorDTO uses Map.copyOf() to copy the metadata, which is smart enough to return the
                    //      original when the original is unmodifiable.
                    assertThat(error.metadata).isSameAs(expectedMetadata);
                } else {
                    assertThat(error.metadata).isNotSameAs(expectedMetadata);
                }

                @SuppressWarnings("DataFlowIssue")
                Throwable ex = catchThrowable(() -> error.metadata.put("can't modify", "me"));
                assertThat(ex).isInstanceOf(UnsupportedOperationException.class);

                break;
            default:
                throw new IllegalArgumentException("Unhandled case: " + metadataArgOption);
        }
    }

    private boolean isUnmodifiableMap(Map<?, ?> map) {
        return Collections.unmodifiableMap(map).getClass().isInstance(map) || Map.copyOf(map) == map;
    }

    @DataProvider(value = {
        "NULL",
        "EMPTY",
        "NOT_EMPTY"
    }, splitBy = "\\|")
    @Test
    public void kitchen_sink_constructor_with_string_arg_works_as_expected(MetadataArgOption metadataArgOption) {
        // given
        String code = UUID.randomUUID().toString();
        String message = UUID.randomUUID().toString();
        Map<String, Object> metadata = generateMetadata(metadataArgOption);

        // when
        final DefaultErrorDTO error = new DefaultErrorDTO(code, message, metadata);

        // then
        assertThat(error.code).isEqualTo(code);
        assertThat(error.message).isEqualTo(message);
        verifyMetadata(error, metadataArgOption, metadata);
    }

    @DataProvider(value = {
        "NULL",
        "EMPTY",
        "NOT_EMPTY"
    }, splitBy = "\\|")
    @Test
    public void kitchen_sink_constructor_with_int_arg_works_as_expected(MetadataArgOption metadataArgOption) {
        // given
        int code = 42;
        String message = UUID.randomUUID().toString();
        Map<String, Object> metadata = generateMetadata(metadataArgOption);

        // when
        DefaultErrorDTO error = new DefaultErrorDTO(code, message, metadata);

        // then
        assertThat(error.code).isEqualTo(String.valueOf(code));
        assertThat(error.message).isEqualTo(message);
        verifyMetadata(error, metadataArgOption, metadata);
    }

    @DataProvider(value = {
        "NULL",
        "EMPTY",
        "NOT_EMPTY"
    }, splitBy = "\\|")
    @Test
    public void constructor_with_Error_object_arg_should_pull_values_from_Error_object(MetadataArgOption metadataArgOption) {
        // given
        DefaultErrorDTO copyError = new DefaultErrorDTO(UUID.randomUUID().toString(), UUID.randomUUID().toString(), generateMetadata(metadataArgOption));

        // when
        DefaultErrorDTO error = new DefaultErrorDTO(copyError);

        // then
        assertThat(error.code).isEqualTo(copyError.code);
        assertThat(error.message).isEqualTo(copyError.message);
        verifyMetadata(error, metadataArgOption, copyError.metadata);
    }

    @DataProvider(value = {
        "NULL",
        "EMPTY",
        "NOT_EMPTY"
    }, splitBy = "\\|")
    @Test
    public void constructor_with_ApiError_arg_should_pull_values_from_ApiError(MetadataArgOption metadataArgOption) {
        // given
        ApiError copyApiError = mock(ApiError.class);
        doReturn(UUID.randomUUID().toString()).when(copyApiError).getErrorCode();
        doReturn(UUID.randomUUID().toString()).when(copyApiError).getMessage();
        doReturn(generateMetadata(metadataArgOption)).when(copyApiError).getMetadata();

        // when
        DefaultErrorDTO error = new DefaultErrorDTO(copyApiError);

        // then
        assertThat(error.code).isEqualTo(copyApiError.getErrorCode());
        assertThat(error.message).isEqualTo(copyApiError.getMessage());
        verifyMetadata(error, metadataArgOption, copyApiError.getMetadata());
    }

}
