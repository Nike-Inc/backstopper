package com.nike.backstopper.springbootsample.controller;

import com.nike.backstopper.apierror.ApiErrorValue;
import com.nike.backstopper.springbootsample.model.SampleModel;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

/**
 * This is sample endpoint is useful for showing JSR 303 Validation integration in Backstopper using {@link ApiErrorValue}.
 *
 * @author Andrey Tsarenko
 * @see SampleModel
 */
@Controller
@RequestMapping("/sample")
public class SampleController {

    @PostMapping
    public SampleModel postSampleModel(@Valid @RequestBody SampleModel sampleModel) {
        return sampleModel;
    }

}
