# Backstopper - servlet-api

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This library is intended to be used as a base for creating framework-specific integrations with other Servlet-based frameworks that Backstopper doesn't [already have support for](../README.md#framework_modules). 

It contains the following classes:

* **`ApiExceptionHandlerServletApiBase`** - An extension of the core `ApiExceptionHandlerBase` that takes in a `HttpServletRequest` and `HttpServletResponse` and does the necessary adaptation for calling the `ApiExceptionHandlerBase` `super` methods.
* **`UnhandledExceptionHandlerServletApiBase`** - An extension of the core `UnhandledExceptionHandlerBase` that takes in a `HttpServletRequest` and `HttpServletResponse` and does the necessary adaptation for calling the `UnhandledExceptionHandlerBase` `super` methods.
* **`RequestInfoForLoggingServletApiAdapter`** - The adapter used by `ApiExceptionHandlerServletApiBase` and `UnhandledExceptionHandlerServletApiBase` for exposing `HttpServletRequest` as the `RequestInfoForLogging` needed by the core Backstopper components.  

## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
