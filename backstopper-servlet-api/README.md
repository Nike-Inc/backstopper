# Backstopper - servlet-api

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 17 and greater.

(NOTE: The [Backstopper 1.x branch](https://github.com/Nike-Inc/backstopper/tree/v1.x) contains a version of 
Backstopper for Java 7+, and for the `javax` ecosystem. The current Backstopper supports Java 17+ and the `jakarta` 
ecosystem.)

This library is intended to be used as a base for creating framework-specific integrations with other Servlet-based
frameworks that Backstopper doesn't [already have support for](../README.md#framework_modules).

It contains the following classes:

* **`ApiExceptionHandlerServletApiBase`** - An extension of the core `ApiExceptionHandlerBase` that takes in a
  `HttpServletRequest` and `HttpServletResponse` and does the necessary adaptation for calling the
  `ApiExceptionHandlerBase` `super` methods.
* **`UnhandledExceptionHandlerServletApiBase`** - An extension of the core `UnhandledExceptionHandlerBase` that
  takes in a `HttpServletRequest` and `HttpServletResponse` and does the necessary adaptation for calling the
  `UnhandledExceptionHandlerBase` `super` methods.
* **`RequestInfoForLoggingServletApiAdapter`** - The adapter used by `ApiExceptionHandlerServletApiBase` and
  `UnhandledExceptionHandlerServletApiBase` for exposing `HttpServletRequest` as the `RequestInfoForLogging` needed
  by the core Backstopper components.

## NOTE - Servlet API dependency required at runtime

This `backstopper-servlet-api` module does not export any transitive Servlet API dependency to prevent runtime 
version conflicts with whatever Servlet environment you deploy to. 

This should not affect most users since this library is likely to be used in a Servlet environment where the
required dependencies are already on the classpath at runtime, however if you receive class-not-found errors related to 
Servlet API classes then you'll need to pull the necessary dependency into your project. 

The dependency you may need to pull in:

* Jakarta Servlet API: 
  [jakarta.servlet:jakarta.servlet-api:\[servlet-api-version\]](https://search.maven.org/search?q=g:jakarta.servlet%20AND%20a:jakarta.servlet-api) 

## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source 
code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
