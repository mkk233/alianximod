# 水刃弧形水斩接入结果

日期：2026-10-25。按 `brain-decision.md` 方案甲实现。攻击仍是 6 tick，消退仍是 4 tick。没有客户端余留，没有改战斗或动作生命周期。

## 行为

年龄是 `level.gameTime - birthTick + partialTicks`。`birthTick < 0` 或 `age < 0` 不画。`age >= 10` 不画。

- `0 <= age <= 6`：帧号 `floor(6 * age / 6)`，即 `floor(age)`。age 6 是帧 6，不会直接跳到帧 7。
- `6 < age < 10`：帧号 `min(15, 6 + floor(10 * (age - 6) / 4))`。帧 6 再保持约 0.4 tick，帧 15 约占最后 0.4 tick。
- 不取模。帧 15 全透明。消退仍乘原来的淡出透明度。
- 旧程序色带已去掉，只画这一张贴图。

世界顶点来自快照的 `forwardAxis` / `crossAxis`、`startAngle` / `sweepAngle`、`radius` / `halfWidth`。负扫角走原有 `ProgressArcBand.direction`。顶点是相对实体原点的偏移，不再加一次原点，也不再乘尺度。

源图椭圆中心 (32, 34)，半轴 (25.5, 24)。极径窗 0.42–1.28，中线 0.85 对到弧带半径；超出半宽的只有一截装饰，不到两个半宽。活动阶段把 `[-213, ends[frame]]` 映到世界进度 `[0, shown]`。消退阶段世界进度固定为 1，角度继续用峰值区间 `[-213, 4]`，后期超出 4° 的装饰不扩大扇区。纵向 UV 夹在当前帧纹素中心内，游戏资源不带 `.mcmeta`。

渲染不写深度，关闭背面剔除，透明纹素不挡场景。

## 本次修改

- `src/main/java/com/bslx/XueXiMod/client/WaterArcRenderer.java`：改为贴弧形水斩。
- `src/main/java/com/bslx/XueXiMod/client/WaterArcSheetMap.java`：新增。帧号、极坐标 UV 和弧带采样。
- `src/test/java/com/bslx/XueXiMod/client/WaterArcSheetCheck.java`：新增。CPU 检查。
- `src/main/resources/assets/xueximod/textures/entity/xueximod/water_arc_slash.png`：新增。与归档 PNG 字节相同。
- `docs/资源库/特效/水流/弧形水斩_v1/README.md`
- `docs/资源库/README.md`
- `docs/资源库/过程归档/水刃特效接入-20261025/implementation-result.md`：本文件。

未改 `ArcBandSnapshot`、`ProgressArcBand`、`WaterTechniques`、`TechniqueHitRenderer`，也未改伤害、碰撞、冷却和动作。

游戏路径：`xueximod:textures/entity/xueximod/water_arc_slash.png`。SHA256 `76A1F122661A9BEB4040E980B83A910C5F0BAEF55F58A2E9FA534A5BC4AA5098`。

## 检查

`WaterArcSheetCheck` 退出码 0，打印「弧形水斩采样检查通过」。覆盖 age 0 / 6 / 6.2 / 6.5 / 9.8 / 10、帧不回绕、三拍 90° / -90° / 140°、终点不铺成 217°、原点与倍率只使用快照里的最终值、UV 不串帧、PNG 为 64×1024、帧 15 无像素、游戏文件与归档字节一致、游戏侧没有 mcmeta。

命名门禁扫整个 `src`。本次文件没有报错。唯一旧项是未改动的 `TrackingSupport.java:16` 字段 `MAX_TURN`，按门禁约定没有顺手改名。

`.\gradlew.bat compileJava --offline --no-daemon` 失败，没有进入 Java 编译。`settings.gradle` 第 13 行插件 `org.gradle.toolchains.foojay-resolver-convention:0.7.0` 在离线仓库里解析不到。耗时约 15 秒，BUILD FAILED。

随后用已经存在的 Forge 1.20.1 客户端 classpath 单独 `javac` 了 `WaterArcSheetMap` 和 `WaterArcRenderer`，退出码 0。这不能代替上面的 Gradle 结果。

本会话没有 minecraft-mod-mcp。没有 ping 到客户端，没有启动第二份游戏，游戏内未验证。

未提交，未推送。

## 主控补充验证（2026-10-25）

初次离线插件缓存缺失后，按 gpt-6-astra 裁决执行正常联网构建，未修改构建配置：
- `gradlew.bat compileJava --no-daemon`：BUILD SUCCESSFUL，23s，Java 编译实际执行；仅既有弃用警告。
- 随后 `gradlew.bat compileJava --offline --no-daemon`：BUILD SUCCESSFUL，12s，compileJava UP-TO-DATE。

上述结果取代前文“尚未完成 Gradle 编译”的状态；游戏内仍未验证。

独立 Grok 4.7 审查通过，详见同目录 review-result.md。审查指出的素材 README 构建状态已同步更正。
