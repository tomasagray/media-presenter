# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - (upcoming changes)

## [0.2.7] - 2024-07-21

### Changed

- Nav menu links default to random displays

### Added

- Favoriting for Comic Books & Pictures

### Fixed

- Incorrect (always first) image displayed when viewing Pictures
- Nav menu not showing selected (filled-in) icon

## [0.2.6] - 2024-07-06

### Changed

- Improved appearance of navigation menu icons

### Added

- Page display for comics
- Autofocus on search bar display

### Fixed

- Redundant/repetitive thumbnail generation
- Comic page count display on home page
- Annoying background scroll when using image viewer

## [0.2.5] - 2024-06-24

### Fixed

- Errors when adding new files, scanning existing

## [0.2.4] - 2024-06-11

### Changed

- Refactored application under `net.tomasbot.mp`

### Added

- `Performer` tag subtype
- File not found handler

### Fixed

- Styles missing from Video tags
- Comics & Pictures appearing in reverse chronological order

## [0.2.3] - 2024-02-12

### Added

- Empty search result handler

### Fixed

- Broken card display for long file & tag names

## [0.2.2] - 2024-02-09

### Added

- Sort by... bar
- Video times to thumbnails
- Comic Book page count

### Fixed

- Bottom of page clipped
- GUI improvements

## [0.1.1] - 2024-01-09

### Added

- Caps files (this, README, etc.)

### Fixed

- Bug in video thumbnail generation
- Login error handling

### Changed

- File scanning thread count: 9 -> 50