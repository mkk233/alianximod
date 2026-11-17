# 空间切割左腰右拔接入

待统一编译。游戏内效果未验证。实施不改伤害、命中体积、无敌帧、射程、ChainRole、规则链、配置或服务端释放。

## 动作时钟和姿态

`OriginalHumanoid.registerSpaceCut`（约 70–102 行）把 `xueximod:space_cut` 改成 1.6 秒、不循环、混合 0.06 秒。共享五骨只持有单位旋转。0.48 秒的 `draw-release` 仍是原有视觉通知，没有伤害消费者。旧的五骨拔刀欧拉表已删除。

双节臂来自 `assets/xueximod/actions/space_cut_left_waist_v5.json`。`SpaceCutSampler` 在动作属于 `xueximod:space_cut` 时采样。JSON 缺失、解析失败或起始掌心超差时记一次日志并返回空帧，不退回五骨挥砍。

## 绘制覆盖

- 普通与 slim：第三人称读该玩家的 `AbstractClientPlayer.getModelName()`，第一人称同样读 `getModelName()`。`PlayerModel.slim` 是 private，没有直接读。上臂和前臂各 6 像素，宽臂 4、细臂 3，不拉长骨骼。
- 袖层：第三人称按该侧袖部件是否显示；第一人称按 `PlayerModelPart` 袖开关。袖外扩 0.25。
- 护甲：只拆精确类 `HumanoidModel`。胸甲手臂可见时先藏起单节手臂，其余护甲、染色、镶边和闪光仍走原绘制，再补两节。头盔和护腿手臂本就不可见，不拆。自定义的非 `HumanoidModel` 护甲留在单节上臂。第一人称沿用原版，不画护甲。
- 第一人称和第三人称、自己和观察者共用同一次 `ActionSyncS2CPacket` 播放记录。第三人称在整次玩家绘制外推入帧；第一人称在左右手绘制各推入一次。

## 左腰右手和剑挂点

右手掌心、前臂和 `SwordHand` 分开求解。刀轴是剑骨局部 -Y，对应换算后的模型刀向。上臂和前臂各长 6，前臂折叠保留。

剑只认主手 `SwordItem`，否则副手 `SwordItem`。动作期间这份物品一律用右手展示上下文画在 `SwordHand`，并抑制它原来的手持绘制。主手在左时不改主手设置，也不改物品栏。另一侧的非挂载物品留在该侧腕部。没有剑时不生成示意刀，释放条件不变。

返修后挂点满足 `M·R展示 = T`。展示平移被扣掉，模型中心落在掌心。第一人称的 0.1 抵消只从 `LocalPlayer` 的 `xBob`、`xBobO`、`yBob`、`yBobO` 取角，顺序仍是先偏航逆、再俯仰逆。详见 `repair-r1-report.md` 和 `repair-compile-report.md`。

## 清理

帧只活在本次绘制的线程栈里。第三人称和第一人称都在 `finally` 弹出。玩家模型由原有 `ModelPartSnapshot` 恢复。动作结束、被下一次释放替换或离开这次绘制后，单节手臂、袖和手持物品回到原版路径。不改库存，不改主手。

## 检查

已跑 `powershell -NoProfile -File tools/check-name-gate.ps1`，编译错误返修后再次通过。

未跑 `gradlew`、打包、`runClient` 或游戏。待统一编译。下一轮编译进程应使用 `JAVA_HOME=C:/Program Files/Java/jdk-17`、`GRADLE_USER_HOME=C:/Users/baishang/.gradle`，PATH 前置该 JDK 的 bin，保持离线与 `-Xmx3G`，不要用 Cursor 临时空缓存。

## 未实现或未验证

- 非 `HumanoidModel` 的自定义护甲不能拆成两节。
- 物品网格长度跟随该物品自己的展示变换，没有强制成 17.5 像素。自定义模型覆盖的 handheld 展示没有逐物品读取。
- 第一人称视角模型不画护甲。外层 0.1 bob 已在挂点里取逆，游戏画面未看。
- 不继承 `SwordItem` 的武器不会被当成拔刀剑，仍留在原手腕。
- 第一人称、第三人称、slim、袖、护甲染色、左手持剑改到右手、观察者和中断恢复都没有进游戏看过。
