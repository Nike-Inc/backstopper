package com.nike.backstopper.springsample.controller;

import com.nike.backstopper.apierror.sample.SampleCoreApiError;
import com.nike.backstopper.exception.ApiException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * The purpose of this controller is to give a place for the container to route errors to that conform to our error
 * contract. In this case we map 404 errors to {@link SampleCoreApiError#NOT_FOUND}, and everything else
 * to {@link SampleCoreApiError#UNHANDLED_FRAMEWORK_ERROR}. Any other more specific mappings would be left as an
 * exercise to the reader.
 *
 * <p>Note that this is a quick &amp; dirty integration with Jetty using
 * {@link org.eclipse.jetty.servlet.ErrorPageErrorHandler#addErrorPage(int, String)} and
 * {@link org.eclipse.jetty.servlet.ErrorPageErrorHandler#addErrorPage(Class, String)} - in particular by doing it this
 * way with what essentially amounts to a redirect you're losing some context of what caused the error because you're
 * losing a handle on the original exception and creating a new one. This gives the caller a reasonable error response
 * payload that conforms to the error contract and prevents information leaking as *all* otherwise unhandled framework
 * errors must come through here, but you lose out on some potentially important debugging information in the logs.
 * Therefore for a production system you'd probably want to find a better way to integrate that does not lose the
 * original exception context.
 */
@Controller
@RequestMapping("/error")
public class ErrorController {

    @RequestMapping
    @ResponseBody
    public void unknownError() {
        throw ApiException.newBuilder()
                          .withApiErrors(SampleCoreApiError.UNHANDLED_FRAMEWORK_ERROR)
                          .withExceptionMessage("Unknown container/framework error.").build();
    }

    @RequestMapping("/404")
    @ResponseBody
    public void notFound() {
        throw ApiException.newBuilder()
                          .withApiErrors(SampleCoreApiError.NOT_FOUND)
                          .withExceptionMessage("Framework/container 404 not found.").build();
    }

}
