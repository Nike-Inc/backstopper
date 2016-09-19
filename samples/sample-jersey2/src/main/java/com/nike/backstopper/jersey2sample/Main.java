package com.nike.backstopper.jersey2sample;

import com.nike.backstopper.jersey2sample.config.Jersey2SampleResourceConfig;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.IOException;

/**
 * Starts up the Backstopper Jersey 2 Sample server (on port 8080 by default).
 *
 * @author Nic Munroe
 */
public class Main {

    @SuppressWarnings("WeakerAccess")
    public static final String PORT_SYSTEM_PROP_KEY = "jersey2Sample.server.port";

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

        ServletContainer jerseyServletContainer = new ServletContainer(new Jersey2SampleResourceConfig());
        contextHandler.addServlet(new ServletHolder(jerseyServletContainer), "/*");
        return contextHandler;
    }
}
