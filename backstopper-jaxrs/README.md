# Backstopper - JAX-RS

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This readme focuses specifically on the integration of Backstopper in non-Jersey JAX-RS applications (there are [Jersey 1](../backstopper-jersey1) and [Jersey 2](../backstopper-jersey2) specific modules for applications in those environments). If you are looking for a different framework integration check out the [relevant section](../README.md#framework_modules) of the base readme to see if one already exists. The [base project README.md](../README.md) and [User Guide](../USER_GUIDE.md) contain the main bulk of information regarding Backstopper. 

## Setup

The `JaxRsApiExceptionHandler` is annotated as a JAX-RS `@Provider`, so configures itself as an `ExceptionHandler` automatically. Note that this should be the *only* `ExceptionHandler` in your application for Backstopper to work properly.

## NOTE - JAX-RS and Servlet API dependencies required at runtime

This `backstopper-jaxrs` module does not export any transitive JAX-RS or Servlet API dependencies to prevent runtime 
version conflicts with whatever JAX-RS and Servlet environment you deploy to. 

This should not affect most users since this library is likely to be used in a JAX-RS/Servlet environment where the
required dependencies are already on the classpath at runtime, however if you receive class-not-found errors related to 
JAX-RS or Servlet API classes then you'll need to pull the necessary dependency into your project. 

The dependencies you may need to pull in:

* JAX-RS: [javax.ws.rs:javax.ws.rs-api:\[jax-rs-version\]](https://search.maven.org/search?q=g:javax.ws.rs%20AND%20a:javax.ws.rs-api)
* Servlet API (choose one of the following, depending on your environment needs):
    + Servlet 3+ API: [javax.servlet:javax.servlet-api:\[servlet-api-version\]](https://search.maven.org/search?q=g:javax.servlet%20AND%20a:javax.servlet-api) 
    + Servlet 2 API: [javax.servlet:servlet-api:\[servlet-2-api-version\]](https://search.maven.org/search?q=g:javax.servlet%20AND%20a:servlet-api)

## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
