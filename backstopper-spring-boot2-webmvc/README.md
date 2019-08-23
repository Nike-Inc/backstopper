# Backstopper - spring-boot2

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This readme focuses specifically on the Backstopper Spring Boot 2 integration utilizing the Spring Web MVC framework. 
If you are looking for a different framework integration check out the 
[relevant section](../README.md#framework_modules) of the base readme to see if one already exists. The 
[base project README.md](../README.md) and [User Guide](../USER_GUIDE.md) contain the main bulk of information 
regarding Backstopper. 

**NOTE: There is a [Spring Boot 2 Web MVC sample application](../samples/sample-spring-boot2-webmvc/) that provides a 
simple concrete example of the information covered in this readme.**

_ALSO NOTE: **This library does not cover Spring WebFlux (Netty) applications**. If you're looking for a 
Spring Boot 2 app using the Spring WebFlux framework, then you should use the 
[backstopper-spring-web-flux](../backstopper-spring-web-flux) integration instead._ This library is just for 
Spring Boot 2 apps using Spring Web MVC (Servlet).

## Backstopper Spring Boot 2 Web MVC Setup, Configuration, and Usage

### Setup

* Pull in the `com.nike.backstopper:backstopper-spring-boot2-webmvc` dependency into your project.
* Register Backstopper components with Spring Boot, either via `@Import({BackstopperSpringboot2WebMvcConfig.class})`, or 
`@ComponentScan(basePackages = "com.nike.backstopper")`. See the javadocs on `BackstopperSpringboot2WebMvcConfig` for 
some related details.
    * This causes `SpringApiExceptionHandler` and `SpringUnhandledExceptionHandler` to be registered with the 
    Spring Boot `HandlerExceptionResolver` error handling chain in a way that overrides the default error handlers so 
    that the Backstopper handlers will take care of *all* errors. It sets up `SpringApiExceptionHandler` with a default 
    list of `ApiExceptionHandlerListener` listeners that should be sufficient for most projects. You can override that 
    list of listeners (and/or many other Backstopper components) if needed in your project's Spring config.
    * It also registers `BackstopperSpringboot2ContainerErrorController` to handle errors that happen outside Spring
    Boot (i.e. in the Servlet container), and make sure they're routed through Backstopper as well.
* Expose your project's `ProjectApiErrors` and a JSR 303 `javax.validation.Validator` implementation in your Spring 
dependency injection config.
    * `ProjectApiErrors` creation is discussed in the base Backstopper readme 
    [here](../README.md#quickstart_usage_project_api_errors).
    * JSR 303 setup and generation of a `Validator` is discussed in the Backstopper User Guide 
    [here](../USER_GUIDE.md#jsr_303_basic_setup). If you're not going to be doing any JSR 303 validation outside what 
    is built-in supported by Spring Web MVC, *and* you don't want to bother jumping through the hoops to get a handle 
    on Spring's JSR 303 validator impl provided by `WebMvcConfigurer.getValidator()`, *and* you don't want to bother 
    creating a real `Validator` yourself then you can simply register `NoOpJsr303Validator.SINGLETON_IMPL` as the 
    `Validator` that gets exposed by your Spring config. `ClientDataValidationService` and 
    `FailFastServersideValidationService` would fail to do anything, but if you don't use those then it wouldn't matter. 
* Setup the reusable unit tests for your project as described in the Backstopper User Guide 
[here](../USER_GUIDE.md#reusable_tests) and shown in the sample application. 

### Usage

The base Backstopper readme covers the [usage basics](../README.md#quickstart_usage). There should be no difference 
when running in a Spring Boot environment, but since Spring Boot integrates a JSR 303 validation system into its core 
functionality we can get one extra nice tidbit: to have Spring Boot run validation on objects deserialized from 
incoming user data you can simply add `@Valid` annotations on the objects you're deserializing for your controller 
endpoints (`@RequestBody` object, `@ModelAttribute` objects, etc). For example:

``` java
@RequestMapping(method=RequestMethod.POST)
@ResponseBody
@ResponseStatus(HttpStatus.CREATED)
public SomeOutputObject postSomeInput(
        @ModelAttribute @Valid HeadersAndQueryParams headersAndQueryParams,
        @RequestBody @Valid SomeInputObject inputObject) {
    
    // ... Normal controller processing
    
}
```    

This method signature with the two `@Valid` annotations would cause both the `@ModelAttribute` `headersAndQueryParams` 
and `@RequestBody` `inputObject` arguments to be run through JSR 303 validation. Any constraint violations caught at 
this time will cause a Spring-specific exception to be thrown with the constraint violation details buried inside. 
This `backstopper-spring-boot2-webmvc` plugin library's error handler listeners know how to convert this to the 
appropriate set of `ApiError` cases (from your `ProjectApiErrors`) automatically using the 
[Backstopper JSR 303 naming convention](../USER_GUIDE.md#jsr303_conventions), which are then returned to the client 
using the standard error contract. 

This feature allows you to enjoy the Backstopper JSR 303 validation integration support automatically at the point 
where caller-provided data is deserialized and passed to your controller endpoint without having to inject and manually 
call a `ClientDataValidationService`.

## NOTE - Spring Boot Autoconfigure, Spring WebMVC, and Servlet API dependencies required at runtime
         
This `backstopper-spring-boot2-webmvc` module does not export any transitive Spring Boot, Spring, or Servlet API dependencies 
to prevent runtime version conflicts with whatever Spring Boot and Servlet environment you deploy to. 

This should not affect most users since this library is likely to be used in a Spring Boot/Servlet environment where the
required dependencies are already on the classpath at runtime, however if you receive class-not-found errors related to 
Spring Boot, Spring, or Servlet API classes then you'll need to pull the necessary dependency into your project. 

The dependencies you may need to pull in:

* Spring Boot Autoconfigure: [org.springframework.boot:spring-boot-autoconfigure:\[spring-boot2-version\]](https://search.maven.org/search?q=g:org.springframework.boot%20AND%20a:spring-boot-autoconfigure) 
* Spring Web MVC: [org.springframework:spring-webmvc:\[spring-version\]](https://search.maven.org/search?q=g:org.springframework%20AND%20a:spring-webmvc)
* Servlet 4.0.0+ API: [javax.servlet:javax.servlet-api:\[servlet-api-version\]](https://search.maven.org/search?q=g:javax.servlet%20AND%20a:javax.servlet-api) 
    
## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code 
and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
