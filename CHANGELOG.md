# Change log
All notable changes to Duorem will be documented in this file.

## [Current version]

### Added
 - Follow the system light or dark theme, with improved readability across menus, dialogs, forms, and system bars.
 - Automatically detect MAC addresses from devices that support NetBIOS. Rooted phones can optionally use root-assisted lookup when NetBIOS is unavailable; manual entry remains available.
 - Ask users to verify new or changed SSH host keys before sending credentials or commands.
 - Add configurable 2×2 home-screen widgets with status-aware Wake/Shutdown and Restart controls for Duorem's configured device. Multiple widgets can use different labels without background polling.

### Changed
 - Minimum supported version is now Android 10. The app is updated for current Android releases, including Android 17 local-network permission.
 - Saved device credentials are encrypted with Android Keystore. Existing settings are migrated automatically.
 - Network discovery, Wake-on-LAN, SSH commands, and connectivity monitoring now stop cleanly when their screen is closed or the network changes.

### Fixed
 - Discover devices even when they do not provide a hostname or MAC address.
 - Allow Wake-on-LAN-only configurations without requiring SSH or hostname resolution.
 - Improve reliability and error reporting for discovery, Wake-on-LAN, SSH shutdown/restart, permissions, and changing network connections.
 - Distinguish device reachability from SSH availability so online devices are not shown as offline merely because SSH is unavailable.
 - Preserve edited settings across screen recreation, validate entered addresses and ports, and keep deleted devices deleted.

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
