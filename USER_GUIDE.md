# Backstopper User Guide

**Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.** 

The [base project README](README.md) covers Backstopper at a surface level, including:

* [Overview](README.md#overview)
* Repository library modules (both [general](README.md#general_modules) and [framework-specific](README.md#framework_modules))
* Framework-specific [sample applications](README.md#samples)
* [Quickstart](README.md#quickstart).

This User Guide is for a more in-depth exploration of Backstopper. 

<a name="table_of_contents"></a>
## Table of Contents


* [Backstopper Key Components](#key_components)
* [JSR 303 Bean Validation Support](#jsr_303_support)
    * [Enabling Basic JSR 303 Validation](#jsr_303_basic_setup)
    * [Throwing JSR 303 Violations in a Backstopper-Compatible Way](#backstopper_compatible_jsr_303_exceptions)
    * [Backstopper Conventions for JSR 303 Bean Validation Support](#jsr303_conventions)
* [Reusable Unit Tests for Enforcing Backstopper Rules and Conventions](#reusable_tests)
    * [Unit Tests to Guarantee JSR 303 Annotation Message Naming Convention Conformance](#setup_jsr303_convention_unit_tests)
    * [Unit Test to Verify Your `ProjectApiErrors` Instance Conforms to Requirements](#setup_project_api_errors_unit_test)
* [Creating New Framework Integrations](#new_framework_integrations)
    * [New Framework Integration Pseudocode](#new_framework_pseudocode)
    * [Pseudocode Explanation and Further Details](#new_framework_pseudocode_explanation)
* [Backstopper Project Meta-Information](#meta_info)
    * [Motivation - Why Does Backstopper Exist?](#motivation)
    * [Goals](#key_goals)
    * [Philosophies](#key_philosophies)    

<a name="key_components"></a>
## Backstopper Key Components

This section should give you a basis for understanding the components of Backstopper and how they interact. See the javadocs for each component for further details. The following list is in rough conceptual order of "closest to error inception" -> "closest to framework response" as the exception flows through the Backstopper system.

* **`com.nike.backstopper.exception.*` exceptions** - Predefined typed exceptions that Backstopper knows how to handle that you can throw in your project to trigger desired behavior (assuming you include the basic `ApiExceptionHandlerListener`s when configuring Backstopper). In particular:
    * `ApiException` - Generic exception that gives you full control and flexibility over what errors and response headers are returned to the caller and what gets logged in the application logs. **When in doubt, throw one of these.**
    * `WrapperException` - Simple wrapper exception that you can use when you want the error handling system to handle the `WrapperException.getCause()` of the wrapper rather than the wrapper itself, but still log the entire stack trace (including the wrapper). This is often necessary in asynchronous scenarios where you want to add stack trace info for the logs but don't want to obscure the true cause of the error.
    * `ClientDataValidationError` and `ServersideValidationError` - Used when you want to have Backstopper handle JSR 303 Bean Validation violations. Rather than create and throw these yourself you should use `ClientDataValidationService` and `FailFastServersideValidationService` to inspect objects needing validation and construct and throw the appropriate exception. See the javadocs on these exceptions and their associated services for more info.
    * `NetworkExceptionBase` and related exceptions in `com.nike.backstopper.exception.network` - These are intended to help handle errors that occur when your application communicates with downstream services. You can create adapters for whatever client framework you use to convert client errors to these exceptions shortly after they are thrown so that Backstopper does the right thing without needing to create and register custom `ApiExceptionHandlerListener`s. See the javadocs on these exception classes for more details on how they should be used, and `DownstreamNetworkExceptionHandlerListener` for details on how they are used and interpreted by Backstopper.
* **`ApiError`** - The core unit of currency in Backstopper. This is a simple interface that defines an API error. It is very basic and closely resembles the data returned by the default error contract. Each `ApiError` contains the following information: 
    * A project/business error code (not to be confused with HTTP status code).
    * A human-readable message.
    * The HTTP status code that the error should map to.
    * A human-readable name for the error that is used in the [JSR 303 Bean Validation convention](#jsr303_conventions) to link up a JSR 303 constraint violation to an `ApiError`, and the name will always show up in the log message output whenever that error is returned to the caller. There should only ever be one `ApiError` with a given name in a given project's `ProjectApiErrors`.
    * Note that there is a `ApiErrorWithMetadata` class to allow you to wrap an existing `ApiError` with extra metadata without having to redefine the original with copy/pasted values.
* **`ProjectApiErrors`** - Contains information about a given project's errors and how it wants certain things handled. It provides the collections of `ApiErrors` associated with the project - both the "core errors" (a set of core reusable errors intended to be shared among multiple projects in an organization) as well as the "project-specific errors". `ProjectApiErrors` also defines a series of methods intended to indicate to `ApiExceptionHandlerListener`s which `ApiError` should be used in certain situations like resource not found, unsupported media type, generic 400, generic 500, generic 503, etc.
* **`ProjectSpecificErrorCodeRange`** - An interface returned by `ProjectApiErrors.getProjectSpecificErrorCodeRange()` that defines the error code range that all project-specific `ApiError`s must fall into. Core errors are excluded from this restriction. This is an optional feature - if you don't care what error codes are used in your project you can use `ProjectSpecificErrorCodeRange.ALLOW_ALL_ERROR_CODES` to allow any error code, although if you are in an organization that has many services (e.g. microservice environment) then this mechanism is an easy way to make sure different projects don't use the same error codes for fundamentally different errors. See the javadocs on this interface for more information on how to easily enforce these rules across projects.
* **`ApiExceptionHandlerListener`** - These are used in a plugin fashion by the `ApiExceptionHandlerBase` for your project to handle "known" exceptions. As new typed exceptions or error cases come up you can create a new `ApiExceptionHandlerListener` that knows how to handle the new exception and returns a `ApiExceptionHandlerListenerResult` (see below) with information on what to do, and register it with your project's `ApiExceptionHandlerBase`. At that point any time the new exception flows through your system it will be converted to the desired error contract for the caller exactly as you specified, and can be shared with other projects that need to handle the new exception the same way. There are a few framework-independent listeners that are relevant to all Backstopper-enabled projects, and each new framework integration usually defines one or more listeners that knows how to convert framework-specific exceptions into the appropriate `ApiError`s (provided by a project's `ProjectApiErrors`).
* **`ApiExceptionHandlerListenerResult`** - A class defining the result of an `ApiExceptionHandlerListener` inspection of a given exception. If the listener doesn't know what to do with the exception it will return `ApiExceptionHandlerListenerResult.ignoreResponse()` to say that a different listener should handle it. If the listener wants to handle the response it will return `ApiExceptionHandlerListenerResult.handleResponse(...)` with the set of `ApiError`s that should be associated with the exception, (optionally) any extra response headers that you want to be included in the response, and (optionally) any extra details that should show up in the application logs when the error's log message is output.   
* **`ApiExceptionHandlerUtils`** - This is used by various Backstopper components and provides numerous helper methods. This is your hook for several pieces of Backstopper behavior, including the format of the message that gets logged when an error is handled. Although it is a "utils" class none of the methods are static so you are free to override anything you wish. 
* **`ApiExceptionHandlerBase`** - Handles all "known" exceptions and conditions by running the exception through the project's list of `ApiExceptionHandlerListener`s. If a listener indicates it wants to handle the exception then the result is converted to a `ErrorResponseInfo` and all information about the request and error context is logged in a single log message which is tagged with a UUID that is also included in the response to the caller for trivial lookup of all the relevant debugging information whenever an error is sent to the caller. 
* **`UnhandledExceptionHandlerBase`** - Serves as the final safety net catch-all for anything not handled successfully by `ApiExceptionHandlerBase`. Works in a similar way to `ApiExceptionHandlerBase` by logging all context about the error and request, except it is designed to always return a generic service exception and *never* fail.
* **`ErrorResponseInfo`** - This is returned by `ApiExceptionHandlerBase` and `UnhandledExceptionHandlerBase` and indicates to the framework what it should return to the caller. Includes the HTTP status code that should be used, a map of extra headers that should be added to the response, and the response payload in a format suitable for the framework.   

There are other classes and components in Backstopper but the above are the major touchpoints and should give you a good grounding for further exploration.

[[back to table of contents]][toc]

<a name="jsr_303_support"></a>
## JSR 303 Bean Validation Support

Guaranteeing that JSR 303 Bean Validation violations *will* map to a specific API error (represented by the `ApiError` values returned by each project's `ProjectApiErrors` instance) is one of the major benefits of Backstopper. It requires throwing an exception that Backstopper knows how to handle that wraps the JSR 303 constraint violations, following a specific message naming convention when declaring constraint annotations, and to be safe you should set up a few unit tests that will catch any JSR 303 constraint annotation declarations that don't conform to the naming convention due to typos, copy-paste errors, or any other reason. The following sections cover these concerns and describe how to enable JSR 303 Bean Validation support in Backstopper.
 
(Note that JSR 303 integration is optional - you can successfully run Backstopper in an environment that does not include any JSR 303 support and it will work just fine.)

<a name="jsr_303_basic_setup"></a>
### Enabling Basic JSR 303 Validation

Enabling JSR 303 in your application is outside the scope of this User Guide - different containers and frameworks set it up in different ways and may depend on the JSR 303 implementation you're using. It is usually not too difficult - consult your framework's docs and google for tutorials and examples to see if your framework has built-in JSR 303 support (if it does and we already have a [sample application](README.md#samples) for your framework you can consult the sample app for an example on how to enable and use the JSR 303 support in Backstopper for that framework). 

In the worst case you can always manually create a `javax.validation.Validator` and use `ClientDataValidationService` to validate your objects. Just make sure a JSR 303 implementation library is on your classpath (e.g. [Hibernate Validator](http://hibernate.org/validator/) or [Apache BVal](http://bval.apache.org/)) and call `javax.validation.Validation.buildDefaultValidatorFactory().getValidator()`. `Validator` is thread safe so you only technically have to create one and can share it around.

[[back to table of contents]][toc]

<a name="backstopper_compatible_jsr_303_exceptions"></a>
### Throwing JSR 303 Violations in a Backstopper-Compatible Way

The JSR 303 `javax.validation.Validator` object does not throw exceptions when it sees an object that violates validation constraints, instead it returns a `Set` of `ConstraintViolation`s. Backstopper works by intercepting exceptions and translating them into `ApiError`s, so we need a mechanism to bridge what the JSR 303 `Validator` returns and what Backstopper needs: `ClientDataValidationError`. `ClientDataValidationError` is an exception that Backstopper knows how to handle and it wraps the `ConstraintViolations` returned by the JSR 303 `Validator` so that Backstopper can convert them to your project's `ApiError`s. It's recommended that you use `ClientDataValidationService` for validating your objects since it will automatically throw an appropriate `ClientDataValidationError` without you needing to worry about it. But if you can't or don't want to use that service you can create and throw `ClientDataValidationError` manually yourself. 
  
There is a similar exception and service for dealing with validating internal server logic (e.g. downstream requests to other services) where if a validation violation occurs you want the original caller to receive a generic HTTP status 5xx type service error since the error had nothing to do with data the caller sent you and they can't do anything about it, but at the same time you want all the debugging information about the JSR 303 errors to show up in the logs. The exception to use in these cases is `ServersideValidationError` and the service that automatically validates objects and throws that exception when it finds violations is `FailFastServersideValidationService`.  

Finally you'll need `ClientDataValidationErrorHandlerListener` and `ServersideValidationErrorHandlerListener` to be registered with Backstopper in your project for these exceptions to be picked up and handled correctly. These are default listeners that should be included with any Backstopper integration (even if you don't plan on using any JSR 303 functionality in your project), so you generally don't need to worry about adding them.

Note that some frameworks have different mechanisms for validation where they throw their own typed exceptions that are wrappers around JSR 303 violations. In these cases a framework-specific listener must be provided that knows how to translate the framework exception into `ApiError`s using the Backstopper JSR 303 naming conventions (see below for details on the naming conventions). `ConventionBasedSpringValidationErrorToApiErrorHandlerListener` in the [Spring Web MVC](backstopper-spring-web-mvc/) Backstopper plugin library is one example of this use case. 

[[back to table of contents]][toc]

<a name="jsr303_conventions"></a>
### Backstopper Conventions for JSR 303 Bean Validation Support

In order for the listeners to be able to accurately map any given JSR 303 annotation violation to a specific `ApiError` for displaying to the client the JSR 303 constraint annotations must be defined with a specific message naming convention: the `message` attribute of the JSR 303 constraint annotation *must* match the `ApiError.getName()` of the `ApiError` instance you want it mapped to, and that `ApiError` must be contained in your project's `ProjectApiErrors.getProjectApiErrors()` collection. 

For example, if your project's `ApiError`s are defined as an enum containing `EMAIL_CANNOT_BE_EMPTY` and `INVALID_EMAIL_ADDRESS` values (and the implementation of the `ApiError.getName()` interface is to just return the enum's `name()` value which is the recommended solution as per [quickstart usage for ApiError](README.md#quickstart_usage_api_error_enum)), then you could annotate an email field in a validatable object like this:

``` java
@NotEmpty(message = "EMAIL_CANNOT_BE_EMPTY")
@Email(message = "INVALID_EMAIL_ADDRESS")
private String email;
```
    
It's easy for typos to cause this naming convention to fail, so make sure you're using the unit tests that alert you when you fail to conform to the naming convention (described in the next step).

[[back to table of contents]][toc]

<a name="reusable_tests"></a>
## Reusable Unit Tests for Enforcing Backstopper Rules and Conventions

The [backstopper-reusable-tests](backstopper-reusable-tests/) library contains several reusable unit tests for making sure your project conforms to the Backstopper rules and conventions.

<a name="setup_jsr303_convention_unit_tests"></a>
### Unit Tests to Guarantee JSR 303 Annotation Message Naming Convention Conformance

##### Message Naming Convention Unit Test - What/Why/How?

Since the messages in the JSR 303 annotations are Strings the compiler cannot verify that you've followed the naming convention correctly. It's easy to have a typo in an JSR 303 annotation's `message` attribute, leading the error handler to be unable to convert it to an `ApiError` and ultimately causing the client to receive a generic service error instead of what you really intended.

Any failures to conform to the message naming convention *should* cause your project to fail to build until the problem is fixed. This is trivially easy to do using the base unit test classes provided by the Backstopper reusable tests library. Simply create an extension of `com.nike.backstopper.apierror.contract.jsr303convention.VerifyJsr303ValidationMessagesPointToApiErrorsTest` and implement the `getAnnotationTroller()` and `getProjectApiErrors()` methods. The annotation troller you return from that method is another custom class you'll create that extends `com.nike.backstopper.apierror.contract.jsr303convention.ReflectionBasedJsr303AnnotationTrollerBase` and should be retrieved/exposed as a singleton. **See the javadocs for `ReflectionBasedJsr303AnnotationTrollerBase` for detailed setup, usage information, and example code.** 

Creating these objects and unit test(s) in your project is not time consuming or difficult, but it does need to be done carefully so read those javadocs and consult the [sample applications](README.md#samples) to make things easy on yourself. It is also highly recommended that you verify the unit test is working by creating an incorrect annotation message in your project and making sure that your project fails to build until the message is fixed.

(Note: If you're using the `@StringConvertsToClassType` JSR 303 annotation from the [backstopper-custom-validators](backstopper-custom-validators/) library then there is a `VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest` unit test you can extend to make sure you don't run into problems using that annotation's `allowCaseInsensitiveEnumMatch` option in combination with enums and Jackson deserialization.)

##### Excluding Specific JSR 303 Annotation Declarations

By default these unit tests will troll *every* JSR 303 annotation declaration in your project looking for problems. But you may have legitimate exclusions that should not be trolled, for example unit tests in your project that are doing negative testing. The methods you are required to implement when extending `ReflectionBasedJsr303AnnotationTrollerBase` give you the mechanisms to define the exclusions for your project. See the javadocs for those methods and the class-level javadocs for `ReflectionBasedJsr303AnnotationTrollerBase` for information on how to implement those methods. 

##### Using the JSR 303 Annotation Troller for Your Own Unit Tests

If you have custom logic you want applied to JSR 303 annotations you can create your own custom unit tests. The `ReflectionBasedJsr303AnnotationTrollerBase` extension you create for your project will provide reusable access to inspect any or all JSR 303 annotation declarations in your project, so any time you find yourself saying "I want to look at some or all of the JSR 303 annotations as part of a unit test to make sure that *\[some requirement is satisfied]*" then it's likely the problem can be solved easily by using the annotation troller.

The `VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest` unit test is an example of using the annotation troller to resolve one of these situations. See the implementation of that unit test and `VerifyJsr303ValidationMessagesPointToApiErrorsTest` for examples of a few different ways to use the annotation troller.

[[back to table of contents]][toc]

<a name="setup_project_api_errors_unit_test"></a>
### Unit Test to Verify Your `ProjectApiErrors` Instance Conforms to Requirements

There are some guarantees that `ProjectApiErrors` makes on how it behaves, but since each project's instance returns a different set of `ApiError`s we need a unit test in each project to verify that the guarantees/requirements are met for each `ProjectApiErrors`.

For your project you simply need to extend `ProjectApiErrorsTestBase` and implement the abstract method to return your project's `ProjectApiErrors` (as usual see the [sample applications](README.md#samples) for concrete examples of this). **If you are using JUnit then the base class' `@Test`s should be picked up and you should be done**. If you're using something else (e.g. TestNG) then depending on how the tests are being run you may need to `@Override` all the test methods, have them simply call the super implementation, and annotate them with your unit test framework's annotations or otherwise set them up so that they run. For example with TestNG:

``` java
public class MyProjectApiErrorsTest extends ProjectApiErrorsTestBase {
    private static final ProjectApiErrors myProjectApiErrors = new MyProjectApiErrors();

    @Override
    protected ProjectApiErrors getProjectApiErrors() {
        return myProjectApiErrors;
    }

    // Only necessary because we're not using JUnit
    @org.testng.annotations.Test
    @Override
    public void verifyErrorsAreInRangeShouldThrowExceptionIfListIncludesErrorOutOfRange() {
        super.verifyErrorsAreInRangeShouldThrowExceptionIfListIncludesErrorOutOfRange();
    }

    // Only necessary because we're not using JUnit
    @org.testng.annotations.Test
    @Override
    public void shouldNotContainDuplicateNamedApiErrors() {
        super.shouldNotContainDuplicateNamedApiErrors();
    }

    // ... etc
}
```

Again, this overriding passthrough trick may or may not be necessary if you're using something besides JUnit - it depends largely on how the tests are being run (e.g. Maven's surefire plugin is notorious for not allowing you to mix & match JUnit and TestNG, but if you run the tests manually with TestNG's runner and a little config setup you may get it to work without the workarounds). ***In any case inspect your unit test report to make sure the tests are running!***

[[back to table of contents]][toc]

<a name="new_framework_integrations"></a>
## Creating New Framework Integrations

It's recommended that you look through the [key components](#key_components) section of this readme and understand how everything fits together before attempting to create a new framework integration.

Note that [backstopper-servlet-api](backstopper-servlet-api/) provides a base you should use when creating any framework integration for a Servlet-API-based framework.
 
Also note that the Backstopper repository contains [framework integrations](README.md#framework_modules) for several different frameworks already. It can be very useful to refer to the classes in these libraries while looking through the documentation below to see concrete examples of what is being discussed. 

<a name="new_framework_pseudocode"></a>
### New Framework Integration Pseudocode

``` java
/**
 * Extension of `ApiExceptionHandlerBase` that knows how to adapt your framework's request object to a
 * Backstopper `RequestInfoForLogging`, and knows how to convert the default error contract to whatever 
 * final payload is needed by your framework. This only handles "known" project and framework exceptions.
 */
private MyFrameworkKnownApiExceptionHandler knownExceptionHandler;
/**
 * Extension of `UnhandledExceptionHandlerBase` that is similar to `knownExceptionHandler` above, except 
 * it is guaranteed to handle *anything* that `knownExceptionHandler` didn't by returning a generic 500 
 * service error type response.
 */
private MyFrameworkUnhandledExceptionHandler unhandledExceptionHandler;

/**
 * This is some bottleneck location in the framework where errors (hopefully all of them) are guaranteed 
 * to pass through as they are converted into responses for the caller.
 */
public MyFrameworkResponseObj frameworkErrorHandlingBottleneck(Throwable ex, 
                                                               MyFrameworkRequestObj request) {
    // Try the known exception handler first.
    try {
        ErrorResponseInfo<MyFrameworkPayloadObj> errorResponseInfo =
            knownExceptionHandler.maybeHandleException(ex, request);
        
        if (errorResponseInfo != null)
            return convertErrorResponseInfoToFrameworkResponse(errorResponseInfo);
    }
    catch (UnexpectedMajorExceptionHandlingError unexpectedEx) {
        logger.error("An UnexpectedMajorExceptionHandlingError error occurred. This means "
                     + "MyFrameworkKnownApiExceptionHandler has a bug that needs to be fixed. Falling " 
                     + "back to MyFrameworkUnhandledExceptionHandler.", unexpectedEx);
    }

    // The known exception handler did not successfully handle it.
    //      Fallback to the unhandled exception handler.
    return convertErrorResponseInfoToFrameworkResponse(
        unhandledExceptionHandler.handleException(ex, request)
    );
}

/**
 * A utility method for converting the Backstopper `ErrorResponseInfo` into your framework's response 
 * object.
 */
public MyFrameworkResponseObj convertErrorResponseInfoToFrameworkResponse(
    ErrorResponseInfo<MyFrameworkPayloadObj> errorResponseInfo
) {
    MyFrameworkResponseObj response = new MyFrameworkResponseObj();

    // Populate the response with the information that was returned by the Backstopper exception handler.
    response.setHttpResponseStatusCode(errorResponseInfo.httpStatusCode);
    response.addHeaders(errorResponseInfo.headersToAddToResponse);
    response.setPayloadEntity(errorResponseInfo.frameworkRepresentationObj);

    return response;
}
``` 

[[back to table of contents]][toc]

<a name="new_framework_pseudocode_explanation"></a>
### Pseudocode Explanation and Further Details

The main trick with creating a new framework integration for Backstopper is finding the bottleneck(s) in the framework where errors pass through before they are converted to a response for the caller. The fictional `frameworkErrorHandlingBottleneck(...)` method from the pseudocode represents this bottleneck. If you can't limit it to one spot in your actual framework then you'll need to perform similar actions in all places where errors are converted to caller responses. Hopefully there is only a small handful of these locations. Containers (e.g. servlet containers that run applications as WAR artifacts) are often contributors to this problem since they may try to detect and return some errors to the caller before your application framework has even seen the request, so it's not uncommon to need two Backstopper integrations - one for your framework and another for your container - at least if you want to guarantee Backstopper handling of *all* errors. If you have the option for the container to forward errors to the framework that is often a reasonable solution.

Different frameworks have different error handling solutions so you may need to explore how best to hook into your framework's error handling system. For example Spring Web MVC has the concept of `HandlerExceptionResolver` - a series of handlers that get called in defined order until one of them handles the exception. This matches well with the `ApiExceptionHandlerBase` and `UnhandledExceptionHandlerBase` concepts in Backstopper, so the Spring Web MVC framework integration has `ApiExceptionHandlerBase` and `UnhandledExceptionHandlerBase` implementations that extend Spring's `HandlerExceptionResolver`, and individual projects are configured so that the Backstopper handlers are attempted before any built-in Spring Web MVC handlers in order to guarantee that Backstopper handles all errors. Jersey's error handling system on the other hand uses `ExceptionMapper`s to associate exception types with specific handlers. Since we want to associate *all* errors with Backstopper this means the Jersey/Backstopper framework integration specifies a single `ExceptionMapper` that handles all `Throwable`s, and that single `ExceptionMapper` coordinates the execution of `ApiExceptionHandlerBase` and `UnhandledExceptionHandlerBase`, looking much more like the pseudocode above than the Spring Web MVC integration.    
 
Once you've found the bottleneck(s) and figured out how to hook into the right places then you can follow a procedure similar to what is outlined in the pseudocode, namely attempting to have your framework's `ApiExceptionHandlerBase` extension handle the exception first, and falling back to your framework's `UnhandledExceptionHandlerBase` extension if that fails. The base classes use constructor injection to take in the dependencies they need, and anything that framework-specific extensions need to implement are defined as abstract methods. See the javadocs on those classes for implementation details, however if your framework extension classes compile and you have a way to hook them up to projects so that the project-specific information can be provided, then you're essentially done.

It's recommended that you provide some helpers to make project configuration for Backstopper in your framework easy and convenient. If the default Backstopper functionality is sufficient then projects should be able to integrate Backstopper quickly and easily without defining much beyond registering their `ProjectApiErrors`, but at the same time it should be easy for projects to override default behavior if necessary (e.g. add custom `ApiExceptionHandlerListener`s, use a different `ApiExceptionHandlerUtils` to modify how errors are logged, override methods on your framework's `ApiExceptionHandlerBase` and `UnhandledExceptionHandlerBase` if necessary, etc). See `BackstopperSpringWebMvcConfig` and `Jersey2BackstopperConfigHelper` for good examples.

[[back to table of contents]][toc]

<a name="meta_info"></a>
## Backstopper Project Meta-Information

<a name="motivation"></a>
### Motivation - Why Does Backstopper Exist? 

The [overview](README.md#overview) section covers the main points - error handling and error responses are a critically important part of APIs since they tell callers what went wrong and what to do when the inevitable errors occur, and a good error handling system can accelerate the debugging process for API developers/production support/etc while a bad one can make it a chore. 

Unfortunately error handling is often left as an afterthought; building a good error handling system for an API seems like it should be a straightforward task, but doing it properly and in a way that makes returning and debugging API errors simple and easy turns out to be a difficult and error-prone process in practice. Just as bad - you often find yourself redoing that process over and over whenever changing frameworks (or sometimes even simply changing projects). Backstopper provides a set of libraries to make this process easy and replicable regardless of what framework your API is running in.

Furthermore it integrates seamlessly with the JSR 303 (a.k.a. Bean Validation) specification - the standard Java method for validating objects. JSR 303 Bean Validation is generally easy to use, easy to understand, and is widely integrated into a variety of frameworks (or you can use it independently in a standalone way). You get to see the validation constraints in the model objects themselves without it getting in the way, and it's easy to create new constraint annotations.

The drawback with JSR 303 is that it was not built for API situations where you need to conform to a strict error contract. There is no built-in way to connect a given JSR 303 constraint violation and a specific project-defined set of error data (i.e. error code, human-readable message, metadata, etc), and the validation constraint messages are raw strings and therefore easy to typo, require obnoxious copy-pasting, and are difficult to maintain over time as they spread throughout your codebase.

Backstopper solves these issues in a way that lets you reap the benefits and avoid the drawbacks of JSR 303 Bean Validation when working in an API environment. You just have to follow some [simple conventions](#jsr303_conventions) and integrate a few [reusable unit tests](#reusable_tests) into your Backstopper-enabled project.

[[back to table of contents]][toc]

<a name="key_goals"></a>
### Backstopper Key Goals

* All API errors for a project should be able to live in one location and easily referenced and reused rather than being spread throughout the codebase. 
* Core errors should be shareable across multiple projects in the same organization. 
* It should be easy to add new error definitions. 
    * The `ApiError` interface and [enum definition convention](README.md#quickstart_usage_api_error_enum) make all these API-error-related goals achievable. 
* It should be easy to add new error handlers for new typed exceptions.
    * The [`ApiExceptionHandlerListener`](README.md#quickstart_usage_add_custom_listener) mechanism is designed to solve this.
* Straightforward implementation - no annotation processing, classpath scanning, or other indirection magic in the core Backstopper functionality.
    * Core components are instrumented with dependency injection annotations, but they are instrumented in such a way that they are not necessary and classes can be used manually without difficulty (i.e. constructor injection rather than field injection). 
    * Framework-specific implementations can add the magic if it's idiomatic to that framework.
    * Classes that must be extended by projects in order for Backstopper to function (e.g. `ProjectApiErrors`) guide the concrete implementation with abstract methods and full javadocs explaining what the methods are and how they should be implemented. If the necessary non-nullable dependencies are supplied and the code compiles successfully then you're likely done.
    * Putting in breakpoints and debugging what Backstopper is doing is therefore a reasonably simple activity. If you want to adjust Backstopper behavior you can usually determine where the hooks are and how to change things just by using breakpoints and exploring the code.
    * No need to refer to documentation for everything - the code and javadocs are usually sufficient.
* It should be easy to add integration for new frameworks.
    * The core Backstopper functionality is free of framework dependencies and designed with hooks so that framework integrations can be as lightweight as possible.
    
[[back to table of contents]][toc]

<a name="key_philosophies"></a>
### Backstopper Key Philosophies

Backstopper was based in large part on common elements found in the error contracts of API industry leaders circa 2014 (e.g. Facebook, Twitter, and others), and was therefore built with a few philosophies in mind. These are general guidelines - not everyone will agree with these ideas and there will always be legitimate exceptions even if you do agree. Therefore Backstopper should have hooks to allow you to override any of this behavior; if you notice an area where it's not possible to override the default behavior please file an issue and we'll see if there's a way to address it.

* There should be a common error contract for *all* errors. APIs are intended to be used programmatically by callers, and changing contracts for different error types, HTTP status codes, etc, makes that programmatic integration more difficult and error prone. Metadata and optional information can be added or removed at will, but the core error contract should be static.
    * Since some error responses might need to contain multiple individual errors (e.g. validation of a request payload that contains multiple problems), the error contract should include an array of individual errors. 
    * Since the error contract should be the same for *all* errors to facilitate easy programmatic handling by callers, this means an error response with a single individual error should still result in an error contract that contains an array - it would simply be an array with one error inside.
* There should be a unique error ID for *all* responses which matches a single application log entry tagged with that error ID and contains all debugging info for the request. 
    * This makes it trivial to find the relevant debugging information when a caller notifies you of an error they received that they don't think they should have received.
    * It should be as *difficult* as possible for callers to fail to give you the information you need to debug an error. Therefore the error ID should show up in both the response body payload and headers. Callers interacting with customer support or API developers often provide limited information (e.g. a screenshot of the error they saw) and/or have limited technical expertise, so by the time they contact you it's usually too late (or unrealistic) to have them go back and look in the headers for an error ID. 
* The application/business error codes returned by the API (not to be confused with HTTP status code) should be integers rather than string-based.
    * Error codes are what API callers program against. They are your contract with callers and therefore they should *never* change. Integer-based codes allow you to completely decouple the interpretation of the error from the error code.
    * Integer-based codes make localization simpler - you can have an error docs page localized to multiple languages/regions without any confusion over what to do with the error codes.
    * There's already a human-readable message field designed to let you give callers a hint as to what went wrong without consulting a docs page. String-based error codes would overlap with the human-readable-message's purpose but are less capable of providing useful information since error codes should not be verbose (overly long error codes make API integration more frustrating).  
    * `ApiError.getErrorCode()` returns a string in order to allow for the necessary flexibility for those who want to use string-based codes despite these concerns, so bypassing this philosophy is trivially easy.
* The human-readable-messages in error contract responses are provided as hints. They are not contractual and are therefore subject to change. They do not need to be localized. The error code is what API integrators should code against, not the message.

[[back to table of contents]][toc]

<a name="license"></a>
## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[toc]:#table_of_contents