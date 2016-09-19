package com.nike.backstopper.jersey1sample.resource;

import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.jersey1sample.error.SampleProjectApiError;
import com.nike.backstopper.jersey1sample.model.RgbColor;
import com.nike.backstopper.jersey1sample.model.SampleModel;
import com.nike.backstopper.service.ClientDataValidationService;
import com.nike.internal.util.Pair;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.nike.backstopper.jersey1sample.resource.SampleResource.SAMPLE_PATH;

/**
 * Contains some sample endpoints. In particular {@link #postSampleModel(SampleModel)} is useful for showing the
 * JSR 303 Bean Validation integration in Backstopper - see that method's source code for more info.
 *
 * <p>The {@code VerifyExpectedErrorsAreReturnedComponentTest} component test launches the server and exercises
 * all these endpoints in various ways to verify the expected errors are returned using the expected error contract.
 *
 * @author Nic Munroe
 */
@Path(SAMPLE_PATH)
public class SampleResource {

    public static final String SAMPLE_PATH = "/sample";
    public static final String CORE_ERROR_WRAPPER_ENDPOINT_SUBPATH = "/coreErrorWrapper";
    public static final String WITH_INT_QUERY_PARAM_SUBPATH = "/withIntQueryParam";
    public static final String TRIGGER_UNHANDLED_ERROR_SUBPATH = "/triggerUnhandledError";

    public static int nextRangeInt(int lowerBound, int upperBound) {
        return (int)Math.round(Math.random() * upperBound) + lowerBound;
    }

    public static RgbColor nextRandomColor() {
        return RgbColor.values()[nextRangeInt(0, 2)];
    }

    private final ClientDataValidationService validationService;

    public SampleResource(ClientDataValidationService validationService) {
        if (validationService == null)
            throw new IllegalArgumentException("validationService cannot be null");

        this.validationService = validationService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public SampleModel getSampleModel() throws JsonProcessingException {
        return new SampleModel(
            UUID.randomUUID().toString(), String.valueOf(nextRangeInt(0, 42)), nextRandomColor().name(), false
        );
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postSampleModel(SampleModel model) throws JsonProcessingException {
        // If the caller didn't pass us a payload we throw a MISSING_EXPECTED_CONTENT error.
        if (model == null) {
            throw ApiException.newBuilder()
                              .withExceptionMessage("Caller did not pass any request payload")
                              .withApiErrors(SampleCoreApiError.MISSING_EXPECTED_CONTENT)
                              .build();
        }
        // Run the object through our JSR 303 validation service, which will automatically throw an appropriate error
        //      if the validation fails that will get translated to the desired error contract.
        validationService.validateObjectsFailFast(model);

        // Manually check the throwManualError query param (normally you'd do this with JSR 303 annotations on the
        // object, but this shows how you can manually throw exceptions to be picked up by the error handling system).
        if (Boolean.TRUE.equals(model.throw_manual_error)) {
            throw ApiException.newBuilder()
                              .withExceptionMessage("Manual error throw was requested")
                              .withApiErrors(SampleProjectApiError.MANUALLY_THROWN_ERROR)
                              .withExtraDetailsForLogging(Pair.of("rgb_color_value", model.rgb_color))
                              .build();
        }

        return Response.status(201).entity(model).build();
    }

    @GET
    @Path(CORE_ERROR_WRAPPER_ENDPOINT_SUBPATH)
    public String failWithCoreErrorWrapper() {
        throw ApiException.newBuilder()
                          .withExceptionMessage("Throwing error due to 'reasons'")
                          .withApiErrors(SampleProjectApiError.SOME_MEANINGFUL_ERROR_NAME)
                          .build();
    }

    @GET
    @Path(WITH_INT_QUERY_PARAM_SUBPATH)
    @Produces(MediaType.TEXT_PLAIN)
    public String withIntQueryParam(@QueryParam("requiredQueryParamValue") Integer someRequiredQueryParam) {
        return "You passed in " + someRequiredQueryParam + " for the required query param value";
    }

    @GET
    @Path(TRIGGER_UNHANDLED_ERROR_SUBPATH)
    public String triggerUnhandledError() {
        throw new RuntimeException("This should be handled by Jersey1UnhandledExceptionHandler.");
    }
}
