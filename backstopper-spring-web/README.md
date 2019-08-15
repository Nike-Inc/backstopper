# Backstopper - spring-web

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This `backstopper-spring-web` module is not meant to be used standalone. It is here to provide common code for any
`spring-web*` based application, including both Spring Web MVC and Spring WebFlux applications. But this module
does not provide Spring+Backstopper integration by itself.

To integrate Backstopper with your Spring application, please choose the correct concrete integration library,
depending on which Spring environment your application is running in:

* [backstopper-spring-web-mvc](../backstopper-spring-web-mvc) - For Spring Web MVC applications.
* [backstopper-spring-web-flux](../backstopper-spring-web-flux) - For Spring WebFlux applications.
  
The links above will take you to an integration-focused readme that will tell you how to integrate Backstopper into
your Spring Web MVC or WebFlux application. If you are looking for a different framework integration check out the 
[relevant section](../README.md#framework_modules) of the base readme to see if one already exists.
 
## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source 
code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
