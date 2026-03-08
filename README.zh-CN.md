# Teleport Commands Fabric 使用说明

TeleportCommandsFabric 是一个面向 Fabric 服务端的传送命令模组，提供完整的常用传送能力，并将管理入口集中在同一套命令体系中，便于长期维护。同时提供客户端接口与Xaero地图联动以提高使用体验。

## 模组特色

这个模组围绕服务端日常管理进行设计。玩家常用的 `back`、`home`、`tpa`、`warp`、`worldspawn`、`rtp`/`wild` 在一套规则下运行，管理员也可以通过 `/teleportcommands` 统一控制模块状态和运行参数。支持与Xaero地图系列模组联动以提高使用体验。

### 主要优势

- 功能集中：常见传送场景都在同一个模组里完成。
- 管理统一：启用/禁用、重载、运行时配置都走 `/teleportcommands`。
- 调整方便：常用参数可在线修改，减少手动改 JSON 的频率。
- 行为一致：多类传送命令共享统一的延迟与冷却逻辑。
- 支持联动：可将 `home` 与 `warp` 同步到 Xaero 航点，便于直接在地图上查看和传送。

## 客户端体验

服务端传送命令的可用性不依赖客户端是否安装本模组，客户端不安装时也可以正常使用 `back`、`home`、`warp`、`tpa`、`worldspawn` 和 `rtp` 等功能。客户端安装本模组后，传送相关交互会更顺手；若再配合 Xaero 地图模组，则可以进一步获得 `home` 和 `warp` 的地图航点联动。

更具体的安装组合与体验差异，可查看 [功能总览](wiki/TeleportCommandsFabric.wiki/2-Features-Overview.md) 和 [Xaero 集成模块](wiki/TeleportCommandsFabric.wiki/5-7-Xaero.md)。

## 快速开始

1. 将模组放入服务端 `mods/` 目录。
2. 启动一次服务端，生成配置文件 `config/teleport_commands.json`。
3. 执行 `/teleportcommands help` 确认命令已注册。
4. 使用 `/teleportcommands config` 系列命令修改配置，或手动修改配置文件后执行 `/teleportcommands reload`。

## 文档导航（Wiki）

详细说明集中维护在 Wiki。README 主要用于项目介绍和快速入口。

### 入门

- [首页](wiki/TeleportCommandsFabric.wiki/Home.md)
- [快速开始](wiki/TeleportCommandsFabric.wiki/1-Quick-Start.md)
- [功能总览](wiki/TeleportCommandsFabric.wiki/2-Features-Overview.md)

### 使用

- [命令说明](wiki/TeleportCommandsFabric.wiki/3-Commands.md)
- [配置文件说明](wiki/TeleportCommandsFabric.wiki/4-Configuration.md)
- [模块细节](wiki/TeleportCommandsFabric.wiki/5-Module-Details.md)

### 维护

- [权限与访问](wiki/TeleportCommandsFabric.wiki/6-Permissions-and-Access.md)
- [数据与存储](wiki/TeleportCommandsFabric.wiki/7-Data-and-Storage.md)
- [故障排查](wiki/TeleportCommandsFabric.wiki/8-Troubleshooting.md)
- [常见问题](wiki/TeleportCommandsFabric.wiki/9-FAQ.md)

## 配置与管理入口

- 配置文件路径：`config/teleport_commands.json`
- 运行时配置命令：`/teleportcommands config ...`
- 重载命令：`/teleportcommands reload`

字段解释和示例请查看 [配置文件说明](wiki/TeleportCommandsFabric.wiki/4-Configuration.md)。
