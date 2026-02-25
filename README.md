# Teleport Commands

Server-side teleport command mod with multiple teleport features and flexible configuration.

- Chinese documentation: [README.zh-CN.md](README.zh-CN.md)

## Overview

Teleport Commands is a server-side teleport command mod that provides multiple teleport features and a flexible configuration system.

Config file: `config/teleport_commands.json`

Available modules:

- `back` - return to previous death location
- `home` - home system
- `tpa` - player teleport requests
- `warp` - global warp points
- `worldspawn` - world spawn point

---

## Player Commands

The following commands can be used by all players without special permissions.

### Back - Return to Death Location

```bash
/back
/back <disableSafetyCheck>
```

- **Function**: Teleport to your last death location
- **Parameter**: `<disableSafetyCheck>` is `true|false` (optional)

### Home - Home System

```bash
/sethome <name>              # Set a home at current location
/home [name]                 # Teleport to specified home (default home if not specified)
/delhome <name>              # Delete specified home
/renamehome <name> <newName> # Rename a home
/defaulthome <name>          # Set default home
/homes                       # View all homes
```

### TPA - Player Teleport Requests

```bash
/tpa <player>                # Request to teleport to a player
/tpahere <player>            # Request a player to teleport to you
/tpaaccept <player>          # Accept a teleport request from a player
/tpadeny <player>            # Deny a teleport request from a player
```

### Warp - Global Warp Points

```bash
/warp <name>                 # Teleport to a global warp point
/warps                       # View all available warp points
```

### WorldSpawn - World Spawn Point

```bash
/worldspawn                  # Teleport to world spawn
/worldspawn <disableSafety>  # Teleport to world spawn (optional safety check disable)
```

- **Parameter**: `<disableSafety>` is `true|false` (optional)

---

## Administrator Commands

The following commands require OP permission (level 4).

### Enable/Disable Modules

```bash
/teleportcommands enable <module>
/teleportcommands disable <module>
```

### Warp Management

```bash
/setwarp <name>              # Create a global warp point at current location
/delwarp <name>              # Delete a global warp point
/renamewarp <name> <newName> # Rename a global warp point
```

### Teleporting Settings

```bash
/teleportcommands config teleporting delay <seconds>
/teleportcommands config teleporting cooldown <seconds>
```

- **delay** - wait time before teleport
- **cooldown** - time before next teleport can be used

### Back Module Configuration

```bash
/teleportcommands config back deleteAfterTeleport <true|false>
```

- **deleteAfterTeleport** - delete previous location record after teleport

### Home Module Configuration

```bash
/teleportcommands config home max <count>
/teleportcommands config home deleteInvalid <true|false>
```

- **max** - maximum homes per player
- **deleteInvalid** - remove invalid locations (world missing)

### TPA Module Configuration

```bash
/teleportcommands config tpa expireTime <seconds>
```

- **expireTime** - request timeout

### Warp Module Configuration

```bash
/teleportcommands config warp max <count>
/teleportcommands config warp deleteInvalid <true|false>
```

- **max** - maximum number of warps (0 = unlimited)
- **deleteInvalid** - remove invalid locations

### WorldSpawn Module Configuration

```bash
/teleportcommands config worldspawn world <worldId>
```

- **world** - world for spawn (e.g. `minecraft:overworld`, `minecraft:nether`, `minecraft:the_end`)

### Reload Configuration

```bash
/teleportcommands reload
```

Reloads the configuration from file.

---

## Config File Structure

`config/teleport_commands.json`:

```json
{
  "version": 0,
  "teleporting": {
    "delay": 5,
    "cooldown": 5
  },
  "back": {
    "enabled": true,
    "deleteAfterTeleport": false
  },
  "home": {
    "enabled": true,
    "playerMaximum": 20,
    "deleteInvalid": false
  },
  "tpa": {
    "enabled": true,
    "requestExpireTime": 300
  },
  "warp": {
    "enabled": true,
    "maximum": 0,
    "deleteInvalid": false
  },
  "worldSpawn": {
    "enabled": true,
    "world_id": "minecraft:overworld"
  }
}
```

## Quick Reference

### Player Commands

| Feature             | Command                          | Description                       |
| ------------------- | -------------------------------- | --------------------------------- |
| Return to previous  | `/back [true/false]`           | Teleport to last death location   |
| Set home            | `/sethome <name>`              | Set home at current location      |
| Teleport to home    | `/home [name]`                 | Teleport to specified home        |
| Delete home         | `/delhome <name>`              | Delete specified home             |
| Rename home         | `/renamehome <name> <newName>` | Rename a home                     |
| Set default home    | `/defaulthome <name>`          | Set default home                  |
| View all homes      | `/homes`                       | List all set homes                |
| Teleport request    | `/tpa <player>`                | Request to teleport to player     |
| Request player here | `/tpahere <player>`            | Request player to teleport to you |
| Accept request      | `/tpaaccept <player>`          | Accept teleport request           |
| Deny request        | `/tpadeny <player>`            | Deny teleport request             |
| Teleport to warp    | `/warp <name>`                 | Teleport to global warp point     |
| View all warps      | `/warps`                       | List all warp points              |
| Random teleport     | `/wild`                        | Teleport to a random location     |
| World spawn         | `/worldspawn [true/false]`     | Teleport to world spawn           |

### Administrator Commands

| Feature             | Command                                                          | Description                     |
| ------------------- | ---------------------------------------------------------------- | ------------------------------- |
| Enable module       | `/teleportcommands enable <module>`                              | Enable specified module         |
| Disable module      | `/teleportcommands disable <module>`                             | Disable specified module        |
| Set warp            | `/setwarp <name>`                                                | Create global warp point        |
| Delete warp         | `/delwarp <name>`                                                | Delete global warp point        |
| Rename warp         | `/renamewarp <name> <newName>`                                   | Rename global warp point        |
| Teleport delay      | `/teleportcommands config teleporting delay <seconds>`           | Set teleport delay time         |
| Teleport cooldown   | `/teleportcommands config teleporting cooldown <seconds>`        | Set teleport cooldown time      |
| Back delete record  | `/teleportcommands config back deleteAfterTeleport <true\|false>`| Delete record after Back        |
| Home max count      | `/teleportcommands config home max <count>`                      | Set max homes per player        |
| Home delete invalid | `/teleportcommands config home deleteInvalid <true\|false>`      | Auto-delete invalid homes       |
| TPA expire time     | `/teleportcommands config tpa expireTime <seconds>`              | Set request expire time         |
| Warp max count      | `/teleportcommands config warp max <count>`                      | Set max warp points             |
| Warp delete invalid | `/teleportcommands config warp deleteInvalid <true\|false>`      | Auto-delete invalid warp points |
| WorldSpawn world    | `/teleportcommands config worldspawn world <worldId>`            | Set world spawn world           |
| Random teleport radius | `/teleportcommands config wild radius <blocks>`               | Set random teleport radius      |
| Reload config       | `/teleportcommands reload`                                       | Reload configuration file       |

## Teleport Delay vs Cooldown

- **Delay**: wait time after issuing the command before teleporting
- **Cooldown**: wait time after teleporting before the next use

Examples:

- `delay: 0, cooldown: 30` = teleport instantly, then wait 30s
- `delay: 5, cooldown: 0` = wait 5s, then no cooldown
- `delay: 5, cooldown: 10` = wait 5s, then 10s cooldown
- `delay: 0, cooldown: 0` = no limits

## Troubleshooting

### Command does not work

- Make sure you have OP level 4
- Check the command syntax

### Config not saved

- Check server logs for errors
- Check file permissions

### Config resets

- Ensure `config/teleport_commands.json` exists
- Use `/teleportcommands reload` to refresh
