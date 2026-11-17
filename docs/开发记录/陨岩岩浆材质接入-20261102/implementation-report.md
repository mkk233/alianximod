# 陨岩熔岩岩壳 v5 接入

status：`implemented_pending_compile`

用户 2026-11-02 回复“采用这个版本”。此前要求“火焰纹理太少了要更像岩浆块”。已按原字节归档并替换运行贴图。没有重画，没有改尺寸或颜色。本阶段额度已用完，返修结果未追加复审。

## 运行引用

方块状态 `blockstates/meteorite.json` 指向 `xueximod:block/meteorite`。方块模型 `models/block/meteorite.json` 的父模型是 `minecraft:block/cube_all`，六面贴图是 `xueximod:block/meteorite_v1`。物品模型 `models/item/meteorite.json` 的父模型是 `xueximod:block/meteorite`。这三份引用本来就正确，没有改。

运行文件仍是 `src/main/resources/assets/xueximod/textures/block/meteorite_v1.png`。内容已换成确认稿。没有新增动画 `.mcmeta`，没有把 `.bbmodel`、放大预览或示意地面放进运行目录。

## 字节核对

确认 PNG 是 PNG 签名 `89504E470D0A1A0A`，32×32，1 帧，颜色类型 6，没有 `tRNS`。逐像素检查 alpha 都是 255，全不透明。

过程稿、正式归档和运行贴图 SHA256 相同：`a0f7027b8b39c4f9f3aaabb9e05713c42e5b0e86697c89d9294848a9b228e1d8`，1324 字节。

替换前运行贴图已备份到 `docs/开发记录/陨岩岩浆材质接入-20261102/runtime-backup/meteorite_v1-before-v5.png`。备份 SHA256 `e8ca06d343a6e6a60082c542869fda9be4b9de040904eef59baf52d28cbc1d3a`，761 字节，与替换前运行文件一致。

模型 `meteorite-magma-v5.bbmodel` SHA256 `fbd11fd97791f43d95d0a36b37bc95511c34ae26108f99e0de054ed86843fd33`。源、预览哈希与过程稿 manifest 一致。制作脚本留在过程目录，没有进入正式目录。

亮黄只是贴图颜色。没有新的真实发光、动画或方块照明。玩法、命中后续飞、伤害、碰撞、体积和照明逻辑未改。没有改 Java，没有跑命名门禁，没有编译。

## 最后写入

二进制复制保留了原稿时间。文档时间是本次写入时间。

| 路径 | 最后写入 | SHA256 |
| --- | --- | --- |
| 过程/正式/运行 PNG | 2026-11-02T02:06:55.8786421+08:00 | `a0f7027b8b39c4f9f3aaabb9e05713c42e5b0e86697c89d9294848a9b228e1d8` |
| 运行备份 | 2026-10-31T03:17:23.2510830+08:00 | `e8ca06d343a6e6a60082c542869fda9be4b9de040904eef59baf52d28cbc1d3a` |
| 正式 `meteorite-magma-v5.aseprite` | 2026-11-02T02:06:55.8751141+08:00 | `2f6dca5d3dde69b0901f6da7392fa94aa67ace8adec6203df410a9f38d7634a7` |
| 正式 `meteorite-magma-v5.bbmodel` | 2026-11-02T02:08:12.5550148+08:00 | `fbd11fd97791f43d95d0a36b37bc95511c34ae26108f99e0de054ed86843fd33` |
| 正式 `texture-preview.png` | 2026-11-02T02:06:55.8970183+08:00 | `33d31464e665e6934f4cb7aaa245c5ce7b1e3a99edce1ba0659afdb543ce7d0f` |
| 正式 `bb-hero.png` | 2026-11-02T02:07:23.7700379+08:00 | `a4dab13dfdacb09bc623fd00470c1b58c205cc5ee28d95147ddbc77d0bb6b06f` |
| 正式 `bb-reverse.png` | 2026-11-02T02:07:23.8061480+08:00 | `8841da5bb40e3af1f9c41428c4f043429d2512411f762c31b273c190d7223632` |
| 正式 `blockbench-active-view.png` | 2026-11-02T02:08:12.5479774+08:00 | `b526f0572bbbd7703ea91232fe4e8514fa6cc56e89a656ca01f102db578e48af` |
| 正式 README.md | 2026-11-02T02:18:32.9508218+08:00 | `eed22a44a4d3ce009c301d4de442f0ed7532a8f7ba9ada0fb471c0673b3aad20` |
| 正式 visual-handoff.md | 2026-11-02T02:18:38.0508779+08:00 | `83457322bb8bec7460d50eca5d1821abd34d9c90e8b5ffdcf92feb79d3583e20` |
| 正式 manifest.json | 2026-11-02T02:19:06.2859620+08:00 | `3afeb2f5502f1d139476ffa325270a57a69bf5483fcfcfa1bd50208c534dd846` |

blockers：[]

本报告、`implementation-result.json` 和 `implementation-ownership.json` 写完后，相关写入停止。编译等待主会话另行授权。旧的 `compileJava` 成功不覆盖这次贴图。
