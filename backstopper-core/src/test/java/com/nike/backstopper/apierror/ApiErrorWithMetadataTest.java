package com.nike.backstopper.apierror;

import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.Pair;

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
    private final Pair<String, Object> extraMetadataOnlyPair = Pair.of("extraOnlyMetadata", extraMetadata.get("extraOnlyMetadata"));
    private final Pair<String, Object> extraMetadataSharedPair = Pair.of("foo", extraMetadata.get("foo"));

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
    public void cconvenience_onstructor_sets_delegate_and_combo_metadata_with_extra_metadata_overriding_delegate_for_same_named_metadata() {
        // given
        Map<String, Object> expectedMetadata = new HashMap<>();
        expectedMetadata.put("delegateOnlyMetadata", delegateMetadata.get("delegateOnlyMetadata"));
        expectedMetadata.put(extraMetadataOnlyPair.getKey(), extraMetadataOnlyPair.getValue());
        expectedMetadata.put(extraMetadataSharedPair.getKey(), extraMetadataSharedPair.getValue()); // foo comes from the extra, not the delegate's metadata.

        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithMetadata, extraMetadataOnlyPair, extraMetadataSharedPair);

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
        @SuppressWarnings("DataFlowIssue")
        Throwable ex = catchThrowable(() -> new ApiErrorWithMetadata(null, extraMetadata));

        // then
        assertThat(ex)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ApiError delegate cannot be null");
    }

    @Test
    public void convenience_constructor_throws_IllegalArgumentException_if_delegate_is_null() {
        // when
        @SuppressWarnings("DataFlowIssue")
        Throwable ex = catchThrowable(() -> new ApiErrorWithMetadata(null, Pair.of("foo", "bar")));

        // then
        assertThat(ex)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ApiError delegate cannot be null");
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
    public void convenience_constructor_supports_delegate_with_null_or_empty_metadata(boolean useNull) {
        // given
        ApiError delegateToUse = delegateWithoutMetadata;
        if (useNull) {
            delegateToUse = mock(ApiError.class);
            doReturn(null).when(delegateToUse).getMetadata();
        }

        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateToUse, extraMetadataOnlyPair, extraMetadataSharedPair);

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
        Map<String, Object> extraMetadataToUse = (useNull) ? null : Collections.emptyMap();

        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithMetadata, extraMetadataToUse);

        // then
        assertThat(awm.getMetadata()).isEqualTo(delegateMetadata);
    }

    @DataProvider(value = {
        "true",
        "false"
    })
    @Test
    public void convenience_constructor_supports_null_or_empty_extra_metadata(boolean useNull) {
        // given
        @SuppressWarnings("unchecked")
        Pair<String, Object>[] extraMetadataToUse = (useNull) ? null : new Pair[0];

        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithMetadata, extraMetadataToUse);

        // then
        assertThat(awm.getMetadata()).isEqualTo(delegateMetadata);
    }

    @Test
    public void constructor_supports_no_metadata_from_either_source() {
        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithoutMetadata, (Map<String, Object>)null);

        // then
        assertThat(awm.getMetadata()).isEmpty();
    }

    @Test
    public void convenience_constructor_supports_no_metadata_from_either_source() {
        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithoutMetadata, (Pair<String, Object>[])null);

        // then
        assertThat(awm.getMetadata()).isEmpty();
    }

    @Test
    public void convenience_constructor_gracefully_ignores_null_pairs() {
        // when
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(
            delegateWithoutMetadata, extraMetadataSharedPair, null, extraMetadataOnlyPair
        );

        // then
        assertThat(awm.getMetadata()).isEqualTo(extraMetadata);
    }

    @Test
    public void hashcode_is_as_expected() {
        // given
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithMetadata, extraMetadata);
        ApiErrorWithMetadata awm2 = new ApiErrorWithMetadata(delegateWithMetadata, extraMetadata);

        // then
        assertThat(awm).isEqualTo(awm2);
        assertThat(awm.hashCode()).isEqualTo(awm.hashCode());
        assertThat(awm.hashCode()).isEqualTo(awm2.hashCode());
    }

    @Test
    public void equals_same_object_is_true() {
        // given
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithMetadata, extraMetadata);

        // then
        //noinspection EqualsWithItself
        assertThat(awm.equals(awm)).isTrue();
    }

    @Test
    @DataProvider(value = {
            "true",
            "false"
    })
    public void equals_null_or_other_class_is_false(boolean useNull) {
        // given
        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(delegateWithMetadata, extraMetadata);

        String otherClass = useNull? null : "";

        // then
        //noinspection EqualsBetweenInconvertibleTypes
        assertThat(awm.equals(otherClass)).isFalse();
    }

    @Test
    @DataProvider(value = {
            "true  | false | false | false | false | false | false ",
            "false | false | false | false | false | true  | true  ",
            "false | true  | false | false | false | false | false ",
            "false | false | true  | false | false | false | false ",
            "false | false | false | true  | false | false | false ",
            "false | false | false | false | true  | false | false ",
            "false | false | false | false | false | false | false ",
    }, splitBy = "\\|")
    public void equals_returns_expected_result(boolean changeName, boolean changeErrorCode, boolean changeErrorMessage, boolean changeHttpStatusCode, boolean changeMetadata, boolean hasExtraMetadata, boolean isEqual) {
        // given
        String name = "someName";
        int errorCode = 42;
        String message = "some error";
        int httpStatusCode = 400;
        Map<String, Object> metadata = MapBuilder.<String, Object>builder().put("foo", UUID.randomUUID().toString()).build();
        Map<String, Object> metadata2 = MapBuilder.<String, Object>builder().put("foo", UUID.randomUUID().toString()).build();

        ApiErrorBase aeb = new ApiErrorBase(name, errorCode, message, httpStatusCode, metadata);
        ApiErrorBase aeb2 = new ApiErrorBase(
                changeName? "name2" : name ,
                changeErrorCode? 43 : errorCode,
                changeErrorMessage? "message2" : message,
                changeHttpStatusCode? 500 : httpStatusCode,
                changeMetadata? metadata2 : metadata);

        ApiErrorWithMetadata awm = new ApiErrorWithMetadata(aeb, extraMetadata);
        ApiErrorWithMetadata awm2 = new ApiErrorWithMetadata(aeb2, hasExtraMetadata? extraMetadata : Collections.emptyMap());

        // then
        assertThat(awm.equals(awm2)).isEqualTo(isEqual);
    }
}