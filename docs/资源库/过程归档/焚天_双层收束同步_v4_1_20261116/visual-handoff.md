# 焚天 v4.1：仅自然结束九 tick 退火

batchId=`待制作-20261031`；taskId=`fire_kindle_v2_art`（关联fire_fentian）；attemptId=`20261116-art-fentian-burnout-sync-06`。
修复已授权；本次新尾段预览状态为 `candidate_pending_user_confirmation`，未声称用户已经看过或再次确认。既有v4满火版获确认的事实不扩展为本次新系数已获确认。
本阶段额度已用完，返修结果未追加复审。

用户原话（按本次任务包逐字记录）：

> 这个版本很好，但是做背景的老特效没有自然熄灭动画而是直接消失和全身的燃烧效果不同步，把这个小瑕疵修一下

只读依据：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/fentian-burnout-sync-impl-20261116.md`。该回执的F40满火→F41零星，不能靠只保留F40一拍作为本次视觉交付。本作者不审Java，不重新裁决机制；本次仅提供最小alpha过渡及实际预览。

## 最小方案与明确取样

不新增运行贴图，不重画像素，不改两个批准PNG/.aseprite。持续0..39逐字节原样；满火、前景序列、几何、染色全部保持。

自然结束到达时，捕获背景**最后实际显示的持续帧索引** `heldFrame∈[0,39]`，并冻结结束前实际 `skyScale`。仅在没有可用持续帧时fallback索引39。本预览用索引39/F40作为满火尾端样例，不要求运行每次强制跳到F40。不要从结束包余额重新算尺寸。

以同一自然结束起点的 `elapsedTicks` 作为两层唯一视觉结束时钟。零起tick 0..8的数组如下，完整机器可读值见 `timeline.json`：

```text
tick                   0     1     2     3     4     5     6     7     8
held sustain alpha   1.00  0.94  0.82  0.66  0.48  0.30  0.15  0.05  0.00
ember alpha          0.00  0.18  0.36  0.55  0.72  0.82  0.65  0.35  0.00
ember source index     40    41    42    44    45    46    48    50    51
foreground index       10    11    12    13    14    15    16    17    18
```

所有源索引零起。背景人类帧号分别F41/F42/F43/F45/F46/F47/F49/F51/F52；冻结满火样例F40。前景不改：前三tick短亮、后六tick终止，索引18原图全透明。

背景先按普通source-over提交heldFrame，alpha乘第一行系数，再在**同一个原背景矩形、同一深度/UV区域尺度**提交余烬样本，alpha乘第二行系数；随后绘制原前景。不是新增空间层片，不另加偏移、RGB变暗或加法发光。原source alpha仍参与相乘，RGB和现有染色保持。两次原背景采样也可实现为等价单次组合，但不可丢掉source-over顺序。

系数按 `elapsedTicks + partialTick` 在相邻键间线性插值；取帧使用floor后的tick。到elapsed≥8，两层已全透明；第8tick保持透明，到elapsed≥9统一删除纯视觉状态。这样九个tick槽完整保留，与前景序列同步归零，末端没有残留3tick。非自然结束、实体消失及既有卸载清理保持原行为；不增加伤害、燃料或资源时钟。

这只是受授权的视觉修补参数，不以GIF的450ms总长重设术式燃烧期限。

## 空间与运行合同不变

同一H=1.8格、包围盒宽深各.6格目标；16模型单位=1格。背景高 `max(1.8,H)*1.35*skyScale`、宽height×128/144、中心Y=entityY+.45H；本示例skyScale=1，高2.43宽2.16、中心距脚.81格。前景高H、宽.75H，以插值包围盒中心沿相机法线偏移 `n*(abs(nx)*hx+abs(ny)*hy+abs(nz)*hz+.04)`；正面偏移.34格。

两层CAMERA_PLANE/FULL_BRIGHT/既有染色；背景先前景后，保留原深度测试、NO_CULL和不写深度。不自移背景，穿身体及低于地面的原遮挡保持。引燃不动。

运行继续引用原 `textures/effect/fentian_v2/conflagration.png` 与 `textures/effect/fentian_v3/conflagration.png`。此目录的九帧图仅为**预览烘焙候选**，不建议复制到运行资源，更不能从BB内嵌图反导运行贴图。源/参数及原模型哈希见manifest；Grok后续按本表接入，接入及编译未由本作者执行。

## 实际二维产物

Aseprite1.3.17，batch ready/live.connected=false。通过喜鹊真实 `aseprite__run_lua` 在隔离batch只读打开批准52帧与19帧源，生成预览；没有修改live文档或保存批准源。

- `source/background_burnout_alpha_preview.aseprite`：128×144，9帧，每帧50ms，冻结满火alpha与原余烬alpha两个可编辑预览层。
- `textures/background_burnout_9tick_preview.png`：128×1296，9帧透明预览条，只是上述source-over的烘焙，用于BB准确展示，不替换原持续条。
- `source/dual_layer_burnout_preview.aseprite`：144×162，9帧，包含AUX假人和两层结束演示。
- `previews/burnout_9tick.gif`：576×648，最近邻4倍，9×50ms。以实底(32,39,45)呈现alpha渐隐，避免GIF二值透明掩盖渐隐；循环回起点不是运行复活。
- `previews/burnout_native_t0.png`及`burnout_t0/t3/t6/t8.png`：原尺寸参照和关键帧。只绘AUX假人色块，未重画火焰。

背景每帧alpha总和实际为1487160、1399764、1219361、981020、713742、451732、223087、76103、0，逐步下降；前景最后一帧alpha总和同为0。这不是满火延后一拍再跳零星。

## 真实Blockbench副本与限制

已从v4原生加载副本，未重做建模，11cubes/3groups/3textures。模型 `blockbench/fentian_dual_layer_burnout_v4_1.bbmodel`；背景只换为Aseprite九帧预览条及对应UV，前景只选原帧；元素尺寸/位置/原点/旋转及完整outliner与v4原件只读比较相同。

实际Desktop5.2.2/plugin1.10.1。原v4工程 `fe507dca-652f-f434-b477-f92726a559a7` 未覆盖关闭。创建副本 `d9c43eb2-d302-684c-1927-59016d61f4f9`，首次复开 `7fa628fc-3a07-adca-a574-ce45af530859`；修正预览材质后保存并再次原生复开为 `2ddaf358-56ef-cd78-263a-4e6afb143ef0`。全部3纹理loaded/error=0，12个原项目可观察状态前后一致，0文档关闭。

初次自检发现BB的emissive分支把输出alpha强制为1，不等价于游戏FULL_BRIGHT。只在本项副本改为BB默认透明材质并关闭面向明暗（SHADE=false、EMISSIVE=false），保留depthTest=true/depthWrite=false；不改贴图和游戏参数。已用该配置重取最终图并复开，旧错误视口未作为最终交付。纯预览映射修正，不是模型复审。

最终四张900×760 `previews/gui_burnout_t0/t3/t6/t8.png` 均来自真实桌面主 `Preview.selected.screenshot` 即时WebGL。全部view_image检查；这是透明PNG，不是整窗或游戏截图。它们保留原alpha，不做离线底色拼图。某些查看器忽略alpha时RGB看似不变，不能据此误判未渐隐：背景同一取样点(300,420)在t0/t3/t6/t8的alpha为255/168/38/0；Aseprite实底GIF用于直接观看退火强度。BB截图只说明几何及选定tick，不证明Forge透明混合已验证。

本项无连续GUI录像，仅四个必要结束阶段。原生副本、截图与参数同版哈希见manifest。BB内嵌PNG可能重新编码，另列只读比较，不当作原始像素权威。

## 写入边界与后续

已读取适用AGENTS、美术技能及模板、上轮已收束交接和本次实现回执；没有读Java或复审代码。只写本新过程目录及独立回执。未写旧源、正式资源目录、src、工作簿、共享索引/批次总记录；未编译、未游戏、未用computer-use/headless/imagegen；无下属。

新系数与尾段仅代表本次已授权制作的交付，不称新版已由用户再次确认。父级可按既有修复授权继续安排Grok最小落地与编译，并展示本版；本美术记录不代替游戏实测。最终工具占用与文件写入已收束，无后台轮询。
