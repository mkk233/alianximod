# implement-01 final

- 状态：实现完成，待统一编译。未做正式复审，未改制作表，未改原批次记录。
- 任务：taskId=projectile-hit-consumption，attemptId=implement-01。
- 批次：沿用待制作-20261031原账本；精确 ASCII batchId 未知。设计 2/2、实现 3/3 均已耗尽。
- 关联：B=fire_spark，当前制作中行 15。
- 角色：本任务首个源码作者。请求模型 cursor/grok-4.7@256k，推理 high。Fast 依据流程现有 priority 配置；本工具未暴露 tier，不声称后端已独立验证。
- 父方：主会话聊天 01a1c9b3-ca26-7a33-91d2-71c2e32bb69b。本回执由该会话的原生等待接收。
- 下属：本执行者未再派 Flash。既有 read-02 已完成且无写入，agent_id=01a1c9b4-b13c-7953-811d-92afa3936660。旧 read-01 失败且无写入。本次没有未收束下属。

## 改动

- `TechniqueHitSpec` 第 160 行增加 `retainAfterEntityHit`，默认 false。只有调用方显式打开才保留弹丸。
- `TechniqueHitEntity` 第 133、602、977–988 行保存该声明，并按结构判断一次性飞行弹丸。命中循环第 451、457 行在本体移除后立即停止。
- `TechniqueHitSettle` 第 103–106 行：普通命中已经成立后走统一 `onExpire`。共享账本仍返回 `PENDING` 时不提前消耗。第 294–297 行让最早接触扫掠同样消耗并停止。
- `BatchSparkEffect` 删除 `burstOnImpact=true`。火苗不再借爆裂提前销毁，实体和地形命中都走一次性弹丸及现有地形规则。
- `BatchFlightBook` 第 75、366–368、434–443 行：火苗视觉按宿主 UUID 绑定；宿主移除时只丢弃自己的视觉并清理自己的飞行记录。
- `BatchCarrierVisual` 第 83–125、360–365 行提供绑定生成、按宿主回收和视觉自身移除时的映射清理。火苗使用 `holdForHost=true`。
- `FireOrbEffect` 第 60 行显式 `retainAfterEntityHit=true`，保留既有直线穿透设计。未改伤害、火种、尺寸或寿命。

## 明确保留

- 陨岩：`retainFlightAfterEntity` 仍为 true，命中实体后继续飞行到地形；其视觉仍使用原绑定，不进入火苗回收。
- 爆裂：`burstOnImpact` 仍走原回调、销毁和范围结算。火苗不再使用该标志。
- 持续与重复命中：`persistentFlow`、`hitInterval > 0`、`endsWhenReleased`、`automaticLifetime` 不消耗。
- 斩击与静止体：没有运动、弧带、跟随光束、钉住、居中体不消耗。陷阱 `trap` 仍等触发后爆开。
- 延迟账本：`settleTally` 返回 `PENDING` 时保留本体，避免伤害尚未入账就消失。
- 未命中、无效、取消或被拒绝的接触不消耗。载荷和湿压仍由 `onExpire` 清理。

## 检查

- 命令：`powershell -NoProfile -File tools/check-name-gate.ps1`。
- 全量退出码：1。8 个问题都在既有客户端文件，不属于本次改动。
- 定向过滤：本次七个源文件无命名问题；过滤命令因全量原退出码仍返回 1，输出为空。
- 编译：无编译权限，待统一编译。未打包，未启动游戏，未做独立测试。

## 写入收束与哈希

写入均已结束。SHA-256 与最后写入时间：

- `FAB51BEA77C563E4ADEB572A981B7E62120527E04A57944C733566F54F3F8AEC`，2026-11-11 16:39:32.218，`BatchSparkEffect.java`。
- `967DBC1E0BEE2A090DC02D7FC256687CD81A9D181F4A617756420B906E0330A8`，2026-11-11 16:41:52.203，`BatchFlightBook.java`。
- `C6F7B55B73C875B15536DF4AF8273F8CCB7E69CA87E7F36E7AEFC3615D5B6D04`，2026-11-11 16:41:52.197，`BatchCarrierVisual.java`。
- `384B1307B9F361D2528DD6C44CCBE0F300CB6C3E540FADA0E1D6F424BBAD25F7`，2026-11-11 16:39:18.124，`TechniqueHitSpec.java`。
- `E6A8AC90C2CA1AB94E61C5C4D4F7007A4E8BB0CDA948610019615D020345195A`，2026-11-11 16:43:19.239，`TechniqueHitEntity.java`。
- `54C4FB88E353C93FE8190DE9A15FCFB604FB9B3925930DBBD35356A92C322AEE`，2026-11-11 16:43:36.263，`TechniqueHitSettle.java`。
- `8E712A44F99C63F45CA57AB2D0CF960119604420E0EEB2EDC80709A2036CB870`，2026-11-11 16:42:32.856，`FireOrbEffect.java`。

其他项目聊天的 idle 或 notLoaded 状态不能单独证明全局收束。本次只确认上述文件已写完；既有未提交改动全部保留，未执行 reset、clean、checkout、commit 或建分支。

## 下一步

父方指定统一编译者后，对最终源码执行 `./gradlew.bat compileJava --offline --no-daemon`。编译和交付条件满足后，再由独立表格写回者处理制作表。
