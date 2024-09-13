package com.nike.backstopper.handler.spring.webflux;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Collection;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

/**
 * Similar to {@link com.nike.backstopper.handler.ApiExceptionHandlerUtils}, but provides helpers specific to this
 * Spring WebFlux Backstopper plugin module.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class SpringWebfluxApiExceptionHandlerUtils {

    /**
     * The default instance you can use if you don't need any customized logic. You can override this class and
     * its methods if you need alternate behavior.
     */
    public static final SpringWebfluxApiExceptionHandlerUtils DEFAULT_IMPL =
        new SpringWebfluxApiExceptionHandlerUtils();

    /**
     * Method for generating a {@link Mono} of {@link ServerResponse} that contains a serialized representation of the
     * given {@link DefaultErrorContractDTO} as its body (JSON serialization by default).
     *
     * <p>NOTE: make sure the {@link DefaultErrorContractDTO} is FULLY populated before calling this method! Changes to
     * the {@link DefaultErrorContractDTO} after calling this method may not be reflected in the
     * returned {@code Mono<ServerResponse>}.
     *
     * <p>The following two methods control the serialized representation of the error contract that will be used
     * as the {@link ServerResponse}'s body: {@link #serializeErrorContractToString(DefaultErrorContractDTO)} and
     * {@link #getErrorResponseContentType(DefaultErrorContractDTO, int, Collection, Throwable, RequestInfoForLogging)}.
     *
     * @return A {@link Mono} of {@link ServerResponse} that contains a serialized representation of the given
     * {@link DefaultErrorContractDTO}.
     */
    public Mono<ServerResponse> generateServerResponseForError(
        DefaultErrorContractDTO errorContractDTO,
        int httpStatusCode,
        Collection<ApiError> rawFilteredApiErrors,
        Throwable originalException,
        RequestInfoForLogging request
    ) {
        return ServerResponse
            .status(httpStatusCode)
            .contentType(
                getErrorResponseContentType(
                    errorContractDTO, httpStatusCode, rawFilteredApiErrors, originalException, request
                )
            ).bodyValue(serializeErrorContractToString(errorContractDTO));
    }

    protected String serializeErrorContractToString(DefaultErrorContractDTO errorContractDTO) {
        return JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(errorContractDTO);
    }

    @SuppressWarnings("unused")
    protected MediaType getErrorResponseContentType(
        DefaultErrorContractDTO errorContractDTO,
        int httpStatusCode,
        Collection<ApiError> rawFilteredApiErrors,
        Throwable originalException,
        RequestInfoForLogging request
    ) {
        // Default to simply application/json.
        return MediaType.APPLICATION_JSON;
    }
}
