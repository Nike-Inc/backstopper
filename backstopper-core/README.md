# Backstopper - core

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 11 and greater.

(NOTE: The [Backstopper 1.x branch](https://github.com/Nike-Inc/backstopper/tree/v1.x) contains a version of
Backstopper for Java 7+, and for the `javax` ecosystem. The current Backstopper supports Java 11+ and the `jakarta`
ecosystem.)

This `backstopper-core` library contains the key core components necessary for a Backstopper system to work.
The [base project README.md](../README.md) and [User Guide](../USER_GUIDE.md) contain the main bulk of information
regarding Backstopper, but in particular:

* [Backstopper key components](../USER_GUIDE.md#key_components) - This describes the main classes contained in this core
  library and what they are for. See the source code and javadocs on classes for further information.
* [Framework-specific modules](../README.md#framework_modules) - The list of specific framework plugin libraries
  Backstopper currently has support for.
* [Sample applications](../README.md#samples) - The list of sample applications demonstrating how to integrate and use
  Backstopper in the various supported frameworks.
* [Creating new framework integrations](../USER_GUIDE.md#new_framework_integrations) - Information on how to create new
  framework integrations.

## More Info

See the [base project README.md](../README.md), [User Guide](../USER_GUIDE.md), and Backstopper repository source code
and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
