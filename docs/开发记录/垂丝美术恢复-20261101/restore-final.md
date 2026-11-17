我先读恢复任务说明，确认这次授权改哪些文件、哪些逻辑必须留下。任务只允许改 `WeepingSettings` 的宽度常量。我先核对这个文件是否仍是上一轮查到的 1.2，再按授权写入。恢复没有写入。当前会话是只读模式，不能改 `WeepingSettings.java`，也没有建立备份。请切到可写模式后再执行这一处修改；现在不要编译。

目标文件仍与上一轮定位一致，没有被别人改过：

- `src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java` 第 16–17 行仍是「大约一个身位」，`SHEET_ACROSS = 1.2`。
- `WeepingCurtainEffect.java` 第 35 行仍是 `spec.width = WeepingSettings.SHEET_ACROSS`，按方案不用改它。

已批准、尚未落地的改动只有那一处：把常量改回 `0.5`，注释改成原版基础宽度、4+4+4 像素铺法。改前应把该 Java 原样备份到 `docs/开发记录/垂丝美术恢复-20261101/pre-restore/` 的对应路径。贴图、渲染器、布局和运动逻辑都保持不动。