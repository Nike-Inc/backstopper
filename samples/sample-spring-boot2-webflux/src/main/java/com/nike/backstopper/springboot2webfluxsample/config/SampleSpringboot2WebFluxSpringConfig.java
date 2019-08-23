package com.nike.backstopper.springboot2webfluxsample.config;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.spring.webflux.config.BackstopperSpringWebFluxConfig;
import com.nike.backstopper.springboot2webfluxsample.controller.SampleController;
import com.nike.backstopper.springboot2webfluxsample.error.SampleProjectApiError;
import com.nike.backstopper.springboot2webfluxsample.error.SampleProjectApiErrorsImpl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import javax.validation.Validation;
import javax.validation.Validator;

import reactor.core.publisher.Mono;

import static com.nike.backstopper.springboot2webfluxsample.controller.SampleController.SAMPLE_FROM_ROUTER_FUNCTION_PATH;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

/**
 * Simple Spring Boot config for the sample app. The {@link ProjectApiErrors} and {@link Validator} beans defined
 * in this class are needed for autowiring Backstopper and the {@link ProjectApiErrors} in particular allows you
 * to specify project-specific errors and behaviors.
 *
 * <p>NOTE: This integrates Backstopper by {@link Import}ing {@link BackstopperSpringWebFluxConfig}. Alternatively,
 * you could integrate Backstopper by component scanning all of the {@code com.nike.backstopper} package and its
 * subpackages, e.g. by annotating with
 * {@link org.springframework.context.annotation.ComponentScan @ComponentScan(basePackages = "com.nike.backstopper")}.
 *
 * @author Nic Munroe
 */
@Configuration
@Import(BackstopperSpringWebFluxConfig.class)
// Instead of @Import(BackstopperSpringWebFluxConfig.class), you could component scan the com.nike.backstopper
//      package like this if you prefer component scanning: @ComponentScan(basePackages = "com.nike.backstopper")
public class SampleSpringboot2WebFluxSpringConfig {

    /**
     * @return The {@link ProjectApiErrors} to use for this sample app.
     */
    @Bean
    public ProjectApiErrors getProjectApiErrors() {
        return new SampleProjectApiErrorsImpl();
    }

    /**
     * NOTE: Spring uses its own system for JSR 303 validation, so this {@code @Bean} is only here to satisfy the
     * dependency injection requirements of {@link com.nike.backstopper.service.ClientDataValidationService} and
     * {@link com.nike.backstopper.service.FailFastServersideValidationService}. With this {@link Validator} defined
     * you could inject one of those services into your controllers and use it as advertised. If you were never going
     * to use those services you could have this return {@link
     * com.nike.backstopper.service.NoOpJsr303Validator#SINGLETON_IMPL} instead and not have to pull in any JSR 303
     * implementation dependency into your project.
     */
    @Bean
    public Validator getJsr303Validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    // ============= STUFF THAT EXERCISES EXCEPTION USE CASES BELOW - NOT FOR A REAL APP! =============
    /**
     * Registers a custom {@link ExplodingWebFilter} at the highest precedence that will throw an exception
     * when the request contains a special header. You wouldn't want this in a real app.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter explodingWebFilter() {
        return new ExplodingWebFilter();
    }

    /**
     * Creates a {@link RouterFunction} to register an endpoint, along with a custom
     * {@link ExplodingHandlerFilterFunction} that will throw an exception when the request contains a special header.
     * You wouldn't want the exploding filter in a real app.
     */
    @Bean
    public RouterFunction<ServerResponse> sampleRouterFunction(SampleController sampleController) {
        return RouterFunctions
            .route(GET(SAMPLE_FROM_ROUTER_FUNCTION_PATH), sampleController::getSampleModelRouterFunction)
            .filter(new ExplodingHandlerFilterFunction());
    }

    public static class ExplodingWebFilter implements WebFilter {
        
        @Override
        public Mono<Void> filter(
            ServerWebExchange exchange, WebFilterChain chain
        ) {
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

    public static class ExplodingHandlerFilterFunction
        implements HandlerFilterFunction<ServerResponse, ServerResponse> {

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

}
