package testonly.componenttest.spring.reusable.testutil;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.DefaultErrorDTO;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import io.restassured.response.ExtractableResponse;
import testonly.componenttest.spring.reusable.controller.SampleController;
import testonly.componenttest.spring.reusable.model.SampleModel;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contains helpers to be used by {@code testonly-spring*} component tests.
 *
 * @author Nic Munroe
 */
public class TestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void verifyErrorReceived(ExtractableResponse<?> response, ApiError expectedError) {
        verifyErrorReceived(response, singleton(expectedError), expectedError.getHttpStatusCode());
    }

    public static DefaultErrorDTO findErrorMatching(DefaultErrorContractDTO errorContract, ApiError desiredError) {
        for (DefaultErrorDTO error : errorContract.errors) {
            if (error.code.equals(desiredError.getErrorCode()) && error.message.equals(desiredError.getMessage())) {
                return error;
            }
        }

        return null;
    }

    public static void verifyErrorReceived(
        ExtractableResponse<?> response,
        Collection<ApiError> expectedErrors,
        int expectedHttpStatusCode
    ) {
        assertThat(response.statusCode()).isEqualTo(expectedHttpStatusCode);
        try {
            DefaultErrorContractDTO errorContract = objectMapper.readValue(
                response.asString(), DefaultErrorContractDTO.class
            );
            assertThat(errorContract.error_id).isNotEmpty();
            assertThat(UUID.fromString(errorContract.error_id)).isNotNull();
            assertThat(errorContract.errors).hasSameSizeAs(expectedErrors);
            for (ApiError apiError : expectedErrors) {
                DefaultErrorDTO matchingError = findErrorMatching(errorContract, apiError);
                assertThat(matchingError).isNotNull();
                assertThat(matchingError.code).isEqualTo(apiError.getErrorCode());
                assertThat(matchingError.message).isEqualTo(apiError.getMessage());
                assertThat(matchingError.metadata).isEqualTo(apiError.getMetadata());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SampleModel randomizedSampleModel() {
        return new SampleModel(
            UUID.randomUUID().toString(),
            String.valueOf(SampleController.nextRangeInt(0, 42)),
            SampleController.nextRandomColor().name(),
            false
        );
    }
}
