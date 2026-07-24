# Changelog

All notable changes to this project will be documented in this file.

Version history below is based on the repository tag history, local commit history, and the public release versions already published for this project. Earlier entries are backfilled from code history and may be less detailed than newer releases.

## Data Version History

The config and storage schema versions are tracked separately from the mod release version.

| Mod version     | Config version |       Storage version | Data compatibility notes                                                                                                                                                            |
| --------------- | -------------: | --------------------: | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 2.4             |              3 |                     1 | Adds the `wild` config section and shared-home publication limits without changing schema versions. Shared-home publications and subscriptions are runtime-only and are not persisted. |
| 2.3.1           |              3 |                     1 | Keeps the v2.3 config/storage schema. Existing preload radius values above the supported maximum are normalized to `3`.                                                            |
| 2.3             |              3 |                     1 | Keeps the v2.2 config/storage schema. The map integration config still uses the historical `xaero` config section. Invalid-dimension home/warp entries are preserved during profile load. |
| 2.2             |              3 |                     1 | Keeps the v2.1 config/storage schema. No persisted config or storage fields were added.                                                                                             |
| 2.1             |              3 |                     1 | Keeps the v2.0 schema versions but adds optional `YRot`/`XRot` fields to named locations and recorded back locations, plus optional player `TpaTrust` data. Older records without these fields remain valid. |
| 2.0             |              3 |                     1 | Introduces NBT storage. Named locations store `UUID`, `Name`, `X/Y/Z`, `Dimension`, `Visible`, `ExpiredTime`, and `Sequence`; player/global/back profiles are split into separate NBT files. Config v3 migrates legacy RTP `radius` to `maxRadius`. |
| 1.7             |              2 |                     5 | Storage schema is reserved for the `expiredTime` field on named locations. Older locations without the field are treated as non-expiring. Internal development version, not published. |
| 1.6.1/1.5.2     |              2 |                     4 | Corrected the declared storage schema for UUID-based home/warp identity, `DefaultHomeUuid`, and `HiddenWarpUuids`; repairs historical files from the earlier `v3` declaration. |
| 1.6             |              2 | 3 declared / 4 layout | Code and saved files still declared storage `v3`, but the effective layout was already the UUID-based layout later corrected to storage `v4`.                                   |
| 1.5.1           |              2 | 3 declared / 4 layout | Code and saved files still declared storage `v3`, but the effective layout was already the UUID-based layout later corrected to storage `v4`.                                   |
| 1.5             |              2 |                     3 | Storage `v3` is the intended `1.5` baseline for Xaero visibility and named-location metadata before the UUID-based layout correction.                                           |
| 1.4             |              2 |                     2 | Kept the `1.3` config and storage schema while refactoring teleport/runtime behavior.                                                                                             |
| 1.3             |              2 |                     2 | Introduced explicit config and storage schema constants; config migration covers the old `wild` section name and storage migration normalizes named-location data.                |
| 1.2 and earlier |   not embedded |          not embedded | Legacy JSON files have no embedded schema constant; migrators treat files without a `version` property as historical data.                                                        |

## [2.4] - 2026-07-24

### Added

- Added runtime shared-home publication and subscription. Players can publish homes with `/sharehome`, subscribe from broadcast messages, browse subscriptions with pagination, sorting, and filtering through `/sharedhomes`, and teleport with `/sharedhome`.
- Added configurable per-player publication limits and broadcast cooldowns through `home.sharedHomeMaximum`, `home.sharedHomeBroadcastCooldownSeconds`, and the matching `/tpc config home` commands.
- Added map synchronization for subscribed shared homes on supported client map integrations.
- Added management pages for homes and warps with context-preserving actions and delete confirmation. Home management also supports publishing and withdrawing shared homes.
- Added `/wild` as an independent long-range random teleport module with its own module switch and configurable minimum and maximum radii.
- Added bounded, batched Wild chunk loading and surface-position search so long-range exploration teleports do not reuse the short-range RTP execution path.

### Changed

- Shared-home broadcasts are dispatched in bounded per-tick work and repeated broadcasts for the same publication are coalesced.
- Updating or deleting an owner's home now reconciles active shared-home publications and subscribed map waypoints.
- Timed-out target safety searches now request worker cancellation and discard late results without delaying later teleport execution.
- Overlapping target teleports to the same destination area now share reference-counted preload tickets, preventing one operation from releasing another operation's chunk preload.

### Fixed

- Fixed home, warp, shared-home, and TPA request suggestions so command completion only offers entries matching the text already typed.
- Fixed a race between `/tpc reload` and live configuration changes that could allow a delayed reload result to overwrite a newly applied setting.
- Fixed integer config commands reporting the requested value when normalization changed the actual stored value. Success messages now show the effective setting.
- Fixed safety worker warmup touching live world chunks during server startup. Warmup now uses synthetic block states and does not request chunk loading.

### Notes

- Shared-home publications, subscriptions, and personal shared-home map visibility are runtime state. They are cleared when the server restarts and do not change the storage schema.
- Temporary homes cannot be published as shared homes.
- Map synchronization protocol v2 carries stable shared-home targets while retaining protocol v1 negotiation for 2.3 peers. Shared-home map synchronization requires both sides to support protocol v2.
- `/wild` is no longer an alias of `/rtp`: RTP remains a short-range random teleport, while Wild loads distant candidate chunks in bounded batches and searches their surface.

## [2.3.1] - 2026-07-14

### Changed

- Limited the configurable target chunk preload radius to a maximum of `3` chunks. Existing larger values are normalized automatically, and `/tpc config teleporting preloadRadius` now enforces the same range.
- Updated `/tpc help` and `/tpc status` to report whether client map synchronization is available instead of presenting bundled client integrations as if they were loaded on the server.
- Marked the bundled Xaero integration as client-only to match its actual runtime role.

### Fixed

- Fixed teleport safety workers attempting to synchronously create missing chunks when the warmup region had not finished loading. Warmup now waits for every required loaded chunk, and parallel safety checks no longer fall back to worker-thread world chunk loading.
- Fixed invalid-dimension home and warp cleanup potentially deleting a newly created or updated waypoint after an earlier teleport lookup became stale. Automatic cleanup now removes only the exact unchanged waypoint that failed resolution.
- Fixed a BlueMap lifecycle race that could use an invalid API instance while BlueMap was being disabled or reloaded.
- Hardened current and legacy map synchronization payload decoding against invalid waypoint counts and unsafe allocation from untrusted count values.

## [2.3] - 2026-07-08

### Added

- Added JourneyMap integration for synced TeleportCommandsFabric homes, warps, and death-location teleport handling.
- Added BlueMap integration for publishing server-side warp markers to BlueMap web maps.
- Added a shared map integration sync protocol used by bundled map integrations, allowing map support to be coordinated through the common `integration` module.
- Added a legacy Xaero sync compatibility path so older Xaero integration clients and servers can continue exchanging waypoint snapshots during the 2.2 to 2.3 transition.

### Changed

- Updated map integration admin commands and descriptions to use the `integration` module name instead of the old Xaero-specific module name.
- Kept the existing `xaero` config section name for backward compatibility, while treating it as the map integration config group at runtime.
- Updated standard build packaging to bundle BlueMap support alongside Xaero and JourneyMap, while keeping the core build free of bundled map integrations.
- Updated Gradle build switches so `includeMapIntegrations=false` produces a core build and standard builds include all bundled map integrations by default.
- Changed invalid-dimension cleanup behavior for homes and warps: storage loading now preserves entries even if a dimension is not available yet, and `deleteInvalid` cleanup happens only when a teleport attempt finds that the target world is unavailable.
- Normalized command return values across modules for clearer command execution semantics in command blocks and admin tooling.
- Improved map waypoint synchronization so clients only receive changed waypoint snapshots instead of repeated unchanged data.
- Improved map integrations so synced waypoints respect the configured waypoint persistence behavior.
- Improved teleport execution state handling so previous-location records are captured before teleport and saved only after successful teleport execution.
- Improved target teleport safety checks for special collision blocks: doors are treated as passable space, while fences, walls, and fence gates are no longer accepted as safe standing support.
- Added a defensive timeout fallback for parallel target teleport safety checks.

### Fixed

- Fixed a data-loss risk where enabling `deleteInvalid` could remove valid warp data during server startup before all dimensions were available.
- Fixed creative/fall-flying state and camera reset edge cases after teleport, especially around cross-dimension teleport behavior.

## [2.2] - 2026-06-18

### Added

- Added `/back preview` to show recorded previous-teleport and death locations with facing, pitch, and clickable teleport actions.
- Added `/tpcancel` so players can cancel their own pending teleport.
- Added runtime info (version and integration status) to `/tpc help` and `/tpc status` admin pages.

### Changed

- Extracted Xaero integration into a separate bundled JAR-in-JAR source set, conditionally included via the `includeXaeroIntegration` Gradle property. The main mod is now Xaero-independent at compile time.
- Decoupled runtime config application with a `RuntimeConfigHooks` registry so integrations can react to config changes without the core mod importing integration code.
- Added CLI page headings to admin command output for consistency with other TPC pages.

### Fixed

- Fixed Xaero map visibility commands (`maphome` / `mapwarp`) built without page arguments not being handled when triggered from the silent (waypoint-delete) path.

## [2.1] - 2026-06-12

### Added

- Added TPA trust rules, allowing players to set per-player or default behavior for `tpa` and `tpahere` requests.
- Added clickable filter and sort controls to `/homes` and `/warps` list pages.
- Added saved teleport rotation support so homes, warps, and recorded teleport targets can preserve facing direction.

### Changed

- Improved target teleport safety block-state reads for lower overhead during safety checks.
- Improved RTP destination safety handling with shared unsafe-block rules.
- Improved admin command organization while keeping the external `/tpc` command surface unchanged.
- Improved Xaero waypoint recognition so TeleportCommandsFabric only handles tagged waypoints created by this mod.
- Updated README content for the 2.1 feature set and current default teleport behavior.

### Fixed

- Fixed custom Xaero waypoint sets not being recognized correctly for TeleportCommandsFabric home/warp teleport actions.
- Fixed normal player-created Xaero waypoints being more likely to be mistaken for TeleportCommandsFabric-managed waypoints during map hide/delete flows.

## [2.0] - 2026-06-03

### Added

- **NBT-based Storage Engine**: Migrated the storage system to a high-performance NBT-based profile storage engine, replacing legacy JSON storage for superior persistence stability.
- **Legacy Migrators**: Built integrated Storage v5 and Config v3 migrators, ensuring seamless automated upgrades of historical JSON settings from older versions.
- **Adaptive Execution RTP Service**: Implemented a new RTP random teleport service supporting adaptive scheduling, seamlessly switching between serial ticks-throttled checks and parallel virtual-thread-based searches under backlog pressure.
- **Configurable Temporary Home TTL**: Added configurable Time-To-Live (TTL) support for Temporary Homes with automated cleanup upon expiration.
- **Native Version Updates**: Built native compatibility for Fabric 26.1.2 and Xaero Minimap/Worldmap 26.1.2.

### Changed

- **Shared Operation Flow**: Introduced a unified `TeleportOperation` model, centralizing teleport state-transitions, chunk preloading, and cooldowns within a shared `TeleportOperationManager`.
- **Shared Query Infrastructure**: Refactored command arguments to share common parsing, sorting, and dimension/prefix filtering nodes across warp and home queries.
- **Asynchronous CLI Page Rendering**: Completed the CLI rendering layer under `WaypointPages` to assemble paged Component outputs asynchronously.
- **Unified Config Applier**: Wired runtime settings adjustments to update live services dynamically via `ConfigApplier`.
- **Lifecycle Decoupling**: Moved server lifecycle event subscriptions out of the primary ModInitializer into a dedicated Lifecycle Manager.
- **Normalized Safety Toggles**: Unified command safety toggle argument names across all packages to `Disable Safety`.
- **Storage Layer Reorganization**: Reorganized the storage package, separating responsibilities into dedicated modules for NBT codecs, disk I/O limiters, and managers for player, global, and recorded location profiles.

### Fixed

- **Translation Quality**: Cleaned localizations to resolve translation key overlaps, and resolved a concurrency issue during translation loading to prevent circular nested cache initialization.
- **Heavy Concurrency Hardening**: Strengthened state checks and boundaries for teleport operations and waypoints updates under multi-player concurrency.

## [1.6.1] - 2026-04-03

### Changed

- Updated the storage schema version to `4` to match the UUID-based home and warp identity format already used by the `1.5.1` and `1.6` storage layout.
- Split storage migration handling so the original `v3` path only applies the older `y` normalization and `xaeroVisible` additions, while the later UUID-based schema upgrades are treated as `v4` migrations.

### Fixed

- Fixed storage migration versioning for historical `v3` files so `1.5.1` and `1.6` storage data is upgraded consistently to the corrected `v4` schema.
- Fixed migration of legacy default-home data so empty or unmatched `DefaultHome` values no longer create invalid empty-string UUID entries in `storage.json`.
- Added migration repair for missing `HiddenWarpUuids` data when upgrading older storage files to the corrected UUID-based schema.

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
