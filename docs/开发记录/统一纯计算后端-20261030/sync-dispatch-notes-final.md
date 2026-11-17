先读任务说明，按它划定的范围做。两处过时说明按已交回的分发事实改，数值和签名不动。额度已用完，未追加复审；待统一编译。写入所有权已释放（`released=true`）。这是记录收尾，不是新的正确性复核。原实施报告 `implementation-A-final.md` 与 `implementation-A-live-native-final.md` 保持原样。没有改逻辑、公式、JNI 签名、字段、stride 或错误码。没有编译，没有测试，没有启动游戏。

## 改了什么

`ComputeCodes.NATIVE_BATCH_FLOOR` 仍是 8。只改了它上面的注释：这个下限只约束公开的六个 Batch 方法；已接入的单次计算在显式 native、库有效且能力支持时按 `count=1` 进入对应 JNI，不受该下限约束；8 不是测过的加速阈值。注释写明运行加载和数值都还没有验证。

`abi-contract.md` 只改了末尾「游戏侧已经接上的入口」。四个原入口名单不变。默认 Java 时标量直接走参考公式。显式 native、库有效加载且对应能力支持时，这六类单次调用按现有批量布局以 `count=1` 进入对应 JNI，不受公开批量下限 8 约束。公开六个 Batch 方法在条数小于 8 时仍走 Java。8 仍不是测出来的加速点。该节写明运行加载、数值、游戏和性能都还没有验证，不能写成已经成功调用 C。

## 没有改什么

没有改其它 Java、C、构建文件或制作表。没有改 `JavaReference`、`ComputeOutput`、`XuexiComputeNative`、`ComputeCalls`、`ComputeSelection`、`ComputeLiveDispatch`。契约里的数值、布局和错误码没有动。使用说明和两份原实施报告没有重写。

变量名检查仍只报未改动的 `OriginalHumanoid.java` 两处旧名（`i`、`to`）。本次注释没有新增违规。检查脚本自己的解析编译不是模组 `compileJava`。

## 元数据

| 路径 | 字节 | UTC |
| --- | ---: | --- |
| `src/main/java/com/bslx/XueXiMod/shu/compute/ComputeCodes.java` | 10073 | 2026-10-30T08:24:05.956Z |
| `docs/开发记录/统一纯计算后端-20261030/abi-contract.md` | 14859 | 2026-10-30T08:24:08.245Z |
| `docs/开发记录/统一纯计算后端-20261030/dispatch-notes-final.md` | 1803 | 2026-10-30T08:24:24.512Z |
| `docs/开发记录/统一纯计算后端-20261030/evidence-dispatch-notes.json` | 860 | 文件内记 2026-10-30T08:25:02.937Z |

证据 `released=true`，无哈希。自身时间取写入该字段前读到的修改时间，这次对齐不改变 860 字节。
