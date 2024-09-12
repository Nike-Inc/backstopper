package com.nike.backstopper.handler.spring.webflux;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.handler.UnexpectedMajorExceptionHandlingError;
import com.nike.backstopper.handler.listener.ApiExceptionHandlerListener;
import com.nike.backstopper.handler.spring.webflux.SpringWebfluxApiExceptionHandler.ResponseContext;
import com.nike.backstopper.handler.spring.webflux.listener.SpringWebFluxApiExceptionHandlerListenerList;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.internal.util.MapBuilder;
import com.nike.internal.util.testing.Glassbox;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

import java.io.EOFException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;
import reactor.netty.channel.AbortedException;

import static com.nike.backstopper.handler.spring.webflux.DisconnectedClientHelper.isClientDisconnectedException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link SpringWebfluxApiExceptionHandler}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class SpringWebfluxApiExceptionHandlerTest {

    private SpringWebfluxApiExceptionHandler handlerSpy;
    private ProjectApiErrors projectApiErrorsMock;
    private SpringWebFluxApiExceptionHandlerListenerList listenerList;
    private ApiExceptionHandlerUtils generalUtils;
    private SpringWebfluxApiExceptionHandlerUtils springUtilsMock;
    private ObjectProvider<ViewResolver> viewResolversProviderMock;
    private ServerCodecConfigurer serverCodecConfigurerMock;
    private List<HttpMessageReader<?>> messageReaders;
    private List<HttpMessageWriter<?>> messageWriters;
    private List<ViewResolver> viewResolvers;

    private ServerWebExchange serverWebExchangeMock;
    private ServerHttpRequest serverHttpRequestMock;
    private ServerHttpResponse serverHttpResponseMock;
    private HttpHeaders serverHttpResponseHeadersMock;
    private URI uri;

    private Throwable exMock;

    @Before
    public void beforeMethod() {
        projectApiErrorsMock = mock(ProjectApiErrors.class);
        listenerList = new SpringWebFluxApiExceptionHandlerListenerList(
            Arrays.asList(mock(ApiExceptionHandlerListener.class), mock(ApiExceptionHandlerListener.class))
        );
        generalUtils = ApiExceptionHandlerUtils.DEFAULT_IMPL;
        springUtilsMock = mock(SpringWebfluxApiExceptionHandlerUtils.class);
        viewResolversProviderMock = mock(ObjectProvider.class);
        serverCodecConfigurerMock = mock(ServerCodecConfigurer.class);
        messageReaders = Arrays.asList(mock(HttpMessageReader.class), mock(HttpMessageReader.class));
        messageWriters = Arrays.asList(mock(HttpMessageWriter.class), mock(HttpMessageWriter.class));
        viewResolvers = Arrays.asList(mock(ViewResolver.class), mock(ViewResolver.class));
        serverWebExchangeMock = mock(ServerWebExchange.class);
        serverHttpRequestMock = mock(ServerHttpRequest.class);
        serverHttpResponseMock = mock(ServerHttpResponse.class);
        serverHttpResponseHeadersMock = mock(HttpHeaders.class);
        uri = URI.create("http://localhost:8080/foo/bar?someQuery=someValue");
        exMock = mock(Throwable.class);

        doAnswer(invocation -> viewResolvers.stream()).when(viewResolversProviderMock).orderedStream();
        doReturn(messageReaders).when(serverCodecConfigurerMock).getReaders();
        doReturn(messageWriters).when(serverCodecConfigurerMock).getWriters();

        doReturn(serverHttpRequestMock).when(serverWebExchangeMock).getRequest();
        doReturn(uri).when(serverHttpRequestMock).getURI();
        doReturn(new HttpHeaders()).when(serverHttpRequestMock).getHeaders();

        doReturn(serverHttpResponseMock).when(serverWebExchangeMock).getResponse();
        doReturn(serverHttpResponseHeadersMock).when(serverHttpResponseMock).getHeaders();

        handlerSpy = spy(new SpringWebfluxApiExceptionHandler(
            projectApiErrorsMock, listenerList, generalUtils, springUtilsMock, viewResolversProviderMock,
            serverCodecConfigurerMock
        ));
    }

    @Test
    public void constructor_sets_fields_as_expected() {
        // when
        SpringWebfluxApiExceptionHandler handler = new SpringWebfluxApiExceptionHandler(
            projectApiErrorsMock, listenerList, generalUtils, springUtilsMock, viewResolversProviderMock,
            serverCodecConfigurerMock
        );

        // then
        assertThat(Glassbox.getInternalState(handler, "projectApiErrors")).isSameAs(projectApiErrorsMock);
        assertThat(Glassbox.getInternalState(handler, "apiExceptionHandlerListenerList"))
            .isSameAs(listenerList.listeners);
        assertThat(Glassbox.getInternalState(handler, "utils")).isSameAs(generalUtils);
        assertThat(handler.springUtils).isEqualTo(springUtilsMock);
        assertThat(handler.messageReaders).isEqualTo(messageReaders);
        assertThat(handler.messageWriters).isEqualTo(messageWriters);
        assertThat(handler.viewResolvers).isEqualTo(viewResolvers);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_ProjectApiErrors() {
        // when
        Throwable ex = catchThrowable(
            () -> new SpringWebfluxApiExceptionHandler(
                null, listenerList, generalUtils, springUtilsMock, viewResolversProviderMock,
                serverCodecConfigurerMock
            )
        );

        // then
        assertThat(ex)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("projectApiErrors cannot be null.");
    }

    @Test
    public void constructor_throws_NullPointerException_if_passed_null_ApiExceptionHandlerListenerList() {
        // when
        Throwable ex = catchThrowable(
            () -> new SpringWebfluxApiExceptionHandler(
                projectApiErrorsMock, null, generalUtils, springUtilsMock, viewResolversProviderMock,
                serverCodecConfigurerMock
            )
        );

        // then
        assertThat(ex).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_ApiExceptionHandlerListenerList_with_null_internal_list() {
        // given
        SpringWebFluxApiExceptionHandlerListenerList
            listenerList = new SpringWebFluxApiExceptionHandlerListenerList(null);

        // when
        Throwable ex = catchThrowable(
            () -> new SpringWebfluxApiExceptionHandler(
                projectApiErrorsMock, listenerList, generalUtils, springUtilsMock, viewResolversProviderMock,
                serverCodecConfigurerMock
            )
        );

        // then
        assertThat(ex)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("apiExceptionHandlerListenerList cannot be null.");
    }

    @Test
    public void constructor_throws_IllegalArgumentException_if_passed_null_ApiExceptionHandlerUtils() {
        // when
        Throwable ex = catchThrowable(
            () -> new SpringWebfluxApiExceptionHandler(
                projectApiErrorsMock, listenerList, null, springUtilsMock, viewResolversProviderMock,
                serverCodecConfigurerMock
            )
        );

        // then
        assertThat(ex)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("apiExceptionHandlerUtils cannot be null.");
    }

    @Test
    public void constructor_throws_NullPointerException_if_passed_null_SpringWebfluxApiExceptionHandlerUtils() {
        // when
        Throwable ex = catchThrowable(
            () -> new SpringWebfluxApiExceptionHandler(
                projectApiErrorsMock, listenerList, generalUtils, null, viewResolversProviderMock,
                serverCodecConfigurerMock
            )
        );

        // then
        assertThat(ex)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("springUtils cannot be null.");
    }

    @Test
    public void constructor_throws_NullPointerException_if_passed_null_ViewResolver_ObjectProvider() {
        // when
        Throwable ex = catchThrowable(
            () -> new SpringWebfluxApiExceptionHandler(
                projectApiErrorsMock, listenerList, generalUtils, springUtilsMock, null,
                serverCodecConfigurerMock
            )
        );

        // then
        assertThat(ex)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("viewResolversProvider cannot be null.");
    }

    @Test
    public void constructor_throws_NullPointerException_if_passed_null_ServerCodecConfigurer() {
        // when
        Throwable ex = catchThrowable(
            () -> new SpringWebfluxApiExceptionHandler(
                projectApiErrorsMock, listenerList, generalUtils, springUtilsMock, viewResolversProviderMock,
                null
            )
        );

        // then
        assertThat(ex)
            .isInstanceOf(NullPointerException.class)
            .hasMessage("serverCodecConfigurer cannot be null.");
    }

    @Test
    public void prepareFrameworkRepresentation_delegates_to_SpringWebfluxApiExceptionHandlerUtils() {
        // given
        Mono<ServerResponse> expectedResult = mock(Mono.class);

        DefaultErrorContractDTO errorContractDTOMock = mock(DefaultErrorContractDTO.class);
        int httpStatusCode = 400;
        Collection<ApiError> rawFilteredApiErrors = mock(Collection.class);
        Throwable originalException = mock(Throwable.class);
        RequestInfoForLogging request = mock(RequestInfoForLogging.class);

        doReturn(expectedResult).when(springUtilsMock).generateServerResponseForError(
            errorContractDTOMock, httpStatusCode, rawFilteredApiErrors, originalException, request
        );

        // when
        Mono<ServerResponse> result = handlerSpy.prepareFrameworkRepresentation(
            errorContractDTOMock, httpStatusCode, rawFilteredApiErrors, originalException, request
        );

        // then
        assertThat(result).isSameAs(expectedResult);
        verify(springUtilsMock).generateServerResponseForError(
            errorContractDTOMock, httpStatusCode, rawFilteredApiErrors, originalException, request
        );
    }

    @Test
    public void handle_works_as_expected_for_exception_handled_by_maybeHandleException()
        throws UnexpectedMajorExceptionHandlingError {

        // given
        ServerResponse expectedResponseObj = mock(ServerResponse.class);
        HttpHeaders responseHeadersMock = mock(HttpHeaders.class);
        MediaType desiredContentTypeHeaderValueMock = mock(MediaType.class);

        doReturn(responseHeadersMock).when(expectedResponseObj).headers();
        doReturn(desiredContentTypeHeaderValueMock).when(responseHeadersMock).getContentType();

        ErrorResponseInfo<Mono<ServerResponse>> errorResponseInfo =
            new ErrorResponseInfo<>(400, Mono.just(expectedResponseObj), Collections.emptyMap());
        doReturn(errorResponseInfo)
            .when(handlerSpy)
            .maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));

        doReturn(Mono.empty()).when(expectedResponseObj)
                              .writeTo(any(ServerWebExchange.class), any(ServerResponse.Context.class));

        // when
        Mono<Void> result = handlerSpy.handle(serverWebExchangeMock, exMock);

        // then
        verify(handlerSpy).processWebFluxResponse(errorResponseInfo, serverHttpResponseMock);
        result.block();
        verify(handlerSpy).write(serverWebExchangeMock, expectedResponseObj);
    }

    @DataProvider(value = {
        "true   |   false",
        "false  |   true",
    }, splitBy = "\\|")
    @Test
    public void handle_calls_maybeHandleException_but_returns_unhandled_Mono_if_response_is_committed_or_client_disconnected(
        boolean responseIsCommitted, boolean clientDisconnected
    ) throws UnexpectedMajorExceptionHandlingError {

        // given
        doReturn(new ErrorResponseInfo<>(400, Mono.just(mock(ServerResponse.class)), Collections.emptyMap()))
            .when(handlerSpy)
            .maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));

        if (responseIsCommitted) {
            doReturn(true).when(serverHttpResponseMock).isCommitted();
        }

        if (clientDisconnected) {
            doReturn("connection reset by peer").when(exMock).getMessage();
        }

        // when
        Mono<Void> result = handlerSpy.handle(serverWebExchangeMock, exMock);

        // then
        verify(handlerSpy).maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));
        verifyMonoIsErrorMono(result, exMock);
    }

    @Test
    public void handle_returns_unhandled_Mono_if_maybeHandleException_returns_null() throws UnexpectedMajorExceptionHandlingError {
        // given
        doReturn(null)
            .when(handlerSpy)
            .maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));

        // when
        Mono<Void> result = handlerSpy.handle(serverWebExchangeMock, exMock);

        // then
        verify(handlerSpy).maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));
        verifyMonoIsErrorMono(result, exMock);
    }

    private void verifyMonoIsErrorMono(Mono<?> mono, Throwable expectedCause) {
        Throwable ex = catchThrowable(() -> mono.block());

        assertThat(ex).isNotNull();

        if (expectedCause instanceof RuntimeException) {
            assertThat(ex).isSameAs(expectedCause);
        }
        else {
            assertThat(ex).hasCause(expectedCause);
        }
    }

    @Test
    public void handle_returns_unhandled_Mono_if_maybeHandleException_throws_UnexpectedMajorExceptionHandlingError()
        throws UnexpectedMajorExceptionHandlingError {
        // given
        doThrow(new UnexpectedMajorExceptionHandlingError("foo", null))
            .when(handlerSpy).maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));

        // when
        Mono<Void> result = handlerSpy.handle(serverWebExchangeMock, exMock);

        // then
        verify(handlerSpy).maybeHandleException(any(Throwable.class), any(RequestInfoForLogging.class));
        verifyMonoIsErrorMono(result, exMock);
    }

    @Test
    public void processWebFluxResponse_works_as_expected() {
        // given
        Map<String, List<String>> headersToAddToResponse = MapBuilder
            .builder("foo", Arrays.asList("bar1", "bar2"))
            .put("blah", Collections.singletonList(UUID.randomUUID().toString()))
            .build();
        ErrorResponseInfo<Mono<ServerResponse>> errorResponseInfo = new ErrorResponseInfo<>(
            400, mock(Mono.class), headersToAddToResponse
        );

        // when
        handlerSpy.processWebFluxResponse(errorResponseInfo, serverHttpResponseMock);

        // then
        headersToAddToResponse.forEach((key, value) -> verify(serverHttpResponseHeadersMock).put(key, value));
    }

    @Test
    public void write_works_as_expected() {
        // given
        ServerResponse responseMock = mock(ServerResponse.class);
        HttpHeaders responseHeadersMock = mock(HttpHeaders.class);
        MediaType expectedContentTypeMock = mock(MediaType.class);

        doReturn(responseHeadersMock).when(responseMock).headers();
        doReturn(expectedContentTypeMock).when(responseHeadersMock).getContentType();

        // when
        handlerSpy.write(serverWebExchangeMock, responseMock);

        // then
        verify(serverHttpResponseHeadersMock).setContentType(expectedContentTypeMock);
        ArgumentCaptor<ResponseContext> responseContextArgumentCaptor = ArgumentCaptor.forClass(ResponseContext.class);
        verify(responseMock).writeTo(eq(serverWebExchangeMock), responseContextArgumentCaptor.capture());
        ResponseContext responseContext = responseContextArgumentCaptor.getValue();
        assertThat(responseContext.messageWriters()).isEqualTo(messageWriters);
        assertThat(responseContext.viewResolvers()).isEqualTo(viewResolvers);
    }

    public enum IsDisconnectedClientErrorScenario {
        ABORTED_EXCEPTION(new AbortedException("Some message"), true),
        CLIENT_ABORT_EXCEPTION(new ClientAbortException(), true),
        EOF_EXCEPTION_1(new EOFException(), true),
        EOF_EXCEPTION_2(new EofException(), true),
        UNKNOWN_EXCEPTION_BUT_BROKEN_PIPE_MESSAGE(
            new RuntimeException("somewhere in here is broken pipe hooray"),
            true
        ),
        UNKNOWN_EXCEPTION_WITH_MOST_SPECIFIC_CAUSE_HAVING_BROKEN_PIPE_MESSAGE(
            new RuntimeException(
                new Exception(
                    new IllegalArgumentException("somewhere in here is broken pipe hooray")
                )
            ),
            true
        ),
        UNKNOWN_EXCEPTION_BUT_CONNECTION_RESET_MESSAGE(
            new RuntimeException("somewhere in here is connection reset by peer hooray"),
            true
        ),
        UNKNOWN_EXCEPTION_WITH_CAUSE_HAVING_CONNECTION_RESET_MESSAGE(
            new RuntimeException(
                new Exception(
                    new IllegalArgumentException("somewhere in here is connection reset by peer hooray")
                )
            ),
            true
        ),
        UNKNOWN_EXCEPTION_NULL_MESSAGE(
            new RuntimeException((String) null),
            false
        ),
        UNKNOWN_EXCEPTION_UNKNOWN_MESSAGE(
            new RuntimeException("foo"),
            false
        );

        public final Throwable ex;
        public final boolean expectedResult;

        IsDisconnectedClientErrorScenario(Throwable ex, boolean expectedResult) {
            this.ex = ex;
            this.expectedResult = expectedResult;
        }
    }

    @DataProvider
    public static List<List<IsDisconnectedClientErrorScenario>> isDisconnectedClientErrorScenarioDataProvider() {
        return Stream.of(IsDisconnectedClientErrorScenario.values())
                     .map(Collections::singletonList)
                     .collect(Collectors.toList());
    }

    @UseDataProvider("isDisconnectedClientErrorScenarioDataProvider")
    @Test
    public void isDisconnectedClientError_works_as_expected(IsDisconnectedClientErrorScenario scenario) {
        // when
        boolean result = isClientDisconnectedException(scenario.ex);

        // then
        assertThat(result).isEqualTo(scenario.expectedResult);
    }

    @Test
    public void order_getters_and_setters_work() {
        // given
        int newOrder = handlerSpy.getOrder() + 42;

        // when
        handlerSpy.setOrder(newOrder);

        // then
        assertThat(handlerSpy.getOrder()).isEqualTo(newOrder);
    }

    @Test
    public void default_order_is_highest_precedence() {
        // given
        SpringWebfluxApiExceptionHandler impl = new SpringWebfluxApiExceptionHandler(
            projectApiErrorsMock, listenerList, generalUtils, springUtilsMock, viewResolversProviderMock,
            serverCodecConfigurerMock
        );

        // when
        int defaultOrder = impl.getOrder();

        // then
        assertThat(defaultOrder).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    private static class ClientAbortException extends RuntimeException {}
    private static class EofException extends RuntimeException {}
}