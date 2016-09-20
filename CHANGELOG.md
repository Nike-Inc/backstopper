# Backstopper Changelog / Release Notes

All notable changes to `Backstopper` will be documented in this file. `Backstopper` adheres to [Semantic Versioning](http://semver.org/).

Note that the `nike-internal-util` library is technically version-independent even though it is currently living in this repository. Check out that library's [CHANGELOG.md](./nike-internal-util/CHANGELOG.md) for details on its changes. This changelog file is specifically for the `backstopper-*` libraries. 

## Why pre-1.0 releases?

Backstopper is used heavily and is stable internally at Nike, however the wider community may have needs or use cases that we haven't considered. Therefore Backstopper will live at a sub-1.0 version for a short time after its initial open source release to give it time to respond quickly to the open source community without ballooning the version numbers. Once its public APIs have stabilized again as an open source project it will be switched to the normal post-1.0 semantic versioning system.

#### 0.x Releases

- `0.9.x` Releases - [0.9.0.1](#0901), [0.9.0](#090)

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
