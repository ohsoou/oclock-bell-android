# Changelog

## [Unreleased] — 2026-04-03

### Added
- Configurable TTS support: pitch, rate, and volume can be controlled from web UI (`NativeAlarm.setTtsConfig`, `NativeAlarm.previewTts`)
- System back button on settings page now navigates to main page instead of closing the app

### Changed
- Improved WebView loading with error overlay and retry button
- Added geolocation permission handling in WebView
- WebView now appends `OClockBellNative/1.0` to the user-agent for native detection

---

## [0.1.0] — 2026-04-03

### Added
- Initial Android wrapper app for native alarm scheduling
- `NativeAlarm` JavaScript bridge (`setAlarm`, `setTestMode`, `getState`, `requestBatteryExemption`)
- Foreground alarm service with `AlarmManager` exact alarm scheduling
- Runtime permission flow for notifications, exact alarms, and battery optimization exemption
- README documenting setup and usage
