# Backstopper - JAX-RS

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 7 and greater.

This readme focuses specifically on the Backstopper JAX-RS integration. If you are looking for a different framework integration check out the [relevant section](../README.md#framework_modules) of the base readme to see if one already exists. The [base project README.md](../README.md) and [User Guide](../USER_GUIDE.md) contain the main bulk of information regarding Backstopper. 

## Setup

The `JaxRsApiExceptionHandler` is annotated as a JAX-RS `@Provider`, so configures itself as an `ExceptionHandler` automatically.

## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
