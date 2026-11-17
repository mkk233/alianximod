# 焚天双层 v4：完成交接

制作状态：`completed_art_preview`；素材状态 `approved_for_recipient_integration`。
素材组合/接入授权：`approved_for_recipient_integration`，依据本目录 `authorization.md`，无需再次索要既定组合接入授权。
batchId=`待制作-20261031`；taskId=`fire_kindle_v2_art`；attemptId=`20261116-art-fentian-dual-layer-05`。
本阶段额度已用完，返修结果未追加复审。

## 已准备原件

正式归档目录：`D:/alianximod/docs/资源库/特效/火焰/焚天_双层叠加_v4/`。

- 背景：`background_fentian_v2.aseprite`、`background_fentian_v2-strip.png`。来源 `D:/alianximod/docs/资源库/特效/火焰/焚天_v2/` 对应源/图。52帧128×144，竖图128×7488，保留原半透明alpha。
- 前景：`foreground_fentian_v3.aseprite`、`foreground_fentian_v3-strip.png`。来源 `D:/alianximod/docs/资源库/特效/火焰/焚天_全身烈焰_v3/` 对应源/图。19帧48×64，竖图48×1216，二值alpha。
- 四份文件逐字节复制，本轮Aseprite实际复开确认52/19帧、128×144及48×64、全部50ms；背景1层，前景4层。哈希及大小见manifest。不重存批准源、改色、重绘、合并运行图或从BB内嵌图反导。

## 固定空间合同

Blockbench适用：同一目标的两层火面有尺寸、挂载、朝向、前后距离、透明及身体遮挡关系。已新建单目标双层工程，不以旧双目标互换工程冒充。

同一辅助假人高H=1.8格，包围盒宽/深各.6格；skyScale=1作为示意取值。换算16模型单位=1格。X横、Y上、+Z正面，目标脚中心为局部(0,0,0)。辅助模型使用AUX_命名，不进入运行资源。

背景高 `max(1.8,H)*1.35*skyScale`，宽 `height*128/144`，中心 `(目标中心X,entityY+.45H,目标中心Z)`。示意高2.43、宽2.16格、中心距脚.81格；模型单位为高38.88、宽34.56、中心(0,12.96,0)，上下界[-6.48,32.4]。不另加向镜头偏移。

前景高H、宽.75H，以插值包围盒中心为原点，沿朝镜头单位法线n偏移 `n*(abs(nx)*hx+abs(ny)*hy+abs(nz)*hz+.04)`。示意半尺寸(.3,.9,.3)格；前景高1.8宽1.35格，正面偏移.34格，支撑面净距.04格；模型单位高28.8宽21.6，正面中心(0,14.4,5.44)。

两层均CAMERA_PLANE，使用完整相机朝向；FULL_BRIGHT及既有染色，背景先、前景后。仅此两层，不加十字、环、火舌或新几何。保持原纹理RGBA及最近邻采样。源帧i（零起）U=[0,1]、V=[i/N,(i+1)/N]，背景N=52，前景N=19。

背景中心面可被身体遮挡，且下界低于脚底。真实BB应如实展示原生深度结果，不擅自前移或关闭深度以伪造改善；BB不能证明Forge最终混合/深度状态。完整运行渲染以Grok接入回执及用户游戏实测为准。

## 动画及运行映射

共享焚天SKY真实4tick段，绝不建立第二套伤害/燃料时钟：

- 背景数组0..39（人类帧F01..40）主动画、40..51（F41..52）尾段；源50ms/帧。原标签gather1..3、ten_pulses4..40、embers41..52为历史源组织，不修改标签。具体主段取样对齐由接入者按固定现行语义执行。
- 前景数组0..9持续循环、10..12由实际段触发短亮。最终实现回执明确自然结束先10..12再13..18，共9tick=450ms；其中纯收束6帧300ms，不将19帧整条循环。
- 两层自然结束分别收束；背景12tick=600ms，前景9tick=450ms，仅视觉尾迹，不延长伤害。非自然结束立即清除。演示阶段或GIF长度不等于运行燃烧期限。
- 背景运行仍引用 `D:/alianximod/src/main/resources/assets/xueximod/textures/effect/fentian_v2/conflagration.png`。
- 前景运行仍引用 `D:/alianximod/src/main/resources/assets/xueximod/textures/effect/fentian_v3/conflagration.png`。
- 引燃kindle_v4保持，暂不设计。仅只读最终 `D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/fentian-dual-layer-impl-20261116.md`；不读Java。该回执确认双层接入、原深度语义和独立尾段。其自身编译字段仍为待统一编译；父级另报告12:01稳定编译exit0、前后1488文件hash相等，本作者未独立读取该编译报告，不能混称亲自编译通过。

## 实际二维和GUI证据

父级已释放GUI。本轮发现并实际调用喜鹊 `aseprite__status/run_lua` 与 `blockbench__get_capabilities/create_project/risky_eval`。Aseprite1.3.17 batch ready、live.connected=false；采用隔离batch，不改live文档。一次Lua导出因dofile作用域取不到sprite报 `attempt to index a nil value (local 'source')`，明确打开两个源后一次重试成功，不是MCP不可用；批准源未保存修改。

二维：`previews/dual_layer_2d_preview.aseprite` 是独立演示源（不是新运行素材）；`dual_layer_2d_native.png` 144×162，`dual_layer_2d_nearest.png` 最近邻4倍576×648，`dual_layer_2d_ending.png` 尾段；`dual_layer_demo.gif` 52帧×50ms。Aseprite组合既有帧，只绘制AUX假人色块；背景144×162，前景90×120置于(27,15)，目标高120像素，符合固定世界比例。二维用平面假人遮住部分背景，只是正面读形，不模拟完整3D深度。演示前40tick存活、每4tick提示，末12tick自然尾段；前景末段前3tick短亮再6tick结束，其后不再绘制。总2.6s仅演示，不是焚天寿命。

真实Blockbench Desktop5.2.2/plugin1.10.1，新工程UUID `349751a0-a77d-026b-4ead-bc3a553fd738`，11cubes/3groups/3textures；原生保存 `blockbench/fentian_dual_layer_v4.bbmodel`，`Codecs.project.load`复开为 `fe507dca-652f-f434-b477-f92726a559a7`，3/3纹理loaded、error=0。每次变更守卫目标UUID，未发生抢占；10个旧工程的UUID/saved/元素数/纹理数前后一致，0文档关闭。

四张900×760真实桌面主 `Preview.selected.screenshot` 即时WebGL视口：
- `previews/gui_front.png`：背景F05、前景F11，正面段短亮。
- `previews/gui_oblique.png`：背景F08、前景F08，两面跟随斜视相机。
- `previews/gui_frozen_side_distance.png`：F05/F11，冻结正面两火面再转侧距镜头；运行中仍跟随相机。
- `previews/gui_ending.png`：自然结束第6tick（零起偏移5），背景数组45/F46，前景数组15/F16，同时各按原尾段收束。

仅本项目火面材质使用SHADE=false模拟fullbright、depthWrite=false与背景/前景renderOrder=1/2，对齐最终回执只写颜色、不写深度、背景先前景后的语义；保留depthTest=true，没有为了视觉前移背景。此设置为视口演示状态，复开脚本重新施加，不声称bbmodel保存了Forge材质。截图是实际主视口，不是整窗或游戏截屏；document visibility记录在evidence。未录BB连续动画，不扩展镜头。

已view_image检查四张GUI图及二维原尺寸、放大、尾段三张PNG。BB内嵌图的重编码差异另列manifest审计；正式接入只能用外部同哈希原件。没有把内嵌图反导为运行资源。

本作者未编译、未打包、未运行游戏；未写src、表、共享索引/总记录或旧目录；未启用桌面控制/headless/imagegen；无下属。美术实例操作及本项写入最终已收束，无后台制作或轮询。已授权双层制作完成，不重复索要选稿；本记录不代替游戏验证或迁页证据。
