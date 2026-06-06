# Teleport Commands Fabric

> 面向 Fabric 服务端的统一传送工具包 — 所有命令，一个管理面板，无需重启。

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.7%2B-brightgreen)]()
[![License](https://img.shields.io/badge/License-GPL--3.0-blue)](LICENSE.txt)

[English](README.md) · [Wiki](https://github.com/EasterGhost/TeleportCommandsFabric/wiki)

---

## 功能概览

| 功能 | 说明 |
| --- | --- |
| 🏠 `/home` `/warp` `/back` `/tpa` `/rtp` `/worldspawn` | 服务器所需的全部传送命令 |
| 🎛️ `/tpc` | 统一管理入口：启停模块、调整上限、重载配置 — 无需手动编辑 JSON |
| 🗺️ Xaero 联动 | 家和传送点在地图上显示。删除地图航点时，服务端同步隐藏 |
| ⚙️ 统一传送规则 | 共享延迟/冷却，可选目标预加载，可配置传送效果，RTP 使用独立安全检查 |
| ⏳ 临时家 | 支持设置有时限的临时家（`/tmphome`），到期自动清理且时长可配置 |
| ⚡ 高并发处理 | 目标传送分批执行，RTP 可处理多人同时随机传送，减少不必要的主线程压力 |

---

## 快速开始

1. 将模组放入服务端 `mods/` 目录。
2. 启动一次 → 自动生成 `config/teleport_commands.json`。
3. 执行 `/tpc help` 确认注册，`/tpc status` 查看模块状态。

常用管理命令速查：

```
/tpc status                     # 查看各模块启用状态
/tpc enable rtp                 # 启用一个模块（rtp）
/tpc config home max 20         # 在线修改home上限
/tpc config teleporting preload true
/tpc debug true                 # 排查问题时开启额外日志
/tpc reload                     # 手动编辑配置文件后重载
```

额外调试日志默认关闭。目标区块预加载和默认目标安全检查也默认关闭；RTP 使用独立的随机落点安全检查。

---

## 是否安装客户端模组

服务端模组可独立运行。安装客户端模组（可选）可进一步提升体验：

| | 仅服务端 | 服务端 + 客户端 |
| --- | :---: | :---: |
| 全部传送命令 | ✅ | ✅ |
| 聊天框可点击按钮 | ✅ | ✅ |
| Xaero 地图航点显示 | — | ✅ |
| 地图右键直达传送 | — | ✅ |
| 受信命令发送体验更顺滑 | — | ✅ |

---

## 链接

[Wiki](https://github.com/EasterGhost/TeleportCommandsFabric/wiki) · [English](README.md) · [Changelog](CHANGELOG.md) · [License](LICENSE.txt)
