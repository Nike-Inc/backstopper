package com.nike.backstopper.handler.jaxrs;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.ApiErrorBase;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.apierror.projectspecificinfo.ProjectSpecificErrorCodeRange;
import com.nike.backstopper.apierror.testutil.ProjectApiErrorsForTesting;
import com.nike.backstopper.handler.ApiExceptionHandlerUtils;
import com.nike.backstopper.handler.ErrorResponseInfo;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport;
import com.nike.internal.util.MapBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests the functionality of {@link JaxRsUnhandledExceptionHandler }
 *
 * @author dsand7
 * @author Michael Irwin
 */
public class JaxRsUnhandledExceptionHandlerTest {

    private static JaxRsUnhandledExceptionHandler handler;
    private static final ApiError EXPECTED_ERROR = new ApiErrorBase("test", 99008, "test", 8);

    private static final ProjectApiErrors testProjectApiErrors = ProjectApiErrorsForTesting.withProjectSpecificData(
        singletonList(EXPECTED_ERROR), ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES
    );

    @BeforeClass
    public static void doBeforeClass() {
        handler = new JaxRsUnhandledExceptionHandler(testProjectApiErrors, ApiExceptionHandlerUtils.DEFAULT_IMPL);
    }

    @Test
    public void prepareFrameworkRepresentationReturnsCorrectStatusCode() {

        int expectedCode = HttpServletResponse.SC_ACCEPTED;
        Response.ResponseBuilder actualResponse = handler.prepareFrameworkRepresentation(null, expectedCode, null, null, null);
        assertThat(actualResponse.build().getStatus()).isEqualTo(expectedCode);
    }

    @Test
    public void generateLastDitchFallbackErrorResponseInfo_returns_expected_value() {
        // given
        Exception ex = new Exception("kaboom");
        RequestInfoForLogging reqMock = mock(RequestInfoForLogging.class);
        String errorId = UUID.randomUUID().toString();
        Map<String, List<String>> headersMap = MapBuilder.builder("error_uid", singletonList(errorId)).build();

        ApiError expectedGenericError = testProjectApiErrors.getGenericServiceError();
        int expectedHttpStatusCode = expectedGenericError.getHttpStatusCode();
        Map<String, List<String>> expectedHeadersMap = new HashMap<>(headersMap);
        String expectedBodyPayload = JsonUtilWithDefaultErrorContractDTOSupport.writeValueAsString(
            new DefaultErrorContractDTO(errorId, singletonList(expectedGenericError))
        );

        // when
        ErrorResponseInfo<Response.ResponseBuilder> response = handler.generateLastDitchFallbackErrorResponseInfo(ex, reqMock, errorId, headersMap);

        // then
        assertThat(response.httpStatusCode).isEqualTo(expectedHttpStatusCode);
        assertThat(response.headersToAddToResponse).isEqualTo(expectedHeadersMap);
        Response builtFrameworkResponse = response.frameworkRepresentationObj.build();
        assertThat(builtFrameworkResponse.getStatus()).isEqualTo(expectedHttpStatusCode);
        assertThat(builtFrameworkResponse.getEntity()).isEqualTo(expectedBodyPayload);
    }
}
