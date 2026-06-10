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
| 📋 列表增强 | `/homes` 和 `/warps` 支持可点击分页、排序和筛选 |
| ⚙️ 一致体验 | 延迟、冷却、区块预加载、安全检测在所有传送类型间统一生效 |
| ⏳ 临时家 | 支持设置有时限的临时家（`/tmphome`），到期自动清理 |
| 🤝 TPA 信任 | 可按全局或单个玩家自动接受/拒绝 TPA 与 TPAHere 请求 |
| 🧭 朝向复原 | home、warp 和传送记录可保存并恢复玩家朝向 |
| ⚡ RTP 性能 | 多人同时随机传送不卡顿 |

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
/tpc config teleporting restoreRotation true
/tpc reload                     # 手动编辑配置文件后重载
```

---

## 是否安装客户端模组

服务端模组可独立运行。安装客户端模组（可选）可进一步提升体验：

| | 仅服务端 | 服务端 + 客户端 |
| --- | :---: | :---: |
| 全部传送命令 | ✅ | ✅ |
| 聊天框可点击按钮 | ✅ | ✅ |
| Xaero 地图航点显示 | — | ✅ |
| 地图右键直达传送 | — | ✅ |
| 可信命令跳过确认弹窗 | — | ✅ |
| TPA 信任自动接受/拒绝 | ✅ | ✅ |

---

## 链接

[Wiki](https://github.com/EasterGhost/TeleportCommandsFabric/wiki) · [English](README.md) · [Changelog](CHANGELOG.md) · [License](LICENSE.txt)
