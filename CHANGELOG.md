# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased] - (upcoming changes)

- Upcoming changes will appear here

## [0.3.4] - 2026-02-10

### Fixed

- Fixed random entity retrieval performance, reliability
- Limited display length of entity titles
- Refactored JavaScript code

### Added

- Added top navigation links to type-specific search pages
- TrashCollectorService to cleanup stray thumbnails, etc.

## [0.3.3] - 2025-05-15

### Fixed

- Automatically rename videos when moving if a video with that name already exists
- Attach Tags to Videos when adding via the "add" directory

### Added

- Support for *.webp images

## [0.3.2] - 2025-05-10

### Fixed

- Invalid files not tracked
- Random entity (Video, Picture, etc.) retrieval performance

## [0.3.1] - 2025-05-03

### Changed

- Switch to external FFmpeg wrapper library

### Fixed

- Ensured minimum Tag length
- Comic duplicates
- Video player rotated orientation
- Search results: images and comics now display properly, video pagination works, all entity types are represented on
  global search page
- Bug: could not upload user data

## [0.3.0] - 2025-04-27

### Added

- Tag prediction
- Skip +/- 10s controls for videos
- Timestamping for Favorites

### Fixed

- Race condition when adding new Tags during file scan
- Fix image display alignment in viewer modal

## [0.2.9] - 2024-10-26

### Added

- Edit tags capability

### Fixed

- Unhandled error when scanning comics & pictures
- Crash when performing initial search indexing

## [0.2.8] - 2024-10-03

### Added

- Remember me (stay logged in)
- Import/export of user data

### Fixed

- All search results are now accessible
- Error when deleting a favorited file
- Picture favorites not correctly displayed

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

- File scanning thread count: 9 → 50