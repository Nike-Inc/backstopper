# Backstopper - spring-web-mvc

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This readme focuses specifically on the Backstopper Spring Web MVC integration. If you are looking for a different framework integration check out the [relevant section](../README.md#framework_modules) of the base readme to see if one already exists. The [base project README.md](../README.md) and [User Guide](../USER_GUIDE.md) contain the main bulk of information regarding Backstopper. 

**NOTE: There is a [Spring Web MVC sample application](../samples/sample-spring-web-mvc/) that provides a simple concrete example of the information covered in this readme.**

## Backstopper Spring Web MVC Setup, Configuration, and Usage

### Spring / Spring Boot Versions

This `backstopper-spring-web-mvc` library can be used in any Spring Web MVC `4.3.x` environment or later. This includes
Spring 5, Spring Boot 1, and Spring Boot 2.

NOTE: This library does not yet have support for the new Spring 5 WebFlux features, but it will cover the traditional
Servlet-based Spring MVC requests the same in Spring 4 / Spring 5 / Spring Boot 1 / Spring Boot 2.
 

### Setup

* Pull in the `com.nike.backstopper:backstopper-spring-web-mvc` dependency into your project.
* Register Backstopper components with Spring Web MVC, either via `@Import({BackstopperSpringWebMvcConfig.class})`, or 
`@ComponentScan(basePackages = "com.nike.backstopper")`. See the javadocs on `BackstopperSpringWebMvcConfig` for some 
related details.
    * This causes `SpringApiExceptionHandler` and `SpringUnhandledExceptionHandler` to be registered with the Spring 
    Web MVC error handling chain in a way that overrides the default Spring Web MVC error handlers so that the 
    Backstopper handlers will take care of *all* errors. It sets up `SpringApiExceptionHandler` with a default list 
    of `ApiExceptionHandlerListener` listeners that should be sufficient for most projects. You can override that list 
    of listeners (and/or many other Backstopper components) if needed in your project's Spring config.
    * It also registers `SpringContainerErrorController` to handle errors that happen outside Spring 
    Web MVC (i.e. in the Servlet container), and make sure they're routed through Backstopper as well. Note that you'll
    need to configure your Servlet container to route container errors to the path this controller listens on 
    (default `/error`) for it to work.
* Expose your project's `ProjectApiErrors` and a JSR 303 `javax.validation.Validator` implementation in your Spring dependency injection config.
    * `ProjectApiErrors` creation is discussed in the base Backstopper readme [here](../README.md#quickstart_usage_project_api_errors).
    * JSR 303 setup and generation of a `Validator` is discussed in the Backstopper User Guide [here](../USER_GUIDE.md#jsr_303_basic_setup). If you're not going to be doing any JSR 303 validation outside what is built-in supported by Spring Web MVC, *and* you don't want to bother jumping through the hoops to get a handle on Spring's JSR 303 validator impl provided by `WebMvcConfigurer.getValidator()`, *and* you don't want to bother creating a real `Validator` yourself then you can simply register `NoOpJsr303Validator#SINGLETON_IMPL` as the `Validator` that gets exposed by your Spring config. `ClientDataValidationService` and `FailFastServersideValidationService` would fail to do anything, but if you don't use those then it wouldn't matter. 
* Setup the reusable unit tests for your project as described in the Backstopper User Guide [here](../USER_GUIDE.md#reusable_tests) and shown in the sample application. 

### Usage

The base Backstopper readme covers the [usage basics](../README.md#quickstart_usage). There should be no difference when running in a Spring Web MVC environment, but since Spring Web MVC integrates a JSR 303 validation system into its core functionality we can get one extra nice tidbit: to have Spring Web MVC run validation on objects deserialized from incoming user data you can simply add `@Valid` annotations on the objects you're deserializing for your controller endpoints (`@RequestBody` object, `@ModelAttribute` objects, etc). For example:

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

This method signature with the two `@Valid` annotations would cause both the `@ModelAttribute` `headersAndQueryParams` and `@RequestBody` `inputObject` arguments to be run through JSR 303 validation. Any constraint violations caught at this time will cause a Spring-specific exception to be thrown with the constraint violation details buried inside. This `backstopper-spring-web-mvc` plugin library's error handler listeners know how to convert this to the appropriate set of `ApiError` cases (from your `ProjectApiErrors`) automatically using the [Backstopper JSR 303 naming convention](../USER_GUIDE.md#jsr303_conventions), which are then returned to the client using the standard error contract. 

This feature allows you to enjoy the Backstopper JSR 303 validation integration support automatically at the point where caller-provided data is deserialized and passed to your controller endpoint without having to inject and manually call a `ClientDataValidationService`.

## NOTE - Spring WebMVC and Servlet API dependencies required at runtime

This `backstopper-spring-web-mvc` module does not export any transitive Spring or Servlet API dependencies to prevent runtime 
version conflicts with whatever Spring and Servlet environment you deploy to. 

This should not affect most users since this library is likely to be used in a Spring/Servlet environment where the
required dependencies are already on the classpath at runtime, however if you receive class-not-found errors related to 
Spring or Servlet API classes then you'll need to pull the necessary dependency into your project. 

The dependencies you may need to pull in:

* Spring Web MVC: [org.springframework:spring-webmvc:\[spring-version\]](https://search.maven.org/search?q=g:org.springframework%20AND%20a:spring-webmvc)
* Servlet API (choose one of the following, depending on your environment needs):
    + Servlet 3+ API: [javax.servlet:javax.servlet-api:\[servlet-api-version\]](https://search.maven.org/search?q=g:javax.servlet%20AND%20a:javax.servlet-api) 
    + Servlet 2 API: [javax.servlet:servlet-api:\[servlet-2-api-version\]](https://search.maven.org/search?q=g:javax.servlet%20AND%20a:servlet-api)
    
## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
