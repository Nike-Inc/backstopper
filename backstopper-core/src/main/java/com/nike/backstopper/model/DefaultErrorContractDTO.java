package com.nike.backstopper.model;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.handler.ApiExceptionHandlerBase;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.handler.UnhandledExceptionHandlerBase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Immutable DTO class used for a response triggered by one or more errors. This class can be converted directly to JSON
 * via Jackson (for example) to satisfy the requirements of a project's error contract assuming those requirements
 * are compatible with this format.
 *
 * <p>This represents the recommended default format for an error contract to return to the caller and mirrors the info
 * available in {@link ApiError}, making the errors your app throws and what shows up to the caller in the response
 * closely related and easy to reason about, but you are not required to use it - ultimately you control what gets
 * returned to the client based on what
 * {@link ApiExceptionHandlerBase#prepareFrameworkRepresentation(DefaultErrorContractDTO, int, Collection, Throwable,
 * RequestInfoForLogging)} or
 * {@link UnhandledExceptionHandlerBase#prepareFrameworkRepresentation(DefaultErrorContractDTO, int, Collection,
 * Throwable, RequestInfoForLogging)} returns.
 *
 * @author Nic Munroe
 */
public class DefaultErrorContractDTO implements Serializable {

    /**
     * The ID associated with this error response/contract. This should be a unique ID - a UUID is recommended. This
     * field is intentionally snake case to ensure that this object converts to snake case when serializing without
     * additional work by the converter (e.g. Jackson for JSON, or any other serializer that happens to be used).
     */
    public final String error_id;
    /**
     * The list of {@link DefaultErrorDTO}s associated with this error response/contract. This list is immutable - an
     * exception will be thrown if you try to modify this list.
     */
    public final List<DefaultErrorDTO> errors;

    // Here for deserialization support only - usage in real code should involve one of the other constructors since
    //      this class is immutable
    protected DefaultErrorContractDTO() {
        this(null, null, null);
    }

    /**
     * Creates a new instance that is a copy of the given instance. {@link #errors} will be populated with a copy of the
     * given argument's error list, not the original list. Delegates to
     * {@link DefaultErrorContractDTO#DefaultErrorContractDTO(String, Collection, Void)}.
     */
    public DefaultErrorContractDTO(DefaultErrorContractDTO copy) {
        this(copy.error_id, copy.errors, null);
    }

    /**
     * Creates a new instance with the given arguments, where the given apiErrors are converted to
     * {@link DefaultErrorDTO} objects via {@link #convertApiErrorsToErrorModelObjects(Collection)}. This constructor
     * ultimately delegates to {@link DefaultErrorContractDTO#DefaultErrorContractDTO(String, Collection, Void)}.
     */
    public DefaultErrorContractDTO(String error_id, Collection<ApiError> apiErrors) {
        this(error_id, convertApiErrorsToErrorModelObjects(apiErrors), null);
    }

    /**
     * Creates a new instance with the given arguments, with the {@link #errors} list created fresh here as a copy of
     * the given errors collection rather than pointing to the original, and wrapped in
     * {@link Collections#unmodifiableList(List)} to make sure it is immutable.
     *
     * <p>NOTE: You should always pass in null for the {@code passInNullForThisArg} Void argument. This argument is only
     * here at all because otherwise there would be type erasure conflicts between this constructor and
     * {@link DefaultErrorContractDTO#DefaultErrorContractDTO(String, Collection)}, and this class wouldn't be able to
     * compile.
     */
    public DefaultErrorContractDTO(String error_id, Collection<DefaultErrorDTO> errorsToCopy,
                                   @SuppressWarnings("UnusedParameters") Void passInNullForThisArg) {
        this.error_id = error_id;
        List<DefaultErrorDTO> errorsList = new ArrayList<>();
        if (errorsToCopy != null) {
            for (DefaultErrorDTO apiError : errorsToCopy) {
                errorsList.add(apiError);
            }
        }
        this.errors = Collections.unmodifiableList(errorsList);
    }

    /**
     * Converts the given list of {@link ApiError}s into a list of {@link DefaultErrorDTO} objects.
     */
    @SuppressWarnings("WeakerAccess")
    protected static List<DefaultErrorDTO> convertApiErrorsToErrorModelObjects(Collection<ApiError> apiErrors) {
        if (apiErrors == null) {
            return null;
        }

        List<DefaultErrorDTO> errorsList = new ArrayList<>();
        for (ApiError apiError : apiErrors) {
            errorsList.add(new DefaultErrorDTO(apiError));
        }

        return errorsList;
    }

}
