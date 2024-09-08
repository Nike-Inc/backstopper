# Nike Internal Util Changelog / Release Notes

All notable changes to the `nike-internal-util` library will be documented in this file. `nike-internal-util` adheres to [Semantic Versioning](http://semver.org/).

Note that the `backstopper-*` libraries are version-independent even though they are currently living in the same repository as `nike-internal-util`. Check out the root [CHANGELOG.md](../CHANGELOG.md) for details on `backstopper-*` library changes. This changelog file is specifically for the `nike-internal-util` library. 

#### 1.x Releases

- `1.10.x` Releases - [1.10.1](#1101)

#### 0.x Releases

- `0.10.x` Releases - [0.10.0](#0100)
- `0.9.x` Releases - [0.9.0.1](#0901), [0.9.0](#090)

## [1.10.1](https://github.com/Nike-Inc/backstopper/releases/tag/nike-internal-util-v1.10.1)

Released on 2024-09-07.

### Graduated to 1.x

- Graduated the version of `nike-internal-util` to `1.10.1` (from `0.10.0`) to reflect the stability and previous work 
  done on the library.

## [0.10.0](https://github.com/Nike-Inc/backstopper/releases/tag/nike-internal-util-v0.10.0)

Released on 2022-02-28.

### Added

- Added `Glassbox` and `TestUtils` classes. These are helpers intended for use during testing.
    + Added by [Nic Munroe][contrib_nicmunroe] in pull request [#60](https://github.com/Nike-Inc/backstopper/pull/60).

## [0.9.0.1](https://github.com/Nike-Inc/backstopper/releases/tag/nike-internal-util-v0.9.0.1)

Released on 2016-09-20.

### Fixed

- Version 0.9.0 was unable to be published in bintray, so `nike-internal-util` bintray publishing was split into its own package separate from the backstopper libraries. No functional code changes. 
    - Fixed by [Nic Munroe][contrib_nicmunroe].

## [0.9.0](https://github.com/Nike-Inc/backstopper/releases/tag/nike-internal-util-v0.9.0)

Released on 2016-09-20.

### Added

- Initial open source code drop for `nike-internal-util`. NOTE: This release never made it to bintray or maven central. Please use 0.9.0.1 instead.
	- Added by [Nic Munroe][contrib_nicmunroe].
	

[contrib_nicmunroe]: https://github.com/nicmunroe
