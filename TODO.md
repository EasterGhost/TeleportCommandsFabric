## 1.7 Release Scope

### Core targets

- 传送锚点失效 / 临时锚点
  - 目标：基于现有 `expiredTime` 完成最小可用闭环。
  - 范围：设置过期时间、列表过滤、传送前过期判定、存储迁移、文档说明。

- RTP 下界基岩顶修正
  - 目标：避免传送到 `y=127` 且脚下为基岩的方块上。
  - 性质：小范围行为修正，适合作为 `1.7` 的 bug fix。

- RTP `min_radius`
  - 目标：为 RTP 增加最小半径约束。
  - 范围：配置项、`/tpc config rtp ...` 管理入口、搜索逻辑约束、文档说明。

### Optional for 1.7

- WaypointCrudService 逻辑原子化与自动化
  - 性质：内部清理。
  - 详细方案：[docs/proposals/crud_refactor.md](file:///f:/LiMuchen/Mods/TeleportCommandsFabric/docs/proposals/crud_refactor.md)

## 1.8+ Candidates

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
  - 详细方案：[docs/proposals/storage_optimization.md](file:///f:/LiMuchen/Mods/TeleportCommandsFabric/docs/proposals/storage_optimization.md)

- 状态对象纯化与 `models -> state` 重命名准备
  - 详细方案：[docs/proposals/storage_optimization.md](file:///f:/LiMuchen/Mods/TeleportCommandsFabric/docs/proposals/storage_optimization.md)

- 异步序列化与存储扩展性优化

  - 详细方案：[docs/proposals/storage_optimization.md](file:///f:/LiMuchen/Mods/TeleportCommandsFabric/docs/proposals/storage_optimization.md)

## Needs Discussion

- 增加 TPA 距离 / 维度限制
  - 需要先明确玩法合理性与默认策略。

- 传送的空间朝向复原
  - 需要先明确是否扩展位置状态模型，以及与已有传送记录的关系。

## Release Prep

- 修正文档中关于 `/teleportcommands` 兼容别名的描述
  - 当前代码只注册 `/tpc`，README / Wiki 文案需要保持一致。
