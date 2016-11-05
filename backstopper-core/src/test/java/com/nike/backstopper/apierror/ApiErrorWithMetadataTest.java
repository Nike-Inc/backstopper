package com.nike.backstopper.apierror;

import com.nike.internal.util.MapBuilder;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.assertj.core.api.ThrowableAssert;
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
 * Tests the functionality of {@link ApiErrorWithMetadata}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class ApiErrorWithMetadataTest {

    private final Map<String, Object> delegateMetadata =
        MapBuilder.builder("foo", (Object) UUID.randomUUID().toString())
                  .put("delegateOnlyMetadata", UUID.randomUUID().toString())
                  .build();
    private final Map<String, Object> extraMetadata =
        MapBuilder.builder("foo", (Object) UUID.randomUUID().toString())
                  .put("extraOnlyMetadata", UUID.randomUUID().toString())
                  .build();

    private final ApiError delegateWithMetadata = new ApiErrorBase(
        "with_metadata", 42, "some error with metadata", 400, delegateMetadata
    );
    private final ApiError delegateWithoutMetadata = new ApiErrorBase(
        "without_metadata", 4242, "some error without metadata", 500
    );

    @Test
    public void constructor_sets_delegate_and_combo_metadata_with_extra_metadata_overriding_delegate_for_same_named_metadata() {
        // given
        Map<String, Object> expectedMetadata = new HashMap<>();
        expectedMetadata.put("delegateOnlyMetadata", delegateMetadata.get("delegateOnlyMetadata"));
        expectedMetadata.put("extraOnlyMetadata", extraMetadata.get("extraOnlyMetadata"));
        expectedMetadata.put("foo", extraMetadata.get("foo")); // foo comes from the extra, not the delegate's metadata.

        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithMetadata, extraMetadata);

        // then
        assertThat(awm.getName()).isEqualTo(delegateWithMetadata.getName());
        assertThat(awm.getErrorCode()).isEqualTo(delegateWithMetadata.getErrorCode());
        assertThat(awm.getMessage()).isEqualTo(delegateWithMetadata.getMessage());
        assertThat(awm.getHttpStatusCode()).isEqualTo(delegateWithMetadata.getHttpStatusCode());
        assertThat(awm.getMetadata()).isEqualTo(expectedMetadata);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_delegate_is_null() {
        // when
        Throwable ex = catchThrowable(new ThrowableAssert.ThrowingCallable() {
            @Override
            public void call() throws Throwable {
                new ApiErrorWithMetadata(null, extraMetadata);
            }
        });

        // then
        assertThat(ex).isInstanceOf(IllegalArgumentException.class);
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void constructor_supports_delegate_with_null_or_empty_metadata(boolean useNull) {
        // given
        ApiError delegateToUse = delegateWithoutMetadata;
        if (useNull) {
            delegateToUse = mock(ApiError.class);
            doReturn(null).when(delegateToUse).getMetadata();
        }

        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateToUse, extraMetadata);

        // then
        assertThat(awm.getMetadata()).isEqualTo(extraMetadata);
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void constructor_supports_null_or_empty_extra_metadata(boolean useNull) {
        // given
        Map<String, Object> extraMetadataToUse = (useNull) ? null : Collections.<String, Object>emptyMap();

        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithMetadata, extraMetadataToUse);

        // then
        assertThat(awm.getMetadata()).isEqualTo(delegateMetadata);
    }

    @Test
    public void constructor_supports_no_metadata_from_either_source() {
        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithoutMetadata, null);

        // then
        assertThat(awm.getMetadata()).isEmpty();
    }
}