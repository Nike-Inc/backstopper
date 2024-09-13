package testonly.componenttest.spring.reusable.filter;

import com.nike.backstopper.exception.ApiException;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;
import testonly.componenttest.spring.reusable.error.SampleProjectApiError;

public class ExplodingWebFilter implements WebFilter {

    @Override
    public @NotNull Mono<Void> filter(ServerWebExchange exchange, @NotNull WebFilterChain chain) {
        HttpHeaders httpHeaders = exchange.getRequest().getHeaders();

        if ("true".equals(httpHeaders.getFirst("throw-web-filter-exception"))) {
            throw ApiException
                .newBuilder()
                .withApiErrors(SampleProjectApiError.ERROR_THROWN_IN_WEB_FILTER)
                .withExceptionMessage("Exception thrown from WebFilter")
                .build();
        }

        if ("true".equals(httpHeaders.getFirst("return-exception-in-web-filter-mono"))) {
            return Mono.error(
                ApiException
                    .newBuilder()
                    .withApiErrors(SampleProjectApiError.ERROR_RETURNED_IN_WEB_FILTER_MONO)
                    .withExceptionMessage("Exception returned from WebFilter Mono")
                    .build()
            );
        }

        return chain.filter(exchange);
    }
}
