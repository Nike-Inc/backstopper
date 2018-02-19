# Backstopper Changelog / Release Notes

All notable changes to `Backstopper` will be documented in this file. `Backstopper` adheres to [Semantic Versioning](http://semver.org/).

Note that the `nike-internal-util` library is technically version-independent even though it is currently living in this repository. Check out that library's [CHANGELOG.md](./nike-internal-util/CHANGELOG.md) for details on its changes. This changelog file is specifically for the `backstopper-*` libraries. 

## Why pre-1.0 releases?

Backstopper is used heavily and is stable internally at Nike, however the wider community may have needs or use cases that we haven't considered. Therefore Backstopper will live at a sub-1.0 version for a short time after its initial open source release to give it time to respond quickly to the open source community without ballooning the version numbers. Once its public APIs have stabilized again as an open source project it will be switched to the normal post-1.0 semantic versioning system.

#### 0.x Releases
   
- `0.11.x` Releases - [0.11.5](#0115), [0.11.4](#0114), [0.11.3](#0113), [0.11.2](#0112), [0.11.1](#0111), [0.11.0](#0110)
- `0.10.x` Releases - [0.10.0](#0100)                     
- `0.9.x` Releases - [0.9.2](#092), [0.9.1.1](#0911), [0.9.1](#091), [0.9.0.1](#0901), [0.9.0](#090)

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