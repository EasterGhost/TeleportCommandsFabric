# Teleport Commands - 使用文档

## 概述

Teleport Commands 是一个服务端传送命令模组，提供多种传送功能和灵活的配置系统。

权限要求：所有配置命令需要 OP 权限（等级4）。

配置文件：`config/teleport_commands.json`

## 命令模块

### 启用/禁用模块

```bash
/teleportcommands enable <模块名>
/teleportcommands disable <模块名>
```

可用模块：

- `back` - 返回上一位置
- `home` - 家系统
- `tpa` - 玩家传送请求
- `warp` - 全局传送点
- `worldspawn` - 世界生成点

## 玩家命令

### Back

```bash
/back
/back <是否禁用安全检查>
```

`<是否禁用安全检查>` 为 `true|false`。

### Home

```bash
/sethome <名称>
/home [名称]
/delhome <名称>
/renamehome <名称> <新名称>
/defaulthome <名称>
/homes
```

### TPA

```bash
/tpa <玩家>
/tpahere <玩家>
/tpaaccept <玩家>
/tpadeny <玩家>
```

### Warp

```bash
/warp <名称>
/warps
/setwarp <名称>
/delwarp <名称>
/renamewarp <名称> <新名称>
```

`setwarp` / `delwarp` / `renamewarp` 需要管理员权限。

### WorldSpawn

```bash
/worldspawn
/worldspawn <是否禁用安全>
```

`<是否禁用安全>` 为 `true|false`。

### 传送系统配置

```bash
/teleportcommands config teleporting delay <秒数>
/teleportcommands config teleporting cooldown <秒数>
```

- `delay` - 传送延迟时间，玩家执行命令后等待多久才传送
- `cooldown` - 传送冷却时间，传送完成后多久才能再次使用

### Back 命令配置

```bash
/teleportcommands config back deleteAfterTeleport <true|false>
```

- `deleteAfterTeleport` - 传送后是否删除上一位置记录

### Home 命令配置

```bash
/teleportcommands config home max <数量>
/teleportcommands config home deleteInvalid <true|false>
```

- `max` - 每个玩家可设置的家数量上限
- `deleteInvalid` - 自动删除无效位置（世界不存在时）

### TPA 命令配置

```bash
/teleportcommands config tpa expireTime <秒数>
```

- `expireTime` - 传送请求的有效期，超时自动过期

### Warp 命令配置

```bash
/teleportcommands config warp max <数量>
/teleportcommands config warp deleteInvalid <true|false>
```

- `max` - 全服传送点数量上限（0 = 无限制）
- `deleteInvalid` - 自动删除无效位置

### WorldSpawn 命令配置

```bash
/teleportcommands config worldspawn world <世界ID>
```

- `world` - 世界生成点所在世界（如 `minecraft:overworld`、`minecraft:nether`、`minecraft:the_end`）

### 重新加载配置

```bash
/teleportcommands reload
```

从配置文件重新加载设置。

## 配置文件结构

`config/teleport_commands.json`：

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

## 配置参考

| 功能           | 命令                                                              |
| -------------- | ----------------------------------------------------------------- |
| 启用模块       | `/teleportcommands enable <模块>`                                 |
| 禁用模块       | `/teleportcommands disable <模块>`                                |
| 传送延迟       | `/teleportcommands config teleporting delay <秒>`                 |
| 传送冷却       | `/teleportcommands config teleporting cooldown <秒>`              |
| Back删除记录   | `/teleportcommands config back deleteAfterTeleport <true\|false>` |
| Home数量上限   | `/teleportcommands config home max <数量>`                        |
| Home删除无效   | `/teleportcommands config home deleteInvalid <true\|false>`       |
| TPA过期时间    | `/teleportcommands config tpa expireTime <秒>`                    |
| Warp数量上限   | `/teleportcommands config warp max <数量>`                        |
| Warp删除无效   | `/teleportcommands config warp deleteInvalid <true\|false>`       |
| WorldSpawn世界 | `/teleportcommands config worldspawn world <世界ID>`              |
| 重载配置       | `/teleportcommands reload`                                        |

## 传送延迟与冷却

- **延迟（delay）**：玩家执行传送命令后的等待时间
- **冷却（cooldown）**：传送完成后再次使用传送的间隔时间

两者相互独立：

- `delay: 0, cooldown: 30` = 立即传送，但30秒内不能再次使用
- `delay: 5, cooldown: 0` = 等待5秒传送，传送后可立即再次使用
- `delay: 5, cooldown: 10` = 等待5秒传送，传送后10秒内不能再次使用
- `delay: 0, cooldown: 0` = 无任何限制

## 故障排除

### 命令无效

- 检查是否有 OP 权限（等级4）
- 验证命令语法是否正确

### 配置未保存

- 查看服务器日志中的错误信息
- 检查配置文件权限

### 配置被重置

- 确认 `config/teleport_commands.json` 文件存在
- 使用 `/teleportcommands reload` 重载配置
