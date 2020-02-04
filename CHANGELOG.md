# Change log
All notable changes to BNT will be documented in this file.

## [Current version]

### Added
 - Duorem is now registered for usage on TV.

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

