package serverconfig.classpathscan;

import com.nike.backstopper.apierror.projectspecificinfo.ProjectApiErrors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.validation.Validation;
import javax.validation.Validator;

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
public class Spring5WebMvcClasspathScanConfig {

    @Bean
    public ProjectApiErrors getProjectApiErrors() {
        return new SampleProjectApiErrorsImpl();
    }

    @Bean
    public Validator getJsr303Validator() {
        return Validation.buildDefaultValidatorFactory().getValidator();
    }
}
