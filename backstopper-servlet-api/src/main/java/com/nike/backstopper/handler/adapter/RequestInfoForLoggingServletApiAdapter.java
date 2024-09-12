package com.nike.backstopper.handler.adapter;

import com.nike.backstopper.handler.RequestInfoForLogging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Adapter that allows {@link HttpServletRequest} to be used as a {@link RequestInfoForLogging}.
 *
 * @author Nic Munroe
 */
public class RequestInfoForLoggingServletApiAdapter implements RequestInfoForLogging {

    private final static Logger logger = LoggerFactory.getLogger(RequestInfoForLoggingServletApiAdapter.class);

    private final HttpServletRequest request;
    private Map<String, List<String>> headersMapCache;

    public RequestInfoForLoggingServletApiAdapter(HttpServletRequest request) {
        if (request == null)
            throw new IllegalArgumentException("request cannot be null");

        this.request = request;
    }

    @Override
    public String getRequestUri() {
        return request.getRequestURI();
    }

    @Override
    public String getRequestHttpMethod() {
        return request.getMethod();
    }

    @Override
    public String getQueryString() {
        return request.getQueryString();
    }

    @Override
    public Map<String, List<String>> getHeadersMap() {
        if (headersMapCache == null) {
            Map<String, List<String>> headersMap = new HashMap<>();

            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String nextHeaderName = headerNames.nextElement();
                    Enumeration<String> headerValues = request.getHeaders(nextHeaderName);
                    if (headerValues != null) {
                        headersMap.put(nextHeaderName, Collections.list(headerValues));
                    }
                }
            }

            headersMapCache = headersMap;
        }

        return headersMapCache;
    }

    @Override
    public String getHeader(String headerName) {
        return request.getHeader(headerName);
    }

    @Override
    public List<String> getHeaders(String headerName) {
        return getHeadersMap().get(headerName);
    }

    @Override
    public Object getAttribute(String key) {
        return request.getAttribute(key);
    }

    @Override
    public String getBody() throws GetBodyException {
        ServletInputStream is = null;
        Reader reader = null;
        try {
            is = request.getInputStream();
            reader = new BufferedReader(
                new InputStreamReader(is, request.getCharacterEncoding() != null
                                          ? request.getCharacterEncoding()
                                          : StandardCharsets.UTF_8.name())
            );
            StringBuilder textBuilder = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                textBuilder.append((char) c);
            }

            return textBuilder.toString();
        } catch (Throwable e) {
            throw new GetBodyException("An error occurred while extracting the request body", e);
        }
        finally {
            safeCloseCloseable(reader);
            safeCloseCloseable(is);
        }
    }

    @SuppressWarnings("WeakerAccess")
    protected void safeCloseCloseable(Closeable closeable) {
        if (closeable == null)
            return;

        try {
            closeable.close();
        } catch (Throwable e) {
            logger.warn("An error occurred closing a Closeable resource. closeable_classname=\""
                        + closeable.getClass().getName() + "\", exception_during_close=\"" + e + "\"");
        }
    }
}
