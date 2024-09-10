# Backstopper - spring-web

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 17 and greater.

(NOTE: The [Backstopper 1.x branch](https://github.com/Nike-Inc/backstopper/tree/v1.x) contains a version of
Backstopper for Java 7+, and for the `javax` ecosystem. The current Backstopper supports Java 17+ and the `jakarta`
ecosystem. It also contains support for Spring 4 and 5, and Springboot 1 and 2.)

This `backstopper-spring-web` module is not meant to be used standalone. It is here to provide common code for any
`spring-web*` based application, including both Spring Web MVC and Spring WebFlux applications. But this module
does not provide Spring+Backstopper integration by itself.

To integrate Backstopper with your Spring application, please choose the correct concrete integration library,
depending on which Spring environment your application is running in:

// TODO javax-to-jakarta: Fix these links to other libraries after the refactor is complete. 

### Spring WebFlux based applications

* [backstopper-spring-web-flux](../backstopper-spring-web-flux) - For Spring WebFlux applications.

### Spring Web MVC based applications

* [backstopper-spring-boot1](../backstopper-spring-boot1) - For Spring Boot 1 applications.
* [backstopper-spring-boot2-webmvc](../backstopper-spring-boot2-webmvc) - For Spring Boot 2 applications using the 
Spring MVC (Servlet) framework. If you want Spring Boot 2 with Spring WebFlux (Netty) framework, then see 
[backstopper-spring-web-flux](../backstopper-spring-web-flux) instead. 
* [backstopper-spring-web-mvc](../backstopper-spring-web-mvc) - For Spring Web MVC applications that are not
Spring Boot.

The links above will take you to an integration-focused readme that will tell you how to integrate Backstopper into
your Spring Web MVC or WebFlux application. If you are looking for a different framework integration check out the 
[relevant section](../README.md#framework_modules) of the base readme to see if one already exists.
 
## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source 
code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
