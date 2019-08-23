package com.nike.backstopper.springsample;

import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.springsample.error.SampleProjectApiError;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Starts up the Backstopper Spring Web MVC Sample server (on port 8080 by default).
 *
 * @author Nic Munroe
 */
public class Main {

    public static final String PORT_SYSTEM_PROP_KEY = "springSample.server.port";

    public static void main(String[] args) throws Exception {
        Server server = createServer(Integer.parseInt(System.getProperty(PORT_SYSTEM_PROP_KEY, "8080")));

        try {
            server.start();
            server.join();
        }
        finally {
            server.destroy();
        }
    }

    public static Server createServer(int port) throws Exception {
        Server server = new Server(port);
        server.setHandler(generateServletContextHandler(generateWebAppContext()));

        return server;
    }

    private static ServletContextHandler generateServletContextHandler(WebApplicationContext context) throws IOException {
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setErrorHandler(generateErrorHandler());
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(generateDispatcherServlet(context)), "/*");
        contextHandler.addEventListener(new ContextLoaderListener(context));
        contextHandler.addFilter(
            ExplodingFilter.class, "/*", EnumSet.allOf(DispatcherType.class)
        );
        return contextHandler;
    }

    private static WebApplicationContext generateWebAppContext() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setConfigLocation("com.nike.backstopper.springsample.config");
        return context;
    }

    private static ErrorHandler generateErrorHandler() {
        ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
        errorHandler.addErrorPage(Throwable.class, "/error");
        return errorHandler;
    }

    private static DispatcherServlet generateDispatcherServlet(WebApplicationContext context) {
        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
        // By setting dispatcherServlet.setThrowExceptionIfNoHandlerFound() to true we get a NoHandlerFoundException thrown
        //      for a 404 instead of being forced to use error pages. The exception can be directly handled by Backstopper
        //      which is much preferred - you don't lose any context that way.
        dispatcherServlet.setThrowExceptionIfNoHandlerFound(true);
        return dispatcherServlet;
    }

    public static class ExplodingFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
        ) throws ServletException, IOException {
            if ("true".equals(request.getHeader("throw-servlet-filter-exception"))) {
                throw ApiException
                    .newBuilder()
                    .withApiErrors(SampleProjectApiError.ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING)
                    .withExceptionMessage("Exception thrown from Servlet Filter outside Spring")
                    .build();
            }
            filterChain.doFilter(request, response);
        }

    }
}
