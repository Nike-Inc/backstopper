# Backstopper Changelog / Release Notes

All notable changes to `Backstopper` will be documented in this file. `Backstopper` adheres to 
[Semantic Versioning](http://semver.org/).

Note that the `nike-internal-util` library is technically version-independent even though it is currently living in 
this repository. Check out that library's [CHANGELOG.md](./nike-internal-util/CHANGELOG.md) for details on its changes. This changelog file is 
specifically for the `backstopper-*` libraries. 

#### 2.x Releases

- `2.0.x` Releases - [2.0.0](#200)

#### 1.x Releases

- `1.15.x` Releases - [1.15.2](#1152)

#### 0.x Releases

- `0.15.x` Releases - [0.15.1](#0151), [0.15.0](#0150)
- `0.14.x` Releases - [0.14.1](#0141), [0.14.0](#0140)
- `0.13.x` Releases - [0.13.0](#0130) 
- `0.12.x` Releases - [0.12.0](#0120)    
- `0.11.x` Releases - [0.11.5](#0115), [0.11.4](#0114), [0.11.3](#0113), [0.11.2](#0112), [0.11.1](#0111), [0.11.0](#0110)
- `0.10.x` Releases - [0.10.0](#0100)                     
- `0.9.x` Releases - [0.9.2](#092), [0.9.1.1](#0911), [0.9.1](#091), [0.9.0.1](#0901), [0.9.0](#090)

## [2.0.0](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v2.0.0)

Released on 2024-09-13.
             
All changes for `2.0.0` were made by [Nic Munroe][contrib_nicmunroe] in pull request
[#72](https://github.com/Nike-Inc/backstopper/pull/72).
        
### Breaking Changes

- Backstopper `2.0.0` represents a major change from the old `javax` ecosystem to the new `jakarta` ecosystem. 
  All `javax` based dependencies and references have been replaced with their `jakarta` counterparts. 
- Java 17 is now the minimum required Java version.
- Support for many older frameworks has been dropped due to these changes, and some new ones added. For the
  frameworks currently supported in `2.x`, see the 
  [Integration Modules](https://github.com/Nike-Inc/backstopper?tab=readme-ov-file#modules) section in the main branch
  readme (includes Servlet API `6.x`, Spring `6.x`, and Spring Boot `3.x`).

NOTE: If you still need support for older Java versions and the `javax` ecosystem, see the 
[Backstopper 1.x branch](https://github.com/Nike-Inc/backstopper/tree/v1.x). The Backstopper `1.x` releases also contain 
support for JAX-RS 2, Jersey 1 and 2, Spring 4 and 5, and Springboot 1 and 2 - see
[here](https://github.com/Nike-Inc/backstopper/tree/v1.x?tab=readme-ov-file#framework_modules).

### Updated

- Migrated all code from `javax` to the `jakarta` ecosystem.
- Updated all libraries to Java 17.
- Updated all dependencies to the latest versions.

### Removed

- Removed support for JAX-RS 2, Jersey 1 and 2, Spring 4 and 5, and Springboot 1 and 2. 
  + These are still supported in the Backstopper `1.x` branch - see 
    [here](https://github.com/Nike-Inc/backstopper/tree/v1.x?tab=readme-ov-file#framework_modules). Everything in the 
    `1.x` branch should be considered deprecated - it's unlikely to see much further development.

### Added

- Added support for Servlet API `6.x`, Spring `6.x`, and Spring Boot `3.x`.
  + See the [Integration Modules](https://github.com/Nike-Inc/backstopper?tab=readme-ov-file#modules) section in the 
    main branch readme for details.
- Added sample apps for Spring `6.x` and Spring Boot `3.x`, for both WebMVC and WebFlux. 
  + See the [samples](https://github.com/Nike-Inc/backstopper?tab=readme-ov-file#modules) section of the main readme 
    for details.


## [1.15.2](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v1.15.2)

Released on 2024-09-08.

### Graduated to 1.x

- Graduated the version of `backstopper` to `1.15.2` (from `0.15.1`) to reflect the stability and previous work
  done on the libraries. No functional changes.

## [0.15.1](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.15.1)

Released on 2023-08-09.

### Fixed

* Fixed header-masking comparisons for the backstopper log. Previously it was doing exact string equals to determine 
  if a header name should be masked in the logs or not, but headers are inherently case-insensitive which could lead 
  to header values ending up in the logs due to case sensitivity issues (e.g. an incoming `authorization` header not 
  being masked because it's not an exact string equals for captial-A `Authorization`). After this fix, any header 
  with a matching name will be masked regardless of case sensitivity. 
  - Fixed by [dearcherian]][[contrib_dearcherian]] in pull request [#67](https://github.com/Nike-Inc/backstopper/pull/67).

## [0.15.0](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.15.0)

Released on 2022-06-03.

### Added

* Added a new [backstopper-reusable-tests-junit5](backstopper-reusable-tests-junit5) library to replace the 
  JUnit-4-based [backstopper-reusable-tests](backstopper-reusable-tests).
  - Added by [Nic Munroe][contrib_nicmunroe] in pull request [#65](https://github.com/Nike-Inc/backstopper/pull/65).

### Deprecated

* Deprecated the JUnit-4-based [backstopper-reusable-tests](backstopper-reusable-tests) (please migrate to the 
  [backstopper-reusable-tests-junit5](backstopper-reusable-tests-junit5) library that does the same thing but for 
  JUnit 5).
  - Deprecated by [Nic Munroe][contrib_nicmunroe] in pull request [#65](https://github.com/Nike-Inc/backstopper/pull/65).

### Migration notes

Migration from the JUnit-4-based [backstopper-reusable-tests](backstopper-reusable-tests) to the new JUnit-5-based 
[backstopper-reusable-tests-junit5](backstopper-reusable-tests-junit5) is pretty straightforward:

1. Make sure your project supports running JUnit 5 tests.
2. Replace your `backstopper-reusable-tests` dependency with the new `backstopper-reusable-tests-junit5` library.
3. In your application's extension of `ReflectionBasedJsr303AnnotationTrollerBase`, you'll need to replace the 
   import of `com.google.common.base.Predicate` with `java.util.function.Predicate` instead.
4. Verify that the reusable tests are being run.

## [0.14.1](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.14.1)

Released on 2022-02-28.

### Updated

* Updated dependency versions to the latest for transitive dependencies exported by Backstopper (where possible due 
  to Java 7 restrictions):
  - SLF4J `1.7.7` -> `1.7.36`
  - Jackson `2.4.2` -> `2.12.6`
  - (`backstopper-reusable-tests` only) JUnit `4.12` -> `4.13.2`
  - (`backstopper-reusable-tests` only) JUnit Dataprovider `1.10.1` -> `1.13.1`
  - (`backstopper-reusable-tests` only) Javassist `3.18.2-GA` -> `3.23.2-GA`
  - (`backstopper-reusable-tests` only) Mockito `1.9.5` -> `2.28.2`
  - Updated by [Nic Munroe][contrib_nicmunroe] in pull request [#63](https://github.com/Nike-Inc/backstopper/pull/63).

## [0.14.0](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.14.0)

Released on 2022-02-28.

No functional changes in version `0.14.0` - this version is just for exercising the Gradle upgrade and migration 
from Bintray to Maven Central.

### Potentially Breaking Changes

* The upgrade to Gradle required adjusting how library dependencies are declared. Mostly this means dependencies
  declared in gradle as `compile` scope turned into `api` scope. This shouldn't affect you - the resulting POMs
  published to Maven Central appear to be identical. Just calling it out as a possibility that something unexpected
  might result.
* The upgrade to Gradle required updating SpringBoot 2 dependency version, which in turn required changing the 
  `backstopper-spring-boot2-webmvc` module to build on Java 8. Since SpringBoot 2.x itself requires Java 8 this 
  shouldn't affect anyone, however it is Spring so things may break anyway for some users.

### Project Build

- Cleaned up bit-rot and generally de-rusted the project. The main changes were bringing the project up to gradle
  `7.4`, migrating to Github Actions for CI build, and setting things up to publish directly to Maven Central.
  + Cleaned up by [Nic Munroe][contrib_nicmunroe] in pull requests
    [#56](https://github.com/Nike-Inc/backstopper/pull/56), [#57](https://github.com/Nike-Inc/backstopper/pull/57),
    [#58](https://github.com/Nike-Inc/backstopper/pull/58), and [#59](https://github.com/Nike-Inc/backstopper/pull/59).

## [0.13.0](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.13.0)

Released on 2020-01-08.

### Added

* Added two new detail key/value pairs to the Backstopper log message when an `ApiException` is handled: 
`exception_cause_class` and `exception_cause_message`. This lets you always know what the cause of the `ApiException` 
was in the case that the `ApiError`(s) in the `ApiException` are too generic, the thrower of the `ApiException` doesn't 
include any extra logging details, and the whole thing ultimately maps to a 4xx (and therefore no stack trace shows up 
in the logs). 
    - Added by [Aniket Joshi][contrib_apjo] in pull request [#46](https://github.com/Nike-Inc/backstopper/pull/46).
    - Resolves issue [#43](https://github.com/Nike-Inc/backstopper/issues/43).
* Added a new feature to `ApiException` to allow you to force stack trace logging on or off, no matter what
HTTP response code is associated with the error. This lets you force stack trace logging on for 4xx errors, or off for
5xx errors, on a per-`ApiException` basis. You specify this using the `ApiException` builder (see 
`ApiException.Builder.withStackTraceLoggingBehavior(...)`).
    - Added by [Aniket Joshi][contrib_apjo] and [Nic Munroe][contrib_nicmunroe] in pull requests 
    [#47](https://github.com/Nike-Inc/backstopper/pull/47) and [#48](https://github.com/Nike-Inc/backstopper/pull/48).
    - Resolves issue [#44](https://github.com/Nike-Inc/backstopper/issues/44).
    
### Fixed

* The `backstopper-reusable-tests` module no longer exports the `ch.qos.logback:logback-classic` dependency 
(it was moved from from a `compile` dependency to `testCompile`). This could conflict with whatever logger impl an 
importing project happened to be using.  
    - Fixed by [Nic Munroe][contrib_nicmunroe] in pull request [#50](https://github.com/Nike-Inc/backstopper/pull/50).   
    
### Deprecated

* Deprecated most of the constructors in `ApiException`. Please move to using the builder.
    - Deprecated by [Nic Munroe][contrib_nicmunroe] in pull request [#48](https://github.com/Nike-Inc/backstopper/pull/48).

## [0.12.0](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.12.0)

Released on 2019-08-23.

### Potentially breaking changes

* Stack dependencies have been moved to compile-only to avoid exporting transitive stack dependencies. This was done
to prevent runtime version conflicts with whatever stack version dependencies you're using in your application 
environment. This should not affect most users since the Backstopper libraries are likely to be used in an environment
where the required stack dependencies are already on the classpath at runtime, however if you receive class-not-found
errors related to the stack classes then you'll need to pull the necessary dependencies into your project. The 
stack-specific Backstopper module readmes for your stack will contain details on which dependencies you might need to 
pull into your project. This includes the following Backstopper libraries and their stack dependencies:
    - [backstopper-servlet-api](backstopper-servlet-api): Servlet API
    - [backstopper-jaxrs](backstopper-jaxrs): Servlet API, JAX-RS API
    - [backstopper-jersey1](backstopper-jersey1): Servlet API, Jersey 1 Server
    - [backstopper-jersey2](backstopper-jersey2): Servlet API, JAX-RS API Jersey 2 Server 
    - [backstopper-spring-web-mvc](backstopper-spring-web-mvc): Servlet API, Spring Web MVC
* The `OneOffSpringFrameworkExceptionHandlerListener` class has been split to accommodate both Spring Web MVC and
Spring WebFlux apps. There's now an abstract `OneOffSpringCommonFrameworkExceptionHandlerListener` that contains
code common to both Web MVC and WebFlux, and specific `OneOffSpringWebMvcFrameworkExceptionHandlerListener`
and `OneOffSpringWebFluxFrameworkExceptionHandlerListener` classes that should be used in their respective Web MVC
and WebFlux environments. This shouldn't affect most Backstopper users unless you were overriding the old 
now-missing class for some reason. If you are an existing user affected by this change, the fix should simply be to 
use `OneOffSpringWebMvcFrameworkExceptionHandlerListener`.
    
### Added

* Added Spring WebFlux support. If you're running a Spring WebFlux app, you can integrate Backstopper easily with 
the new [backstopper-spring-web-flux](backstopper-spring-web-flux) library. Click that link to go to the readme
with full integration setup instructions. See the new 
[Spring Boot 2 WebFlux Sample App](samples/sample-spring-boot2-webflux) for a concrete example of 
Backstopper+Spring WebFlux.
    - Added by [Nic Munroe][contrib_nicmunroe] in pull request [#41](https://github.com/Nike-Inc/backstopper/pull/41).
* Added Spring Boot 1 and Spring Boot 2 (Web MVC) Backstopper libraries. If you're using Spring Boot 1 or 
Spring Boot 2 (with Web MVC, not WebFlux), then you should use these libraries *instead of 
`backstopper-spring-web-mvc`*:
[backstopper-spring-boot1](backstopper-spring-boot1) or 
[backstopper-spring-boot2-webmvc](backstopper-spring-boot2-webmvc). See the readmes in those modules for full
configuration details. See the new [Spring Boot 1 Sample App](samples/sample-spring-boot1) or 
[Spring Boot 2 Web MVC Sample App](samples/sample-spring-boot2-webmvc) for a concrete example.
    - Added by [Nic Munroe][contrib_nicmunroe] in pull request [#40](https://github.com/Nike-Inc/backstopper/pull/40).
* Added support to Spring Web MVC based Backstopper apps for Servlet container errors that happen outside Spring
to still be handled by Backstopper (e.g. 404 errors that are caught by the Servlet container and never reach Spring).
To utilize this feature on non-Springboot apps you'll need to configure your Servlet container to route 
Servlet-container-caught errors to `/error`. For Springboot apps you'll need to pull in and use the new
[backstopper-spring-boot1](backstopper-spring-boot1) or 
[backstopper-spring-boot2-webmvc](backstopper-spring-boot2-webmvc) library (whichever is appropriate) instead of
`backstopper-spring-web-mvc`, and this new feature will be configured automatically.
    - Added by [Nic Munroe][contrib_nicmunroe] in pull request [#40](https://github.com/Nike-Inc/backstopper/pull/40). 
* Added support for a few missing Spring framework exception types and added some extra log details for others.
In particular, added support for many Spring Security exceptions (`AccessDeniedException` will now map to a 403,
for example). 
    - Added by [Nic Munroe][contrib_nicmunroe] in pull request [#38](https://github.com/Nike-Inc/backstopper/pull/38).
    - Resolves issue [#35](https://github.com/Nike-Inc/backstopper/issues/35).
* Added some `testonly-spring*` modules to this repository to cover all the various Spring combinations:
Spring Web MVC 4, Spring Web MVC 5, Spring Boot 1, Spring Boot 2 Web MVC, and Spring Boot 2 WebFlux - testing both
direct import of the Backstopper Spring integration bean as well as blanket `com.nike.backstopper` component scanning
in each case. 
    - Added by [Nic Munroe][contrib_nicmunroe] in pull requests [#40](https://github.com/Nike-Inc/backstopper/pull/40)
    and [#41](https://github.com/Nike-Inc/backstopper/pull/41).     

### Changed

* Changed `SpringApiExceptionHandler`'s order to be highest plus 1, to allow for Spring Boot's `DefaultErrorAttributes` 
to execute and populate the request attributes with its error info.
    - Changed by [Nic Munroe][contrib_nicmunroe] in pull request [#37](https://github.com/Nike-Inc/backstopper/pull/37).
    
### Project Build

* Upgraded to Gradle `5.5.1` and got rid of plugins for console summaries.
    - Upgraded by [Nic Munroe][contrib_nicmunroe] in pull request [#36](https://github.com/Nike-Inc/backstopper/pull/36).
* Changed the Travis CI config to use `openjdk8` instead of `oraclejdk8`. 
    - Changed by [Nic Munroe][contrib_nicmunroe] in pull request [#36](https://github.com/Nike-Inc/backstopper/pull/36).
* Upgraded Jacoco to `0.8.4`.
    - Upgraded by [Nic Munroe][contrib_nicmunroe] in pull request [#40](https://github.com/Nike-Inc/backstopper/pull/40). 
* Upgraded `gradle-bintray-plugin` to `1.8.4`.

## [0.11.5](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.11.5)

Released on 2018-02-19.

### Added

- Added new overridable `JaxRsApiExceptionHandler.setContentType(...)` method to allow you to send a `Content-Type`
response header other than `application/json` in JAX-RS based Backstopper projects. Still defaults to `application/json` 
as before for backwards compatibility.
    - Added by [Nic Munroe][contrib_nicmunroe] in pull request [#31](https://github.com/Nike-Inc/backstopper/pull/31). 

## [0.11.4](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.11.4)

Released on 2018-01-19.

### Updated

- Updated the version of `org.reflections:reflections` (used by the `backstopper-reusable-tests` module) from 
`0.9.9-RC1` to `0.9.11`. 
    - This fixed the following error when a project relied on `backstopper-reusable-tests` but also 
    a newer version of `org.reflections:reflections` than what backstopper was compiled with: 
    `NoSuchMethodError: org.reflections.util.ClasspathHelper.forPackage(Ljava/lang/String;[Ljava/lang/ClassLoader;)Ljava/util/Set;`. 
    One place where this showed up was when using Kotlin in a Backstopper-powered project since Kotlin pulls in 
    `org.reflections:reflections:0.9.11`. Compiling backstopper with the latest `org.reflections:reflections` version 
    seems to fix the issue.    
    - Fixed by [Nic Munroe][contrib_nicmunroe] in pull request [#28](https://github.com/Nike-Inc/backstopper/pull/28).

### Project Build

- Upgraded to Jacoco `0.8.0`.
    - Done by [Nic Munroe][contrib_nicmunroe] in pull request [#28](https://github.com/Nike-Inc/backstopper/pull/28). 

## [0.11.3](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.11.3)

Released on 2017-10-26.

### Added

- Updated `ApiException` constructors to use the `ApiError` message as the exception's message if no message is provided.    
    - Fixed by [Robert Abeyta][contrib_rabeyta] in pull request [#25](https://github.com/Nike-Inc/backstopper/pull/25).
- Added `equals` and `hashcode` methods to `ApiErrorBase` and `ApiErrorWithMetadata`. Updated `ApiErrorComparator` accordingly with equals and hashcode updates and updated sort to check `errorCode` after `name`
    - Fixed by [Robert Abeyta][contrib_rabeyta] in pull request [#26](https://github.com/Nike-Inc/backstopper/pull/26).

## [0.11.2](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.11.2)

Released on 2017-08-28.

### Fixed

- `SortedApiErrorSet` now allows multiple same-named `ApiError`s if they have different metadata. Use case example: throwing the same error multiple times for a given object during validation where the only difference is the field affected, and the field is held in the metadata.  
    - Fixed by [Nic Munroe][contrib_nicmunroe] in pull request [#22](https://github.com/Nike-Inc/backstopper/pull/22).
- `WrapperException.toString()` now guarantees that info about a non-null cause is included.
    - Fixed by [Nic Munroe][contrib_nicmunroe] in pull request [#22](https://github.com/Nike-Inc/backstopper/pull/22).
- The reusable test to verify `@StringConvertsToClassType` on enums pointed to enums that could be deserialized in a case insensitive way if specified in the annotation (`VerifyEnumsReferencedByStringConvertsToClassTypeJsr303AnnotationsAreJacksonCaseInsensitiveTest`) was incorrect. It was doing the case-insensitivity check in the wrong situation. 
    - Fixed by [Nic Munroe][contrib_nicmunroe] in pull request [#22](https://github.com/Nike-Inc/backstopper/pull/22).

### Project Build

- Upgraded to Gradle 4.1.
    - Done by [Nic Munroe][contrib_nicmunroe] in pull request [#22](https://github.com/Nike-Inc/backstopper/pull/22).    
    
## [0.11.1](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.11.1)

Released on 2017-02-01.

### Added

- Added convenience constructor to `ApiErrorWithMetadata` that takes a vararg of `Pair<String, Object>` so that you can inline the extra metadata without having to create and populate a `Map` separately.
    - Added by [Nic Munroe][contrib_nicmunroe].

### Fixed

- Fixed the Spring module's `OneOffSpringFrameworkExceptionHandlerListener` to recognize and properly handle `NoHandlerFoundException` so that it will be mapped to the "404 not found" `ApiError` for your project. 
    - Fixed by [Nic Munroe][contrib_nicmunroe].

## [0.11.0](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.11.0)

Released on 2016-12-01.

### Added

- Added base JAX-RS module for applications running in a non-Jersey JAX-RS environment.
    - Added by [Michael Irwin][contrib_mikesir87].

## [0.10.0](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.10.0)

Released on 2016-11-29.

### Added

- Added the ability to specify extra response headers that should be returned to the caller with the response. These can be set in `ApiExceptionHandlerListenerResult`, so listeners can specify them, and `ApiException` has been modified to support them directly as well.
    - Added by [Nic Munroe][contrib_nicmunroe].

## [0.9.2](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.9.2)

Released on 2016-11-04.

### Fixed

- Fixed a bug with `ApiErrorWithMetadata` where it wasn't including the delegate's metadata. 
    - Fixed by [Nic Munroe][contrib_nicmunroe].

## [0.9.1.1](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.9.1.1)

Released on 2016-10-24.

### Fixed

- Backstopper version 0.9.1 was released pointing to a snapshot version of the nike-internal-util dependency. 0.9.1.1 fixes that to point at the correct nike-internal-util version. 
    - Fixed by [Nic Munroe][contrib_nicmunroe].

## [0.9.1](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.9.1)

Released on 2016-10-24.

***NOTE: The artifacts released for this version point to incorrect dependencies - please use version 0.9.1.1 instead.***

### Fixed

- `ApiExceptionHandlerUtils.DEFAULT_IMPL` was not being initialized correctly, resulting in the "mask sensitive headers" functionality being turned off for `DEFAULT_IMPL`. `DEFAULT_IMPL` will now correctly mask the default set of sensitive headers. 
    - Fixed by [Nic Munroe][contrib_nicmunroe].

## [0.9.0.1](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.9.0.1)

Released on 2016-09-20.

### Fixed

- Version 0.9.0 was unable to be published in bintray, so `nike-internal-util` bintray publishing was split into its own package separate from the backstopper libraries. No functional code changes.
    - Fixed by [Nic Munroe][contrib_nicmunroe].

## [0.9.0](https://github.com/Nike-Inc/backstopper/releases/tag/backstopper-v0.9.0)

Released on 2016-09-20.

### Added

- Initial open source code drop for Backstopper. NOTE: This release never made it to bintray or maven central. Please use 0.9.0.1 instead.
	- Added by [Nic Munroe][contrib_nicmunroe].
	

[contrib_nicmunroe]: https://github.com/nicmunroe
[contrib_mikesir87]: https://github.com/mikesir87 
[contrib_rabeyta]: https://github.com/rabeyta
[contrib_apjo]: https://github.com/apjo
[contrib_dearcherian]: https://github.com/dearcherian
