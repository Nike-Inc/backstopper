# Backstopper - spring-web-flux

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater
(although for this Backstopper+Spring WebFlux module, Java 8 is required since Spring WebFlux requires Java 8).

This readme focuses specifically on the Backstopper Spring WebFlux integration. If you are looking for a different 
framework integration check out the [relevant section](../README.md#framework_modules) of the base readme to see if 
one already exists. The [base project README.md](../README.md) and [User Guide](../USER_GUIDE.md) contain the main 
bulk of information regarding Backstopper. 

**NOTE: There is a [Spring Boot 2 WebFlux sample application](../samples/sample-spring-boot2-webflux/) that provides 
a simple concrete example of the information covered in this readme.**

_ALSO NOTE: **This library does not cover Spring Web MVC (Servlet) applications.** Spring Web MVC and Spring WebFlux 
(Netty) are [mutually exclusive](https://stackoverflow.com/questions/53883037/can-i-use-springmvc-and-webflux-together). 
If you're looking for a Spring Web MVC based integration, then you should see the following Backstopper libraries
depending on your application:_

* [backstopper-spring-boot1](../backstopper-spring-boot1) - For Spring Boot 1 + Spring MVC applications.
* [backstopper-spring-boot2-webmvc](../backstopper-spring-boot2-webmvc) - For Spring Boot 2 + Spring MVC applications. 
* [backstopper-spring-web-mvc](../backstopper-spring-web-mvc) - For Spring Web MVC applications that are not
Spring Boot.

## Backstopper Spring WebFlux Setup, Configuration, and Usage

### Setup

* Pull in the `com.nike.backstopper:backstopper-spring-web-flux` dependency into your project.
* Register Backstopper components with Spring WebFlux, either via `@Import({BackstopperSpringWebFluxConfig.class})`, or 
`@ComponentScan(basePackages = "com.nike.backstopper")`. See the javadocs on `BackstopperSpringWebFluxConfig` for some 
related details.
    * This causes `SpringWebfluxApiExceptionHandler` and `SpringWebfluxUnhandledExceptionHandler` to be registered 
    with the Spring WebFlux error handling chain in a way that overrides the default Spring WebFlux error handlers so 
    that the Backstopper handlers will take care of *all* errors. It sets up `SpringWebfluxApiExceptionHandler` with a 
    default list of `ApiExceptionHandlerListener` listeners that should be sufficient for most projects. You can 
    override that list of listeners (and/or many other Backstopper components) if needed in your project's Spring 
    config.
* Expose your project's `ProjectApiErrors` and a JSR 303 `javax.validation.Validator` implementation in your Spring 
dependency injection config.
    * `ProjectApiErrors` creation is discussed in the base Backstopper readme 
    [here](../README.md#quickstart_usage_project_api_errors).
    * JSR 303 setup and generation of a `Validator` is discussed in the Backstopper User Guide 
    [here](../USER_GUIDE.md#jsr_303_basic_setup). If you're not going to be doing any JSR 303 validation outside what 
    is built-in supported by Spring WebFlux, *and* you don't want to bother jumping through the hoops to get a handle 
    on Spring's JSR 303 validator impl provided by `WebFluxConfigurer.getValidator()`, *and* you don't want to bother 
    creating a real `Validator` yourself then you can simply register `NoOpJsr303Validator#SINGLETON_IMPL` as the 
    `Validator` that gets exposed by your Spring config. `ClientDataValidationService` and 
    `FailFastServersideValidationService` would fail to do anything, but if you don't use those then it wouldn't matter. 
* Setup the reusable unit tests for your project as described in the Backstopper User Guide 
[here](../USER_GUIDE.md#reusable_tests) and shown in the sample application. 

### Usage

The base Backstopper readme covers the [usage basics](../README.md#quickstart_usage). There should be no difference 
when running in a Spring WebFlux environment, but since Spring WebFlux integrates a JSR 303 validation system into its 
core functionality we can get one extra nice tidbit: to have Spring WebFlux run validation on objects deserialized 
from incoming user data you can simply add `@Valid` annotations on the objects you're deserializing for your controller 
endpoints (`@RequestBody` object, `@ModelAttribute` objects, etc). For example:

``` java
@RequestMapping(method=RequestMethod.POST)
@ResponseBody
@ResponseStatus(HttpStatus.CREATED)
public Mono<SomeOutputObject> postSomeInput(
        @ModelAttribute @Valid HeadersAndQueryParams headersAndQueryParams,
        @RequestBody @Valid SomeInputObject inputObject
) {
    
    // ... Normal controller processing
    
}
```    

This method signature with the two `@Valid` annotations would cause both the `@ModelAttribute` `headersAndQueryParams` 
and `@RequestBody` `inputObject` arguments to be run through JSR 303 validation. Any constraint violations caught at 
this time will cause a Spring-specific exception to be thrown with the constraint violation details buried inside. 
This `backstopper-spring-web-flux` plugin library's error handler listeners know how to convert this to the appropriate 
set of `ApiError` cases (from your `ProjectApiErrors`) automatically using the 
[Backstopper JSR 303 naming convention](../USER_GUIDE.md#jsr303_conventions), which are then returned to the client 
using the standard error contract. 

This feature allows you to enjoy the Backstopper JSR 303 validation integration support automatically at the point 
where caller-provided data is deserialized and passed to your controller endpoint without having to inject and 
manually call a `ClientDataValidationService`.

## NOTE - Spring WebFlux and Spring Context dependencies required at runtime

This `backstopper-spring-web-flux` module does not export any transitive Spring dependencies to prevent runtime 
version conflicts with whatever Spring environment you deploy to. 

This should not affect most users since this library is likely to be used in a Spring WebFlux environment where the
required dependencies are already on the classpath at runtime, however if you receive class-not-found errors related to 
Spring WebFlux or Spring Context classes then you'll need to pull the necessary dependency into your project. 

The dependencies you may need to pull in:

* Spring WebFlux: [org.springframework:spring-webflux:\[spring-version\]](https://search.maven.org/search?q=g:org.springframework%20AND%20a:spring-webflux)
* Spring Context: [org.springframework:spring-context:\[spring-version\]](https://search.maven.org/search?q=g:org.springframework%20AND%20a:spring-context)
    
## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
