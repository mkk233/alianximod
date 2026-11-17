# 焚天 v3：用户指定的全身烈焰

状态：`approved_for_recipient_integration`。接收术式：`fire_fentian`。
batchId=`待制作-20261031`；taskId=`fire_kindle_v2_art`；attemptId=`20261116-art-kindle-fentian-swap-04`。

用户认可刚完成的全身烈焰，并明确“把这个效果给焚天”。完整逐字回复与双向批准映射：
`D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/approval-mapping.md`。
它是批准用于焚天，不是引燃已采用，不再要求用户重复确认同一素材。

- `fentian_v3-strip.png`：从原 `kindle_v3_strip.png` 按字节复制，48×1216，单帧 48×64，共 19 帧，二值 alpha；SHA256 `EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070`。
- `fentian_v3.aseprite`：从原 v3 源按字节复制，4 层、19 帧、每帧 50ms；SHA256 `3EC04641BBBC573BAAABCF088DA61B6B79B7418159C2D1F6F3287D5B8B06BB42`。源内部旧名字与 candidate 元数据保留为制作历史，本文件及批准映射说明当前用途。
- F01–10 持续循环（0 起算 0–9）；每次焚天真实 4 tick 段触发 F11–13 三帧短亮（10–12）；结束用 F14–19 六帧收束（13–18，300ms），F19 空白。不要把 19 帧整条循环。
- 保留几何：height=H、width=.75H，目标包围盒中心，加朝镜头支撑面偏移 .04 格，CAMERA_PLANE。H=1.8 时高 1.8、宽 1.35 格；完整偏移公式见下述交接。颜色不改。
- 短亮、持续和尾焰仅视觉，不改原伤害、段时钟、目标动作或燃料逻辑。组合命中照常显示。

拟运行资源：`D:/alianximod/src/main/resources/assets/xueximod/textures/effect/fentian_v3/conflagration.png`。本作者没有写运行资源或 Java。
同版真实空间预览、模型、参数与哈希：
`D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/visual-handoff.md` 和同目录 `manifest.json`。
正面与斜视图右侧为本焚天；冻结侧距图左前方为本焚天。

旧引燃 v3 制作文件及反馈历史未覆盖；被拒绝的更早 v2 仍保留。资源用途确认不等于接入完成、编译通过或游戏实测。本阶段额度已用完，返修结果未追加复审。
