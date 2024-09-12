package testonly.componenttest.spring.reusable.testutil;

import com.nike.backstopper.exception.ApiException;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import testonly.componenttest.spring.reusable.error.SampleProjectApiError;

/**
 * Servlet filter that will throw an {@link ApiException} for
 * {@link SampleProjectApiError#ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING} when it sees the following header in
 * the request: {@code throw-servlet-filter-exception: true}
 *
 * @author Nic Munroe
 */
public class ExplodingServletFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        if ("true".equals(request.getHeader("throw-servlet-filter-exception"))) {
            throw ApiException
                .newBuilder()
                .withApiErrors(SampleProjectApiError.ERROR_THROWN_IN_SERVLET_FILTER_OUTSIDE_SPRING)
                .withExceptionMessage("Exception thrown from Servlet Filter outside Spring")
                .build();
        }
        filterChain.doFilter(request, response);
    }

}
