package com.nike.backstopper.model;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.apierror.testutil.BarebonesCoreApiErrorForTesting;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Tests the functionality of {@link DefaultErrorContractDTO}.
 *
 * @author Nic Munroe
 */
public class DefaultErrorContractDTOTest {

    @Test
    public void defaultConstructorShouldNotBlowUp() {
        DefaultErrorContractDTO erv = new DefaultErrorContractDTO();
        assertThat(erv.error_id, nullValue());
        assertThat(erv.errors, notNullValue());
        assertThat(erv.errors.size(), is(0));
    }

    @Test
    public void shouldUsePassedInErrorId() {
        UUID uuid = UUID.randomUUID();
        DefaultErrorContractDTO erv = new DefaultErrorContractDTO(uuid.toString(), null);
        assertThat(erv.error_id, is(uuid.toString()));
    }

    @Test
    public void shouldNotExplodeIfYouPassInNullErrorCollection() {
        DefaultErrorContractDTO erv = new DefaultErrorContractDTO(null, null);
        assertThat(erv.errors, notNullValue());
        assertThat(erv.errors
                .size(), is(0));
    }

    @Test
    public void shouldCorrectlyTranslateApiErrorsToIndividualErrorViews() {
        List<ApiError> apiErrors = Arrays.asList(BarebonesCoreApiErrorForTesting.NO_ACCEPTABLE_REPRESENTATION,
                                                 BarebonesCoreApiErrorForTesting.MALFORMED_REQUEST,
                                                 BarebonesCoreApiErrorForTesting.OUTSIDE_DEPENDENCY_RETURNED_AN_UNRECOVERABLE_ERROR);
        DefaultErrorContractDTO erv = new DefaultErrorContractDTO(null, apiErrors);
        assertThat(erv.errors
                .size(), is(apiErrors.size()));
        for (int i = 0; i < apiErrors.size(); i++) {
            ApiError ae = apiErrors.get(i);
            DefaultErrorDTO iev = erv.errors.get(i);
            assertThat(iev.code, is(ae.getErrorCode()));
            assertThat(iev.message, is(ae.getMessage()));
        }
    }
}
