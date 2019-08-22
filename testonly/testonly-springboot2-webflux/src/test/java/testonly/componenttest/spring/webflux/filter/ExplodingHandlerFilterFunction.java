package testonly.componenttest.spring.webflux.filter;

import com.nike.backstopper.exception.ApiException;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;
import testonly.componenttest.spring.reusable.error.SampleProjectApiError;

public class ExplodingHandlerFilterFunction implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    @Override
    public Mono<ServerResponse> filter(
        ServerRequest serverRequest,
        HandlerFunction<ServerResponse> handlerFunction
    ) {
        HttpHeaders httpHeaders = serverRequest.headers().asHttpHeaders();

        if ("true".equals(httpHeaders.getFirst("throw-handler-filter-function-exception"))) {
            throw ApiException
                .newBuilder()
                .withApiErrors(SampleProjectApiError.ERROR_THROWN_IN_HANDLER_FILTER_FUNCTION)
                .withExceptionMessage("Exception thrown from HandlerFilterFunction")
                .build();
        }

        if ("true".equals(httpHeaders.getFirst("return-exception-in-handler-filter-function-mono"))) {
            return Mono.error(
                ApiException
                    .newBuilder()
                    .withApiErrors(SampleProjectApiError.ERROR_RETURNED_IN_HANDLER_FILTER_FUNCTION_MONO)
                    .withExceptionMessage("Exception returned from HandlerFilterFunction Mono")
                    .build()
            );
        }

        return handlerFunction.handle(serverRequest);
    }
}
