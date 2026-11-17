# 留焰 v1 空间补全交接（美术草稿预览齐全 / 待用户新3D确认）

日期：2026-11-12。B=fire_flame_trail；batchId=待制作-20261031；taskId=flame_trail_art_preview。三维 attemptId=20261112-mcp-restored；本次二维补全 attemptId=20261112-aseprite-batch-preview。

本次交付状态：美术草稿预览齐全，待用户新3D确认。原真实 Blockbench 工程、必要视角和关键阶段已收束且没有重做；本次通过 Aseprite 原生 batch 后端从同一份 v1 复制源补齐独立最近邻 PNG 和 GIF，并实际查看。明确为“Aseprite原生批处理预览，非GUI操作”，不声称 live GUI 成功。用户本次澄清批处理满足二维工具步骤，无需强求现场GUI；真实GUI/禁用headless冒充的要求仍用于Blockbench。旧live失败保留为历史，不再构成二维步骤阻塞。未获用户新3D确认、未正式采用归档或接入，不具备迁待验收资格。

## 输入、状态与确认绑定

- 现行输入按本次父级任务包及已收齐的 Flash 事实包执行。制作中第3行，B3=fire_flame_trail；未重新领取、未读取/修改制作表。N/O 原文和已批准参数保持不动。
- 原二维是 2026-10-31 用户暂用 v1，本次不重画。旧批准不代表本次三维已确认。
- 功能：本次未修改、未复核；按输入存在首版 fire_spark/fire_orb/fire_meteorite 宿主。组合示意不证明通用宿主支持已实现。
- 美术：同版 Aseprite 原生批处理二维预览及真实 Blockbench 三维草稿预览齐全；正式采用仍待用户新3D确认。
- 用户确认：本次3D待用户确认。
- 正式接入：本次未接入，不复制到 src，不交 Grok 接入，不迁待验收。
- 原批次设计2/2、实现3/3保持，未追加正式复审。绘图/预览制作及文件映射记录不记作新增复审。
- Blockbench 适用：涉及路径、采样位置、尺寸、环绕、前后遮挡、生命周期关系。

确认对象绑定本目录原 v1 源/PNG、新增 Aseprite batch 接触表/GIF、下列未变更 .bbmodel、实际主视口截图、path-sampling.json、capture-log.json 及本文参数。父级接收后展示这组同版本产物并取得用户新3D确认；本记录不代替用户回复。

## 文件映射

本目录绝对路径：D:/alianximod/docs/资源库/过程归档/留焰_v1_空间补全_20261112/

- 二维源字节副本：flame_trail-v1-source.aseprite。原件 D:/alianximod/docs/资源库/特效/火焰/留焰_v1/flame_trail.aseprite。二者 SHA256：656C31D277D03FDBF9D4A6AAF5C398CB13ED1F0D97B07240928071B25C7FFA55。
- 原尺寸导出字节副本：flame_trail-v1-strip.png。原件 D:/alianximod/docs/资源库/特效/火焰/留焰_v1/flame_trail-strip.png。二者 SHA256：F1B0D723B4AEA57B37FCA4928AFAF492E8F1020D864183F36EDABF379BE95284。
- 尺寸：每帧64×64，整条64×1024，16帧从上到下 F01→F16。原80ms/帧仅为展示节拍，1.28秒一轮，不是世界寿命。
- 本次正式交付的二维草稿接触表：2d-v1-contact-aseprite-batch.png，848×848，最近邻3×，4列4行，从左到右/从上到下F01→F16。SHA256：31C6DAD6C419731EDB9FC92E780C4AE12704CA71B069D2756C185428D0BB6249。
- 本次二维GIF草稿：2d-v1-80ms-aseprite-batch.gif，288×288，最近邻4×，16帧，每帧80ms，总1.28秒；保持源帧顺序和时长。SHA256：49A7364DFA29CA87E54F091241B2DBB8737765BD1BE1AA24177745D9945D383A。
- 上述PNG/GIF均由Aseprite1.3.17原生批处理生成，非GUI操作。棋盘底与边框只是预览背景；原64×64源画面未重画、未增加动画帧、未改变时长。GIF按格式量化调色板，不是运行透明纹理；游戏素材仍以未变更原条带为准。80ms展示节拍与40tick世界寿命分开，未新增40tick重定时GIF，也未修改运行映射。
- 二维导出脚本：export-aseprite-batch-preview.lua。经原生aseprite__run_lua调用，backend=batch、sprite=本目录复制源、mutates=false、save=false；只显式保存上述新增预览，不回存原源。
- 前一attempt独立素材接触表保留：2d-v1-contact-native-blockbench.png，1152×904，最近邻3×。这是 Blockbench 原生画布按同版 PNG 裁片所得，底部“Aseprite阻塞”文字仅反映当时状态；本次不改此历史成果，当前二维工具证据用上方新PNG/GIF。SHA256：18390BFAD475F1ECA741332B4A81DE078B1EDDB4298D36FC83224A8E7DF5CE68。
- 原帧裁片：v1-frame-01.png 至 v1-frame-16.png，64×64；由真实 Blockbench 中载入的同版条带裁出，非重画。透明像素的浏览器解码/编码不是原 PNG 字节复制，确认用上方完整原条带 SHA 锚定。
- 主可编辑工程：flame-trail-v1-spatial-draft.bbmodel。SHA256：C2FF79809435C5E219A4E7DF727B30719CB93E1F5CEF9F23EE7F4084A37ADC31。
- 整体透视：viewport-overview-t32.png。SHA256：F1157C7A1CC4D2C22C5BD907EA5F955DBE3A1D00886BEFCAECC860DD355B8324。
- 侧视：viewport-side-t32.png。SHA256：41B3106C75FB93FEBDE3FFC6AF17D92D9BFD3C3B6C81A9ACF28403FCB80AE3E4。
- 背视：viewport-back-t32.png。SHA256：A4CD59A4DF9ACBF7AF81253774C3E119C57A4F2ADFDB78451C138D775DE209A7。
- 尺度近景：viewport-scale-close-t32.png。SHA256：7100642E52F799D5E03546A6F04E8A2407117DBEAE7731757DC1F8853EFFE818。
- 连续关键阶段：viewport-phase-t00/t08/t20/t32/t48/t64/t72.png，每张1706×1215。分别为起点、延伸中、继续延伸、演示运动终点、旧点消退、末端余焰、全部熄灭。对应 phase-t00/t08/t20/t32/t48/t64/t72.bbmodel 是同一版本的阶段快照，并非额外美术版本。
- path-sampling.json 保存每点位置、出生时刻、采样数量/间距；capture-log.json 保存真实视口相机、阶段、存活点数及截图路径；asset-manifest.json 保存产物绝对路径、大小和 SHA256。
- 制作脚本：build-spatial-preview.js、refine-spatial-preview.js、capture-phases.js、capture-necessary-views.js。仅供在该真实 Blockbench 项目中通过原生 MCP 复现，含项目 UUID 防护；不是后台调度或运行资源。

## 空间、材质与时间

- 明确采用16模型单位=1世界格。地面网格每16单位一格，人偶高28.8单位=1.8格。火焰采样面8×8单位=0.5×0.5格；标尺8单位长，中间刻度4单位=0.25格。贴图透明边缘不扩大判定。
- 坐标：+Y向上，+X为直线路径前进方向，+Z为场景深度。三组场景并列仅为方便对照，不表示三个载体必须同时出现。
- 采样平面中心就是路径采样点。点出生后留在已经过的世界位置；白色“示意辅助_移动宿主”继续前进。
- 三条演示路径：直线21点，最大间距4单位；上升下降31点，最大间距约3.94113单位；旋转/环绕50点，最大间距约3.94449单位；每载体均小于批准上限256点。示意路径几何和数量不是新运行参数。
- 环绕只沿青色中心线与离散点生成，不填满中心、不把整个环绕/漏斗体积当成伤害。不存在原版火方块、改地形或新攻击逻辑。
- 层级：根组“留焰_v1_空间补全_草稿_待用户确认3D”下有三组“留焰_…_采样点”和“示意辅助_地面_玩家_宿主_标尺_说明”。所有人偶、地面、青线、标注和白色载体都是示意辅助，不进入运行资源。
- 每张采样面使用原条带对应64×64帧，UV覆盖整帧，不拉伸为连续带。无厚度，不构造新3D伤害网格。
- 所有实际截图均由脚本将采样面重朝当前真实主视口相机后拍下；无沿路径滚转。保存的 bbmodel 是该视角的固定平面朝向，手动转动相机不会自动维持 billboard，须执行对应相位/相机操作重新对齐。它不是运行时 billboard 实现。
- 当前主工程保存为 T32 整体透视，平面 Euler 约[-13.0343°,26.5651°,0°]，origin在各采样点。该旋转仅是截图相机朝向，不是留焰的内在滚转。
- 世界单点寿命40tick；示意 age∈[0,40)可见，age≥40隐藏。帧映射 floor(age×16/40)，转为1基编号F01至F16。
- **衰减为美术说明用线性 alpha=1-age/40，不声称运行代码实际采用这条公式，也不构成修改运行逻辑的指令。** 颜色来源仍是同版原帧，年龄衰减贴图只存在示意工程。Blockbench default透明材质 + force_translucent 用于展示，不用其 emissive 模式将 alpha 当亮度。
- 宿主0至32tick走完示意路径后停住；32tick、路径速度与采样出生相位都是演示选择，不是施法时长/真实宿主速度。最后一点在T72消失。三维没有输出GIF，三维动态交付仍使用原真实视口关键阶段序列；本次新增GIF仅为二维v1原80ms展示节拍。

## 本次 Aseprite batch 补全证据

- 初次即调用 mcp__magpie__aseprite__render_preview({backend:'batch',sprite:绝对复制源路径,frames:'all',columns:4,maxSize:768,checkerboard:true,pixelGrid:false}) 成功，实际返回并查看16帧像素预览。无循环探测，无审批拒绝。
- mcp__magpie__aseprite__run_lua 以 backend='batch', mutates=false, save=false 加载相同复制源，运行本目录 export-aseprite-batch-preview.lua，成功保存新PNG/GIF；返回Aseprite1.3.17、ui=false、64×64源、16帧、16个80ms。使用原生Aseprite脚本API生成最近邻副本并合成预览背景，未使用ImageGen或其他绘图程序重画。
- 新PNG已通过view_image实际查看；新GIF再次经Aseprite batch render_preview加载全部16帧并实际查看，再以batch run_lua只读读取GIF，确认288×288、16帧、全部80ms、总1280ms、ui=false。无源回存。
- 更新文档前对照旧manifest的45个既有文件，哈希变更为0；原v1复制源、原条带、全部3D工程/视口和旧脚本保持不变。本次仅新增二维PNG/GIF/Lua脚本，更新本交接、独占回执和manifest。
- 本次未调用Blockbench重做或编辑三维，不恢复live GUI尝试、不关闭用户文档、不改全局设置。批处理完成二维要求不表示GUI连通问题已被修好。

## 前一 attempt 的 live 故障与真实 Blockbench 证据（保留历史）

- 本会话实际发现并调用 mcp__magpie__aseprite__app_state / open_sprite / run_lua / render_preview / api_docs / status。
- app_state 返回 live、ui=true、Aseprite1.3.17、sprites=[]；一次只读 run_lua 返回 app.fs.isFile=true、ui=true、count=0。状态查询后来也返回 listening=true、connected=true。
- open_sprite 打开原 .aseprite 与草稿 PNG 均返回“didn't answer in 50s”；run_lua 的 app.open 副本与 Image{fromFile=...} 同样50秒超时。一次只读调用返回“Aseprite disconnected before answering”，后续状态曾恢复；这不证明打开文件成功。render_preview 返回无活动文档。没有Aseprite成功打开/保存的证据。
- 源文件/PNG 均存在且哈希匹配；原 .aseprite 头为64×64、16帧、80ms字段。未修补原件、未重启/关闭实例、未改全局配置、未转桌面鼠标控制。前一attempt当时留待父级协调；现已按本次授权通过Aseprite原生batch补齐，不再要求补GUI作为交付条件。
- 本会话实际调用 mcp__magpie__blockbench__get_capabilities，返回 desktop/win32、Blockbench5.2.2、plugin1.10.1。
- create_project 创建 free 项目 flame-trail-v1-spatial-draft-20261112，UUID=e380502f-05e4-5cab-c7a2-4ff5f7e3c7c5。最终514元素、8组、71贴图，0原生动画；阶段由真实主视口逐相位捕获。
- risky_eval 在真实编辑器中导入纹理、创建 Cube/Group/Texture、执行 Undo 事务、调用 Codecs.project.compile 保存工程；截图使用 Preview.selected（id=main、canvas.isConnected=true）.render() 后取其实际 canvas.toDataURL，并原样存PNG。未用offscreen、headless、离线合成、ImageGen或手绘假截图。
- capture_app_screenshot 报 document.visibilityState=hidden，应用整窗截图可能旧画面（返回空工程旧帧）。该整窗图没有作为最终预览证据。最终PNG来自真实已连接主视口强制render，并已读取查看，不依赖隐藏窗口自动重绘。
- 一次读取 require('electron') 返回“The module electron is not supported”，未绕过、未改配置、未通过桌面前置窗口。
- capture-phases 的原生调用50秒超时，但其既有异步任务随后完成：capture-log.json 与最终工程于20:59:17写出。未重跑该捕获任务；后续必要视角调用成功返回 writesSettled=true，已记录完整产物。超时不改写成额度耗尽，也不等同执行立即停止。
- 原冷却项目 UUID=fe1b1c81-3962-2fbb-bc94-6a37e1b6e5ef 始终保留，前后均saved=true。未关闭或覆盖其内容。

## 运行边界和下一步

运行资源按父级事实包已是 src/main/resources/assets/xueximod/textures/effect/flame_trail_v1/flame_trail.png，同原条带哈希；本次未重查源码或追审旧README记录差异，未写运行文件。待替换/正式接入的决定须由同版本确认后的Grok接入者另行给回执，不能把本工程和辅助模型直接复制到游戏。

Blockbench不能证明Forge内相机朝向、渲染排序、穿墙遮挡、碰撞/伤害、真实载体支持、实际帧映射或alpha公式已如此实现。没有代码改动、编译、游戏验证、打包或启动游戏。制作表和共享README未写，仍留制作中。

父级下一步：接收本次预览齐全回执；展示新增Aseprite batch接触表/GIF与原整体/侧背/尺度/关键阶段；明确二维为原生批处理、非GUI操作；取得绑定上述同版本产物的用户新3D视觉确认。无需再等live GUI步骤。未确认前不正式采用归档、不送接入、不迁页。

写入已收束；没有子代理，没有后台继续制作或监控任务。原生父代理ID未暴露，直接父级按本次真实spawn关系；本会话没有可用的原生父级消息工具，不猜ID、不用Flash ID或其他聊天工具代替，以完整final回传。

