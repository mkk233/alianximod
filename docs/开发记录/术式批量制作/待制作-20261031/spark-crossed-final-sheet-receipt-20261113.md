# 火苗 v3.1 确认归档写表回执

- batchId：`待制作-20261031`
- taskId：`fire_spark_crossed_launch`
- attemptId：`20261113-sheet-spark-cross-02`
- stage：`delivery`
- 操作角色：`sheet-write`
- 请求模型：`cursor/grok-4.7@500k` · high + Fast。沿用客户端 `service_tier="priority"` 配置；宿主未独立返回验证，不记成后端已验证事实。
- 真实直接父代理：主会话。父方接收时间由主会话收到本 final 后填写。
- 结果：`fire_spark` 已从制作中第 28 行迁入待验收第 23 行。表格同步已完成。尚未游戏实测，不能迁入已完成。

## 前置与额度

- `20261113-art-spark-cross-archive-01`：操作角色 `art`（`antigravity/gemini-4-carbon`），已完成并关闭。真实代理 ID 未在回执暴露，不补猜。回执 `docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-art-archive-20261113.md`，9112 字节，SHA256 `92CB68A347C47535F8B42268E1EBDE1E475182D6B3843EB8AA07EB4CA1DBFC6F`。
- `20261113-integrate-spark-cross-01`：代理 `01a1d33e-3c20-7043-b0bc-3968034b071a`，已完成并关闭。回执 `docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-v31-integration-report-20261113.md`，7093 字节，SHA256 `2F4F93A63177324AF6DBA46685775CD7DA8A12619BB5AD8283328A46B4FA6072`。
- `20261113-compile-spark-cross-02`：代理 `01a1d311-b1b5-7a33-8295-e2010c6d0f5c`，已完成并关闭。`compileJava` 于 2026-11-13 12:04:01.801 至 12:04:26.039 实际执行 javac，退出码 `0`。
- `20261113-compile-spark-cross-03`：指定统一编译者直接执行，子代理标识不适用，已完成并关闭。同一命令于 2026-11-13 12:56:27.882 至 12:56:39.566 运行，退出码 `0`；`:compileJava` 为 `UP-TO-DATE`。回执 `docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-compile-03-20261113.md`，11930 字节，SHA256 `1AFF9CE2B95558A3EE625D33D7490B127B172FEE19C0913AEA2EB0F35CD1901B`。
- 复审额度：设计 `2/2`、实现 `3/3`。本阶段额度已用完，返修结果未追加复审。

## 第 8 节核对

- 功能实现、必要对齐和相关写入已收束。
- 正式美术已归档到 `docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/`，用户同版本确认原话为「采用」。
- 运行贴图 `assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png` 为 5005 字节，SHA256 `672DA9132E7E91A83636836A62ADE4778781D51592A541041C511653238DEC5E`；`fire_spark.png.mcmeta` 为 64 字节，SHA256 `B2CAA414505E1C556CF68460294471D97198A50F3D69ED1FD9B717510C0D6F4A`。`.bbmodel` 与示意辅助未进入运行资源。
- `compile-03` 覆盖最后一次相关写入，退出码 `0`。本次 `:compileJava` 是 `UP-TO-DATE`，不改写成重新执行 javac。
- 详细需求快照已写入 `docs/开发记录/术式批量制作/待制作-20261031/spark-crossed-v31-requirements-snapshot-20261113.json`，23359 字节，SHA256 `966CA5419FBEAAF56E4F387A39E797E5CE6580B52609A8D3BA02A2594D498166`。

## 表迁移

- 读取基线：SHA256 `9F81EFB3A5E614F180444CB7F51858A7CD1D8C5275106CDE4D4B5BB735835C21`，178433 字节，LastWriteTime `2026-11-13 12:20:59.331`。实时重开为已完成 37、待制作 0、制作中 25、待验收 16，总数 78。
- 备份：`docs/开发记录/术式批量制作/待制作-20261031/备份/spark-crossed-v31-writeback-术式制作表-20261113-130713-1.xlsx`，哈希等于基线。
- 提交前：无 `EXCEL.exe`、无 `~$` 锁文件，独占读取哈希仍为基线。
- 提交后：2026-11-13 13:07:13.390，179499 字节，SHA256 `AB344CCAAFBE723AD0ED6C6DD0834EB3B264EC5184227F655FAE9FC1B0F12C26`。
- 重开：已完成 37、待制作 0、制作中 24、待验收 17，总数 78，四页 `B` 唯一。`fire_spark` 仅位于待验收第 23 行。制作中已无该编号。资源状态明细本来没有 `fire_spark`，保持原样。待制作原始数据第 23 行历史档案未动。
- 字段：`N` 28 字，`O` 60 字，`DV` 保留全部 2205 字前缀并追加 2026-11-13 阶段记录与待验收流程标记。其余列原样保留。
- 相对备份变化部分：仅 `xl/worksheets/sheet3.xml`、`xl/worksheets/sheet4.xml`。样式、数据验证、第 2 行批注、`xl/workbook.xml`、数据表、资源状态明细、待制作原始数据未变。

## 实测四行

- 编号 / 名称：`fire_spark` / 火苗
- 前置：创造栏或给予测试法杖；装配火苗。运行贴图为 `assets/xueximod/textures/effect/fire_spark_v3/fire_spark.png`，并有同目录 `fire_spark.png.mcmeta`。正式三维归档为 `docs/资源库/特效/火焰/火苗_十字穿插直射_v3.1/`。基础法杖仍是 6 槽。
- 步骤：进入世界后分别做第一人称原地直射、斜射、高低仰角观察。再看命中或撞块后火苗消失、目标约 20 tick 原版燃烧，以及基础 2 点火种。
- 预期：`CameraFacingTag.NONE`，按发射方向锁定 `±45°` 十字穿插双面片，火头朝前、火尾朝后。第一人称在眼下 `0.245` 格展开饱满 `×` 双翼，不退化、不随镜头转圈。命中伤害 1、火种 2、点燃 20 tick，碰撞、弹道和留焰采样不变。用户反馈：（留空待填）

## 边界

- 未修改 `src/`、`docs/资源库/` 或其他批次目录。
- 未运行 `gradlew`，未打包，未启动游戏，未派子代理。
- 文件句柄已关闭。
