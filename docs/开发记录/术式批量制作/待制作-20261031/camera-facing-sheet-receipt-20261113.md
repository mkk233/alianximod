# 持续面向玩家标签与留焰迁回制作中写表回执

- batchId：`待制作-20261031`
- taskId：`camera_facing_tag_and_trail_fix`
- attemptId：`20261113-sheet-facing-tag-01`
- stage：`sheet-write`
- 操作角色：sheet-write（指定唯一制作表与批次记录写回者）
- 请求模型：`cursor/grok-4.7@500k` · high + Fast。沿用客户端 `service_tier="priority"` 配置；后端未单独返回 Fast 验证结果，不把该配置写成已验证事实。
- 可观察返回身份 / 后端追踪：未暴露，记未知。
- 原生工具：本执行者直接完成，未再派生子代理。子代理标识不适用。
- 真实直接父代理：主会话。父方接收时间由主会话收到本 final 后填写，本回执不代填。
- 关联 B：`fire_flame_trail`（待验收迁回制作中）、`rule_surface_anchor`（留制作中，只追加搁置记录）
- 额度：设计 2/2、实现 3/3，均已用完。本单未追加正式正确性复审，未重置计数。返修结果未追加复审。

## 结果

`fire_flame_trail` 已从待验收第 23 行迁回制作中第 24 行。`N` 恢复最近一次已批准的 572 字详细需求；`O` 在原文后合并 `CameraFacingTag.NONE` 的世界弹道切向规则；`DV` 保留待验收全部原文并追加本次设计反馈、状态纠正和制作中流程标记。

`rule_surface_anchor` 仍在制作中第 3 行。`N`、`O` 及其他列未改，`DV` 只追加 2026-11-13 搁置记录。其余 9 项既有面向镜头特效未迁页、未改单元格。

## 基线与提交

- 基线：SHA256 `06055D2AA6231BD185C48CE89A37C6D3E36E0E9B41821712AFFB19E7C98A1A38`，178752 字节，LastWriteTime `2026-11-13 02:22:22.978`。提交前无 `EXCEL.exe`、无锁文件，独占读取哈希仍等于基线。
- 备份：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/备份/camera-facing-writeback-术式制作表-20261113-034426-1.xlsx`，SHA256 等于基线。
- 候选：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/camera-facing-writeback-candidate-preserved.xlsx`，SHA256 `19BAC0145187FC842A6A1B88E3D7FBE3A74F41576C41DAE8C88A95C9A67EAE2D`。
- 提交时间：2026-11-13 03:45:43.176。
- 提交后：`D:/alianximod/docs/术式制作表.xlsx`，182886 字节，LastWriteTime `2026-11-13 03:45:43.176`，SHA256 `19BAC0145187FC842A6A1B88E3D7FBE3A74F41576C41DAE8C88A95C9A67EAE2D`，与候选逐字节一致。
- 重开核对：已完成 37、待制作 3、制作中 21、待验收 17，总数 78，四页 `B` 唯一。待验收已无留焰。制作中第 24 行是留焰，第 3 行仍是地表定位。
- 相对备份只改 `xl/workbook.xml`、`xl/worksheets/sheet3.xml`、`xl/worksheets/sheet4.xml`。样式、数据验证、表头批注、数据表、资源状态明细和待制作原始数据未改。

## 标签与不迁表范围

- `CAMERA_PLANE`：`SPARK`、`SEED`、`KINDLE`、`DETONATE`、`DUST`、`FENTIAN_SKY_FLAME`、`FIRE_SEED_ORBIT_FLAME`、`WATER_PRISON_SHELL`。
- `UPRIGHT_CAMERA_YAW`：`SWORD_RETURN_ALL_VORTEX`。
- `NONE`：留焰 `TRAIL`、地表定位 `SURFACE`，以及 `SHOCK`、空贴图、未登记或 null。
- 上述 9 项既有面向镜头特效只由底座代码接入，本次不迁表、不改表行。

## 依赖回执

- 实现：代理 `01a1d12d-19e7-7170-921a-5edf84f2cb20`（Kant），`20261113-impl-facing-tag-02`，回执 `camera-facing-tag-and-trail-fix-20261113.md`。
- 编译：代理 `01a1d13c-27a0-7f72-98fe-b2493dcd3e63`（Arendt），`20261113-compile-facing-tag-01`。命令全文 `.\gradlew.bat compileJava --offline --no-daemon`，2026-11-13 03:31:31.694 至 03:31:59.878，退出码 0，晚于最后一次相关写入。回执 `camera-facing-compile-20261113.md`。
- 本写表单没有运行 `gradlew`，没有修改 `src/` 或 `docs/资源库/`，没有启动游戏。

## 实测四行

- 编号 / 名称：`fire_flame_trail` / 留焰
- 前置：创造栏或给予测试法杖；装配一个会移动的攻击载体（火苗、火球或陨岩），再在同一链上装留焰。运行贴图为 `assets/xueximod/textures/effect/flame_trail_v3/flame_trail.png`，正式归档为 `docs/资源库/特效/火焰/留焰_真实翻腾尾迹_v3/`。基础法杖仍是 6 槽。
- 步骤：进入世界后释放带留焰的载体。让火径先擦到一个生物，再让载体本体碰到同一个生物。再分别以斜向、高低仰角和顺着视线方向发射，观察整段尾迹。
- 预期：载体沿真实路径留下约 40 tick、直径 0.5 格的火焰轨迹，不产种、不放原版火、不破坏地形；同一施法上下文和同一目标只结算一次命中。采样面为 `CameraFacingTag.NONE`，沿三维弹道切向直立拼接，连续成带，不再断成水平锯齿横条。用户反馈：（留空待填）

## 收束

制作表、备份、返修快照、实测清单、批次记录和本回执均已写入并关闭句柄。没有下属 Flash，没有仍在运行的写入子进程。下一步由父会话向用户汇报标签清单、留焰断线修复、编译与写表结果，并附实测四行。
