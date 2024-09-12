package serverconfig.directimport;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.springboot.config.BackstopperSpringboot3WebMvcConfig;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import testonly.componenttest.spring.reusable.controller.SampleController;
import testonly.componenttest.spring.reusable.error.SampleProjectApiErrorsImpl;
import testonly.componenttest.spring.reusable.testutil.ExplodingServletFilter;

/**
 * Springboot config that uses {@link Import} to integrate Backstopper via direct import of
 * {@link BackstopperSpringboot3WebMvcConfig}.
 *
 * @author Nic Munroe
 */
@SpringBootApplication
@Import({
    // Import core Backstopper+Springboot1 support.
    BackstopperSpringboot3WebMvcConfig.class,
    // Import the controller.
    SampleController.class
})
@SuppressWarnings("unused")
public class Springboot3_0WebMvcDirectImportConfig {

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
    public FilterRegistrationBean<?> explodingServletFilter() {
        FilterRegistrationBean<?> frb = new FilterRegistrationBean<>(new ExplodingServletFilter());
        frb.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return frb;
    }
}
