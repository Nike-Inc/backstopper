package com.nike.backstopper.model;

import org.junit.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the functionality of {@link ApiErrorValueMetadata}.
 *
 * @author Andrey Tsarenko
 */
public class ApiErrorValueMetadataTest {

    @Test
    public void getters() {
        ApiErrorValueMetadata apiErrorValueMetadata = new ApiErrorValueMetadata("errorCode", 400, "message");

        assertThat(apiErrorValueMetadata.getErrorCode()).isEqualTo("errorCode");
        assertThat(apiErrorValueMetadata.getHttpStatusCode()).isEqualTo(400);
        assertThat(apiErrorValueMetadata.getMessage()).isEqualTo("message");
    }

    @Test
    public void equals() {
        ApiErrorValueMetadata firstApiErrorValueMetadata = new ApiErrorValueMetadata("errorCode", 400, "message");
        ApiErrorValueMetadata secondApiErrorValueMetadata = new ApiErrorValueMetadata("errorCode", 400, "message");
        ApiErrorValueMetadata thirdApiErrorValueMetadata = new ApiErrorValueMetadata("secondErrorCode", 400, "message");

        assertThat(firstApiErrorValueMetadata).isEqualTo(firstApiErrorValueMetadata);
        assertThat(firstApiErrorValueMetadata).isEqualTo(secondApiErrorValueMetadata);

        assertThat(firstApiErrorValueMetadata).isNotEqualTo(thirdApiErrorValueMetadata);
        assertThat(firstApiErrorValueMetadata).isNotEqualTo(new Object());
        assertThat(firstApiErrorValueMetadata).isNotEqualTo(null);
    }

    @Test
    public void hashcode() {
        ApiErrorValueMetadata apiErrorValueMetadata = new ApiErrorValueMetadata("errorCode", 400, "message");

        assertThat(apiErrorValueMetadata.hashCode()).isEqualTo(
                Objects.hash("errorCode", 400, "message"));
    }

}
