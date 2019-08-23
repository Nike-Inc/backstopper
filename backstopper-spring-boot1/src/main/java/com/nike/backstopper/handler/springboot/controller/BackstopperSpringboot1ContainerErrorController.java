package com.nike.backstopper.handler.springboot.controller;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.servletapi.UnhandledServletContainerErrorHelper;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletRequest;

/**
 * The purpose of this controller is to give a place for the Servlet container to route errors to that would otherwise
 * be served by Spring Boot's default {@code BasicErrorController}. Since this controller is handled by Spring, once
 * the container forwards the request here, we can simply extract the original exception from the request attributes
 * and throw it to let Backstopper handle it. We use {@link
 * UnhandledServletContainerErrorHelper#extractOrGenerateErrorForRequest(ServletRequest, ProjectApiErrors)} for
 * this purpose.
 *
 * <p>If this controller is registered with Spring, then {@code BasicErrorController} will not be registered, and this
 * will be used for container error handling instead.
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
@SuppressWarnings("WeakerAccess")
public class BackstopperSpringboot1ContainerErrorController implements ErrorController {

    protected final @NotNull ProjectApiErrors projectApiErrors;
    protected final @NotNull UnhandledServletContainerErrorHelper unhandledServletContainerErrorHelper;
    protected final String errorPath;

    @SuppressWarnings("ConstantConditions")
    public BackstopperSpringboot1ContainerErrorController(
        @NotNull ProjectApiErrors projectApiErrors,
        @NotNull UnhandledServletContainerErrorHelper unhandledServletContainerErrorHelper,
        @NotNull ServerProperties serverProperties
    ) {
        if (projectApiErrors == null) {
            throw new NullPointerException("ProjectApiErrors cannot be null.");
        }

        if (unhandledServletContainerErrorHelper == null) {
            throw new NullPointerException("UnhandledServletContainerErrorHelper cannot be null.");
        }

        if (serverProperties == null) {
            throw new NullPointerException("ServerProperties cannot be null.");
        }

        this.projectApiErrors = projectApiErrors;
        this.unhandledServletContainerErrorHelper = unhandledServletContainerErrorHelper;
        this.errorPath = serverProperties.getError().getPath();
    }

    @RequestMapping
    public void error(ServletRequest request) throws Throwable {
        throw unhandledServletContainerErrorHelper.extractOrGenerateErrorForRequest(request, projectApiErrors);
    }

    @Override
    public String getErrorPath() {
        return errorPath;
    }

}
