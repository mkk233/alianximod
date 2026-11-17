# 用户实测返修：水幻形显示整面青色，确认稿应为镂空水线

本单固定 Cursor CLI `--model grok-4.7-high-fast`，继续原 D 作者。只处理本用户反馈的运行表现不一致，完成必要定位和最小修复，不是新增正式审查。设计2/2、实现3/3已经用尽；不复审全系统、不重审旧结论、不调用其他代理，不把本单写成第4轮PASS。最终明确“本阶段额度已用完，返修结果未追加复审”。

用户原话：“水幻型的特效为什么和预览不一样”。输入截图：
`C:/Users/baishang/AppData/Local/Temp/codex-clipboard-7947b32f-7a02-4902-9a5b-f15300c949c4.png`
截图表现：Steve身体各面几乎整面青色半透明填充，头部尤为明显，没有确认稿中的镂空水线。不能仅凭截图预定某个根因，也不能说是正常光照差异。

对照目标是已确认且已授权接入的原版 v2.1：
`docs/资源库/特效/水流/水幻形_Steve水线_v2_1/`
其中 `bb_hold_three_quarter.png` 是真实Blockbench确认稿，头/胸/四肢中部镂空、边缘青色水线；`water_phantom_steve_v2_1.bbmodel` 原SHA256 4ADFCC75ED1E06E62ECE5FD05E6B109787FB1AC46B29CBFC5A8AF8C5425695F7，`steve_water_lines.png` 9E7E5B01A7E09C393D3FFA1800468BD61AE919F68CF628152B00D9C347793DA5，`water_flakes.png` CEFDEE9E3E3FB7F701E19FC9CB3438C586C4841D3A50E3F8C9399A635BD4D70C。此前游戏网格lines.bin SHA256为26EF5093F3FC9CAD03272F7BDFAD06DF771953E161B69310B2FF7E37B89F9025，不代表转换一定正确。

先读现行AGENTS.md、xueximod-art-preview技能、`integrate-d-report.md`、`runtime-visual-mapping.json`和正式 `visual-handoff.md`。这是同版运行接入返修，保持源稿/造型/动画/原PNG不变，不需要重新造美术或重新确认同版。不要操作Blockbench活跃项目。如果必须改确认稿本身，停止那部分并准确交回原因，不以改稿掩盖运行bug。

唯一源码作者与允许写入范围：
- `src/main/java/com/bslx/XueXiMod/client/SteveWaterLineMesh.java`
- `src/main/java/com/bslx/XueXiMod/client/SteveWaterLineVisual.java`
- `src/main/java/com/bslx/XueXiMod/client/SteveWaterLineReload.java`
- 同目录 `CloneOutlineVisual.java`、`CloneRenderer.java`、`CloneAfterimagePlayback.java`，仅直接涉及该水线材质/叠绘症状的必要修改。
- `src/main/resources/assets/xueximod/models/effect/water_phantom_steve_v21/lines.bin`，如证实转换错误可重导，需保留转换步骤和新哈希。
- 两张运行PNG只可恢复为已确认原图的逐字节副本，禁止重绘改变确认版。
- 本批 `fix-game-visual-01-report.md`、相关小型定向证据文件及必要转换脚本、`runtime-visual-mapping.json`。
- 正式资源目录README/visual-handoff与资源库README只在确有运行映射变化时更新，保留确认源与历史预览。
不改其他源码、游戏逻辑、配置、平衡、注册、lang、CloneEntity或制作表；不覆盖用户改动，不reset/clean/checkout。必要范围外依赖先报告准确文件与原因。

必要定位只围绕本缺陷：
1. 对比确认源贴图/嵌入纹理/游戏资源的实际alpha与UV，而不只比文件存在或UV都在0..1。明确逻辑64UV、实际128bitmap在每个方体的映射；从确认稿头部/胸部等选代表面，对照实际采样位置与透明中心。
2. 核对导出的mesh每面顶点UV顺序、归一化、纹理选择/绑定、shader采样和透明混合；判断有没有额外整面底色、未纹理化水色方体或重复驻留表现盖住水线。必要时查相关RenderType的本地1.20.1定义，不扩大为渲染系统审查。
3. 区分文件源正确、转换正确、运行资源更新、游戏加载版本四个环节。可以只读必要本地构建资源/启动日志/进程启动时间或资源路径，绝不打印完整游戏命令行、accessToken或session等凭据。用户未要求操作游戏，本单不连接/操控客户端、不热替换运行状态、不启动游戏。没有运行态证据就写尚不能确认，不能把问题推给用户旧版本。
4. 根因明确且在已批准契约范围内，直接完成最小修复，保留Steve尺寸、alpha水线、0.84秒散水/partialTick、既有预算/缓存/维度清理和旧LEGACY分身。
5. 做能直接证明本修复的必要定向检查（如代表面UV对应透明中心/边线的采样数值、确认资源与导出一致、必要namegate），不要新增独立测试阶段或全系统复审。离线采样仅是诊断证据，不能称为真实游戏截图或游戏验收通过。

当前没有编译授权；不要Gradle/build/jar/reobf/runClient、不要发布、不改表。本次单独完成后停写，主会话确认其他写入收束再派固定Cursor最终编译。先前16:16的编译不能覆盖本次返修。

最终一次性交回：具体根因与证据；准确改动文件/行号；确认素材是否保持；旧转换与新转换变化（如适用）；定向检查结果；用户当前客户端怎样加载修复的准确说明与未证实边界；尚未编译/尚未游戏实测；相关写入已停止。不要复述假想根因或只调alpha把错误遮住。没有定位到根因就如实报告，不虚构修好。
