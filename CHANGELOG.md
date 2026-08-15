# Changelog

All notable changes to Xperi-R are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

> Changes staged for the next release go here. Move them down when you cut a tag.

---

## [0.8.2] - 2026-08-15

### Added

* Camera key overrider — remap the hardware camera key's half-press, full-press, and long-press to custom actions
* Per-app refresh rate control — cap the display refresh rate for specific apps, restored automatically when you leave them
* Xperia 1 V support
* New app icon and branding assets
* Issue templates, PR template, and Dependabot config for the repo
* New `build-apk.yml` CI workflow

### Changed

* Forked from [Shouko](https://github.com/ivaniskandar/shouko); app ID, app name, and theme rebranded to Xperi-R throughout
* README rewritten with screenshots, download links (Obtainium, GitHub Releases, F-Droid), and a link to this changelog
* Code cleanup

### Removed

* Old Shouko app icon and branding assets
* Old CI workflow (replaced by `build-apk.yml`)

---

<!--
HOW TO MAINTAIN THIS FILE

When you're ready to cut a new release:

1. Rename [Unreleased] to the new version and today's date, e.g.:
   ## [0.7.0] - 2026-07-15

2. Add a fresh empty [Unreleased] section at the top.

3. Use these section headers (only include the ones that apply):
   ### Added      — new features
   ### Changed    — changes to existing behaviour
   ### Deprecated — features to be removed in a future release
   ### Removed    — removed features
   ### Fixed      — bug fixes
   ### Security   — security-related changes

4. Keep entries short and user-facing. Write for someone reading the F-Droid
   update description, not for another developer reading the diff.

5. Commit the CHANGELOG update in the same commit as the pubspec version bump.
-->
