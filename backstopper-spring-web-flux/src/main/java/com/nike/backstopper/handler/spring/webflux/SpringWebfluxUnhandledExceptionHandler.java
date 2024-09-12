package com.nike.backstopper.handler.spring.webflux;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.handler.UnhandledExceptionHandlerBase;
import com.nike.backstopper.handler.adapter.RequestInfoForLoggingWebFluxAdapter;
import com.nike.backstopper.handler.spring.webflux.SpringWebfluxApiExceptionHandler.ResponseContext;
import com.nike.backstopper.model.DefaultErrorContractDTO;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import reactor.core.publisher.Mono;

import static com.nike.backstopper.handler.spring.webflux.DisconnectedClientHelper.isClientDisconnectedException;

/**
 * An extension of {@link UnhandledExceptionHandlerBase} that acts as a final catch-all exception handler for
 * Spring WebFlux. This class translates *all* exceptions to a {@link ProjectApiErrors#getGenericServiceError()},
 * which is then converted to a {@link ServerResponse} for the caller.
 *
 * <p>This hooks into Spring WebFlux via its {@link WebExceptionHandler} interface, and specifically the
 * {@link WebExceptionHandler#handle(ServerWebExchange, Throwable)} method.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
public class SpringWebfluxUnhandledExceptionHandler
    extends UnhandledExceptionHandlerBase<Mono<ServerResponse>> implements WebExceptionHandler, Ordered {

    /**
     * The sort order for where this handler goes in the spring exception handler chain. We default to -2 so this gets
     * executed after any custom handlers, but before any default spring handlers (Spring Boot 3's
     * {@code DefaultErrorWebExceptionHandler} is ordered at -1, see {@code
     * ErrorWebFluxAutoConfiguration.errorWebExceptionHandler(...)}. And {@code WebFluxResponseStatusExceptionHandler}
     * is ordered at 0, see {@code WebFluxConfigurationSupport.responseStatusExceptionHandler(...)}).
     */
    private int order = -2;

    protected final Set<ApiError> singletonGenericServiceError;
    protected final int genericServiceErrorHttpStatusCode;

    protected final SpringWebfluxApiExceptionHandlerUtils springUtils;

    protected final List<HttpMessageReader<?>> messageReaders;
    protected final List<HttpMessageWriter<?>> messageWriters;
    protected final List<ViewResolver> viewResolvers;

    @Inject
    public SpringWebfluxUnhandledExceptionHandler(
        @NotNull ProjectApiErrors projectApiErrors,
        @NotNull ApiExceptionHandlerUtils generalUtils,
        @NotNull SpringWebfluxApiExceptionHandlerUtils springUtils,
        @NotNull ObjectProvider<ViewResolver> viewResolversProvider,
        @NotNull ServerCodecConfigurer serverCodecConfigurer
    ) {
        super(projectApiErrors, generalUtils);

        //noinspection ConstantConditions
        if (springUtils == null) {
            throw new NullPointerException("springUtils cannot be null.");
        }
        
        //noinspection ConstantConditions
        if (viewResolversProvider == null) {
            throw new NullPointerException("viewResolversProvider cannot be null.");
        }

        //noinspection ConstantConditions
        if (serverCodecConfigurer == null) {
            throw new NullPointerException("serverCodecConfigurer cannot be null.");
        }

        this.singletonGenericServiceError = Collections.singleton(projectApiErrors.getGenericServiceError());
        this.genericServiceErrorHttpStatusCode = projectApiErrors.getGenericServiceError().getHttpStatusCode();

        this.springUtils = springUtils;
        this.viewResolvers = viewResolversProvider.orderedStream().collect(Collectors.toList());
        this.messageReaders = serverCodecConfigurer.getReaders();
        this.messageWriters = serverCodecConfigurer.getWriters();
    }

    @Override
    protected Mono<ServerResponse> prepareFrameworkRepresentation(
        DefaultErrorContractDTO errorContractDTO,
        int httpStatusCode,
        Collection<ApiError> rawFilteredApiErrors,
        Throwable originalException,
        RequestInfoForLogging request
    ) {
        return springUtils.generateServerResponseForError(
            errorContractDTO, httpStatusCode, rawFilteredApiErrors, originalException, request
        );
    }

    @Override
    protected ErrorResponseInfo<Mono<ServerResponse>> generateLastDitchFallbackErrorResponseInfo(
        Throwable ex,
        RequestInfoForLogging request,
        String errorUid,
        Map<String, List<String>> headersForResponseWithErrorUid
    ) {
        DefaultErrorContractDTO errorContract = new DefaultErrorContractDTO(errorUid, singletonGenericServiceError);

        // We can't trust the springUtils in this class since it could be causing the problem.
        //      Use the DEFAULT_IMPL instead.
        Mono<ServerResponse> serverResponse =
            SpringWebfluxApiExceptionHandlerUtils.DEFAULT_IMPL.generateServerResponseForError(
                errorContract, genericServiceErrorHttpStatusCode, singletonGenericServiceError, ex, request
            );

        return new ErrorResponseInfo<>(
            genericServiceErrorHttpStatusCode,
            serverResponse,
            headersForResponseWithErrorUid
        );
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerRequest fluxRequest = ServerRequest.create(exchange, messageReaders);
        RequestInfoForLogging requestInfoForLogging = new RequestInfoForLoggingWebFluxAdapter(fluxRequest);

        ErrorResponseInfo<Mono<ServerResponse>> errorResponseInfo = handleException(ex, requestInfoForLogging);

        // Before we try to write the response, we should check to see if it's already committed, or if the client
        //      disconnected.
        // This short circuit logic due to an already-committed response or disconnected client was copied from
        //      Spring Boot's AbstractErrorWebExceptionHandler class.
        if (exchange.getResponse().isCommitted() || isClientDisconnectedException(ex)) {
            return Mono.error(ex);
        }

        // We handled the exception, and the response is still valid for output (not committed, and client still
        //      connected). Add any custom headers desired by the ErrorResponseInfo, and return a Mono that writes
        //      the response.
        processWebFluxResponse(errorResponseInfo, exchange.getResponse());

        return errorResponseInfo.frameworkRepresentationObj.flatMap(
            serverResponse -> write(exchange, serverResponse)
        );
    }

    protected void processWebFluxResponse(
        ErrorResponseInfo<Mono<ServerResponse>> errorResponseInfo,
        ServerHttpResponse response
    ) {
        // Add any extra headers from the ErrorResponseInfo.
        for (Map.Entry<String, List<String>> header : errorResponseInfo.headersToAddToResponse.entrySet()) {
            response.getHeaders().put(header.getKey(), header.getValue());
        }
    }

    // Copied and slightly modified from Spring Boot 3.3.3's AbstractErrorWebExceptionHandler class.
    protected Mono<? extends Void> write(ServerWebExchange exchange, ServerResponse response) {
        // force content-type since writeTo won't overwrite response header values
        exchange.getResponse().getHeaders().setContentType(response.headers().getContentType());
        return response.writeTo(exchange, new ResponseContext(messageWriters, viewResolvers));
    }

    /**
     * See the javadocs for {@link #order} for info on what this is for.
     */
    @Override
    public int getOrder() {
        return order;
    }

    /**
     * See the javadocs for {@link #order} for info on what this is for.
     */
    @SuppressWarnings("unused")
    public void setOrder(int order) {
        this.order = order;
    }
}
