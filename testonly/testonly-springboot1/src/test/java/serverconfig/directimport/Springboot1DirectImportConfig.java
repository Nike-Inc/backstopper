package serverconfig.directimport;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;
import com.nike.backstopper.handler.springboot.config.BackstopperSpringboot1Config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;

import javax.validation.Validation;
import javax.validation.Validator;

import testonly.componenttest.spring.reusable.controller.SampleController;
import testonly.componenttest.spring.reusable.error.SampleProjectApiErrorsImpl;
import testonly.componenttest.spring.reusable.testutil.ExplodingServletFilter;

/**
 * Springboot config that uses {@link Import} to integrate Backstopper via direct import of
 * {@link BackstopperSpringboot1Config}.
 *
 * @author Nic Munroe
 */
@SpringBootApplication
@Import({
    // Import core Backstopper+Springboot1 support.
    BackstopperSpringboot1Config.class,
    // Import the controller.
    SampleController.class
})
public class Springboot1DirectImportConfig {

    @Bean
    public ProjectApiErrors getProjectApiErrors() {
        return new SampleProjectApiErrorsImpl();
    }

    @Bean
    public Validator getJsr303Validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Bean
    public FilterRegistrationBean explodingServletFilter() {
        FilterRegistrationBean frb = new FilterRegistrationBean(new ExplodingServletFilter());
        frb.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return frb;
    }
}
