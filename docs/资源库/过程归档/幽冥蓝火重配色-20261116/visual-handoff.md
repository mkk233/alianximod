# 幽冥蓝火重配色 art-v1-20261116

状态：完整可审阅候选，待同版本用户确认。二维沿用原稿；真实 Blockbench 场景、原生保存与复开、整体/侧面/关键帧已补齐。未正式采用、未接入。

batch：领域附魔底座实施-20261029 / 火系六式-20261029。task=nether-blue-visual；二维版本 art-v1-20261116；本次完成 attempt=art-v1-bb-finish-20261116。既有设计 2/2、实现 3/3 已耗尽，本次只是已授权美术返修，没有新增正式裁决/复审。直接父级为本次派发主会话；原生父代理标识未由工具暴露。没有派生代理。

## 输入与确认边界

用户本轮否定截图右侧暗黄绿乘色，要求直接采用幽冥原生 / MC 灵魂火青蓝，保留各火焰形状、动画和 alpha 亮度层次。该要求覆盖旧乘色建议；不代表本候选已获视觉确认。

父级已收 Flash 最终事实（代理已关闭、无写入）：当前 `NetherDomainTint` 为逐通道乘 RGB(90,160,255)，没有 LUT。幽冥自身使用白色顶点与已蓝色像素。实际运行参考 `src/main/resources/assets/xueximod/textures/effect/nether_gloom_v2/nether_gloom.png` 为 64×1024、16 帧，SHA256 `C4FA3B1B82BB29E8BD5040FDE4527BC9DBD8451083608DBB619C2AE02C9CB66F`。作者只读哈希确认与过程 v2 竖条完全相同。旧索引/README 的“未接入”与此实际文件矛盾，按实际基准工作，未覆盖索引。幽冥 v3 仅草稿，本次没有用作运行参考。

参考批准记录：资源库幽冥 v1 旧暂用说明、幽冥青色魂火 v2 的 N/O 摘录、幽冥 v2 领域预览中“用户选定二维”记录；当前纠正与实际运行事实优先。没有写表，也没有伪称重新读取当前工作簿 N/O/DV。运行代码由 Flash 另查，本作者没有重复读 Java。

截图烈焰最初读取自历史路径 `过程归档/引燃_全身燃烧重绘_v3_20261116/source/kindle_v3.aseprite`，SHA256 `3EC04641BBBC573BAAABCF088DA61B6B79B7418159C2D1F6F3287D5B8B06BB42`。**2026-11-16 最新用户批准原素材互换后，这个形状的正式用途是「焚天_全身烈焰_v3」，不再称作目前引燃。** 原橙色形状的批准不等于本蓝色候选已获确认。

## 最新用途映射（覆盖历史文件名语义）

本候选保留已有内部文件名，避免重画、重复导出或覆盖他人原图；接入方必须按此映射选材，不能按历史 ID 的 kindle/fentian 字样判断当前用途。

- `fentian_conflagration` → **引燃_承接焚天_v4**。正式原图 `D:/alianximod/docs/资源库/特效/火焰/引燃_承接焚天_v4/kindle_v4-strip.png`，SHA256 `3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76`；128×7488，128×144×52。蓝色候选 `textures/fentian_conflagration_blue.png`，SHA256 `B4690B7972F22F3BEAF4237F72E38607AEC9A9DA4249CA7DEF83461E8CCEB77B`。
- `kindle_v3_screenshot` → **焚天_全身烈焰_v3**。正式原图 `D:/alianximod/docs/资源库/特效/火焰/焚天_全身烈焰_v3/fentian_v3-strip.png`，SHA256 `EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070`；48×1216，48×64×19。蓝色候选 `textures/kindle_v3_screenshot_blue.png`，SHA256 `165857578C6EC44E9D4016A0AC598037269EF7C6F228BC9275FEBD40FAD1A9B2`。
- 本作者只读正式源并核对哈希。两项正式原图与本候选原色参考的解码 BGRA 哈希均相同；前者 PNG 文件字节不同但解码像素相同，未据文件哈希差异重画。完整源/参考/像素哈希见 `evidence/approved-source-remap.json`。
- 原橙色用途互换的运行作者为 Sartre `01a1e246-a7e1-7861-acdf-2569d7b9ecee`。本作者未读取/修改其 Java 或运行文件，未独立确认它的接入完成状态。下列旧运行路径是首次 Flash 事实包的输入快照，不冒充互换后的当前运行映射。

本地 Minecraft 1.20.1 客户端 extra.jar 中 `soul_fire_flame.png`、`soul_fire_0.png`、`soul_fire_1.png` 只读抽取到 references，仅为原版颜色参考，不是本次新增绘画，也不送运行接入。

## 颜色模式与资源

候选模式名供 Grok 对接：`SOUL_PALETTE_TEXTURE`。领域判定仍采用已有语义；是否在幽冥内的查询、命中、伤害、尺寸、锚点、相位和生命周期都不由本美术修改。

- 普通火模式：继续原橙火像素与原采样布局。
- 幽冥火模式：使用本目录对应 `_blue.png` 预重配色图集，顶点 RGB=(255,255,255)；alpha、亮度相关运行强度参数仍沿用既有值。**蓝图集上不再乘 (90,160,255)**，否则会再次变暗/偏色。
- 幽冥原生 `nether_gloom_v2` 保持本色与白顶点，不二次映射。
- 本次交接优先使用预重配色资源，避免运行时量化与本次预览出现差异。`textures/soul_luma_lookup_256.png` 是辅助色阶样本，256×1；不是已接入 shader，也不是必须改架构的要求。其整数查表近似不代替已导出的精确像素。

主参考七色：`#0B3843 #085C66 #0795A2 #0EC4CC #44EDE7 #A7FFF0 #E2FFF4`。暗部少量深青，火身青蓝，亮芯浅青近白。截图烈焰（现焚天 v3）六色精确映射：

- `#D2310D → #085C66`
- `#F5540D → #0795A2`
- `#FF8B14 → #0EC4CC`
- `#FFC129 → #44EDE7`
- `#FFE367 → #A7FFF0`
- `#FFF8B7 → #E2FFF4`

其它火焰用同一按亮度排序的连续色阶：在编码 RGB8 上计算 `Y=0.2126R+0.7152G+0.0722B`，按上述六个原色的 Y 对应新色做分段线性插值，四舍五入 RGB；Y=0 对应黑、Y=48 对应 #0B3843，最亮 Y=255 对应 #F0FFFA。它是直接更换像素颜色，不是各通道 RGB 相乘。每像素 alpha 保留原数值，完全透明像素不改；明暗层次保持从暗到亮的顺序，不把整张纹理染成单色。

仅 `liaoyuan_ground_embers` 保留 Y<40 或接近中性的炭灰像素（R-G<10 且 R-B<20）。其它输入都是明确的火焰/火光贴图。未读取/改色金属剑身、岩壳、烟雾、冷却蒸汽、灰烬地面或实体皮肤。

## 图集映射与可编辑源

以下历史输入运行路径均相对 `src/main/resources/assets/xueximod/textures/effect/`。每项候选是 `textures/<id>_blue.png`；原尺寸导出不加边距、不裁剪、不重排。相应 `source/<id>_blue.aseprite` 含三个图层：隐藏原橙色、隐藏旧乘色、可见新青蓝；帧序与 alpha 保持一致。这是从实际输入图集建立的可编辑重配色源，不冒称原始分层绘画文档。用途互换按上一节，不从旧路径反推当前用途。

1. `fire_seed` ← `fire_seed/fire_seed_flame.png`；图集 32×256，单帧 32×32，8 帧，100ms。
2. `kindle_v1` ← 历史输入 `kindle_v1/kindle.png`；64×1024，64×64，16 帧，80ms；保留为旧 KINDLE 比较，不覆盖最新引燃 v4。
3. `fire_spark_v3` ← `fire_spark_v3/fire_spark.png`；64×1024，64×64，16 帧，80ms。
4. `flame_trail_v3` ← `flame_trail_v3/flame_trail.png`；64×1024，64×64，16 帧，80ms。
5. `shock_ring_v1` ← `shock_ring_v1/shock_ring.png`；64×1024，64×64，16 帧；源时长逐帧为 40,40,30,40,40,40,30,40,40,40,30,40,40,40,30,40ms。
6. `seed_sow_v3` ← `seed_sow_v3/seed_sow.png`；64×1024，64×64，16 帧，50ms。
7. `detonate_v1` ← `detonate_v1/detonate.png`；64×1024，64×64，16 帧，80ms。
8. `liaoyuan_ground_flame` ← `liaoyuan_v2/ground_flame.png`；32×960，**32×40、24 帧**，50ms；也供现有自定义目标附着火使用。
9. `liaoyuan_ground_embers` ← `liaoyuan_v2/ground_embers.png`；64×1536，64×64，24 帧，50ms。
10. `liaoyuan_spread_wave` ← `liaoyuan_v2/spread_wave.png`；128×1536，128×128，12 帧，50ms。
11. `fentian_conflagration` ← 历史输入 `fentian_v2/conflagration.png`，**最新用途引燃_承接焚天_v4**；128×7488，**128×144、52 帧**，50ms。源文件和正式 README 均确认该布局；标签 gather 1–3、ten_pulses 4–40、embers 41–52。不得按 128×128 切帧。
12. `kindle_v3_screenshot`，**最新用途焚天_全身烈焰_v3**；48×1216，48×64，19 帧，50ms。标签 sustain_loop 1–10、segment_pulse 11–13、extinguish_once 14–19。蓝色候选尚未指定/创建运行路径。

上述毫秒值记录 Aseprite 原件/预览节奏，不改运行时按年龄、生命周期或真实伤害阶段采样的规则。UV 仍为 U=[0,1]、第 i 帧 V=[i/N,(i+1)/N]，i 从 0 起。最近邻，blur=false，保持现有 clamp 与渲染类型。所有候选属于本任务目录，不复制到游戏。

## 二维实际预览

同帧四栏顺序始终为：原橙火 / 旧乘色 / 新青蓝 / 幽冥 v2 本色参考。前三栏取同一原帧，第四栏是自己的参考动画，不冒充同形状。假人和背景均为示意辅助。

- `previews/fire_seed_comparison.gif`：实际常驻火种，8 帧、100ms，0.8 秒循环。
- `previews/attached_flame_comparison.gif`：实际 `liaoyuan_v2/ground_flame` 火焰贴到实体参照，24 帧、50ms，1.2 秒循环。保留 32×40 原像素；这个二维排列只比较颜色，不设定新的运行挂点/尺寸。
- `previews/kindle_v1_comparison.gif`：旧 KINDLE 输入贴图，16 帧、80ms，1.28 秒循环；不表示最新引燃用途仍采用它。
- `previews/kindle_v3_screenshot_comparison.gif`：现焚天 v3 形状的蓝火候选，46 帧、50ms，2.3 秒演示；持续、短亮、熄灭、空白阶段完全沿用原预览序列。
- 上述四份均有 `_native.png`、`.png`、`_native.gif` 和 `source/review_*.aseprite`；native 为 352×112，放大为最近邻 3 倍 1056×336。
- `previews/palette_and_native_references.png`：幽冥实际配色、MC 两款魂火/粒子参考、六色映射。
- `previews/all_source_shapes_same_frame.png`：12 项关键帧总览，序号对应上列，每格左原色/中旧色/右新色，仅为尺寸缩放阅读图，原始导出尺寸不变。

## Blockbench 与空间交接

适用：火焰附着实体、朝向、遮挡与偏移有空间关系，不能跳过真实 BB。

原 attempt 遇到的外部项目切换已由父级明确协调解除，不再是当前阻塞。依据：父级只读 `wait_threads(timeoutMs:0)` 取得 Planck `01a1e08a-817e-7b60-b465-7a8baaca1f4b` completed/status notLoaded，完成 turn `01a1e245-c927-7bc3-8db5-d5b39a531364`；其 final 声明收到并发提醒后未再操作实例、无下属、写入收束，原工程已经保存。该协调结果由父级提供，本作者没有重复查看其它会话实时内容。

本次先实际调用 `mcp__blockbench.get_capabilities`，确认真实 desktop 5.2.2 / plugin 1.10.1。选择自己的空页 `803970b1-7113-95b6-9e54-ee1415c7edff`，通过真实 GUI 的原生 Group/Cube/Texture 建场景，原生 `Codecs.project.compile/write` 保存，再用 `Codecs.project.load` 重新打开检查。复开页 UUID `7fd9fc60-dd61-6b41-eafd-daefc3fb48ea`。没有 headless、手写模型 JSON 或离线替代预览。

**模型**：`blockbench/nether_blue_palette_art_v1.bbmodel`，SHA256 `E98E82EFED2B227B3957FDBC757D5AAD69B4A4E61769D78685464FA6FDB522E0`；80 个 cube、20 个 group、18 张 texture。原生复开后全部纹理 complete=true、error=0，默认可见焚天 v3 F06 的原色/旧色/新色三片；其它比较模式保存在隐藏 FIRE 元素中。不是可直接接入 Forge 的运行模型。

实际场景参数（仅比较辅助，不改运行几何）：

- 换算：此场景 16 模型单位示意 1 格；假人高 28.8，即 1.8 格参照。X 横向、Y 向上、Z 为假人前方；实体脚底 Y=0，三位假人中心 X=-54/-18/18；幽冥本色参考 X=54。
- 层级：`AUX_*` 为地板、编号与假人；`COMPARE_V3_*_焚天_全身烈焰_v3`、`COMPARE_KINDLEV4_*_引燃_承接焚天_v4`、`COMPARE_RUNTIME_*`、`COMPARE_SEED_*`、`COMPARE_ATTACHED_*` 为对应比较组；`REFERENCE_*` 为幽冥与 MC 魂火参照。所有 AUX 和参考模型只留归档，不进游戏。
- 平面与锚点：零厚度、双面、XY 火焰片；全身比较组旋转中心 `[列X,14.4,0]`。焚天 v3 平面 21.6×28.8；引燃 v4 为 25.6×28.8，以保留 128:144 比例；旧 KINDLE 28.8×28.8；目标附着火 14.4×18，保留 32:40 比例。原点下边缘 Y=0。
- 火种仅放三个 6×6 示意挂点，列中心加 X=-6/6/0，Y=18/18/29，Z=5.44；这是便于看颜色与遮挡的挂载参照，**不是新增运行数量、挂点或尺寸建议**。
- 正面/冻结侧面平面 Z=5.44，与假人头部前面 Z=3.15 相隔 2.29 单位。冻结侧面相机 `[100,28,50]` 看向 `[18,14.4,1]`，只显示新蓝色列，能观察片面与实体距离；不声称运行时固定朝向。
- 整体图采用正交斜视 `[24,48,170]` 看向 `[0,15,0]`；为避免比较片穿入假人，V3/KINDLEV4/RUNTIME/ATTACHED 示意跟随该相机旋转，偏移距离用 `abs(nx)*4.8+abs(ny)*14.4+abs(nz)*4.8+0.64`，n 为相机朝向法线。**这只是本场景的展示参数，不替代现有运行 billboard/offset 算法**。火种三片未套该旋转。
- UV：每张图集按其真实整图尺寸设 UV，south 面取 `[0,(F-1)*frameHeight,width,F*frameHeight]`，F 为 1 起始帧号。透明 PNG alpha 不改。火焰使用 emissive/double 面材质；颜色比较时临时关闭所有比较纹理材质的 SHADE，模拟相同白色顶点条件，而非 Forge 光照。
- BB 交付的是静态关键帧，不假装存在运行时间线；完整像素动画看 Aseprite/GIF。各比较前三列取相同 F，第四列是幽冥自己的 F06；上方小片为原版 MC soul_fire_0 参考，不是新焚天粒子。
- 重新打开 .bbmodel 的默认编辑器镜头可能不同；本次已将真实复开页调成正面可见状态。需要重现截图时以 `evidence/blockbench-captures.json` 的相机、UV、可见性及 `scripts/capture_blockbench.js` 为准，执行前核实自己的项目 UUID，不能盲目对其它文档执行。

**真实视口证据共 13 张**（1280×640，未作外部图像加工）：

- `previews/bb_v3_overall_F06.png`、`bb_v3_side_frozen_plane.png`：现焚天 v3 整体/侧面。
- `previews/bb_v3_front_F11.png`、`bb_v3_extinguish_F16.png`、`bb_v3_off_F19.png`：现焚天短亮/熄灭/完全消失阶段。
- `previews/bb_kindle_v4_overall_F06.png`、`bb_kindle_v4_side_frozen_plane.png`、`bb_kindle_v4_gather_F02.png`、`bb_kindle_v4_embers_F46.png`：现引燃 v4 整体/侧面/聚集/余烬阶段。
- `previews/bb_fire_seed_attached_overall_F06.png`、`bb_actual_target_flame_overall_F06.png`：代表性火种与实际目标附着火来源。
- `previews/bb_runtime_kindle_overall_F06.png`：旧 KINDLE 输入，仅历史对照。
- `previews/bb_reopened_model_front_F06.png`：保存模型原生复开后的真实视口。

全部为连接中真实 BB 的 main Preview 原生按需渲染截图。一次整窗截图工具提示窗口被覆盖/最小化可能有旧 UI，未把该整窗图当落盘证据；实际采用可主动渲染的 3D 视口结果。已有六个外部页的名称、保存状态及所采集的元素/UV/纹理指纹前后相同；尤其 `daozhang_v1_candidate` 保持 saved=false，没有关闭、保存或覆盖。核对见 `evidence/blockbench-before.json` 与 `evidence/blockbench-reopen-verification.json`。

Forge 光照、透明排序、运行中的领域边界切换、各客户端镜头及原版着火层不是 BB 能证明的运行效果。其它七种来源已做二维同帧对照，本场景只为上述五种实体相关代表形状提供空间比较，不声称逐一模拟了所有运行渲染器。

## Grok 后续边界

Flash 提供当前染色覆盖 BatchCarrierVisualRenderer 的 SPARK/TRAIL/SHOCK/SEED/KINDLE/DETONATE，FireSeedFlameClient，以及 FireVisualClient 的焚天、燎原地表、余烬、扩散波、自定义目标火。建议这些入口共用上述蓝火模式与资源映射，避免一部分仍乘色、一部分换贴图。具体实现交 Grok，不在此写 Java。

原版魂火粒子、实体着火层、燎里、炉身还有未覆盖边界。后续应明确按同一蓝火模式处理其中的火焰部分；原版魂火已有本色可直接复用。不要把金属/灰烬/烟气整图强行映射。此次候选没有宣称这些边界已覆盖，也没有生成它们的正式资源。

待替换的是旧乘色表现，普通原橙火仍保留。具体新运行文件名由确认后的 Grok 接入任务登记，本文不创建那些路径。接入回执：**未接入**。用户确认：**未获本版本确认**。迁待验收：**不适用本候选，缺同版确认与接入**。

## 文件核对与写入范围

仅本任务目录有写入；没有 Java、运行资源、制作表、共享索引或其他作者目录改动。`evidence/input-sha256.json` 锁定输入；`evidence/aseprite-reopen-verification.json` 是官方 Aseprite 批处理重新打开 12 个源后的核对：alpha 差异 0、可见轮廓差异 0、可见源像素与 PNG 差异 0。热波原时长已核对并修正源；不是正式代码复审或新增测试阶段。

续派核对原 manifest 的 89 个二维源/导出/参考/预览文件，SHA256 全部未变，二维没有重画或重导。最新正式原图也只读核对，未覆盖。现已补齐同版真实 BB 与交接；完整候选制作完成不等于用户已确认。

没有运行 compileJava、build、jar、打包、runClient 或游戏验证。真实工具异常如实记录，没有把断连记成模型额度耗尽。本次写入已收束，没有任务后台写入/下属代理，GUI 操作已结束。下一步由直接父级展示本版本；用户确认后才可交 Grok 按蓝火资源模式接入。不能将此次完成回执作为确认、采用、运行接入或迁表证据。
