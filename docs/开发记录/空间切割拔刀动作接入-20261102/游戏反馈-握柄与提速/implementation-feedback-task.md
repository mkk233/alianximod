# 用户游戏反馈返修：正确握柄与快拔（实现阶段，不编译）

固定 Cursor CLI --model grok-4.7-high-fast。同一可写作者044b78d1-5a7e-4853-ab82-af2512815f4b。用户已授权修复并要求美术只用MCP；主会话已完成真实Blockbench/Aseprite MCP示意和预览，不需要你控制桌面或重画。
输入：本目录read-feedback-report.md，以及 D:/alianximod/docs/资源库/动画/剑术/空间切割_握柄快拔_v6/visual-handoff.md、animation-keyframes.json、native-grip-proof.json。完整阅读，按已明确修复范围实现。

只允许源码修改以下文件：
1. src/main/java/com/bslx/XueXiMod/client/action/spacecut/SpaceCutItemMount.java
2. src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java（只space_cut动作）
3. src/main/java/com/bslx/XueXiMod/client/action/spacecut/SpaceCutSampler.java（只将资源路径指向新v6，采样/四元数不动）
4. 新增 src/main/resources/assets/xueximod/actions/space_cut_grip_fast_v6.json
保留旧v5运行JSON和所有旧正式素材字节。若需要其他源码，返回原因不要越界。

修复目标：把原版实际SwordItem的缠把点(3.5,3.5,8)/16贴到右掌，不是模型中心(8,8,8)/16。真实刀轴为normalize(1,1,0)，法线+Z；处理完整源轴与ItemDisplayTransform后对齐SwordHand，不要靠加一个世界偏移凑图。不得重复展示旋转或逆方向。精确方程是最终渲染变换后的柄点==掌心、最终(柄到尖)向量==剑骨刀轴。第一第三人称都正确，掌心轨迹不变，宽/细臂及左右主手、主/副手剑走共用修复入口。保持真实模型与缩放、其余物品不丢失、结束时恢复。若仍只支持默认handheld，边界如实记录；别将自定义形状说成全部支持。

所有视觉时间由1.6统一到0.5（倍率0.3125），前带末0.15、斩击末0.1875、收尾0.5；九骨times及duration、动作会话clip关键时间与视觉通知同步。不是只缩JSON让会话停到1.6。不改伤害、20tick冷却、现有1秒space_cut特效、逻辑规则或一分为三，不加硬直、不加施法锁。动作和特效由同次释放起播。

实施前备份四个现有文件（新增资源不覆盖）。文件只有你一个作者。不要通读整仓，不碰分身等他人任务，不reset/clean。完成作者必要自查：真实像素柄点和实际刀轴在t0、0.15、0.1875、0.5通过完整第一/第三人称最终变换的误差，两个视角应包含实际显示缩放；时序字段一致；主副手分支及无剑不造剑保持。旧“模型中心对齐即握刀正确”的检查不能复用。运行命名门禁允许，不派独立测试，不写镜像实现测试充数。

本单不授权Gradle：不要修完顺手编译，不build/jar/runClient、不游戏。结束写本目录 implementation-feedback-report.md / implementation-feedback-evidence.json（最小改动清单、哈希、mtime、写入停止、自查证据），以及正式v6目录 runtime-mapping.md/json。不要改主会话所有README/manifest/根索引/调度记录，不改制作表。最后一次性返回结果并明确停止所有源码写入。

这是同批用户返修，设计2/2、实现正式复核2/3不重置；本单是授权实现与作者自查，不是独立复核。完成后主会话再集中派剩余第3轮实现复核，此处不要另开复审。编译只在写入收束后另行派发，用已有JDK17+ C:/Users/baishang/.gradle缓存。
