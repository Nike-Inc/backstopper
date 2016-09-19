package com.nike.backstopper.springsample;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.io.IOException;

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
        contextHandler.addServlet(new ServletHolder(new DispatcherServlet(context)), "/*");
        contextHandler.addEventListener(new ContextLoaderListener(context));
        return contextHandler;
    }

    private static WebApplicationContext generateWebAppContext() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setConfigLocation("com.nike.backstopper.springsample.config");
        return context;
    }

    private static ErrorHandler generateErrorHandler() {
        ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
        errorHandler.addErrorPage(HttpServletResponse.SC_NOT_FOUND, "/error/404");
        errorHandler.addErrorPage(Throwable.class, "/error");
        return errorHandler;
    }
}
