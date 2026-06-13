# Teleport Commands Fabric

> A unified teleport toolkit for Fabric servers — all commands, one admin panel, zero restarts.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.7%2B-brightgreen)]()
[![License](https://img.shields.io/badge/License-GPL--3.0-blue)](LICENSE.txt)

[中文文档](README.zh-CN.md) · [Wiki](https://github.com/EasterGhost/TeleportCommandsFabric/wiki/EN-Home)

---

## What It Does

| Feature | What it does |
| --- | --- |
| 🏠 `/home` `/warp` `/back` `/tpa` `/rtp` `/worldspawn` | Every teleport command your server needs |
| 🎛️ `/tpc` | One admin surface: enable/disable modules, tune limits, reload config — no JSON editing |
| 🗺️ Xaero sync | Homes and warps shown on the map. Delete a map waypoint, and it hides server-side |
| 📋 Better lists | `/homes` and `/warps` include clickable paging, sorting, and filtering controls |
| ⚙️ Shared behavior | Delay, cooldown, chunk preloading, and safety checks apply consistently across all teleport types |
| ⏳ Temporary homes | Time-limited homes (`/tmphome`) with automatic cleanup on expiry |
| 🤝 TPA trust | Auto-accept or auto-deny TPA / TPAHere requests globally or per player |
| 🧭 Rotation restore | Saved homes, warps, and teleport records can preserve facing direction |
| ⚡ RTP performance | Handles concurrent random teleports without lag |

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

By default, extra debug logs are disabled. Target chunk preloading and default target safety checks are also disabled by default; RTP uses its own independent safety logic.

---

## With vs. Without Client Mod

Adding the client mod (optional) improves the experience:

| | Server only | Server + Client |
| --- | :---: | :---: |
| All teleport commands | ✅ | ✅ |
| Clickable chat buttons | ✅ | ✅ |
| Waypoints on Xaero map | — | ✅ |
| Map right-click → teleport | — | ✅ |
| Trusted commands skip confirmation | — | ✅ |
| TPA trust auto-accept / deny | ✅ | ✅ |

---

## Links

[Wiki](https://github.com/EasterGhost/TeleportCommandsFabric/wiki/EN-Home) · [中文文档](README.zh-CN.md) · [Changelog](CHANGELOG.md) · [License](LICENSE.txt)
