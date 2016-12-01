# Backstopper - JAX-RS

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This readme focuses specifically on the integration of Backstopper in non-Jersey JAX-RS applications (there are [Jersey 1](../backstopper-jersey1) and [Jersey 2](../backstopper-jersey2) specific modules for applications in those environments). If you are looking for a different framework integration check out the [relevant section](../README.md#framework_modules) of the base readme to see if one already exists. The [base project README.md](../README.md) and [User Guide](../USER_GUIDE.md) contain the main bulk of information regarding Backstopper. 

## Setup

The `JaxRsApiExceptionHandler` is annotated as a JAX-RS `@Provider`, so configures itself as an `ExceptionHandler` automatically. Note that this should be the *only* `ExceptionHandler` in your application for Backstopper to work properly.

## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
