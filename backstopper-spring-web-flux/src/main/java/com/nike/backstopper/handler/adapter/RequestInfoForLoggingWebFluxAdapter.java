package com.nike.backstopper.handler.adapter;

import com.nike.backstopper.handler.RequestInfoForLogging;

import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link RequestInfoForLogging} that knows how to handle Spring WebFlux {@link ServerRequest}.
 *
 * @author Nic Munroe
 */
public class RequestInfoForLoggingWebFluxAdapter implements RequestInfoForLogging {

    protected final @NotNull ServerRequest request;
    protected final @NotNull URI requestUri;

    public RequestInfoForLoggingWebFluxAdapter(@NotNull ServerRequest request) {
        //noinspection ConstantConditions
        if (request == null) {
            throw new NullPointerException("request cannot be null");
        }
        
        this.request = request;
        this.requestUri = request.uri();

        //noinspection ConstantValue
        if (requestUri == null) {
            throw new NullPointerException("request.uri() cannot be null");
        }
    }

    @Override
    public String getRequestUri() {
        return requestUri.getRawPath();
    }

    @Override
    public String getRequestHttpMethod() {
        HttpMethod method = request.method();
        //noinspection ConstantValue
        if (method == null) {
            return null;
        }
        return method.name();
    }

    @Override
    public String getQueryString() {
        return requestUri.getRawQuery();
    }

    @Override
    public Map<String, List<String>> getHeadersMap() {
        return request.headers().asHttpHeaders();
    }

    @Override
    public String getHeader(String headerName) {
        List<String> result = request.headers().header(headerName);
        //noinspection ConstantValue
        if (result == null || result.isEmpty()) {
            return null;
        }

        return result.get(0);
    }

    @Override
    public List<String> getHeaders(String headerName) {
        return request.headers().header(headerName);
    }

    @Override
    public Object getAttribute(String key) {
        return request.attribute(key).orElse(null);
    }

    @Override
    public String getBody() throws GetBodyException {
        // This may be technically possible ... somehow. But it's not readily obvious, and no need to waste a bunch
        //      of time trying to figure it out until it's actually something people want.
        throw new GetBodyException(
            "Cannot extract the body from a WebFlux ServerRequest.",
            new UnsupportedOperationException()
        );
    }
}
