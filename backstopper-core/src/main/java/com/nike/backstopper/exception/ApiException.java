package com.nike.backstopper.exception;

import com.nike.backstopper.apierror.ApiError;
import com.nike.internal.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 * Generic API RuntimeException to explicitly invoke the error handling system. Most of the time you'll want to create
 * an instance using the {@link ApiException.Builder}, e.g.
 * {@code
 * ApiException.newBuilder().withApiErrors(SOME_ERROR, OTHER_ERROR).withExceptionMessage("super useful message").build()}.
 * The builder has many options that directly affect the response and what is logged, so take a close look at the
 * available methods.
 *
 * @author Nic Munroe
 */
public class ApiException extends RuntimeException {

    /**
     * The {@link ApiError}s associated with this instance. Will never be null or empty.
     */
    private final List<ApiError> apiErrors;
    /**
     * Any extra details you want logged when this error is handled. Will never be null, but might be empty.
     * NOTE: This will always be a mutable list so it can be modified at any time.
     */
    private final List<Pair<String, String>> extraDetailsForLogging;

    /**
     * Handles the simple common case where you just want to throw a single {@link ApiError} and nothing else.
     */
    public ApiException(ApiError error) {
        super();
        if (error == null)
            throw new IllegalArgumentException("error cannot be null");
        this.apiErrors = new ArrayList<>(singletonList(error));
        this.extraDetailsForLogging = new ArrayList<>();
    }

    /**
     * Creates an instance on top of {@link Exception#Exception(String)} super constructor. You can safely pass in null
     * for message if you have no message. If another exception caused this to be thrown then you'll want
     * {@link #ApiException(List, List, String, Throwable)} instead so that the proper super constructor is used.
     *
     * <p>NOTE: Most of the time you wouldn't want to use this constructor directly - use the
     * {@link ApiException.Builder} instead.
     */
    public ApiException(List<ApiError> apiErrors, List<Pair<String, String>> extraDetailsForLogging, String message) {
        super(message);

        if (apiErrors == null || apiErrors.isEmpty())
            throw new IllegalArgumentException("apiErrors cannot be null or empty");

        if (extraDetailsForLogging == null)
            extraDetailsForLogging = new ArrayList<>();

        this.apiErrors = new ArrayList<>(apiErrors);
        this.extraDetailsForLogging = new ArrayList<>(extraDetailsForLogging);
    }

    /**
     * Creates an instance on top of {@link Exception#Exception(String, Throwable)} super constructor. You can safely
     * pass in null for message if you have no message. If another exception did *not* cause this to be thrown then
     * you'll want {@link #ApiException(List, List, String)} instead so that the proper super constructor is used.
     *
     * <p>NOTE: Most of the time you wouldn't want to use this constructor directly - use the
     * {@link ApiException.Builder} instead.
     */
    public ApiException(List<ApiError> apiErrors, List<Pair<String, String>> extraDetailsForLogging, String message,
                        Throwable cause) {
        super(message, cause);

        if (apiErrors == null || apiErrors.isEmpty())
            throw new IllegalArgumentException("apiErrors cannot be null or empty");

        if (extraDetailsForLogging == null)
            extraDetailsForLogging = new ArrayList<>();

        this.apiErrors = new ArrayList<>(apiErrors);
        this.extraDetailsForLogging = new ArrayList<>(extraDetailsForLogging);
    }

    /**
     * Shortcut for calling {@link ApiException.Builder#newBuilder()} directly.
     */
    public static Builder newBuilder() {
        return Builder.newBuilder();
    }

    /**
     * The {@link ApiError}s associated with this instance. Will never be null or empty.
     */
    public List<ApiError> getApiErrors() {
        return apiErrors;
    }

    /**
     * Any extra details you want logged when this error is handled. Will never be null, but might be empty.
     */
    public List<Pair<String, String>> getExtraDetailsForLogging() {
        return extraDetailsForLogging;
    }

    /**
     * Builder for {@link ApiException}.
     */
    @SuppressWarnings("WeakerAccess")
    public static class Builder {
        private List<ApiError> apiErrors = new ArrayList<>();
        private List<Pair<String, String>> extraDetailsForLogging = new ArrayList<>();
        private String message;
        private Throwable cause;

        public Builder() {}

        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * Adds the given errors to what will ultimately become {@link ApiException#apiErrors}.
         */
        public Builder withApiErrors(Collection<ApiError> apiErrors) {
            this.apiErrors.addAll(apiErrors);
            return this;
        }

        /**
         * Adds the given errors to what will ultimately become {@link ApiException#apiErrors}.
         */
        public Builder withApiErrors(ApiError... apiErrors) {
            return withApiErrors(Arrays.asList(apiErrors));
        }

        /**
         * Adds the given logging details to what will ultimately become {@link ApiException#extraDetailsForLogging}.
         */
        public Builder withExtraDetailsForLogging(Collection<Pair<String, String>> extraDetailsForLogging) {
            this.extraDetailsForLogging.addAll(extraDetailsForLogging);
            return this;
        }

        /**
         * Adds the given logging details to what will ultimately become {@link ApiException#extraDetailsForLogging}.
         */
        @SafeVarargs
        public final Builder withExtraDetailsForLogging(Pair<String, String>... extraDetailsForLogging) {
            return withExtraDetailsForLogging(Arrays.asList(extraDetailsForLogging));
        }

        /**
         * The given message will be used as part of the {@link Exception#Exception(String)} or
         * {@link Exception#Exception(String, Throwable)} super constructor. Could be used as context for what went
         * wrong if the API Errors aren't self explanatory.
         */
        public Builder withExceptionMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * The given throwable will be applied to the {@link Exception#Exception(String, Throwable)} super constructor.
         * If another error caused this {@link ApiException} in the first place then you should definitely include it
         * here so it can be logged and help with debugging the issue.
         */
        public Builder withExceptionCause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        /**
         * Creates the {@link ApiException} from the data this builder contains.
         */
        public ApiException build() {
            if (this.cause == null)
                return new ApiException(apiErrors, extraDetailsForLogging, message);
            else
                return new ApiException(apiErrors, extraDetailsForLogging, message, cause);
        }
    }
}
