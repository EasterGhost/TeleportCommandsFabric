# Changelog

All notable changes to this project will be documented in this file.

Version history below is based on the repository tag history, local commit history, and the public release versions already published for this project. Earlier entries are backfilled from code history and may be less detailed than newer releases.

## [1.6] - 2026-03-26

### Added

- Added Minecraft `26.1` compatibility for the master branch release line.
- Added a fallback Xaero map waypoint teleport interception path so tagged `warp` and `home` waypoints can still dispatch TeleportCommands commands when newer Xaero UI callback signatures do not match the older integration hook.

### Changed

- Updated the project build and dependency setup for `26.1`, including Fabric API coordinates, Loom plugin ID usage, and separate Xaero compile-only version targeting.
- Updated message-sending and networking integration code to match the newer `26.1` / Fabric API surface.
- Updated the project toolchain target to Java `25`.

### Fixed

- Fixed Xaero-related compatibility on `26.1` by avoiding hard binding to newer dimension identifier signatures and using a safer fallback when resolving Xaero world IDs.
- Fixed Xaero sync payload registration to use the newer clientbound and serverbound play registry APIs.
- Fixed teleport position conversion for `26.1` by updating the affected `ChunkPos` / `BlockPos` handling.

## [1.5.1] - 2026-03-25

### Added

- Added `updatehome` and `updatewarp` so existing homes and warps can be moved to the current location without recreating them.
- Added pagination support and interactive page-picker navigation for `homes` and `warps`, with improved clickable list display for large result sets.

### Changed

- Refactored shared command execution, suggestion, pagination, visibility, and chat UI helpers to reduce duplication across `back`, `home`, `warp`, and related command flows.
- Refactored config and storage handling by splitting manager, migrator, and data responsibilities into dedicated classes and helpers.
- Updated command registration to use Fabric's direct registration callback path instead of the older mixin-based command registration hook.
- Updated persistence flow to use dirty tracking, synchronized asynchronous file I/O, asynchronous config writes, and a forced storage flush during shutdown.

### Fixed

- Fixed a shutdown issue where background save or cleanup work could terminate unexpectedly while the server was closing.
- Fixed translation lookup fallback so missing keys now fall back to `en_us` immediately.
- Fixed disconnect and reconnect edge cases by delaying cleanup of previous-teleport and cooldown state instead of removing it immediately on logout.
- Fixed asynchronous config and storage read edge cases to improve reliability under concurrent access.
- Improved unsafe teleport block checking and related world-resolution reliability.

## [1.5] - 2026-03-21

### Added

- Added Xaero map visibility controls for both `warp` and `home`, including direct toggle commands from synced waypoint flows.
- Added `/gwarpmap` for administrators to manage global warp visibility on the map separately from each player's personal hide/show state.
- Added `/back tp` to return to the location recorded before the last teleport command execution.
- Added direct integration from Xaero death waypoint teleport into `/back death`.
- Added stable UUID identity for homes and warps, together with storage migration support for existing saved data.

### Changed

- Refactored the command implementation into dedicated command, formatter, message, and service helpers for more consistent behavior across `back`, `home`, `warp`, `tpa`, `rtp`, and admin flows.
- Changed Xaero waypoint sync behavior to work cleanly with default waypoint-set usage while keeping TeleportCommands-managed waypoints identifiable.
- Changed home and warp persistence to use stable UUID-based identity, including migration of default-home references away from name-only matching.
- Changed Xaero teleport interception in the default `Default` waypoint-set flow to recognize only TeleportCommands-tagged waypoints instead of matching generic `W` / `H` symbols.
- Updated Xaero waypoint deletion handling so deleting synced waypoints can silently map back to `mapwarp` and `maphome` visibility changes instead of only removing the local marker.
- Improved Xaero-related command interception, trusted-command flow, and chat interaction formatting for smoother client-side use.
- Refined global and per-player map visibility management so warp administration and personal visibility control are handled separately.

### Fixed

- Fixed several Xaero interaction edge cases around waypoint teleport, deletion, and command routing.
- Fixed command-side name matching for TeleportCommands-managed Xaero waypoints so normal default-set waypoints are less likely to be mistaken for `warp` or `home` entries.
- Fixed clickable `rename`, `delete`, `maphome`, `mapwarp`, and related chat actions so names containing escaped characters such as `\\` and `\"` are passed correctly.
- Fixed confusing Xaero waypoint-set configuration cases by normalizing blank, `Current`, and legacy TeleportCommands set names back to `Default`.

## [1.4] - 2026-03-16

### Added

- Added `TeleportSafety` and `TeleportService` to centralize teleport safety checks, delayed teleport flow, cooldown handling, and preload behavior.
- Added `TranslationHelper` with cached language file loading to reduce repeated JSON parsing during runtime.
- Added `/tpc` as a short admin command alias while keeping `/teleportcommands` for compatibility.
- Added support for quoted and escaped names in Xaero-triggered `home` and `warp` commands.

### Changed

- Refactored TPA request storage to use `requestId` as the primary internal index.
- Refactored dimension lookup into `WorldResolver`.
- Updated admin command presentation so help text and status actions prefer `/tpc`.
- Updated README and Chinese README to document `/tpc` as the primary admin entry.
- Cleaned duplicated and unused code, including unused planned legacy aliases in `XaeroCompat`.

### Fixed

- Fixed teleport cooldown timing so cooldown is counted when teleport actually succeeds.
- Fixed delayed teleport success messaging so it is no longer shown too early.
- Fixed `/back` death location deletion timing so it happens after a successful teleport instead of before.
- Fixed a high-Y teleport safety issue.
- Fixed RTP behavior when the player's dimension changes while a random-teleport search is still in progress.
- Improved teleport safety logic and unified command-side teleport behavior.

## [1.3] - 2026-03-09

### Added

- Added Xaero integration features, including syncing homes and warps to Xaero waypoints.
- Added RTP / random teleport support, with compatibility for the legacy `/wild` command name.
- Added admin runtime configuration and module management commands under a unified admin entry.
- Added configurable sync interval, request expiration handling, join delay, and shared scheduler behavior for sync and TPA flows.
- Added teleport preload experiments and precise Y-coordinate teleport support groundwork.

### Changed

- Refactored administrator commands into the newer admin command structure.
- Renamed wild/random teleport naming to `rtp` while retaining `/wild` compatibility.
- Moved away from unstable Xaero container code paths and deprecated internal usages.
- Standardized config file encoding to UTF-8 and improved config/storage handling.
- Updated project tooling to JDK 21 and refreshed project metadata, icon, and README content.

### Fixed

- Fixed several storage, timer, and crash issues across teleport and sync flows.
- Fixed a bug where TPA failure could show the wrong message.
- Fixed invalid-home cleanup behavior and several offline-player command edge cases.
- Fixed crashes in single-player and container reuse issues affecting Xaero-related state.
- Fixed language support and multiple smaller robustness issues in RTP and sync code.

## [1.2] - 2026-02-20

### Added

- Initial public release of TeleportCommandsFabric for Minecraft 1.21.11.
- Added the main teleport command set: `back`, `home`, `warp`, and `tpa`.
- Added language/i18n support for teleport command messages.
- Added configurable limits for homes and warps.
- Added TPA permission control.
- Added a client-side trusted-command confirmation bypass mixin for smoother command interactions.
- Added project documentation, license metadata, and initial README content.

### Changed

- Refactored the teleport command structure and introduced configuration-driven behavior.
- Improved TPA flow and updated translations and project metadata.

### Fixed

- Fixed missing or outdated translation strings, including TPA request-not-found messaging.
- Performed early cleanup of assets, formatting, and build configuration issues.

## [1.1] - 2026-01-20

### Added

- Established the initial TeleportCommandsFabric project structure and base mod configuration.
- Added basic storage and configuration files for server-side teleport data.

### Changed

- Iterated on the early command structure and project layout in preparation for the first public 1.2 release.
- Started building the overall direction of the mod as a server-focused teleport command toolkit.

### Fixed

- Fix a vital crash when teleport to same location due to a wrong datastructure used.

## [1.0] - 2025

It is old teleportcommands mod just edit the version compatibility of minecraft.
