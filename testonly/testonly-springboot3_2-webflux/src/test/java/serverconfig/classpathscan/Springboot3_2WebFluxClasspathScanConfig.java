package serverconfig.classpathscan;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.WebFilter;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import testonly.componenttest.spring.reusable.controller.SampleWebFluxController;
import testonly.componenttest.spring.reusable.error.SampleProjectApiErrorsImpl;
import testonly.componenttest.spring.reusable.filter.ExplodingHandlerFilterFunction;
import testonly.componenttest.spring.reusable.filter.ExplodingWebFilter;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static testonly.componenttest.spring.reusable.controller.SampleWebFluxController.SAMPLE_FROM_ROUTER_FUNCTION_PATH;

/**
 * Springboot config that uses {@link ComponentScan} to integrate Backstopper via classpath scanning of the
 * {@code com.nike.backstopper} package.
 *
 * @author Nic Munroe
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    // Component scan the core Backstopper+Spring WebFlux support.
    "com.nike.backstopper",
    // Component scan the controller (note this is the reusable WebFlux controller, not the reusable servlet controller).
    "testonly.componenttest.spring.reusable.controller"
})
@SuppressWarnings("unused")
public class Springboot3_2WebFluxClasspathScanConfig {

    @Bean
    public ProjectApiErrors getProjectApiErrors() {
        return new SampleProjectApiErrorsImpl();
    }

    @Bean
    public Validator getJsr303Validator() {
        //noinspection resource
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebFilter explodingWebFilter() {
        return new ExplodingWebFilter();
    }

    @Bean
    public RouterFunction<ServerResponse> sampleRouterFunction(SampleWebFluxController sampleController) {
        return RouterFunctions
            .route(GET(SAMPLE_FROM_ROUTER_FUNCTION_PATH), sampleController::getSampleModelRouterFunction)
            .filter(new ExplodingHandlerFilterFunction());
    }
}
