# 用户实测返修02：水线人形缺失一侧手臂

固定 Cursor CLI `--model grok-4.7-high-fast`，原D作者继续。用户最新截图显示镂空水线已经出现，但反馈“缺了一个手”。只定位并修复这一具体缺陷；属于原批准v2.1接入的延续返修，设计2/2、实现3/3沿用，不重置，不追加正式复审、不spawn。

用户截图：`C:/Users/baishang/AppData/Local/Temp/codex-clipboard-4a6dde4f-db40-4e9c-86bf-efa1be41819d.png`。主会话也保存副本到本批 `user-game-visual-02.png`。
目标仍是 `docs/资源库/特效/水流/水幻形_Steve水线_v2_1/` 的已确认六方体Steve、两只宽臂、水线镂空、原散水动画。用户没有要求改造型。

先读本批 `fix-game-visual-01-report.md`、`compile-game-visual-01-report.md`、`runtime-visual-mapping.json`、正式visual-handoff，以及现行AGENTS/美术技能。上一版已经编译并经用户再次截图，不默认归因于没有重启。修复01的新绘制路径已被实际运行，沿用其镂空效果。

单一文件作者：本单独占 `client/SteveWaterLineMesh.java`、`SteveWaterLineVisual.java`、`SteveWaterLineReload.java`；只在直接需要时可改 `CloneOutlineVisual.java`、`CloneRenderer.java`、`CloneAfterimagePlayback.java` 的水线相关路径。必要时可修正运行 `models/effect/water_phantom_steve_v21/lines.bin` 的转换结果（须有确认模型的对应证据和新哈希），不能更改确认版bbmodel/原PNG/动画或游戏逻辑。相关本批返修报告、定向证据/小脚本、runtime-visual-mapping归本单。任何范围外依赖准确报告，不顺手改其他系统。保留用户所有未提交改动。

必要定位与修复：
1. 分别追到左右手臂：确认模型元素/所属组 → lines.bin部件 → 返修01透明纹素展开 → 最终顶点/变换。查明缺失在哪一层，不先认定是截图遮挡。
2. 特别核对手臂镜像UV、反向U/V、顶点顺序、不同面朝向在纹素展开中的处理。检查是否生成零面/零面积，或把手臂挤到躯干/另一手臂、错误scale为隐藏值。这里列的是定位方向，不是预设根因。
3. 根因明确后按同一批准源做最小修复；不要复制另一只手臂贴上去掩盖通用转换错误，不重绘贴图，不把镂空改回整面填充。不改变原尺寸、朝向、散水时长或全局渲染规则。
4. 必要的针对性证据按部件给出：六个主体各自输出面数与非退化范围，双臂在驻留时刻的独立位置/可见scale，代表性反向UV的对应纹素/几何角点。不能只给全部部件的总数，也不能以“74个部件都存在”证明手臂能画出。该证据是本次缺陷修复自查，不新增正式复审或独立测试阶段。不要扫描全仓。
5. 运行已有命名门禁；源码停写后交回主会话再统一编译。本单不Gradle/build/jar/reobf/runClient、不操控或启动游戏、不改制作表。没有实机复测不能说游戏已恢复。

输出 `fix-game-visual-02-report.md`、`fix-game-visual-02-evidence.json`：准确根因、改动文件/行号、必要定向检查结果、资源是否变化与加载方式、未编译/未实机复测/写入已停止。明确“本阶段额度已用完，返修结果未追加复审”。保留修复01历史记录，不用它或旧R3 PASS为本次新源码背书。未定位清楚就如实交回，不能猜测修好。
