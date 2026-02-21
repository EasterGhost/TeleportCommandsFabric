# Teleport Commands - 使用文档

服务端传送命令模组，具有多种传送功能和灵活的配置选项。

## 概述

Teleport Commands 是一个服务端传送命令模组，提供多种传送功能和灵活的配置系统。

配置文件：`config/teleport_commands.json`

可用模块：

- `back` - 返回上一死亡位置
- `home` - 家系统
- `tpa` - 玩家传送请求
- `warp` - 全局传送点
- `worldspawn` - 世界生成点

---

## 玩家命令

以下命令所有玩家均可使用，无需特殊权限。

### Back - 返回死亡位置

```bash
/back
/back <是否禁用安全检查>
```

- **功能**：传送到上一次死亡的位置
- **参数**：`<是否禁用安全检查>` 为 `true|false`（可选）

### Home - 家系统

```bash
/sethome <名称>          # 在当前位置设置一个家
/home [名称]             # 传送到指定的家（不指定则传送到默认家）
/delhome <名称>          # 删除指定的家
/renamehome <名称> <新名称>  # 重命名家
/defaulthome <名称>      # 设置默认家
/homes                   # 查看所有已设置的家
```

### TPA - 玩家传送请求

```bash
/tpa <玩家>              # 请求传送到指定玩家
/tpahere <玩家>          # 请求指定玩家传送到你这里
/tpaaccept <玩家>        # 接受指定玩家的传送请求
/tpadeny <玩家>          # 拒绝指定玩家的传送请求
```

### Warp - 全局传送点

```bash
/warp <名称>             # 传送到指定的全局传送点
/warps                   # 查看所有可用的传送点
```

### WorldSpawn - 世界生成点

```bash
/worldspawn                   # 传送到世界生成点
/worldspawn <是否禁用安全>    # 传送到世界生成点（可选择禁用安全检查）
```

- **参数**：`<是否禁用安全>` 为 `true|false`（可选）

---

## 管理员命令

以下命令需要 OP 权限（等级4）。

### 启用/禁用模块

```bash
/teleportcommands enable <模块名>
/teleportcommands disable <模块名>
```

### Warp 管理

```bash
/setwarp <名称>              # 在当前位置创建全局传送点
/delwarp <名称>              # 删除指定的全局传送点
/renamewarp <名称> <新名称>  # 重命名全局传送点
```

### 传送设置

```bash
/teleportcommands config teleporting delay <秒数>
/teleportcommands config teleporting cooldown <秒数>
```

- **delay** - 传送延迟时间，玩家执行命令后等待多久才传送
- **cooldown** - 传送冷却时间，传送完成后多久才能再次使用

### Back 模块配置

```bash
/teleportcommands config back deleteAfterTeleport <true|false>
```

- **deleteAfterTeleport** - 传送后是否删除上一位置记录

### Home 模块配置

```bash
/teleportcommands config home max <数量>
/teleportcommands config home deleteInvalid <true|false>
```

- **max** - 每个玩家可设置的家数量上限
- **deleteInvalid** - 自动删除无效位置（世界不存在时）

### TPA 模块配置

```bash
/teleportcommands config tpa expireTime <秒数>
```

- **expireTime** - 传送请求的有效期，超时自动过期

### Warp 模块配置

```bash
/teleportcommands config warp max <数量>
/teleportcommands config warp deleteInvalid <true|false>
```

- **max** - 全服传送点数量上限（0 = 无限制）
- **deleteInvalid** - 自动删除无效位置

### WorldSpawn 模块配置

```bash
/teleportcommands config worldspawn world <世界ID>
```

- **world** - 世界生成点所在世界（如 `minecraft:overworld`、`minecraft:nether`、`minecraft:the_end`）

### 重新加载配置

```bash
/teleportcommands reload
```

从配置文件重新加载设置。

---

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

## 命令速查

### 玩家命令速查

| 功能         | 命令                            | 说明                   |
| ------------ | ------------------------------- | ---------------------- |
| 返回上一位置 | `/back [true/false]`          | 传送到上一次死亡的位置 |
| 设置家       | `/sethome <名称>`             | 在当前位置设置家       |
| 传送到家     | `/home [名称]`                | 传送到指定的家         |
| 删除家       | `/delhome <名称>`             | 删除指定的家           |
| 重命名家     | `/renamehome <名称> <新名称>` | 重命名家               |
| 设置默认家   | `/defaulthome <名称>`         | 设置默认家             |
| 查看所有家   | `/homes`                      | 列出所有已设置的家     |
| 传送请求     | `/tpa <玩家>`                 | 请求传送到玩家         |
| 请求玩家来   | `/tpahere <玩家>`             | 请求玩家传送到你这里   |
| 接受请求     | `/tpaaccept <玩家>`           | 接受传送请求           |
| 拒绝请求     | `/tpadeny <玩家>`             | 拒绝传送请求           |
| 传送到Warp   | `/warp <名称>`                | 传送到全局传送点       |
| 查看所有Warp | `/warps`                      | 列出所有传送点         |
| 世界生成点   | `/worldspawn [true/false]`    | 传送到世界生成点       |

### 管理员命令速查

| 功能               | 命令                                                                 | 说明                       |
| ------------------ | -------------------------------------------------------------------- | -------------------------- |
| 启用模块           | `/teleportcommands enable <模块>`                                  | 启用指定功能模块           |
| 禁用模块           | `/teleportcommands disable <模块>`                                 | 禁用指定功能模块           |
| 设置Warp           | `/setwarp <名称>`                                                  | 创建全局传送点             |
| 删除Warp           | `/delwarp <名称>`                                                  | 删除全局传送点             |
| 重命名Warp         | `/renamewarp <名称> <新名称>`                                      | 重命名全局传送点           |
| 传送延迟           | `/teleportcommands config teleporting delay <秒>`                  | 设置传送延迟时间           |
| 传送冷却           | `/teleportcommands config teleporting cooldown <秒>`               | 设置传送冷却时间           |
| Back删除记录       | `/teleportcommands config back deleteAfterTeleport <true \| false>` | 传送后是否删除上一位置记录 |
| Home数量上限       | `/teleportcommands config home max <数量>`                         | 设置玩家最多可设置的家     |
| Home删除无效传送点 | `/teleportcommands config home deleteInvalid <true \| false>`       |                            |
| TPA过期时间        | `/teleportcommands config tpa expireTime <秒>`                     | 设置传送请求有效期         |
| Warp数量上限       | `/teleportcommands config warp max <数量>`                         | 设置全服传送点数量上限     |
| Warp删除无效传送点 | `/teleportcommands config warp deleteInvalid <true \| false>`       | 自动删除无效传送点         |
| WorldSpawn世界     | `/teleportcommands config worldspawn world <世界ID>`               | 设置世界生成点所在世界     |
| 重载配置           | `/teleportcommands reload`                                         | 重新加载配置文件           |

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
