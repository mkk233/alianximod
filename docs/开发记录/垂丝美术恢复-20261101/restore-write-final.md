我先读任务说明，确认授权范围后再按里面的要求做。任务是最小恢复：只把 `SHEET_ACROSS` 从 1.2 改回 0.5。我先核对当前源码是否仍与已查结果一致，再备份后改这一处。源码仍是 `SHEET_ACROSS = 1.2`，和已查结果一致。我再核对原注释和四张瀑布图的比对路径，避免改错。注释要改回 4+4+4 铺法。我去对一下原版那句原文，并确认四张贴图路径后再备份、改常量。文件仍是上一轮看到的 `1.2` 和「大约一个身位」。我先把这份 Java 原样备份，并核对四张瀑布图的 SHA256。常量已改回 `0.5`。我核对备份仍是改前内容，并跑变量名门禁。写入已经结束，待编译。本单只恢复了水帘基础宽度，没有跑 Gradle。

**唯一源码变更**

`src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java` 第 16–17 行：

- 注释改为「原版基础宽度，按 4+4+4 像素铺法。」
- `SHEET_ACROSS` 从 `1.2` 恢复为 `0.5`

改前文件与上一轮定位一致，仍是「大约一个身位」和 `1.2`，因此按批准方案写入。`WeepingCurtainEffect.java` 第 35 行仍是 `spec.width = WeepingSettings.SHEET_ACROSS`，没有改。可见宽度和判定宽度继续共用这个常量。

**备份**

`docs/开发记录/垂丝美术恢复-20261101/pre-restore/src/main/java/com/bslx/XueXiMod/shu/technique/release/WeepingSettings.java`

备份仍是改前原文：第 16–17 行为「大约一个身位」，`SHEET_ACROSS = 1.2`。

**四张瀑布图 SHA256**

运行贴图与 `docs/资源库/特效/水流/垂丝聚能瀑布_v1/` 逐张一致，也与上一轮记录一致：

| 文件 | SHA256 |
| --- | --- |
| `waterfall_runtime_left.png` | `DEC6990BCF962707FBC25E5218837DF7ADD4A34227C95F58C65F0E27428443E8` |
| `waterfall_runtime_middle.png` | `6BB9C161784BB89F9F5B95580EF56AC92E53ECDD5ED694EDAE923F29FA73AAE7` |
| `waterfall_runtime_right.png` | `CF6D6241880EF0833ABB53EB46EA847581BD52F15B3AF4A16EE4CD12D70A368F` |
| `waterfall_thin.png` | `E1DA1C1DC49776AD1BFBDB09B8EC01E919D6354F9CB09BFB1E8236759BE9A76D` |

贴图、渲染器、布局和其余未提交改动都没动。变量名门禁只报了未改文件 `OriginalHumanoid.java` 的两处旧名，本次改动的文件没有新违规。