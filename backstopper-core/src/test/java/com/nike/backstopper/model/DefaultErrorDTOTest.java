package com.nike.backstopper.model;

import com.nike.backstopper.apierror.ApiError;
import com.nike.internal.util.MapBuilder;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.ThrowableAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        switch(metadataArgOption) {
            case NULL:
                return null;
            case EMPTY:
                return new HashMap<>();
            case NOT_EMPTY:
                return MapBuilder.<String, Object>builder().put("foo", UUID.randomUUID().toString()).put("bar", 42).build();
            default:
                throw new IllegalArgumentException("Unhandled case: " + metadataArgOption);
        }
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
                assertThat(error.metadata)
                    .isNotSameAs(expectedMetadata)
                    .isEqualTo(expectedMetadata);
                Throwable ex = catchThrowable(new ThrowableAssert.ThrowingCallable() {
                    @Override
                    public void call() throws Throwable {
                        error.metadata.put("can't modify", "me");
                    }
                });
                assertThat(ex).isInstanceOf(UnsupportedOperationException.class);

                break;
            default:
                throw new IllegalArgumentException("Unhandled case: " + metadataArgOption);
        }
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
