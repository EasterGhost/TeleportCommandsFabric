# Teleport Commands Fabric

> A unified teleport toolkit for Fabric servers — all commands, one admin panel, zero restarts.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.7%2B-brightgreen)]()
[![License](https://img.shields.io/badge/License-GPL--3.0-blue)](LICENSE.txt)

[中文文档](README.zh-CN.md) · [Wiki](https://github.com/EasterGhost/TeleportCommandsFabric/wiki/EN-Home)

---

## What It Does

| Feature | What it does |
| --- | --- |
| 🏠 `/home` `/warp` `/back` `/tpa` `/rtp` `/wild` `/worldspawn` | Fixed-point, player-to-player, short-range random, and long-range exploration teleports |
| 🎛️ `/tpc` | One admin surface: enable/disable modules, tune limits, reload config — no JSON editing |
| 🤝 Shared homes | Publish a home to online players, subscribe from chat, and use it through `/sharedhomes` |
| 🗺️ Map integrations | Sync homes, subscribed shared homes, and warps to Xaero/JourneyMap clients, and publish public warp markers to BlueMap web maps |
| 📋 Better lists | `/homes`, `/sharedhomes`, and `/warps` include clickable paging, sorting, filtering, and management controls |
| ⚙️ Consistent timing | Global delay and cooldown apply across teleport flows, while known targets, RTP, and Wild use safety logic suited to their workloads |
| ⏳ Temporary homes | Time-limited homes (`/tmphome`) with automatic cleanup on expiry |
| 🤝 TPA trust | Auto-accept or auto-deny TPA / TPAHere requests globally or per player |
| 🧭 Rotation restore | Saved homes, warps, and teleport records can preserve facing direction |
| ⚡ Random teleport performance | Batches short-range RTP and long-range Wild work for normal and high-player-count servers |

---

## Quick Start

1. Drop into server `mods/`.
2. Start once → `config/teleport_commands.json` generated.
3. Run `/tpc help` to confirm, `/tpc status` to see active modules.

Common admin commands when you need them:

```
/tpc status                     # see which modules are on
/tpc enable rtp                 # turn a module on
/tpc config home max 20         # change a limit, live
/tpc config teleporting restoreRotation true
/tpc reload                     # reload after manual file edits
```

By default, extra debug logs are disabled. Target chunk preloading and default target safety checks are also disabled by default; RTP and Wild use their own independent destination-selection and safety logic.

Shared-home publications and subscriptions are session state: source homes remain stored normally, but active shares and subscriptions reset when the server restarts.

---

## Build Variants

TeleportCommandsFabric provides two build variants:

- **Standard build**: includes the core teleport features and bundled map integrations for Xaero, JourneyMap, and BlueMap. Recommended for most users.
- **Core build**: includes only the core teleport features, without bundled map integration. Use this if you only need server-side teleport commands or do not use bundled map integrations.

Compatibility notes:

- The standard build is affected by both Minecraft/Fabric compatibility and bundled map-integration compatibility.
- The core build only depends on the core Minecraft/Fabric compatibility of TeleportCommandsFabric, so it is suitable when bundled map integration is temporarily unavailable or not needed.

If you are not sure which one to download, choose the standard build.

---

## With vs. Without Client Mod

Adding the client mod (optional) improves the experience:

| | Server only | Server + Client |
| --- | :---: | :---: |
| All teleport commands | ✅ | ✅ |
| Clickable chat buttons | ✅ | ✅ |
| Xaero/JourneyMap home, shared-home, and warp waypoints | — | ✅ |
| BlueMap web warp markers | ✅ | ✅ |
| Map right-click → teleport | — | ✅ |
| Trusted commands skip confirmation | — | ✅ |
| TPA trust auto-accept / deny | ✅ | ✅ |

BlueMap markers require BlueMap on the server, but do not require TeleportCommandsFabric on clients.

---

## Links

[Wiki](https://github.com/EasterGhost/TeleportCommandsFabric/wiki/EN-Home) · [中文文档](README.zh-CN.md) · [Changelog](CHANGELOG.md) · [License](LICENSE.txt)
