package com.nike.backstopper.handler.spring;

import com.nike.backstopper.apierror.ApiError;
import com.nike.backstopper.handler.RequestInfoForLogging;
import com.nike.backstopper.model.DefaultErrorContractDTO;
import com.nike.backstopper.model.util.JsonUtilWithDefaultErrorContractDTOSupport;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.util.Collection;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Similar to {@link com.nike.backstopper.handler.ApiExceptionHandlerUtils}, but provides helpers specific to this
 * Spring Web MVC Backstopper plugin module.
 *
 * @author Nic Munroe
 */
@Named
@Singleton
@SuppressWarnings("WeakerAccess")
public class SpringApiExceptionHandlerUtils {

    /**
     * The default instance you can use if you don't need any customized logic. You can override this class and
     * its methods if you need alternate behavior.
     */
    public static final SpringApiExceptionHandlerUtils DEFAULT_IMPL = new SpringApiExceptionHandlerUtils();

    /**
     * Reusable static method for generating a ModelAndView that will be serialized to a JSON representation of the
     * DefaultErrorContractDTO.
     *
     * @return A ModelAndView that will be serialized to a JSON representation of the DefaultErrorContractDTO. (NOTE:
     * make sure the DefaultErrorContractDTO is FULLY populated before calling this method! Changes to the
     * DefaultErrorContractDTO after calling this method may not be reflected in the returned ModelAndView).
     */
    public ModelAndView generateModelAndViewForErrorResponse(
        DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
        Throwable originalException, RequestInfoForLogging request
    ) {
        MappingJackson2JsonView view = new MappingJackson2JsonView();
        view.setExtractValueFromSingleKeyModel(true);
        view.setObjectMapper(getObjectMapperForJsonErrorResponseSerialization(
            errorContractDTO, httpStatusCode, rawFilteredApiErrors, originalException, request
        ));
        ModelAndView mv = new ModelAndView(view);
        mv.addObject(errorContractDTO);
        return mv;
    }

    /**
     * @return The {@link ObjectMapper} that should be used by {@link
     * #generateModelAndViewForErrorResponse(DefaultErrorContractDTO, int, Collection, Throwable,
     * RequestInfoForLogging)} for serializing the error contract. Defaults to {@link
     * JsonUtilWithDefaultErrorContractDTOSupport#DEFAULT_SMART_MAPPER}.
     */
    @SuppressWarnings("UnusedParameters")
    protected ObjectMapper getObjectMapperForJsonErrorResponseSerialization(
        DefaultErrorContractDTO errorContractDTO, int httpStatusCode, Collection<ApiError> rawFilteredApiErrors,
        Throwable originalException, RequestInfoForLogging request
    ) {
        return JsonUtilWithDefaultErrorContractDTOSupport.DEFAULT_SMART_MAPPER;
    }
}
