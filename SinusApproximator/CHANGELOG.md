# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.40.1] - 2026-08-12

### Changed
- Update to SKaiNET 0.40.1 from Maven Central.
- Bump Kotlin to 2.4.10 to match the compiler SKaiNET's 0.40.1 klibs are built with.

## [0.34.0] - 2026-07-07

### Changed
- Update to SKaiNET 0.34.0 from Maven Central.
- Align Compose Multiplatform to 1.10.1 to match the shared `:skainet-ui`
  design system (prevents a Skiko `UnsatisfiedLinkError` at first render on
  desktop macOS/Metal).

## [0.6.1]

### Added
- Initial implementation of training into the sample.
- Loss values visualization in time during training.
- Model visualization in a separate screen tab.

### Changed
- Improved UI and visualization.
- Refactored training logic into a separate class.
- Upgraded to Compose Multiplatform 1.10.0.
- Fixed frozen UI on Wasm builds.

## [0.5.0] - 2025-12-04

### Changed
- Update to latest SKaiNET 0.5.0.

## [0.4.0] - 2025-12-04

### Changed
- Update to SKaiNET 0.4.0.
