<img src="backstopper_logo.png" />

# Backstopper - Keep Your API Errors in the Field of Play

[ ![Download](https://api.bintray.com/packages/nike/maven/backstopper/images/download.svg) ](https://bintray.com/nike/maven/backstopper/_latestVersion)
[![][travis img]][travis]
[![Code Coverage](https://img.shields.io/codecov/c/github/Nike-Inc/backstopper/master.svg)](https://codecov.io/github/Nike-Inc/backstopper?branch=master)
[![][license img]][license]

**Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.** 

## TL;DR

Backstopper guarantees that a consistent error contract which you can define will be sent to callers *no matter the source of the error* when properly integrated into your API framework. No chance of undesired information leaking to the caller like stack traces, and callers can always rely on an error contract they can programmatically process.  

If you prefer hands-on exploration rather than readmes and user guides, the [sample applications](#samples) provide concrete examples of using Backstopper that are simple, compact, and straightforward. 

The rest of this readme is intended to get you oriented and running quickly, and the [User Guide](USER_GUIDE.md) contains more in-depth details.

<a name="overview"></a>
## Additional Features Overview

* Backstopper trivializes the task of creating and throwing errors that will be sent to the caller with the desired error contract, and allows you to gather all of your project-specific API error definitions in one place (e.g. as an enum) for easy access, modification, and addition.
* All errors are logged with full request context and error debugging info along with a UUID. The caller is sent the same UUID in the error response payload so you can instantly connect an individual error response with the application log message containing full debugging details. This makes the usual tedious process of debugging why an API error was shown to the caller simple and straightforward.
* Includes optional support for defining a common set of "core API errors" that can be shared via jar library so that all projects in your organization use the same set of API errors for common error cases. Individual projects are then free to focus on their project-specific API errors.
* Contains optional integration support for Java's JSR 303 Bean Validation system (`@NotNull`, `@Max`, etc) so that validation errors occurring due to invalid payloads sent by callers (among other use cases) will be automatically converted to your predefined set of project API errors.
* Integration libraries are included for several API frameworks already, and adding support for other frameworks is a relatively straightforward process. Backstopper is geared primarily towards HTTP-based APIs, but could be used in other circumstances if desired and without polluting your project with unwanted HTTP API dependencies.

### Barebones Example (assumes [framework integration](#quickstart_integration) is already done)

##### 1. Define your API's errors

``` java
public enum MyProjectError implements ApiError {
    // Constructor args for this example are: errorCode, message, httpStatusCode
    EMAIL_CANNOT_BE_EMPTY(42, "Email must be specified", 400),
    INVALID_EMAIL_ADDRESS(43, "Invalid email address format", 400),
    // -- SNIP -- 
}
```
 
##### 2a. Use JSR 303 Bean Validation to define object validation (optional)

``` java
public class Payload {
    @NotEmpty(message = "EMAIL_CANNOT_BE_EMPTY")
    @Email(message = "INVALID_EMAIL_ADDRESS")
    private String email;
    // -- SNIP -- 
}
```

##### --AND/OR-- 
##### 2b. Throw errors manually anytime (doesn't have to be just for validation)
  
``` java
throw ApiException.newBuilder()
                  .withApiErrors(MyProjectError.INVALID_EMAIL_ADDRESS)
                  .withExceptionMessage("Error validating email field.")
                  .withExtraResponseHeaders(Pair.of("received-email", singletonList(payload.email)))
                  .withExtraDetailsForLogging(Pair.of("received_email", payload.email))
                  .build();
```  

##### --AND/OR-- 
##### 2c. Define your API's errors using @ApiErrorValue annotation

> currently supported Spring 4/Spring Boot 1.x/Spring 2.x

```java
public class Payload {

     @ApiErrorValue
     @NotBlank
     public String foo;

     @ApiErrorValue(errorCode = "BLANK_BAR", httpStatusCode = 400)
     @NotBlank(message = "bar should not be blank")
     public String bar;
     // -- SNIP -- 
}
```

##### 3. Send payload that will cause errors to be thrown

`POST /profile`

``` json
{
	"email": "not@a@valid@email",
	"etc": "-- SNIP --"
}
```

##### 4. Receive expected error response

`HTTP Status Code: 400 Bad Request`

``` json
{
  "error_id": "408d516f-68d1-41f3-adb1-b3dc0affaaf2",
  "errors": [
    {
      "code": 43,
      "message": "Invalid email address format",
      "metadata": {
        "field": "email"
      }
    }
  ]
}
```

##### 5. Use the error_id to locate debugging details in the logs (a 5xx error would also include the stack trace)

```
2016-09-21_12:14:00.620 |-WARN  c.n.b.h.j.Jersey1ApiExceptionHandler - ApiExceptionHandlerBase handled 
↪exception occurred: error_uid=408d516f-68d1-41f3-adb1-b3dc0affaaf2, 
↪exception_class=com.nike.backstopper.exception.ClientDataValidationError, returned_http_status_code=400, 
↪contributing_errors="INVALID_EMAIL_ADDRESS", request_uri="/profile", request_method="POST", 
↪query_string="null", request_headers="Host=localhost:8080, --snip-- Content-Type=application/json",
↪client_data_validation_failed_objects="com.myorg.Payload", 
↪constraint_violation_details="Payload.email|org.hibernate.validator.constraints.Email|INVALID_EMAIL_ADDRESS"
```

<a name="general_modules"></a>
### General-Purpose Modules

* [backstopper-core](backstopper-core/) - The core library providing the majority of the Backstopper functionality.
* [backstopper-reusable-tests](backstopper-reusable-tests/) - There are some rules around defining your project's set of API errors and conventions around integration with Java's JSR 303 Bean Validation system that must be followed for Backstopper to function at its best. This library contains reusable tests that are intended to be included in a Backstopper-enabled project's unit test suite to guarantee that these rules are adhered to and fail the build with descriptive unit test errors when those rules are violated. These are technically optional but ***highly*** recommended - integration is generally quick and easy. 
* [backstopper-custom-validators](backstopper-custom-validators/) - This library contains JSR 303 Bean Validation annotations that have proven to be useful and reusable. These are entirely optional. They are also largely dependency free so this library is usable in non-Backstopper projects that utilize JSR 303 validations. 
* [backstopper-jackson](backstopper-jackson/) - Contains a few utilities that help integrate Backstopper and Jackson for serializing error contracts to JSON. Optional.
* [backstopper-servlet-api](backstopper-servlet-api/) - Intermediate library intended to ease integration with Servlet-based frameworks. If you're building a Backstopper integration library for a Servlet-based framework that we don't already have support for then you'll want to use this.
* [nike-internal-util](nike-internal-util/) - A small utilities library that provides some reusable helper methods and classes. It is "internal" in the sense that it is not intended to be directly pulled in and used by non-Nike projects. That said you can use it if you want to, just be aware that some liberties might be taken regarding version numbers, backwards compatibility, etc over time when compared with libraries specifically intended for public consumption. 
* [backstopper-annotation-post-processor](backstopper-annotation-post-processor/) - The annotation Processor that writes metadata file for `@ApiErrorValue` annotation including enclosed JSR 303 constraint annotations or a valid constraint annotation such as Hibernate/custom. 

<a name="framework_modules"></a>
### Framework-Specific Modules 
  
* [backstopper-jaxrs](backstopper-jaxrs) - Integration library for JAX-RS. If you want to integrate Backstopper into a JAX-RS project other than Jersey then start here (see below for the Jersey-specific modules).
* [backstopper-jersey1](backstopper-jersey1/) - Integration library for the Jersey 1 framework. If you want to integrate Backstopper into a project running in Jersey 1 then start here. There is a [Jersey 1 sample project](samples/sample-jersey1/) complete with integration tests you can use as an example.
* [backstopper-jersey2](backstopper-jersey2/) - Integration library for the Jersey 2 framework. If you want to integrate Backstopper into a project running in Jersey 2 then start here. There is a [Jersey 2 sample project](samples/sample-jersey2/) complete with integration tests you can use as an example.
* [backstopper-spring-web-mvc](backstopper-spring-web-mvc/) - Base Integration library for the Spring Web MVC (Servlet) 
framework. If you want to integrate Backstopper into a project running in Spring Web MVC then start here. Works for 
both Spring 4 and Spring 5, and used as a foundation for Backstopper support in Spring Boot 1 and 2 (when using Web 
MVC with Spring Boot - see below for links to Spring Boot specific integration modules). There is a 
[Spring Web MVC sample project](samples/sample-spring-web-mvc/) complete with integration tests you can use as an 
example.
* [backstopper-spring-web-flux](backstopper-spring-web-flux/) - Integration library for the Spring WebFlux (Netty)
framework. If you want to integrate Backstopper into a project running in Spring WebFlux then start here. There is a 
[Spring Boot 2 WebFlux sample project](samples/sample-spring-boot2-webflux/) complete with integration tests you can 
use as an example.
* [backstopper-spring-boot1](backstopper-spring-boot1/) - Integration library for the Spring Boot 1 framework. 
If you want to integrate Backstopper into a project running in Spring Boot 1 then start here. There is a 
[Spring Boot 1 sample project](samples/sample-spring-boot1/) complete with integration tests you can use as an example.
* [backstopper-spring-boot2-webmvc](backstopper-spring-boot2-webmvc/) - Integration library for the Spring Boot 2 
framework *if and only if you're using the Spring Web MVC Servlet runtime* (if you're running a Spring 
Boot 2 + WebFlux Netty application, then you do not want this library and should use 
[backstopper-spring-web-flux](backstopper-spring-web-flux) instead). If you want to integrate Backstopper into a 
project running in Spring Boot 2 + Web MVC then start here. There is a 
[Spring Boot 2 Web MVC sample project](samples/sample-spring-boot2-webmvc/) complete with integration tests you 
can use as an example.
   
<a name="samples"></a>
### Framework Integration Sample Applications
   
Note that the sample apps are an excellent source for framework integration examples, but they are also very helpful for giving you an overview and exploring what you can do with Backstopper regardless of framework: how to create and throw errors, how they show up for the caller in the response, what Backstopper outputs in the application logs when errors occur, and how to find the relevant log message given a specific error response. The `VerifyExpectedErrorsAreReturnedComponentTest` component tests in the sample apps exercise a large portion of Backstopper's functionality - you can learn a lot by running that component test, seeing what the sample app returns in the error responses, and exploring the associated endpoints and framework configuration in the sample apps to see how it all fits together. 
   
* [samples/sample-jersey1](samples/sample-jersey1/)
* [samples/sample-jersey2](samples/sample-jersey2/)
* [samples/sample-spring-web-mvc](samples/sample-spring-web-mvc/)
* [samples/sample-spring-boot1](samples/sample-spring-boot1/)
* [samples/sample-spring-boot2-webmvc](samples/sample-spring-boot2-webmvc/)
* [samples/sample-spring-boot2-webflux](samples/sample-spring-boot2-webflux/)
* [sample/spring-boot-with-apierrorvalue-annotation](samples/sample-spring-boot-with-apierrorvalue-annotation/)

<a name="quickstart"></a>
## Quickstart

Getting started is a matter of integrating Backstopper into your project and learning how to use its features. The following sections will help guide you in getting started, and the [sample applications](#samples) should be consulted for concrete examples. 

<a name="quickstart_integration"></a>
### Quickstart - Integration

The first thing to do is see if there is a [framework integration plugin library](#framework_modules) already created for the framework you're using. If so then refer to that framework-specific library's readme as well as its [sample project](#samples) to learn how to integrate Backstopper into your project. 

If a framework-specific plugin library does not already exist for your project then you'll need to create your own integration. If the result is potentially reusable for others using the same framework then please consider [contributing](CONTRIBUTING.md) it back to the Backstopper project so others can benefit! The [new framework integrations](USER_GUIDE.md#new_framework_integrations) section has full details, and in particular the [pseudo-code section](USER_GUIDE.md#new_framework_pseudocode) should give you a quick idea of what is required.

**IMPORTANT NOTE: Your project integration should not be considered complete until you have added and enabled the reusable unit tests that enforce Backstopper rules and conventions.** See the [Reusable Unit Tests for Enforcing Backstopper Rules and Conventions](USER_GUIDE.md#reusable_tests) section of the User Guide for information on setting these up. 

<a name="quickstart_usage"></a>
### Quickstart - Usage

Once your project is properly integrated with Backstopper a large portion of errors should be handled for you (framework errors, errors resulting from validation of incoming payloads, etc), however for most API projects you'll need to throw errors or interact with Backstopper in other situations. Again, the [sample projects](#samples) are excellent for showing how this is done in practice, but here are a few common use cases and how to solve them:

<a name="quickstart_usage_api_error_enum"></a>
##### Defining a set of `ApiError`s

Defining groups of `ApiError`s as enums has proven to be a useful pattern. Normally you'd want to break `ApiError`s out into a group of "core errors" that you could share with projects across your organization (see `SampleCoreApiError` for an example) and different sets of `ApiError`s for each individual project (see any of the [sample application](#samples) `SampleProjectApiError` classes for an example). For the purpose of this example here is a mishmash showing how to define an enum of errors with different properties (basic code/message/http-status errors, "mirror" errors, and errors with metadata): 

``` java
public enum MyProjectApiError implements ApiError {
    GENERIC_SERVICE_ERROR(10, "An error occurred while fulfilling the request", 500),
    // Mirrors GENERIC_SERVICE_ERROR for the caller, but will show up in the logs with a different name
    SOME_OTHER_SERVICE_ERROR(GENERIC_SERVICE_ERROR),
    GENERIC_BAD_REQUEST(20, "Invalid request", 400),
    // Includes metadata in the response payload sent to the caller
    SOME_OTHER_BAD_REQUEST(30, "You failed to pass the required foo", 400,
                           MapBuilder.builder("missing_field", (Object)"foo").build()),
    // Also a mirror for another ApiError, but includes extra metadata that will show up in the response
    YET_ANOTHER_BAD_REQUEST(GENERIC_BAD_REQUEST, MapBuilder.builder("field", (Object)"bar").build());

    private final ApiError delegate;

    MyProjectApiError(ApiError delegate) { this.delegate = delegate; }

    MyProjectApiError(ApiError delegate, Map<String, Object> additionalMetadata) {
        this(new ApiErrorWithMetadata(delegate, additionalMetadata));
    }

    MyProjectApiError(int errorCode, String message, int httpStatusCode) {
        this(errorCode, message, httpStatusCode, null);
    }

    MyProjectApiError(int errorCode, String message, int httpStatusCode, Map<String, Object> metadata) {
        this(new ApiErrorBase(
            "delegated-to-enum-name-" + UUID.randomUUID().toString(), errorCode, message, httpStatusCode, 
            metadata
        ));
    }

    @Override
    public String getName() { return this.name(); }

    @Override
    public String getErrorCode() { return delegate.getErrorCode(); }

    @Override
    public String getMessage() { return delegate.getMessage(); }

    @Override
    public int getHttpStatusCode() { return delegate.getHttpStatusCode(); }

    @Override
    public Map<String, Object> getMetadata() { return delegate.getMetadata(); }

}  
```

<a name="quickstart_usage_project_api_errors"></a>
##### Defining a `ProjectApiErrors` for your project

Backstopper needs a `ProjectApiErrors` defined for each project in order to work. If possible you should create an abstract base class that is setup with the core errors for your organization - see `SampleProjectApiErrorsBase` for an example. Then each individual project would simply need to extend the base class and fill in the project-specific set of `ApiError`s and the error range it's using. See any of the [sample application](#samples) `SampleProjectApiErrorsImpl` classes for an example. The javadocs for `ProjectApiErrors` contains in-depth information as well.

<a name="quickstart_usage_throw_api_exception"></a>
##### Manually throwing an arbitrary error with full control over the resulting error contract, response headers, and logging info

``` java
// The only requirement is that you have at least one ApiError. Everything else is optional.
throw ApiException.newBuilder()
                  .withApiErrors(MyProjectApiError.FOO_ERROR, MyProjectApiError.BAD_THING_HAPPENED)
                  .withExceptionMessage("Useful message for exception in the logs")
                  .withExceptionCause(originalCause)
                  .withExtraResponseHeaders(
                      Pair.of("useful-single-header-for-caller", singletonList("thing1")),
                      Pair.of("also-useful-multivalue-header", Arrays.asList("thing2", "thing3"))
                  )
                  .withExtraDetailsForLogging(
                      Pair.of("important_info", "foo"),
                      Pair.of("also_important", "bar")
                  )
                  .build();
```

<a name="quickstart_usage_add_custom_listener"></a>
##### Creating a custom `ApiExceptionHandlerListener` to handle a typed exception

This is only really necessary if you can't (or don't want to) throw an `ApiException` and need Backstopper to properly handle a typed exception it wouldn't otherwise know about. Many projects never need to do this. 
 
``` java
public static class MyFrameworkExceptionHandlerListener implements ApiExceptionHandlerListener {
    @Override
    public ApiExceptionHandlerListenerResult shouldHandleException(Throwable ex) {
        if (ex instanceof MyFrameworkException) {
            // The exception is a MyFrameworkException, so this listener should handle it.
            MyFrameworkException myEx = (MyFrameworkException)ex;
            SortedApiErrorSet apiErrors =
                SortedApiErrorSet.singletonSortedSetOf(MyProjectApiError.SOME_OTHER_BAD_REQUEST);
            List<Pair<String, String>> extraDetailsForLogging = Arrays.asList(
                Pair.of("important_foo_info", myEx.foo()),
                Pair.of("important_bar_info", myEx.bar())
            );
            List<Pair<String, List<String>>> extraResponseHeaders = Arrays.asList(
                Pair.of("foo-info", myEx.foo()),
                Pair.of("bar-info", myEx.bar())
            );
            return ApiExceptionHandlerListenerResult.handleResponse(
                apiErrors, extraDetailsForLogging, extraResponseHeaders
            );
        }

        // The exception wasn't a MyFrameworkException, so this listener should ignore it.
        return ApiExceptionHandlerListenerResult.ignoreResponse();
    }
}
```
 
After defining a new `ApiExceptionHandlerListener` you'll need to register it with the `ApiExceptionHandlerBase` running your Backstopper system. This is a procedure that is often different for each framework integration. 

## User Guide

For further details please consult the [User Guide](USER_GUIDE.md).

<a name="license"></a>
## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)

[travis]:https://travis-ci.org/Nike-Inc/backstopper
[travis img]:https://api.travis-ci.org/Nike-Inc/backstopper.svg?branch=master

[license]:LICENSE.txt
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg
