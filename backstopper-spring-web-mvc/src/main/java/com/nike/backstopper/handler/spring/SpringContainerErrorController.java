package com.nike.backstopper.handler.spring;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.servletapi.UnhandledServletContainerErrorHelper;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletRequest;

import static com.nike.backstopper.handler.spring.SpringContainerErrorController.SpringbootErrorControllerIsNotOnClasspath;


/**
 * The purpose of this controller is to give a place for the Servlet container to route errors to that would otherwise
 * be served by the container with an HTML page (for example). Since this controller is handled by Spring, once the
 * container forwards the request here, we can simply extract the original exception from the request attributes and
 * throw it to let Backstopper handle it. We use {@link
 * UnhandledServletContainerErrorHelper#extractOrGenerateErrorForRequest(ServletRequest, ProjectApiErrors)} for
 * this purpose.
 *
 * <p>NOTE: You'll need to configure your Servlet container to forward exceptions and errors it handles outside of
 * Spring (like 404s) to {@code /error} for this controller to be able to handle them.
 *
 * <p>ALSO NOTE: We add a {@link Conditional} annotation for {@link SpringbootErrorControllerIsNotOnClasspath} to
 * prevent this controller from being registered when your application is running in a Springboot environment, since
 * this controller listens on the same path as Springboot's default {@code BasicErrorController} and we'd get a
 * conflict otherwise.
 *
 * <p>If you're in a Springboot environment, you should pull in the {@code backstopper-spring-boot1} or
 * {@code backstopper-spring-boot2} library (whichever is appropriate for your app) and register
 * {@code BackstopperSpringboot[1/2]ContainerErrorController} to take the place of this class (and override the default
 * {@code BasicErrorController}).
 */
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
// Use a @Conditional to prevent this class from being registered if we're running in a Springboot 1 or Springboot 2
//      application. This is necessary because this class would conflict with the auto-registered BasicErrorController
//      since they both listen to the same path.
// As mentioned in the class javadocs, if you're in a Springboot environment then you should pull in the
//      backstopper-spring-boot1 or backstopper-spring-boot2 library and
//      register BackstopperSpringboot[1/2]ContainerErrorController to take the place of this class.
@Conditional(SpringbootErrorControllerIsNotOnClasspath.class)
public class SpringContainerErrorController {

    protected final @NotNull ProjectApiErrors projectApiErrors;
    protected final @NotNull UnhandledServletContainerErrorHelper unhandledServletContainerErrorHelper;

    @SuppressWarnings("ConstantConditions")
    public SpringContainerErrorController(
        @NotNull ProjectApiErrors projectApiErrors,
        @NotNull UnhandledServletContainerErrorHelper unhandledServletContainerErrorHelper
    ) {
        if (projectApiErrors == null) {
            throw new NullPointerException("ProjectApiErrors cannot be null.");
        }

        if (unhandledServletContainerErrorHelper == null) {
            throw new NullPointerException("UnhandledServletContainerErrorHelper cannot be null.");
        }

        this.projectApiErrors = projectApiErrors;
        this.unhandledServletContainerErrorHelper = unhandledServletContainerErrorHelper;
    }

    @RequestMapping
    public void error(ServletRequest request) throws Throwable {
        throw unhandledServletContainerErrorHelper.extractOrGenerateErrorForRequest(request, projectApiErrors);
    }

    /**
     * A {@link ConfigurationCondition} for use with the {@link Conditional} annotation that can be used to prevent
     * the inclusion of a bean during classpath scanning / importing. This particular class will prevent bean registration
     * if Springboot 1 or Springboot 2's {@code ErrorController} is on the classpath.
     *
     * <p>This is used to prevent {@link SpringContainerErrorController} from being registered if you're running in
     * a Springboot environment, because that controller would conflict with the auto-registered
     * {@code BasicErrorController} since they both listen to the same path.
     *
     * @author Nic Munroe
     */
    protected static class SpringbootErrorControllerIsNotOnClasspath implements ConfigurationCondition {

        @Override
        public ConfigurationPhase getConfigurationPhase() {
            return ConfigurationPhase.REGISTER_BEAN;
        }

        @Override
        public boolean matches(
            ConditionContext context, AnnotatedTypeMetadata metadata
        ) {
            if (
                // Springboot 1
                isClassAvailableOnClasspath("org.springframework.boot.autoconfigure.web.ErrorController")
                // Springboot 2
                || isClassAvailableOnClasspath("org.springframework.boot.web.servlet.error.ErrorController")
            ) {
                // We're in a Springboot 1 or Springboot 2 application. Return false to prevent registration.
                return false;
            }

            // Didn't detect ErrorController on the classpath, so return true.
            return true;
        }

        protected boolean isClassAvailableOnClasspath(String classname) {
            try {
                Class.forName(classname);
                return true;
            }
            catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
