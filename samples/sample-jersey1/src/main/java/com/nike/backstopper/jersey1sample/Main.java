package com.nike.backstopper.jersey1sample;

import com.nike.backstopper.jersey1sample.config.Jersey1SampleConfigHelper;

import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.core.servlet.WebAppResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.util.Collections;

/**
 * Starts up the Backstopper Jersey 1 Sample server (on port 8080 by default).
 *
 * @author Nic Munroe
 */
public class Main {

    @SuppressWarnings("WeakerAccess")
    public static final String PORT_SYSTEM_PROP_KEY = "jersey1Sample.server.port";

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
        server.setHandler(generateServletContextHandler());

        return server;
    }

    private static ServletContextHandler generateServletContextHandler() throws IOException {
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setContextPath("/");

        ResourceConfig rc = new WebAppResourceConfig(Collections.<String, Object>emptyMap(), contextHandler.getServletContext());
        rc.getSingletons().add(Jersey1SampleConfigHelper.generateJerseyApiExceptionHandler());
        rc.getSingletons().add(Jersey1SampleConfigHelper.generateSampleResource());
        ServletContainer jerseyServletContainer = new ServletContainer(rc);
        contextHandler.addServlet(new ServletHolder(jerseyServletContainer), "/*");
        return contextHandler;
    }

}
