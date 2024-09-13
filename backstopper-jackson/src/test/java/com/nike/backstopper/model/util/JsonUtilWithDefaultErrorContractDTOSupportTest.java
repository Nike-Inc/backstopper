package com.nike.backstopper.model.util;

import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.DefaultErrorDTO;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport.ErrorContractSerializationFactory;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport.MetadataPropertyWriter;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport.SmartErrorCodePropertyWriter;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.testing.Glassbox;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

/**
 * Verifies the functionality of {@link JsonUtilWithDefaultErrorContractDTOSupport}
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class JsonUtilWithDefaultErrorContractDTOSupportTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private void verifyErrorContractsMatch(DefaultErrorContractDTO actual, DefaultErrorContractDTO expected) {
        assertThat(actual.error_id).isEqualTo(expected.error_id);
        assertThat(actual.errors.size()).isEqualTo(expected.errors.size());
        for (int i = 0; i < actual.errors.size(); i++) {
            DefaultErrorDTO actualError = actual.errors.get(i);
            DefaultErrorDTO expectedError = expected.errors.get(i);
            verifyErrorsMatch(actualError, expectedError);
        }
    }

    private void verifyErrorsMatch(DefaultErrorDTO actual, DefaultErrorDTO expected) {
        assertThat(actual.code).isEqualTo(expected.code);
        assertThat(actual.message).isEqualTo(expected.message);
        assertThat(actual.metadata).isEqualTo(expected.metadata);
    }

    @Test
    public void writeValueAsString_serializes_Error_with_metadata_if_metadata_exists() throws IOException {
        // given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("meta", UUID.randomUUID().toString());

        DefaultErrorDTO error = new DefaultErrorDTO(42, "bar", metadata);

        // when
        String resultString = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(error);

        // then
        assertThat(resultString).contains("metadata");
        DefaultErrorDTO result = objectMapper.readValue(resultString, DefaultErrorDTO.class);
        verifyErrorsMatch(result, error);
    }

    @Test
    public void writeValueAsString_serializes_Error_without_metadata_if_metadata_is_missing() throws IOException {
        // given
        DefaultErrorDTO error = new DefaultErrorDTO(42, "bar", null);

        // when
        String resultString = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(error);

        // then
        assertThat(resultString).doesNotContain("metadata");
        DefaultErrorDTO result = objectMapper.readValue(resultString, DefaultErrorDTO.class);
        verifyErrorsMatch(result, error);
    }

    @Test
    public void writeValueAsString_serializes_ErrorContract_with_metadata_if_metadata_exists() throws IOException {
        // given
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("meta", UUID.randomUUID().toString());
        DefaultErrorDTO withMetadata = new DefaultErrorDTO(42, UUID.randomUUID().toString(), metadata);
        DefaultErrorDTO noMetadata = new DefaultErrorDTO(43, UUID.randomUUID().toString(), null);
        DefaultErrorContractDTO
            contract = new DefaultErrorContractDTO(UUID.randomUUID().toString(), Arrays.asList(withMetadata, noMetadata), null);

        // when
        String resultString = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(contract);

        // then
        DefaultErrorContractDTO result = objectMapper.readValue(resultString, DefaultErrorContractDTO.class);
        verifyErrorContractsMatch(result, contract);
    }

    @Test
    public void writeValueAsString_serializes_Error_with_code_as_JSON_number_if_possible() throws IOException {
        // given
        DefaultErrorDTO error = new DefaultErrorDTO(42, "bar", null);

        // when
        String resultString = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(error);

        // then
        assertThat(resultString).contains("\"code\":42");
        DefaultErrorDTO result = objectMapper.readValue(resultString, DefaultErrorDTO.class);
        verifyErrorsMatch(result, error);
    }

    private void verifyResultIsDefaultErrorContract(String result) throws IOException {
        DefaultErrorContractDTO defaultResponse = objectMapper.readValue(JsonUtilWithDefaultErrorContractDTOSupport.DEFAULT_ERROR_RESPONSE_STRING, DefaultErrorContractDTO.class);
        DefaultErrorContractDTO resultContract = objectMapper.readValue(result, DefaultErrorContractDTO.class);
        assertThat(resultContract.errors)
            .hasSameSizeAs(defaultResponse.errors)
            .isNotEmpty();
        for (int i = 0; i < defaultResponse.errors.size(); i++) {
            DefaultErrorDTO expectedError = defaultResponse.errors.get(i);
            DefaultErrorDTO actualError = resultContract.errors.get(i);
            verifyErrorsMatch(actualError, expectedError);
        }
    }

    @Test
    public void writeValueAsString_returns_generic_response_if_error_occurs_during_serialization() throws IOException {
        // given
        Object blowup = mock(Object.class); // Jackson doesn't like Mockito mocks

        // when
        String resultString = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(blowup);

        // then
        assertThat(resultString).isNotNull();
        verifyResultIsDefaultErrorContract(resultString);
    }

    @Test
    public void writeValueAsString_serializes_non_Error_objects_like_a_default_ObjectMapper() throws IOException {
        // given
        NonErrorObject nonErrorObject = new NonErrorObject("foo", 42, 42.42, MapBuilder.builder("bar", UUID.randomUUID().toString()).build());
        String defaultSerialization = objectMapper.writeValueAsString(nonErrorObject);

        // when
        String resultString = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(nonErrorObject);

        // then
        assertThat(resultString).isEqualTo(defaultSerialization);
    }

    private static class NonErrorObject {
        public final String someString;
        public final Integer someInt;
        public final Double someDouble;
        public final Map<String, String> someMap;

        private NonErrorObject(String someString, Integer someInt, Double someDouble, Map<String, String> someMap) {
            this.someString = someString;
            this.someInt = someInt;
            this.someDouble = someDouble;
            this.someMap = someMap;
        }
    }

    @DataProvider(value = {
        "foo",
        "42.42"
    }, splitBy = "\\|")
    @Test
    public void writeValueAsString_serializes_Error_with_code_as_string_if_code_is_not_parseable_to_an_integer(String notAnInt) throws IOException {
        // given
        DefaultErrorDTO error = new DefaultErrorDTO(notAnInt, "bar", null);

        // when
        String resultString = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(error);

        // then
        assertThat(resultString).contains("\"code\":\"" + notAnInt + "\"");
        DefaultErrorDTO result = objectMapper.readValue(resultString, DefaultErrorDTO.class);
        verifyErrorsMatch(result, error);
    }

    @DataProvider(value = {
        "true   |   true",
        "true   |   false",
        "false  |   true",
        "false  |   false"
    }, splitBy = "\\|")
    @Test
    public void generateErrorContractObjectMapper_builds_serializers_as_expected(
        boolean excludeEmptyMetadataFromJson, boolean serializeErrorCodeFieldAsIntegerIfPossible
    ) throws IOException {
        ObjectMapper customMapper = JsonUtilWithDefaultErrorContractDTOSupport
            .generateErrorContractObjectMapper(excludeEmptyMetadataFromJson, serializeErrorCodeFieldAsIntegerIfPossible);

        {
            // given
            DefaultErrorDTO error = new DefaultErrorDTO(42, "bar", null);

            // when
            String resultString = customMapper.writeValueAsString(error);

            // then
            if (excludeEmptyMetadataFromJson)
                assertThat(resultString).doesNotContain("metadata");
            else
                assertThat(resultString).contains("metadata");

            if (serializeErrorCodeFieldAsIntegerIfPossible)
                assertThat(resultString).contains("\"code\":42");
            else
                assertThat(resultString).contains("\"code\":\"42\"");

            DefaultErrorDTO result = objectMapper.readValue(resultString, DefaultErrorDTO.class);
            verifyErrorsMatch(result, error);
        }

        {
            // and given
            DefaultErrorDTO notAnIntCodeError = new DefaultErrorDTO("foo", "bar", null);

            // when
            String resultString = customMapper.writeValueAsString(notAnIntCodeError);

            // then
            assertThat(resultString).contains("\"code\":\"foo\"");

            DefaultErrorDTO result = objectMapper.readValue(resultString, DefaultErrorDTO.class);
            verifyErrorsMatch(result, notAnIntCodeError);
        }
    }

    @Test
    public void writeValueAsString_uses_DEFAULT_ERROR_RESPONSE_STRING_if_defaultResponseIfErrorDuringSerialization_is_null()
        throws IOException {
        // given
        Object blowup = mock(Object.class); // Jackson doesn't like Mockito mocks

        // when
        String result = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(blowup, objectMapper, null);

        // then
        verifyResultIsDefaultErrorContract(result);
    }

    @Test
    public void code_coverage_hoops() {
        new JsonUtilWithDefaultErrorContractDTOSupport();
    }

    @Test
    public void ErrorContractSerializationFactory_findPropWriter_returns_null_if_it_cannot_find_() {
        // given
        ErrorContractSerializationFactory impl = new ErrorContractSerializationFactory(null, true, true);

        // when
        BeanPropertyWriter result = impl.findPropWriter(Collections.emptyList(), UUID.randomUUID().toString());

        // then
        assertThat(result).isNull();
    }

    @Test
    public void writeValueAsString_works_for_non_Error_objects() {
        // given
        FooClass foo = new FooClass();

        // when
        String result = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(foo);

        // then
        assertThat(result).isEqualTo("{\"metadata\":\"foo\",\"code\":\"42\"}");
    }

    @Test
    public void writeValueAsString_does_not_blow_up_on_null_metadata() {
        // given
        DefaultErrorDTO error = new DefaultErrorDTO(42, "bar", null);
        Glassbox.setInternalState(error, "metadata", null);

        // when
        String result = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(error);

        // then
        assertThat(result).isEqualTo("{\"code\":42,\"message\":\"bar\"}");
    }

    @Test
    public void MetadataPropertyWriter_serializeAsField_still_works_for_non_Error_objects() {
        // given
        final MetadataPropertyWriter mpw = new MetadataPropertyWriter(mock(BeanPropertyWriter.class));

        // when
        Throwable ex = catchThrowable(
            () -> mpw.serializeAsField(new Object(), mock(JsonGenerator.class), mock(SerializerProvider.class)));

        // then
        // We expect a NPE because mocking a base BeanPropertyWriter is incredibly difficult and not worth the effort.
        assertThat(ex).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void SmartErrorCodePropertyWriter_serializeAsField_still_works_for_non_Error_objects() {
        // given
        final SmartErrorCodePropertyWriter secpw = new SmartErrorCodePropertyWriter(mock(BeanPropertyWriter.class));

        // when
        Throwable ex = catchThrowable(
            () -> secpw.serializeAsField(new Object(), mock(JsonGenerator.class), mock(SerializerProvider.class)));

        // then
        // We expect a NPE because mocking a base BeanPropertyWriter is incredibly difficult and not worth the effort.
        assertThat(ex).isInstanceOf(NullPointerException.class);
    }

    public static class FooClass {
        public String metadata = "foo";
        public String code = "42";
    }

}