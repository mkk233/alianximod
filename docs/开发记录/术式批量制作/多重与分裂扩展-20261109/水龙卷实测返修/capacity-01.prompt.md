# 组合水龙卷体积上限 48

用户 2026-11-09 明确批准：组合水龙卷的入卷体积上限改为 48 立方格；独立旋流保持 12。实测日志原因是占用超限，一只铁傀儡约 6.3，第二只会使 12.6 超过 12；当时水势仍充足。不取消溃败，不改收费、座位、湿度和几何。

只允许改这四个文件：
- src/main/java/com/bslx/XueXiMod/shu/balance/BalanceKeys.java
- src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainDefinition.java
- src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/motion/VolumeMotionRuntime.java
- docs/配置平衡指南.md

要求：
1. BalanceKeys 增加 LIFTED_OCCUPIED_VOLUME_LIMIT = "liftedOccupiedVolumeLimit"，放在 OCCUPIED_VOLUME_LIMIT 旁边。
2. DomainDefinition 在 occupiedVolumeLimit 后注册新键，RawDefault 48，边界与现有体积键相同 strictlyPositive(1_000_000)，单位 CUBIC_BLOCKS。说明写“水龙卷入卷体积上限。只在组合成立时读取”。把原 occupiedVolumeLimit 说明改为“独立旋流入卷体积上限。组合成立时不读这份”。
3. VolumeMotionRuntime 抽一个私有方法：combination(host) 为真时读新键，缺省 48；否则读 occupiedVolumeLimit，缺省 12。acquire 里的 limit 和 occupancyText 都改调这个方法。不要复制条件，不要改其他判定。
4. 配置平衡指南第 174 行的 water_vortex 键清单加入新键。表格里 occupiedVolumeLimit 生效时机改为独立旋流入卷；新增 liftedOccupiedVolumeLimit 一行，默认 48，单位与边界同上，只在组合成立时读取。

先读当前文件再补丁。每次 apply_patch 只改一个允许文件，并传该文件当前 sha256。不要删除、改名或改其他文件。改完调用 name_gate。不要编译，不要改制作表，不要启动游戏。

最终用中文报告：改动点、四个文件的最终 SHA256、命名门退出码，并写明未编译、未游戏实测。
