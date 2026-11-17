# 归档收尾（2026-10-27）

`compile.log` 记录 `compileJava UP-TO-DATE`、`BUILD SUCCESSFUL in 12s`。状态已写成「已接入游戏，编译通过，待用户游戏内验证」。`game_integrated=true`，`compiled=true`，`in_game_tested=false`。未打包，未启动游戏，未提交。

## 改过的文档

- `docs/资源库/特效/水流/周身水环_细碎毛边_v8/README.md`：补 runtime 三文件和游戏路径，写明三层 768 面、48 tick 一圈、16 帧每 2 tick、水体帧高 24、外缘和内缘帧高 40、原模型倾斜与共根旋转。去掉「只整理资源、没有改游戏代码」。
- 同目录 `asset_manifest.json`：原 13 条保留，源文件和预览哈希未变。新增三份 runtime，并记下与游戏文件相同的 SHA-256。README 哈希改为 `9577ddf66cbe6b8bf52d28d2dc5848ab4af8a74b43c9af1d8616146ff4bc8a56`（3403 字节）。清单自身不收录自身哈希。
- `docs/资源库/README.md`：只改了 v8 正式项的采用状态。
- `docs/资源库/过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/README.md`：只改了采用状态。旧制作说明和旧日志未动。

## 核验

`final-archive-validation.json` 记录当前清单：9248 字节，SHA-256 `df03e953757646e974e4801d72fb149ad00a023857f6a25f9187b6bdbd7b3f47`。16 个文件逐项比对，0 处不一致。三份 runtime 与 `src/main/resources` 字节相同。`archive-result.md` 里的旧清单哈希仍是当时快照。

## 编译之外

`GuardSnapshotPoseCheck` 已执行，14 条断言、0 失败。`GuardSyncWireCheck` 最终完成 9 组核心检查、55 条断言，全部通过；协议运行时初始化一项仍为 `UNEXECUTED`，协议版本 2 及双端严格匹配已静态核对。最终日志见 `guard-check-final.log`。整份 `compileTestJava` 失败在无关旧测试，没有改那些文件。游戏内效果仍由用户验证。
