# Backstopper - jersey2

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This readme focuses specifically on the Backstopper Jersey 2 integration, which builds upon the JAX-RS integration. If you are looking for a different framework integration check out the [relevant section](../README.md#framework_modules) of the base readme to see if one already exists. The [base project README.md](../README.md) and [User Guide](../USER_GUIDE.md) contain the main bulk of information regarding Backstopper. 

**NOTE: There is a [Jersey 2 sample application](../samples/sample-jersey2/) that provides a simple concrete example of the information covered in this readme.**

## Backstopper Jersey 2 Setup, Configuration, and Usage

### Setup

* Pull in the `com.nike.backstopper:backstopper-jersey2` dependency into your project.
* Register Backstopper components with Jersey 2. Jersey has many ways to configure itself, so this is often a project-specific process. `Jersey2BackstopperConfigHelper` contains some helpers that will be useful. See the [Jersey 2 sample application](../samples/sample-jersey2/)'s `Jersey2SampleResourceConfig` and `Main` classes for a concrete example which should help guide you even if you don't end up registering things the same way in your project. 
    * This causes `Jersey2ApiExceptionHandler` to be registered with the Jersey 2 error mapping system so that the Backstopper handlers will take care of *all* `Throwable`s.
    * Your project's `ProjectApiErrors` will need to be provided when the `Jersey2ApiExceptionHandler` is created. `ProjectApiErrors` creation is discussed in the base Backstopper readme [here](../README.md#quickstart_usage_project_api_errors).
* Setup the reusable unit tests for your project as described in the base Backstopper User Guide [here](../USER_GUIDE.md#reusable_tests) and shown in the sample application. 

### Usage

The base Backstopper readme covers the [usage basics](../README.md#quickstart_usage). There should be no difference when running in a Jersey 2 environment, other than `Jersey2ApiExceptionHandler` knowing how to handle Jersey 2 framework exceptions properly (this should happen automatically without any effort from you).

## NOTE - Jersey 2 and Servlet API dependencies required at runtime

This `backstopper-jersey2` module does not export any transitive Jersey 2 or Servlet API dependencies to prevent runtime 
version conflicts with whatever Jersey 2 and Servlet environment you deploy to. 

This should not affect most users since this library is likely to be used in a Jersey 2/Servlet environment where the
required dependencies are already on the classpath at runtime, however if you receive class-not-found errors related to 
Jersey 2 or Servlet API classes then you'll need to pull the necessary dependency into your project. 

The dependencies you may need to pull in:

* Jersey 2: [org.glassfish.jersey.core:jersey-server:\[jersey2-version\]](https://search.maven.org/search?q=g:org.glassfish.jersey.core%20AND%20a:jersey-server)
* Servlet API (choose one of the following, depending on your environment needs):
    + Servlet 3+ API: [javax.servlet:javax.servlet-api:\[servlet-api-version\]](https://search.maven.org/search?q=g:javax.servlet%20AND%20a:javax.servlet-api) 
    + Servlet 2 API: [javax.servlet:servlet-api:\[servlet-2-api-version\]](https://search.maven.org/search?q=g:javax.servlet%20AND%20a:servlet-api)
    
## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
