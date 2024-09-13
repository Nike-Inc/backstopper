# Backstopper - testonly-springboot3_2-webflux

Backstopper is a framework-agnostic API error handling and (optional) model validation solution for Java 17 and greater.

(NOTE: The [Backstopper 1.x branch](https://github.com/Nike-Inc/backstopper/tree/v1.x) contains a version of
Backstopper for Java 7+, and for the `javax` ecosystem. The current Backstopper supports Java 17+ and the `jakarta`
ecosystem. The Backstopper 1.x releases also contain support for Spring 4 and 5, and Springboot 1 and 2.)

This submodule contains tests to verify that the 
[backstopper-spring-web-flux](../../backstopper-spring-web-flux) module's functionality works as expected in 
Spring Boot 3.2.x WebFlux (Netty) environments, for both classpath-scanning and direct-import Backstopper configuration 
use cases.

## More Info

See the [base project README.md](../../README.md), [User Guide](../../USER_GUIDE.md), and Backstopper repository 
source code and javadocs for all further information.

## License

Backstopper is released under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
