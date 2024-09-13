package testonly.componenttest.spring.reusable.jettyserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import testonly.componenttest.spring.reusable.testutil.ExplodingServletFilter;

/**
 * Helper class for {@code testonly-spring*} component tests that configures and starts/stops a Spring MVC server
 * running in Jetty. No Backstopper integration is done here - it's all up to the Spring config class you pass into
 * the constructor.
 *
 * @author Nic Munroe
 */
public class SpringMvcJettyComponentTestServer {

    private final Server server;

    public SpringMvcJettyComponentTestServer(int port, Class<?> springConfigClass) {
        server = new Server(port);
        server.setHandler(generateServletContextHandler(generateWebAppContext(springConfigClass)));
    }

    public void startServer() throws Exception {
        server.start();
    }

    public void shutdownServer() throws Exception {
        if (server != null) {
            server.stop();
            server.destroy();
        }
    }

    private static ServletContextHandler generateServletContextHandler(WebApplicationContext context) {
        ServletContextHandler contextHandler = new ServletContextHandler();
        contextHandler.setErrorHandler(generateErrorHandler());
        contextHandler.setContextPath("/");
        contextHandler.addServlet(new ServletHolder(generateDispatcherServlet(context)), "/*");
        contextHandler.addEventListener(new ContextLoaderListener(context));
        contextHandler.addFilter(
            ExplodingServletFilter.class, "/*", EnumSet.allOf(DispatcherType.class)
        );
        return contextHandler;
    }

    private static WebApplicationContext generateWebAppContext(Class<?> springConfigClass) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setConfigLocation(springConfigClass.getPackage().getName());
        return context;
    }

    private static ErrorHandler generateErrorHandler() {
        ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
        errorHandler.addErrorPage(Throwable.class, "/error");
        return errorHandler;
    }

    private static DispatcherServlet generateDispatcherServlet(WebApplicationContext context) {
        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
        // By setting dispatcherServlet.setThrowExceptionIfNoHandlerFound() to true we get a NoHandlerFoundException
        //      thrown for a 404 instead of being forced to use error pages.
        dispatcherServlet.setThrowExceptionIfNoHandlerFound(true);
        return dispatcherServlet;
    }

}
