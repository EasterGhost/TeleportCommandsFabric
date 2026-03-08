# Teleport Commands Fabric

A server-side teleport command mod for Fabric, focused on daily server operations. It also provides a client-side integration path for Xaero map mods to improve in-game usability.

- Chinese README: [README.zh-CN.md](README.zh-CN.md)

## Why This Mod

TeleportCommandsFabric provides a complete teleport toolkit for Fabric servers. Core modules and admin controls follow one command path, which keeps setup and maintenance straightforward as the server grows. It supports integration with Xaero map mod series for a smoother user experience.

### Highlights

- Complete command family: `back`, `home`, `tpa`, `warp`, `worldspawn`, `rtp`/`wild`, and `xaero` sync.
- Unified admin entry: `/teleportcommands` for module switch, reload, and runtime config updates.
- Runtime-first operations: common settings can be adjusted without manual JSON editing.
- Consistent behavior: shared delay/cooldown flow across teleport commands.
- Xaero integration: syncs homes and warps to waypoints for direct map viewing and teleport access.

## Client Experience

Server-side teleport commands remain available regardless of whether clients install this mod. Players can still use `back`, `home`, `warp`, `tpa`, `worldspawn`, and `rtp` with no client installation. Installing the client mod improves teleport-related interaction, and pairing it with Xaero map mods adds waypoint sync for `home` and `warp`.

For a more detailed comparison between client installation options, see [Features Overview](wiki/TeleportCommandsFabric.wiki/2-Features-Overview.md) and [Xaero Integration Module](wiki/TeleportCommandsFabric.wiki/5-7-Xaero.md).

## Quick Start

1. Put the mod into server `mods/`.
2. Start once to generate `config/teleport_commands.json`.
3. Check command registration: `/teleportcommands help`.
4. Use `/teleportcommands config` commands for runtime updates, or run `/teleportcommands reload` after manual file edits.

## Documentation

Detailed docs are maintained in the wiki. This README focuses on project overview and quick entry points.

### Getting Started

- [Home](wiki/TeleportCommandsFabric.wiki/Home.md)
- [Quick Start](wiki/TeleportCommandsFabric.wiki/1-Quick-Start.md)
- [Features Overview](wiki/TeleportCommandsFabric.wiki/2-Features-Overview.md)

### Usage

- [Commands](wiki/TeleportCommandsFabric.wiki/3-Commands.md)
- [Configuration](wiki/TeleportCommandsFabric.wiki/4-Configuration.md)
- [Module Details](wiki/TeleportCommandsFabric.wiki/5-Module-Details.md)

### Operations

- [Permissions and Access](wiki/TeleportCommandsFabric.wiki/6-Permissions-and-Access.md)
- [Data and Storage](wiki/TeleportCommandsFabric.wiki/7-Data-and-Storage.md)
- [Troubleshooting](wiki/TeleportCommandsFabric.wiki/8-Troubleshooting.md)
- [FAQ](wiki/TeleportCommandsFabric.wiki/9-FAQ.md)

## Admin Basics

- Config file: `config/teleport_commands.json`
- Runtime config command: `/teleportcommands config ...`
- Reload command: `/teleportcommands reload`

For field-level details and examples, see [Configuration](wiki/TeleportCommandsFabric.wiki/4-Configuration.md).
