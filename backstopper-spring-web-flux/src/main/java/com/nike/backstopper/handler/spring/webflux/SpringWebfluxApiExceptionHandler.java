package com.nike.backstopper.handler.spring.webflux;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerBase;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.handler.UnexpectedMajorExceptionHandlingError;
import com.nike.backstopper.handler.adapter.RequestInfoForLoggingWebFluxAdapter;
import com.nike.backstopper.handler.spring.webflux.listener.SpringWebFluxApiExceptionHandlerListenerList;
import com.nike.backstopper.model.DefaultErrorContractDTO;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.NestedExceptionUtils;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import reactor.core.publisher.Mono;

/**
 * An {@link ApiExceptionHandlerBase} extension that hooks into Spring WebFlux via its
 * {@link WebExceptionHandler} interface, and specifically the
 * {@link WebExceptionHandler#handle(ServerWebExchange, Throwable)} method.
 *
 * <p>Any errors not handled here are things we don't know how to deal with and will fall through to
 * {@link SpringWebfluxUnhandledExceptionHandler}.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class SpringWebfluxApiExceptionHandler extends ApiExceptionHandlerBase<Mono<ServerResponse>>
    implements WebExceptionHandler, Ordered {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Copied from Spring WebFlux {@link org.springframework.web.server.adapter.HttpWebHandlerAdapter}.
     */
    public static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS = new HashSet<>(
        Arrays.asList("AbortedException", "ClientAbortException", "EOFException", "EofException"));

    /**
     * The sort order for where this handler goes in the spring exception handler chain. We default to {@link
     * Ordered#HIGHEST_PRECEDENCE}, so that this is tried first before any other handlers.
     */
    private int order = Ordered.HIGHEST_PRECEDENCE;

    protected final SpringWebfluxApiExceptionHandlerUtils springUtils;

    protected final List<HttpMessageReader<?>> messageReaders;
    protected final List<HttpMessageWriter<?>> messageWriters;
    protected final List<ViewResolver> viewResolvers;
    
    @Inject
    public SpringWebfluxApiExceptionHandler(
        @NotNull ProjectApiErrors projectApiErrors,
        @NotNull SpringWebFluxApiExceptionHandlerListenerList apiExceptionHandlerListeners,
        @NotNull ApiExceptionHandlerUtils generalUtils,
        @NotNull SpringWebfluxApiExceptionHandlerUtils springUtils,
        @NotNull ObjectProvider<ViewResolver> viewResolversProvider,
        @NotNull ServerCodecConfigurer serverCodecConfigurer
    ) {
        super(projectApiErrors, apiExceptionHandlerListeners.listeners, generalUtils);

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
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerRequest fluxRequest = ServerRequest.create(exchange, messageReaders);
        RequestInfoForLogging requestInfoForLogging = new RequestInfoForLoggingWebFluxAdapter(fluxRequest);

        ErrorResponseInfo<Mono<ServerResponse>> errorResponseInfo;
        try {
            errorResponseInfo = maybeHandleException(
                ex, requestInfoForLogging
            );
        }
        catch (UnexpectedMajorExceptionHandlingError ohNoException) {
            logger.error(
                "Unexpected major error while handling exception. "
                + SpringWebfluxUnhandledExceptionHandler.class.getName() + " should handle it.", ohNoException
            );
            return Mono.error(ex);
        }

        if (errorResponseInfo == null) {
            // We didn't know how to handle the exception, so return Mono.error(ex) to indicate that error handing
            //      should continue.
            return Mono.error(ex);
        }

        // Before we try to write the response, we should check to see if it's already committed, or if the client
        //      disconnected.
        // This short circuit logic due to an already-committed response or disconnected client was copied from
        //      Spring Boot 2's AbstractErrorWebExceptionHandler class.
        if (exchange.getResponse().isCommitted() || isDisconnectedClientError(ex)) {
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

    // Copied from Spring Boot 2's AbstractErrorWebExceptionHandler class.
    protected Mono<? extends Void> write(ServerWebExchange exchange, ServerResponse response) {
        // force content-type since writeTo won't overwrite response header values
        exchange.getResponse().getHeaders().setContentType(response.headers().getContentType());
        return response.writeTo(exchange, new ResponseContext(messageWriters, viewResolvers));
    }

    /**
     * Copied from Spring Boot 2's {@code AbstractErrorWebExceptionHandler} class.
     */
    public static boolean isDisconnectedClientError(Throwable ex) {
        return DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.getClass().getSimpleName())
               || isDisconnectedClientErrorMessage(NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
    }

    /**
     * Copied from Spring Boot 2's {@code AbstractErrorWebExceptionHandler} class.
     */
    public static boolean isDisconnectedClientErrorMessage(String message) {
        message = (message != null) ? message.toLowerCase() : "";
        return (message.contains("broken pipe") || message.contains("connection reset by peer"));
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
    @SuppressWarnings({"unused"})
    public void setOrder(int order) {
        this.order = order;
    }

    public static class ResponseContext implements ServerResponse.Context {

        private final List<HttpMessageWriter<?>> messageWriters;
        private final List<ViewResolver> viewResolvers;

        protected ResponseContext(
            List<HttpMessageWriter<?>> messageWriters,
            List<ViewResolver> viewResolvers
        ) {
            this.messageWriters = messageWriters;
            this.viewResolvers = viewResolvers;
        }

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return messageWriters;
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return viewResolvers;
        }
    }
}
