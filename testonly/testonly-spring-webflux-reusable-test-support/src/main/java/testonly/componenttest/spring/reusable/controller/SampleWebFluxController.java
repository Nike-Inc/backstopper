package testonly.componenttest.spring.reusable.controller;

import com.nike.backstopper.exception.ApiException;
import com.nike.internal.util.Pair;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Arrays;
import java.util.UUID;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import testonly.componenttest.spring.reusable.error.SampleProjectApiError;
import testonly.componenttest.spring.reusable.model.RgbColor;
import testonly.componenttest.spring.reusable.model.SampleModel;

import static java.util.Collections.singletonList;

/**
 * Contains some sample Spring WebFlux endpoints.
 *
 * <p>NOTE: This is mostly a copy of the reusable {@code SampleController} from
 * testonly-spring-webmvc-reusable-test-support, except modified to use WebFlux return types
 * ({@link Mono} and {@link Flux}) and augmented with a few other WebFlux use cases and features.
 */
@Controller
@RequestMapping(SampleWebFluxController.SAMPLE_PATH)
@SuppressWarnings({"unused", "WeakerAccess"})
public class SampleWebFluxController {

    public static final String SAMPLE_PATH = "/sample";
    public static final String CORE_ERROR_WRAPPER_ENDPOINT_SUBPATH = "/coreErrorWrapper";
    public static final String WITH_REQUIRED_QUERY_PARAM_SUBPATH = "/withRequiredQueryParam";
    public static final String WITH_REQUIRED_HEADER_SUBPATH = "/withRequiredHeader";
    public static final String TRIGGER_UNHANDLED_ERROR_SUBPATH = "/triggerUnhandledError";
    public static final String SAMPLE_FROM_ROUTER_FUNCTION_PATH = "/sample/fromRouterFunction";
    public static final String SAMPLE_FLUX_SUBPATH = "/flux";
    public static final String MONO_ERROR_SUBPATH = "/monoError";
    public static final String FLUX_ERROR_SUBPATH = "/fluxError";

    public static int nextRangeInt(int lowerBound, int upperBound) {
        return (int)Math.round(Math.random() * upperBound) + lowerBound;
    }

    public static RgbColor nextRandomColor() {
        return RgbColor.values()[nextRangeInt(0, 2)];
    }

    @GetMapping(produces = "application/json")
    @ResponseBody
    public Mono<SampleModel> getSampleModel() {
        return Mono.just(
            new SampleModel(
                UUID.randomUUID().toString(), String.valueOf(nextRangeInt(0, 42)), nextRandomColor().name(), false
            )
        );
    }

    /**
     * Note how the {@link Valid} annotation causes Spring to run the given request payload object through
     * JSR 303 validation after deserialization and before calling this method.
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SampleModel> postSampleModel(@Valid @RequestBody SampleModel model) {
        // Manually check the throwManualError query param (normally you'd do this with JSR 303 annotations on the
        // object, but this shows how you can manually throw exceptions to be picked up by the error handling system).
        if (Boolean.TRUE.equals(model.throw_manual_error)) {
            throw ApiException.newBuilder()
                              .withExceptionMessage("Manual error throw was requested")
                              .withApiErrors(SampleProjectApiError.MANUALLY_THROWN_ERROR)
                              .withExtraDetailsForLogging(Pair.of("rgb_color_value", model.rgb_color))
                              .withExtraResponseHeaders(
                                  Pair.of("rgbColorValue", singletonList(model.rgb_color)),
                                  Pair.of("otherExtraMultivalueHeader", Arrays.asList("foo", "bar"))
                              )
                              .build();
        }

        return Mono.just(model);
    }

    @GetMapping(path = CORE_ERROR_WRAPPER_ENDPOINT_SUBPATH)
    public void failWithCoreErrorWrapper() {
        throw ApiException.newBuilder()
                          .withExceptionMessage("Throwing error due to 'reasons'")
                          .withApiErrors(SampleProjectApiError.SOME_MEANINGFUL_ERROR_NAME)
                          .build();
    }

    @GetMapping(path = WITH_REQUIRED_QUERY_PARAM_SUBPATH, produces = "text/plain")
    @ResponseBody
    public Mono<String> withRequiredQueryParam(@RequestParam(name = "requiredQueryParamValue") int someRequiredQueryParam) {
        return Mono.just("You passed in " + someRequiredQueryParam + " for the required query param value");
    }

    @GetMapping(path = WITH_REQUIRED_HEADER_SUBPATH, produces = "text/plain")
    @ResponseBody
    public Mono<String> withRequiredHeader(@RequestHeader(name = "requiredHeaderValue") int someRequiredHeader) {
        return Mono.just("You passed in " + someRequiredHeader + " for the required header value");
    }

    @GetMapping(path = TRIGGER_UNHANDLED_ERROR_SUBPATH)
    public void triggerUnhandledError() {
        throw new RuntimeException("This should be handled by SpringUnhandledExceptionHandler.");
    }

    public Mono<ServerResponse> getSampleModelRouterFunction(ServerRequest request) {
        return ServerResponse.ok().bodyValue(
            new SampleModel(
                UUID.randomUUID().toString(),
                String.valueOf(nextRangeInt(0, 42)),
                nextRandomColor().name(),
                false
            )
        );
    }

    @GetMapping(path = SAMPLE_FLUX_SUBPATH, produces = "application/json")
    @ResponseBody
    public Flux<SampleModel> getSampleModelFlux() {
        return Flux.fromIterable(
            Arrays.asList(
                new SampleModel(
                    UUID.randomUUID().toString(),
                    String.valueOf(nextRangeInt(0, 42)),
                    nextRandomColor().name(),
                    false
                ),
                new SampleModel(
                    UUID.randomUUID().toString(),
                    String.valueOf(nextRangeInt(0, 42)),
                    nextRandomColor().name(),
                    false
                )
            )
        );
    }

    @GetMapping(path = MONO_ERROR_SUBPATH, produces = "application/json")
    @ResponseBody
    public Mono<SampleModel> getMonoError() {
        return Mono.error(
            () -> ApiException
                .newBuilder()
                .withApiErrors(SampleProjectApiError.WEBFLUX_MONO_ERROR)
                .build()
        );
    }

    @GetMapping(path = FLUX_ERROR_SUBPATH, produces = "application/json")
    @ResponseBody
    public Flux<SampleModel> getFluxError() {
        return Flux.error(
            () -> ApiException
                .newBuilder()
                .withApiErrors(SampleProjectApiError.WEBFLUX_FLUX_ERROR)
                .build()
        );
    }
}
