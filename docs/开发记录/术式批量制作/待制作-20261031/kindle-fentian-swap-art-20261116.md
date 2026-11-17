# 引燃 / 焚天确认版互换：美术归档回执

状态：`approved_for_recipient_integration`，不是待用户重复确认。
batchId=`待制作-20261031`；taskId=`fire_kindle_v2_art`；attemptId=`20261116-art-kindle-fentian-swap-04`。
作者可观察 `CODEX_THREAD_ID=01a1e08a-817e-7b60-b465-7a8baaca1f4b`；直接父级为当前派发主会话，沿用其给出的可观察会话 `01a1ce06-b681-7c90-b11d-260d3c169911`，不冒称未知父代理 UUID。

模型角色沿用任务包 `gpt-6-astra max`，合法降级依据仍为批次记录第 408/431 行 Carbon、Argon `Unknown model`。本轮没有新模型探测/换路由/改派 Grok；实际后端服务模型未独立暴露。设计 2/2、实现 3/3 不变。**本阶段额度已用完，返修结果未追加复审。**

## 用户确认及已完成映射

用户逐字原话：

> 这个效果很好但是不适合作为燃烧的特效，因为这个火比较猛不像是初级术式这样你把现在焚天的特效先拿来给引燃用，把这个效果给焚天

- 原焚天 v2 → `fire_kindle`。新图集 `D:/alianximod/docs/资源库/特效/火焰/引燃_承接焚天_v4/kindle_v4-strip.png`，83272 字节，128×7488 / 52 帧，SHA256 `3AE6A95E41D92991B828EDC7491CBB40A18D503CDCF700281A50D901D6AA6A76`。源 `D:/alianximod/docs/资源库/特效/火焰/引燃_承接焚天_v4/kindle_v4.aseprite`，73130 字节，SHA256 `8A9E64C672466E5C1F764860307EF6E24E66A5545A71D725172DDD1BF52F6A11`。
- 全身燃烧 v3 → `fire_fentian`。新图集 `D:/alianximod/docs/资源库/特效/火焰/焚天_全身烈焰_v3/fentian_v3-strip.png`，12139 字节，48×1216 / 19 帧，SHA256 `EF3476EB01225D2E7478F9BBA5430F28BCF2E1D12198F3F482A634C155A04070`。源 `D:/alianximod/docs/资源库/特效/火焰/焚天_全身烈焰_v3/fentian_v3.aseprite`，19782 字节，SHA256 `3EC04641BBBC573BAAABCF088DA61B6B79B7418159C2D1F6F3287D5B8B06BB42`。

四个文件均按字节复制，与来源 SHA256 一致；没有重画像素、变色、重存源或另做候选。原源内旧名字/candidate 元数据作为历史保留，本批准映射说明现在用途。没有把原 v3 写成引燃已采用；更早被拒绝 v2 保留不采用。

## 真实空间交付

同体型双目标原生模型：
`D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/blockbench/kindle_fentian_approved_swap.bbmodel`；
SHA256 `229A4BC82A4503A09994460E971C3895E19F41EB5205414133AE3C176DC8E426`。

主预览（正面/斜视左为引燃、右为焚天；侧图屏幕顺序反转）：
- `D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/previews/gui_front_approved_recipients.png`
- `D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/previews/gui_oblique_approved_recipients.png`
- `D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/previews/gui_frozen_side_distance.png`
- `D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/previews/gui_front_ending_stages.png`
- `D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/previews/gui_reopened_front.png`

批准原话/用途绑定：`D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/approval-mapping.md`。
完整参数/限度：`D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/visual-handoff.md`。
全部产物路径/大小/哈希：`D:/alianximod/docs/资源库/过程归档/引燃焚天_确认版互换_20261116/manifest.json`。

## 接入边界

引燃保留旧焚天几何：H=1.8、scale=1 时面高 2.43、宽 2.16、中心距脚 .81 格；CAMERA_PLANE。原四帧视觉块不等于 4 tick 伤害时钟，播放接收引燃真实 10 tick 段。源标签 F01–03 gather、F04–40 ten_pulses、F41–52 embers；数组区间对应 0–39 主视觉和 40–51 尾焰，源只读实际核对。未读正在修改的 Java，具体接线以 Grok 回执为准。

焚天保留新稿几何：宽 .75H、高 H，包围盒中心+相机支撑面净距 .04 格；CAMERA_PLANE。持续 10 帧，每次真实 4 tick 段触发 3 帧短亮，结束 6 帧收束，不能把 19 帧整条循环。均不改伤害、动作或组合显示。

原生默认深度测试下，引燃中心面会被假人部分遮挡且几何下界低于地面；已如实展示，没有擅自改锚点、前移或关深度。BB 不能证明 Forge 混合/深度状态，Grok 按既有渲染语义接入并记录，而不是据此重新选素材。

计划 runtime：引燃 `textures/effect/kindle_v4/kindle.png`；焚天 `textures/effect/fentian_v3/conflagration.png`。本作者未写这些路径，Grok 可从已稳定归档或对应同哈希原图接入；不再索要已选素材确认。

## 工具事实与收束

Aseprite 1.3.17 本轮实测 live=true、batch ready；隔离 batch 只读打开复制源，确认 52 帧、128×144、全 50ms 和源标签，没有改六个 live 文档。未另造二维 GIF，避免将旧演示时钟充作互换运行验证。

独立 Blockbench Desktop 5.2.2/plugin 1.10.1，新项目 `d8549d69-5160-499c-12b6-87d88f8bc273`，27 cubes/5 groups/3 textures；原生保存并复开为 `b4bf15d9-b19a-a51a-5fd0-c3aefeed7fe6`，3/3 纹理 loaded=true/error=0。截图来自真实桌面主视口即时 WebGL 渲染；不是 headless、桌面控制、生成图或离线拼图。已 view_image 检查全部五张图。

本轮初始当前项目为 `nether_blue_palette_art_v1_20261116`，与任务包先前快照不同；未修改它或其他用户工程。所有五个原项目的可观察 saved 状态和元素/纹理数量前后一致，0 个文档关闭。所有原始源/图集保留。BB 内嵌 PNG 审计发现引燃 RGB 有 825 个通道不同：非透明像素 738 个通道最大差 6/255，透明像素 87 个通道不同（总体最大差 235）；尺寸和全部 alpha 一致。焚天及辅助纹理解码 RGBA 完全一致。原因未另行排查，不宣称全部内嵌图逐像素一致。外部四份正式复制件仍逐字节一致且是接入权威源；不得从 BB 内嵌预览图反导运行资源。

首次只读审计因“全部内嵌 RGBA 相同”断言失败退出 1，随后改为量化并披露差异，诊断脚本退出 0；这不是将失败伪报为像素全等。没有为消除差异重画、替换批准 PNG 或增加 GUI 修复轮次。

共享 MCP 并发提醒在本项保存、截图及复开全部结束后收到；收到后未再向 Aseprite/Blockbench 实例发起写入。此前操作均通过本项精确 UUID 守卫，未观察到操作中的实例抢占；没有据其他聊天等待状态编造 MCP 故障，也不操作其蓝火工程。此后仅本项允许目录内的本地文档、清单与只读文件核验。

文件复制为 PowerShell Copy-Item，退出码 0；文本/脚本使用 apply_patch；`check_archives.py` 为只读文件/像素/哈希检查，不绘制或改图片。无未解决 MCP 调用错误，没有模型新降级或审批绕过。窗口被遮挡时仍用真实主视口即时渲染，不把它说成整窗截图或游戏画面。

本作者只写两个新正式目录、新过程目录和本回执；未改旧焚天 v2、原 v3、拒绝旧稿、Java/src、总索引、表或总记录。未运行 Gradle、编译、打包、游戏；未录连续三维动画。归档与实际复用示意已完成，用户用途确认已获；并行 Grok 接入/编译状态由其回执负责。本回执不替代迁页证据。

所有工具调用已返回，最终交付时写入收束，无后台制作进程，无下属。未发现可用的原生父级短讯入口，不编造发送成功；直接 final 回传，不等待重复批准。
