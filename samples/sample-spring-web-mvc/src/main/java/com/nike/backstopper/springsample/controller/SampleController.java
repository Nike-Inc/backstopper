package com.nike.backstopper.springsample.controller;

import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.service.ClientDataValidationService;
import com.nike.backstopper.springsample.error.SampleProjectApiError;
import com.nike.backstopper.springsample.model.RgbColor;
import com.nike.backstopper.springsample.model.SampleModel;
import com.nike.internal.util.Pair;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

import javax.validation.Valid;

import static com.nike.backstopper.springsample.controller.SampleController.SAMPLE_PATH;

/**
 * Contains some sample endpoints. In particular {@link #postSampleModel(SampleModel)} is useful for showing the
 * JSR 303 Bean Validation integration in Backstopper - see that method's javadocs and source code for more info.
 *
 * <p>The {@code VerifyExpectedErrorsAreReturnedComponentTest} component test launches the server and exercises
 * all these endpoints in various ways to verify the expected errors are returned using the expected error contract.
 */
@Controller
@RequestMapping(SAMPLE_PATH)
@SuppressWarnings({"unused", "WeakerAccess"})
public class SampleController {

    public static final String SAMPLE_PATH = "/sample";
    public static final String CORE_ERROR_WRAPPER_ENDPOINT_SUBPATH = "/coreErrorWrapper";
    public static final String WITH_REQUIRED_QUERY_PARAM_SUBPATH = "/withRequiredQueryParam";
    public static final String TRIGGER_UNHANDLED_ERROR_SUBPATH = "/triggerUnhandledError";

    public static int nextRangeInt(int lowerBound, int upperBound) {
        return (int)Math.round(Math.random() * upperBound) + lowerBound;
    }

    public static RgbColor nextRandomColor() {
        return RgbColor.values()[nextRangeInt(0, 2)];
    }

    @GetMapping(produces = "application/json")
    @ResponseBody
    public SampleModel getSampleModel() {
        return new SampleModel(
            UUID.randomUUID().toString(), String.valueOf(nextRangeInt(0, 42)), nextRandomColor().name(), false
        );
    }

    /**
     * Note how the {@link Valid} annotation causes Spring Web MVC to run the given request payload object through
     * JSR 303 validation after deserialization and before calling this method. Alternatively you could omit the
     * {@code @Valid} annotation, inject a {@link ClientDataValidationService} into this class, and call
     * {@link ClientDataValidationService#validateObjectsFailFast(Object...)} passing in the request payload object -
     * if it fails validation an appropriate exception would be thrown immediately. Both solutions are
     * functionally equivalent.
     *
     * <p>In this simple case the {@code @Valid} annotation is easier and simpler, but there are some more complex
     * use cases where using {@link ClientDataValidationService} yourself ends up being a better (or the only)
     * solution.
     */
    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseBody
    @ResponseStatus(HttpStatus.CREATED)
    public SampleModel postSampleModel(@Valid @RequestBody SampleModel model) {
        // Manually check the throwManualError query param (normally you'd do this with JSR 303 annotations on the
        // object, but this shows how you can manually throw exceptions to be picked up by the error handling system).
        if (Boolean.TRUE.equals(model.throw_manual_error)) {
            throw ApiException.newBuilder()
                              .withExceptionMessage("Manual error throw was requested")
                              .withApiErrors(SampleProjectApiError.MANUALLY_THROWN_ERROR)
                              .withExtraDetailsForLogging(Pair.of("rgb_color_value", model.rgb_color))
                              .build();
        }

        return model;
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
    public String withRequiredQueryParam(@RequestParam(name = "requiredQueryParamValue") int someRequiredQueryParam) {
        return "You passed in " + someRequiredQueryParam + " for the required query param value";
    }

    @GetMapping(path = TRIGGER_UNHANDLED_ERROR_SUBPATH)
    public void triggerUnhandledError() {
        throw new RuntimeException("This should be handled by SpringUnhandledExceptionHandler.");
    }
}
