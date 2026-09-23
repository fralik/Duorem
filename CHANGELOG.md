# Change log
All notable changes to Duorem will be documented in this file.

## [Current version]

## [1.2.0] - 2026-09-23

### Changed
 - Minimum Android version is now Android 8.0 (API 26); target and compile SDK are Android 17 (API 37).
 - Updated Gradle/AGP and migrated support libraries to AndroidX/Material, replacing JCenter.
 - Replaced AsyncTask, connectivity broadcasts, DHCP/shell-based network detection and UI-thread socket operations with cancellable network workers and network callbacks.
 - Updated SSH to the maintained JSch fork; new and changed server keys require explicit confirmation.
 - Encrypt saved device settings with Android Keystore and migrate existing plaintext preferences without changing the app ID.
 - Handle system bar and keyboard insets, exported components, backup exclusions, and Android 17 local-network permission.

### Fixed
 - Discovery no longer discards devices without reverse DNS or readable MAC addresses. On Android 10+, new WOL devices require manual MAC entry.
 - WOL-only configurations no longer depend on SSH connectivity or DNS resolution.
 - Polling and commands close sockets and stop with the screen lifecycle; command failures are no longer inferred from nonempty SSH output.
 - Preserve edited settings across screen recreation, validate addresses and ports, and do not resurrect deleted settings during activity destruction.
 - Add JVM and device regression tests, updated build instructions, and localized permission/error messages.

## [1.1.4] - 2020-02-04

### Added
 - Duorem is now registered for usage on TV.
 - Added Chinese (@8qewt) and Italian (@Asbesbopispa) translations.

## [1.1.3] - 2018-05-26

### Fixed
 - Fixed buttons being not visible on the main screen (portrait orientation) after update of ConstraintLayout library.
 - Show MAC address during network discovery.

## [1.1.2] - 2018-05-21

### Added
 - Added [fastlane](https://github.com/fastlane/fastlane/blob/2.28.7/supply/README.md#images-and-screenshots) images and description files.

## [1.1.1] - 2017-12-09

### Added
 - It is now possible to use  WOL part of the app independently from `shutdown`. This means that IP and SSH information is not necessary to wake up a remote host.
 - MAC address fields change focus automatically after user enters two characters. Manual entering of MAC address should be easier.
 - Paste full MAC address in any MAC field.

### Changed
 - Changed device configuration page layout. Grouped fields that belong to WOL together and fields that belong to SSH/IP together.
 - Removed Save button and replaced it with a menu item.
 - Updated versions of dependency libraries.

### Fixed
 - Do not lose user entered information on device configuration page, if user switches to another app.

## [1.1.0] - 2017-07-13

### Changed
 - Changed default shutdown command from `poweroff` to `shutdown -h now`.

### Added
 - Option to type in your own shutdown command. User can overrive the default.
 - Japanese translation. Provided by [naofum](https://github.com/naofum).
 - Russian translation.

### Fixed
 - Power off command didn't always work from the first time, but always by the second time. Now it should always work first time user tries to power off.

## [1.0.0] - 2017-05-17

### Added
 - Initial release
