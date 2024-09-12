package serverconfig.classpathscan;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import testonly.componenttest.spring.reusable.error.SampleProjectApiErrorsImpl;
import testonly.componenttest.spring.reusable.testutil.ExplodingServletFilter;

/**
 * Springboot config that uses {@link ComponentScan} to integrate Backstopper via classpath scanning of the
 * {@code com.nike.backstopper} package.
 *
 * @author Nic Munroe
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    // Component scan the core Backstopper+Springboot1 support.
    "com.nike.backstopper",
    // Component scan the controller.
    "testonly.componenttest.spring.reusable.controller"
})
@SuppressWarnings("unused")
public class Springboot3_3WebMvcClasspathScanConfig {

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
