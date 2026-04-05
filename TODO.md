## New features

- 添加传送锚点的失效，并据此实现临时锚点设置（需讨论临时锚点的具体行为逻辑）。
- 修改rtp在下界的逻辑：不且仅不tp到y=127且脚下为基岩的方块上。
- 修改rtp的逻辑：引入min_radius。
- 添加共享锚点机制（需讨论共享锚点的具体行为逻辑）。
- 添加多维度的worldspawn。
- 增加tpa距离/维度限制（仍需讨论合理性）。
- 传送的空间朝向复原。
- 添加tpa trust系统。
- home动态配额。

## Improvements

- WaypointCrudService 逻辑原子化与自动化（基于约定的 Key/Log 自动生成）
  - 详细方案：[docs/proposals/crud_refactor.md](file:///f:/LiMuchen/Mods/TeleportCommandsFabric/docs/proposals/crud_refactor.md)
- 传送点查询效率与状态索引优化（引入 $O(1)$ 级 Map 索引）
  - 详细方案：[docs/proposals/storage_optimization.md](file:///f:/LiMuchen/Mods/TeleportCommandsFabric/docs/proposals/storage_optimization.md)
- 状态对象纯化与 `models -> state` 重命名准备（移除对 `storage` 的反向依赖）
  - 详细方案：[docs/proposals/storage_optimization.md](file:///f:/LiMuchen/Mods/TeleportCommandsFabric/docs/proposals/storage_optimization.md)
- 异步序列化与存储扩展性优化（降低主线程序列化负担）
  - 详细方案：[docs/proposals/storage_optimization.md](file:///f:/LiMuchen/Mods/TeleportCommandsFabric/docs/proposals/storage_optimization.md)

## Bug Fix

- none

## Others

- none
