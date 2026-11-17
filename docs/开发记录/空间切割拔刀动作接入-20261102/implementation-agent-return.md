我先通读这份任务说明，确认授权范围和交付要求后再动手。任务要求先验证写权限。我先在本批目录写入会话标记，再承接已有定位和坐标换算。写权限已确认。接下来读取定位报告、坐标换算和正式视觉交接，按已确认方案直接实现。定位和换算已经对齐。我接着读现有姿态写入和 v5 关键帧，确认双节臂和持剑挂点该接在哪。坐标换算已核对：Blockbench 里先按 ZYX 合成，再把结果翻到玩家模型。接下来会按这个结果接双节手臂和右手挂点，不改伤害时钟。左腰右拔 v5 已接到现有的 `xueximod:space_cut` 播放入口。1.6 秒姿态只在客户端绘制，0.48 秒的 `draw-release` 仍是视觉通知，不推迟伤害，也不加长 20 tick 冷却。待统一编译。游戏效果未验证。

共享五骨剪辑改成单位旋转，不再充当拔刀动作。双节臂和独立 `SwordHand` 由 `SpaceCutSampler` 按运行 JSON 求解：父级先在 Blockbench 空间用 ZYX 合成，再一次性写成 `(qx, -qy, qz, qw)` 和 `(x, 24-y, z)`。上臂、前臂各 6 像素。起始掌心偏离 `[5.3, 15.5, -3.6]` 超过 0.05 时拒绝这套姿态。

主手有 `SwordItem` 就挂那一把，否则用副手剑。动作期间一律画在右手刀骨上，并抑制这份物品原来的手持绘制。主手在左时不改主手、不改物品栏。没有剑就不生成示意刀。

普通与 slim、袖层、自己和观察者、第一人称和第三人称都走这一帧。精确类 `HumanoidModel` 的胸甲手臂拆成两节，染色和镶边仍走原绘制。绘制结束在 `finally` 弹出帧，并由原有模型快照恢复。

**不能按规格完全落地的部分：** 非 `HumanoidModel` 的自定义护甲仍是单节；剑长跟随物品自己的展示变换，没有强制成 17.5 像素；第一人称约有 0.1 的俯仰残差，视角里不画护甲；不继承 `SwordItem` 的武器留在原手腕。

**检查：** `tools/check-name-gate.ps1` 通过。未跑 `gradlew`、未打包、未进游戏。`RetargetProfile`、`RetargetedPose`、`RigidPoseRetarget`、`FirstPersonHandBasis` 未改，哈希与写入前一致。

| 文件 | sha256 | mtime |
| --- | --- | --- |
| `OriginalHumanoid.java` | `aa8e3de5ea422bf96106de68913b467f79fb059b7428b43289af09fb3ae1a314` | 2026-11-02T05:03:56 |
| `ThirdPersonPoseWriter.java` | `ecfb41bf937921769e4ff28211876e5aa8b973c487cb0f3e5829766a62656a5f` | 2026-11-02T05:00:48 |
| `ActionRenderQueries.java` | `774a0d8fa49bd89a67bc64283187c183016a9e6e81cea1e76e6bd89395a73fcd` | 2026-11-02T05:00:50 |
| `EntityRenderDispatcherActionMixin.java` | `a56b2dd0945717687d215cb047b49653ebe1c525570cf071bbc28663dcb136d9` | 2026-11-02T05:02:19 |
| `LivingEntityRendererBodyMixin.java` | `2ec824d216a295687c8b0ab4ef6ee31d44ef04f5717a328b4c028d31359074b0` | 2026-11-02T05:02:20 |
| `ItemInHandSkinMixin.java` | `518badda36a56f2355a694e5b3b38d04dc79a8ab0f2b2e00e074caf5d4aea4ff` | 2026-11-02T05:02:22 |
| `HumanoidArmorSkinMixin.java` | `ab7d27a26aa659092ddb207055acbd57c562c339c9615b2340a91e8d89f08cd7` | 2026-11-02T05:02:24 |
| `ItemInHandRendererActionMixin.java` | `984d51aecfa8c06af6cfe6333eae93c5462c0e6799055e70840947793710aa0c` | 2026-11-02T05:02:27 |
| `SpaceCutFrame.java` | `787c6d944738bf864ec4b24535557765faa2c3908082bb63d3c0b37c3ecf42d8` | 2026-11-02T04:57:45 |
| `SpaceCutScope.java` | `b99f05c82c711d6a5b7fcd35804a8ce3771b8fb0f092b2d319aa1a5b0a8563a7` | 2026-11-02T04:57:58 |
| `SpaceCutSampler.java` | `4c2837db6ba99178e5403fb5da93cd3811b0e41a2b3650e278edfa3ce054f569` | 2026-11-02T04:58:33 |
| `SpaceCutMatrices.java` | `c2fa2ee382ab58f7456c3bb248e41f1b9dcd810dfc5df800538d7d99eb7bc501` | 2026-11-02T04:58:38 |
| `SpaceCutBodyPose.java` | `5b5017a0f5c0006a46bf904ced8dccae62b69b0cd557792c12e0e4f68c2a7291` | 2026-11-02T04:58:49 |
| `SpaceCutArmDraw.java` | `0b6e06270b0689ee60a9fbee75d95bc9ef23ddf303107371df9e8a9b29d110d5` | 2026-11-02T04:59:42 |
| `SpaceCutItemMount.java` | `60c62a0c5c2980cf1bd97f0d6b8a38ea9348d273e5f32c18334b7906d8954a72` | 2026-11-02T05:02:18 |
| `space_cut_left_waist_v5.json` | `64b0fe3eb2f629c3fb12b90ca1d3a8c64e1664c7661dc7794c97473a8b313c46` | 2026-11-02T04:56:46 |
| `runtime-mapping.md` | `bcb0071130e18ddf93c0c3000d1c88785e736c080ab0c6426670adf5e7514357` | 2026-11-02T05:05:09 |
| `runtime-mapping.json` | `4620a49d6d3aac9c342b914e25feda7a4c5e7a6ab3c42dbf46bf02944b0f2298` | 2026-11-02T05:05:11 |
| `implementation-report.md` | `a459752b16d158638c9b94caa7238c7d620a55260f985613511e0098c6d4c669` | 2026-11-02T05:05:22 |
| `implementation-evidence.json` | `885b1cd941479dede5eba5ce31bbbceb659d58d454e2d540b68d300535a6df9e` | 2026-11-02T05:05:31 |

关键接线：`OriginalHumanoid.java` 70–102 行，`ThirdPersonPoseWriter.java` 33 行，`ActionRenderQueries.java` 101 行，`EntityRenderDispatcherActionMixin.java` 63 行，`LivingEntityRendererBodyMixin.java` 39 行，`HumanoidArmorSkinMixin.java` 151 行，`ItemInHandSkinMixin.java` 59 行，`ItemInHandRendererActionMixin.java` 78 行与 200 行。

运行资源是 `xueximod:actions/space_cut_left_waist_v5.json`，对应 `animation-keyframes.json`。bbmodel、图集和 GIF 没有复制进游戏资源。

停止全部写入。
