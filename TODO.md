## Current Decisions

- [ ] CLI 列表命令采用 Minecraft style command/subcommand，不使用 shell-style `--` 参数。

  - `/warps`、`/homes` 等分页列表使用固定顺序子命令。
  - 第一版不支持乱序输入；UI 生成的点击命令也统一输出规范顺序。
  - 推荐语义：`/warps [page] filter <prefix|dimension> <value> sort <name|sequence> [asc|desc]`。
  - `prefix` 明确限定为单字符首字母筛选；`dimension` 只作为筛选，不作为排序字段。
  - `sequence` 表示添加顺序：新建位置初始为 `-1`，加入 profile 时分配 `max(sequence)+1`，更新时保留旧序号。

## 2.0 Release Scope

### Core targets

- [x] 建立新的 NBT 存储层骨架

  - 已完成 `storage/player`、`storage/global`、`storage/record`、`storage/schema` 分层。
  - 已完成公共支持层：`NbtFileIO`、`ProfileLifecycleSupport`。

- [x] 完成玩家档案存储系统（`player`）

  - 已完成 `PlayerProfile`、`PlayerProfileNbtCodec`、`PlayerProfileIO`、`PlayerProfileLifecycle`、`PlayerProfileManager`。
  - 已完成玩家加入 / 退出事件接入。
  - 已完成按需加载、异步保存、定时 flush、延迟卸载。

- [x] 完成全局档案存储系统（`global`）

  - 已完成 `GlobalProfile`、`GlobalProfileNbtCodec`、`GlobalProfileIO`、`GlobalProfileLifecycle`、`GlobalProfileManager`。
  - 已完成服务端启动时加载、关闭时 flush。
  - 已完成单线程异步 IO 执行模型。

- [x] 完成轻量记录系统（`record`）

  - 已完成死亡点与传送前位置记录模型。
  - 已完成 `recorded_locations.dat` 的 NBT 读写。
  - 已完成服务端启动加载、关闭保存。

- [ ] 完成旧存储向 2.0 NBT 存储的迁移器

  - `storage.json` -> `player/*.dat`
  - `storage.json` -> `global.dat`
  - 旧的死亡点 / 传送前位置 -> `recorded_locations.dat`

- [ ] 将业务模块切换到新 storage

  - `home` 统一通过 `PLAYER_PROFILE_MANAGER.submit(...)` 修改。
  - `warp` 统一通过 `GLOBAL_PROFILE_MANAGER.submit(...)` 修改。
  - `/back` 统一读取 `RECORDED_LOCATION_MANAGER`。

- [ ] 完成 `record` 与 `/back`、传送流程的正式联动

  - 死亡点读取接到新记录系统。
  - 传送前位置写入接到实际传送链路。
  - 明确 `/back death` 与 `/back tp` 的优先级和提示文案。

- [ ] 迁移旧模块对旧 `StorageManager` 的依赖

  - 逐步移除旧 JSON 存储路径。
  - 清理仍依赖旧存储的模块接线。
  - 评估何时移除 `StorageManager.tick()`。

- [ ] 完成 2.0 版本验证与收尾

  - 用真实命令链路验证 `player/global/record` 三套存储。
  - 验证停服 flush、重启恢复、单人多次开关存档。
  - 补齐 Wiki / README 中与 2.0 行为相关的描述。

## Small Optimizations

- ✅ 启动/关闭时并行化存储 Manager

  - 启动：`loadStorageManagers()` 同时提交 `globalLoad` 和 `recordLoad`，`allOf.join()` 等待两者完成。
  - 关闭：`shutdownStorageManagers()` 三路并行 + `allOf.join()`，本地引用捕获防 TOCTOU。
  - 涉及文件：`TeleportCommands.java`（`initializeMod` / `shutdown*` 方法）。

## 2.1+ Candidates

- 共享锚点机制

  - 涉及权限、归属、可见性、Xaero 同步与存储模型，建议单独版本处理。
- 多维度 WorldSpawn

  - 涉及配置结构、命令语义与默认行为，建议单独版本处理。
- TPA trust 系统

  - 会改变现有 TPA 交互模型，建议与权限设计一起处理。
- home 动态配额

  - 涉及权限组、玩家覆盖与配置来源，建议后置。
- warp 锚点靠近解锁

  - 涉及复杂交互逻辑和数据存储方式改变。
- 传送点查询效率与状态索引优化

  - 详细方案：[docs/proposals/storage_optimization.md](docs/proposals/storage_optimization.md)
- 状态对象纯化与 `models -> state` 重命名准备

  - 详细方案：[docs/proposals/storage_optimization.md](docs/proposals/storage_optimization.md)
- 异步序列化与存储扩展性优化

  - 详细方案：[docs/proposals/storage_optimization.md](docs/proposals/storage_optimization.md)

## Needs Discussion

- 配置异步保存线程池的停服收尾

  - 讨论是否为 `ConfigManager` 增加 shutdown / flush 入口，确保 pending config save 在关服时完成并关闭 executor。

- 增加 TPA 距离 / 维度限制

  - 需要先明确玩法合理性与默认策略。
- 传送的空间朝向复原

  - 需要先明确是否扩展位置状态模型，以及与已有传送记录的关系。

## Release Prep

- 修正文档中关于 `/teleportcommands` 兼容别名的描述
  - 当前代码只注册 `/tpc`，README / Wiki 文案需要保持一致。
