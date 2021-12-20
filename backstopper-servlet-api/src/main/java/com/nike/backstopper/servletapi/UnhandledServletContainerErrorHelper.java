package com.nike.backstopper.servletapi;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.exception.WrapperException;
import com.nike.internal.util.Pair;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;

/**
 * This class is intended to help with integrating Backstopper with Servlet containers for handling otherwise-unhandled
 * errors that occur outside of a stack, but instead originate in the servlet container itself. The recommended way to
 * use this class is:
 *
 * <ol>
 *     <li>
 *         Configure your servlet container to map all Throwables and other error conditions (e.g. 404) that *it* (the
 *         servlet container) handles to a page like /error that your Backstopper-enabled stack is listening on.
 *     </li>
 *     <li>
 *         When the servlet container catches one of those errors and forwards it to your catch-all /error page,
 *         you should then call {@link #extractOrGenerateErrorForRequest(ServletRequest, ProjectApiErrors)} to
 *         extract or generate the Throwable for Backstopper to handle.
 *     </li>
 *     <li>
 *         Since you're now in the context of your Backstopper-enabled stack, you can throw whatever is returned by
 *         {@link #extractOrGenerateErrorForRequest(ServletRequest, ProjectApiErrors)} and Backstopper will handle
 *         it appropriately.
 *     </li>
 * </ol>
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class UnhandledServletContainerErrorHelper {

    protected static final List<String> DEFAULT_THROWABLE_REQUEST_ATTR_NAMES = Arrays.asList(
        // Try the Springboot 2 attrs first.
        //      Corresponds to org.springframework.boot.web.reactive.error.DefaultErrorAttributes.ERROR_ATTRIBUTE.
        "org.springframework.boot.web.reactive.error.DefaultErrorAttributes.ERROR",
        //      Corresponds to org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR_ATTRIBUTE.
        "org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR",

        // Try the Springboot 1 attr next.
        //      Corresponds to org.springframework.boot.autoconfigure.web.DefaultErrorAttributes.ERROR_ATTRIBUTE.
        "org.springframework.boot.autoconfigure.web.DefaultErrorAttributes.ERROR",
        
        // Fall back to the Servlet API value last.
        //      Corresponds to javax.servlet.RequestDispatcher.ERROR_EXCEPTION.
        "javax.servlet.error.exception"
    );

    protected static final List<String> DEFAULT_ERROR_STATUS_CODE_REQUEST_ATTR_NAMES = Collections.singletonList(
        // Servlet API value.
        //      Corresponds to javax.servlet.RequestDispatcher.ERROR_STATUS_CODE.
        "javax.servlet.error.status_code"
    );

    public @NotNull Throwable extractOrGenerateErrorForRequest(
        ServletRequest request,
        @NotNull ProjectApiErrors projectApiErrors
    ) {
        Throwable ex = extractErrorThrowable(request);

        if (ex != null) {
            // We found the desired Throwable embedded in the request, so return it, wrapped in a WrapperException
            //      for the additional context of this code in the stack trace.
            return new WrapperException("Caught a container exception.", ex);
        }

        // This case (no Throwable found for the request) can happen when the stack (e.g. Spring/Springboot/Jersey/etc)
        //      never sees the request, i.e. when the container redirects to an error page path for some reason
        //      without an associated exception.
        //      One common reason for this is a 404 caught at the container level, although there may be other reasons.

        // See if it's a 404.
        Integer errorStatusCode = extractErrorStatusCode(request);
        if (errorStatusCode != null && errorStatusCode == 404) {
            // It's a 404, but without an exception. Create a synthetic-but-generic exception to cover this
            //      that will be mapped by backstopper to a 404.
            return ApiException
                .newBuilder()
                .withApiErrors(projectApiErrors.getNotFoundApiError())
                .withExceptionMessage("Synthetic exception for container 404.")
                .withExtraDetailsForLogging(
                    Pair.of("synthetic_exception_for_container_404", "true")
                )
                .build();
        }
        // When spring security enforces scopes via  http.anyRequest().hasAnyAuthority("SCOPE_use:adminApi", "ROLE_ARMORY_ADMIN"),
        // a 403 will be returned if principal is missing scope
        if (errorStatusCode != null && errorStatusCode == 403) {
            // It's a 403, but without an exception. Create a synthetic-but-generic exception to cover this
            //      that will be mapped by backstopper to a 403.
            return ApiException
              .newBuilder()
              .withApiErrors(projectApiErrors.getForbiddenApiError())
              .withExceptionMessage("Synthetic exception for container 403.")
              .withExtraDetailsForLogging(
                Pair.of("synthetic_exception_for_container_403", "true")
              )
              .build();
        }
        else {
            // It's not a 404. Create a synthetic-but-generic exception to cover this that will be mapped
            //      by backstopper to a 500.
            // NOTE: If you hit this case, and it's one that we could cover more correctly like the 404 case
            //      above, then please submit an issue to the Backstopper issue tracker on GitHub:
            //      https://github.com/Nike-Inc/backstopper/issues
            return ApiException
                .newBuilder()
                .withApiErrors(projectApiErrors.getGenericServiceError())
                .withExceptionMessage("Synthetic exception for unhandled container status code: " + errorStatusCode)
                .withExtraDetailsForLogging(
                    Pair.of("synthetic_exception_for_unhandled_status_code", String.valueOf(errorStatusCode))
                )
                .build();
        }
    }

    protected @NotNull List<String> getThrowableRequestAttrNames() {
        return DEFAULT_THROWABLE_REQUEST_ATTR_NAMES;
    }

    protected @Nullable Throwable extractErrorThrowable(@NotNull ServletRequest request) {
        for (String throwableAttrName : getThrowableRequestAttrNames()) {
            Object throwableObj = request.getAttribute(throwableAttrName);
            if (throwableObj instanceof Throwable) {
                return (Throwable)throwableObj;
            }
        }

        // We couldn't find the throwable in the request attributes, so return null.
        return null;
    }

    protected @NotNull List<String> getErrorStatusCodeRequestAttrNames() {
        return DEFAULT_ERROR_STATUS_CODE_REQUEST_ATTR_NAMES;
    }

    protected @Nullable Integer extractErrorStatusCode(@NotNull ServletRequest request) {
        for (String errorStatusCodeAttrName : getErrorStatusCodeRequestAttrNames()) {
            Integer errorStatusCode = extractRequestAttrAsInteger(request, errorStatusCodeAttrName);
            if (errorStatusCode != null) {
                return errorStatusCode;
            }
        }

        // We couldn't find the status code in the request attributes, so return null.
        return null;
    }

    protected @Nullable Integer extractRequestAttrAsInteger(@NotNull ServletRequest request, @NotNull String attrName) {
        Object attrObj = request.getAttribute(attrName);
        if (attrObj == null) {
            return null;
        }

        if (attrObj instanceof Integer) {
            return (Integer)attrObj;
        }

        // The attr is not null, but also not an Integer. Try to parse its string representation it to an Integer.
        try {
            return Integer.parseInt(attrObj.toString());
        }
        catch (Exception ex) {
            // Couldn't be parsed to an Integer, and we have no other options, so return null.
            return null;
        }
    }

}
