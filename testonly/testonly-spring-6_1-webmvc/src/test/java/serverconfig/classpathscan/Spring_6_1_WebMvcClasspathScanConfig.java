package serverconfig.classpathscan;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import testonly.componenttest.spring.reusable.error.SampleProjectApiErrorsImpl;

/**
 * Spring config that uses {@link ComponentScan} to integrate Backstopper via classpath scanning of the
 * {@code com.nike.backstopper} package.
 * 
 * @author Nic Munroe
 */
@Configuration
@ComponentScan(basePackages = {
    // Component scan the core Backstopper+Spring support.
    "com.nike.backstopper",
    // Component scan the controller.
    "testonly.componenttest.spring.reusable.controller"
})
@EnableWebMvc
@SuppressWarnings("unused")
public class Spring_6_1_WebMvcClasspathScanConfig {

    @Bean
    public ProjectApiErrors getProjectApiErrors() {
        return new SampleProjectApiErrorsImpl();
    }

    @Bean
    public Validator getJsr303Validator() {
        //noinspection resource
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
