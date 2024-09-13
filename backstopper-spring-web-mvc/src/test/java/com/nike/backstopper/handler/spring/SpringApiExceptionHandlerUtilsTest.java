package com.nike.backstopper.handler.spring;

import com.nike.backstopper.apierror.testing.base.BaseSpringEnabledValidationTestCase;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;
import com.nike.backstopper.model.DefaultErrorContractDTO;

import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the functionality of {@link com.nike.backstopper.handler.ApiExceptionHandlerUtils}.
 *
 * @author Nic Munroe
 */
public class SpringApiExceptionHandlerUtilsTest extends BaseSpringEnabledValidationTestCase {

    @Test
    public void generateModelAndViewForErrorResponseShouldGenerateModelAndViewWithErrorContractAsOnlyModelObject() {
        DefaultErrorContractDTO
            erv = new DefaultErrorContractDTO("someRequestId", Arrays.asList(BarebonesCoreApiErrorForTesting.NO_ACCEPTABLE_REPRESENTATION,
                                                                             BarebonesCoreApiErrorForTesting.UNSUPPORTED_MEDIA_TYPE));

        ModelAndView mav = new SpringApiExceptionHandlerUtils().generateModelAndViewForErrorResponse(erv, -1, null, null, null);
        assertThat(mav.getModel().size(), is(1));
        assertThat(mav.getModel().values().iterator().next() == erv, is(true));
    }
}
