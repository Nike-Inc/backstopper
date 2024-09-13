package com.nike.backstopper.handler.spring.webflux;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.testing.Glassbox;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import reactor.core.publisher.Mono;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests the functionality of {@link SpringWebfluxApiExceptionHandlerUtils}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class SpringWebfluxApiExceptionHandlerUtilsTest {

    private SpringWebfluxApiExceptionHandlerUtils utilsSpy;

    @Before
    public void beforeMethod() {
        utilsSpy = spy(new SpringWebfluxApiExceptionHandlerUtils());
    }

    @Test
    public void generateServerResponseForError_works_as_expected() {
        // given
        DefaultErrorContractDTO errorContractDtoMock = mock(DefaultErrorContractDTO.class);
        int statusCode = 400;
        @SuppressWarnings("unchecked")
        Collection<ApiError> errors = mock(Collection.class);
        Throwable ex = mock(Throwable.class);
        RequestInfoForLogging requestMock = mock(RequestInfoForLogging.class);

        String expectedSerializedContract = UUID.randomUUID().toString();
        doReturn(expectedSerializedContract).when(utilsSpy).serializeErrorContractToString(errorContractDtoMock);

        MediaType expectedResponseContentType = MediaType.IMAGE_JPEG;
        doReturn(expectedResponseContentType).when(utilsSpy).getErrorResponseContentType(
            errorContractDtoMock, statusCode, errors, ex, requestMock
        );

        // when
        Mono<ServerResponse> resultMono = utilsSpy.generateServerResponseForError(
            errorContractDtoMock, statusCode, errors, ex, requestMock
        );

        // then
        verify(utilsSpy).getErrorResponseContentType(errorContractDtoMock, statusCode, errors, ex, requestMock);
        verify(utilsSpy).serializeErrorContractToString(errorContractDtoMock);
        ServerResponse result = resultMono.block();
        assertThat(requireNonNull(result).statusCode().value()).isEqualTo(statusCode);
        assertThat(result.headers().getContentType()).isEqualTo(expectedResponseContentType);
        // Yes this is awful. But figuring out how to spit out the ServerResponse's body to something assertable
        //      in this test is also awful.
        assertThat(Glassbox.getInternalState(result, "entity")).isEqualTo(expectedSerializedContract);
    }

    @DataProvider(value = {
        "true   |   true",
        "true   |   false",
        "false  |   true",
        "false  |   false",
    }, splitBy = "\\|")
    @Test
    public void serializeErrorContractToString_uses_JsonUtilWithDefaultErrorContractDTOSupport_to_serialize_error_contract(
        boolean errorCodeIsAnInt, boolean includeExtraMetadata
    ) {
        // given
        List<ApiError> apiErrors = Arrays.asList(
            new ApiErrorBase(
                "FOO",
                (errorCodeIsAnInt) ? "42" : "fortytwo",
                "foo message",
                400,
                (includeExtraMetadata)
                ? MapBuilder.builder("foo", (Object)"bar").put("baz", "bat").build()
                : null
            ),
            new ApiErrorBase(
                "BAR",
                (errorCodeIsAnInt) ? "123" : "blahblah",
                "bar message",
                400,
                (includeExtraMetadata)
                ? MapBuilder.builder("stuff", (Object)"things").put("yay", "whee").build()
                : null
            )
        );
        DefaultErrorContractDTO errorContract = new DefaultErrorContractDTO(UUID.randomUUID().toString(), apiErrors);

        // when
        String result = utilsSpy.serializeErrorContractToString(errorContract);

        // then
        assertThat(result).isEqualTo(JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(errorContract));
    }

    @Test
    public void getErrorResponseContentType_returns_APPLICATION_JSON_UTF8_by_default() {
        // given
        DefaultErrorContractDTO errorContractDtoMock = mock(DefaultErrorContractDTO.class);
        int statusCode = 400;
        @SuppressWarnings("unchecked")
        Collection<ApiError> errors = mock(Collection.class);
        Throwable ex = mock(Throwable.class);
        RequestInfoForLogging requestMock = mock(RequestInfoForLogging.class);

        // when
        MediaType result = utilsSpy.getErrorResponseContentType(
            errorContractDtoMock, statusCode, errors, ex, requestMock
        );

        // then
        assertThat(result).isEqualTo(MediaType.APPLICATION_JSON);
        verifyNoMoreInteractions(errorContractDtoMock, errors, ex, requestMock);
    }

}
