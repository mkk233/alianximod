# 引爆 v2-3D：三维径向爆炸云候选

## 状态与输入

- 状态：`candidate_pending_user_confirmation`。未接入、未编译、未游戏实测，不可据此迁待验收。
- 版本：`fire_detonate_v2_3d_candidate_20261116`。
- batchId：`待制作-20261031`；taskId：`fire_detonate_v2_art`；attemptId：`20261116-art-detonate-3d-01`。
- 直接父会话：`01a1ce06-b681-7c90-b11d-260d3c169911`。本轮作者的原生 agent_id 与后端实际服务模型未暴露，不编造。
- 用户最新原话：“效果不太行，应该参考原版爆炸的效果三维的扩散开而不是简单的二维效果”。
- 旧 `引爆_短促火团_v2_20261116` 被拒稿完整保留，没有覆盖。本次不是旧整张 CAMERA_PLANE 的旋转版本。
- N/O/DV 原样保存在 `evidence/input-snapshot-used.json`；输入制作中第9行 `fire_detonate`。源表快照 SHA256：`1473D5EF076D4B8B19A59CBCC92DF3A394CDD888FB03E4B4025AC250D1F264B1`，187795字节。本轮没有读写工作簿。
- N不变：本目标即时消费当时共享余额S，资源火伤 `0.2*S*(1+F)`，同次真实释放同目标最多一次；S0没有资源伤害和额外引爆，载体伤害照常。1–9点仍结算，不新增范围伤害、不复制目标余额。燃烧共享余额的顺序关系、陨石仅中心父段等完整边界以输入快照为准。
- 消费越多视觉越强仍保留；本稿视觉扩散空间不是命中范围、伤害半径或碰撞体。
- 本阶段额度已用完，返修结果未追加复审。设计2/2、实现3/3不重置。本轮为授权美术返修，不是正式复审。

## 查看顺序及同版绑定

1. `previews/detonate_v2_3d_main.gif`：首选，橙火/亮灰云，固定同一目标、同一镜头，完整爆开—峰值—破裂—消散。
2. `previews/detonate_v2_3d_side.gif`：独立固定侧上方镜头，粒子世界位置与主视角逐帧完全一致，只有每团朝向跟随该镜头；可看前后厚度和上下面扩张。
3. `previews/detonate_v2_3d_strength.gif` 和 `previews/strength_comparison_detail.png`：从左到右实际消费S0、10、50、100；S0始终没有额外粒子。
4. `previews/detonate_v2_3d_soul.gif`：同位置、同数量、同节奏的幽冥蓝变体。
5. `previews/gui_peak_threequarter.png`、`gui_peak_side_top.png`、`gui_breakup_threequarter.png`：原始真实GUI视口；`previews/puff_keyframes.png` 是单团二维关键帧，不是整体爆炸示意。
6. `blockbench/fire_detonate_v2_3d_candidate.bbmodel`：实际GUI创建、原生保存并复开。183个可编辑Cube，其中104个为4组非零示例的粒子面，17组、16张嵌入贴图。当前保存主视角第7帧姿态。
7. 本文、`manifest.json`、两份Aseprite源、贴图、模型及上述实际预览绑定同一候选版本。用户尚未确认本版本。

GIF不是离线重绘：每条序列由真实 Desktop Blockbench 主视口的20张PNG编码。原始帧均在 `previews/main|side|strength|soul/`，对应相机、粒子位置、帧号在 `evidence/captures_*.json`。编码只做每条序列一致的透明边界裁切、GIF调色板量化和时长设置；原始PNG完整保留。裁切坐标见 `evidence/native-gif-encoding.json`。关键帧拼板仅为补充，不替代动画。

## 二维素材与参考

- 用真实 Aseprite 1.3.17 的 MCP batch Lua 绘制，`live.connected=false`，没有触及用户实时文档。不是ImageGen，也不是Python重绘。
- `source/explosion_puff_orange.aseprite`、`source/explosion_puff_soul.aseprite`：每份32×32，16帧，各50ms，2个可编辑层（灰云壳、亮芯/彩色边）。这是一团粒子的局部生命周期，不是整个三维云。
- `textures/explosion_puff_orange-strip.png`、`explosion_puff_soul-strip.png`：32×512竖条，原尺寸，16帧由上到下。`frames/orange|soul/puff_01.png` 至 `puff_16.png` 为原尺寸帧。
- UV帧k为零基：`[0,32*k,32,32*(k+1)]`。不平滑插帧，纹理最近邻，透明像素不做柔光。末帧完全透明。
- 橙火由淡黄亮芯、少量橙边转为亮灰/中灰云壳，然后偏心开洞、断裂、碎屑收束；蓝变体以 `E2FFF4/A7FFF0/44EDE7/0795A2` 的现有幽冥蓝色族替换亮部，灰云偏冷。两版逐像素alpha完全相同。
- Aseprite重新打开逐帧与strip比较：32帧全部零差异，各50ms。每份alpha像素数：29、79、166、259、355、427、434、397、308、242、144、102、46、20、1、0。
- 只读本地1.20.1缓存中的 `assets/minecraft/textures/particle/explosion_0..15.png`、`particles/explosion.json`、`HugeExplosionParticle`、`HugeExplosionSeedParticle`。提取副本和SHA在 `reference/`、`evidence/local-reference.json`，不是候选运行资源。
- 参考事实：原版局部粒子用随年龄变化的爆炸帧，亮度15728880，生命周期6–9tick；大爆炸种子在不同xyz位置发出多个局部爆炸粒子。候选借鉴“多位置粒子云+亮灰开裂帧”，没有照搬原版4格散布或原版数量，更没有把它们当引爆伤害范围。候选明确增加了径向运动。

关键资产SHA256：

- 橙源：`0C3EC19FE36D420F9D909B57AEFBB059D29739E9883D9ED77795B9EF657DBC86`
- 蓝源：`EADC6C6DD861261C65A54A0A09C678C2F36C55DF79F5E9E4ECA8B93F4A8C4F73`
- 橙strip：`28666B39C8A80E0A18EB04DDAE7F7422A546B179307DB0990F7F8109ADF8D0F7`
- 蓝strip：`C4B656B506B6CB721BCEF39CFED9BA99AA0AE21F2EDED7839057FC3573835856`
- 模型：`000F4370871D97886DAF2BB8411AAAB417847A62D3807E6CAD9786C4D0C26E0E`
- 所有PNG/GIF、输入副本、脚本、文档的哈希和尺寸统一见 `manifest.json`。

## 三维构造与挂点

Blockbench适用：适用。本效果的xyz径向扩张、深度遮挡、每团独立朝向、尺寸和时间关系必须真实展示。

- 换算：16模型单位=1游戏方块。假人高28.8u=1.8b，头/躯干/腿保持普通MC体型比例；地面格为16u=1b。
- 轴向：+X目标右侧，+Y向上，+Z目标正前方。
- 预览命中锚点：相对目标脚底 `(0,16,1.8)u`，即躯干前表面。实际接入应取载体的真实世界命中点，不额外统一加Y1.0，也不继承旧稿0.20b相机推移。
- 每个非零引爆是26团：24个不等大小、不等延迟的外扩云团，另2个短寿命中心亮团。它们拥有**不同的三维世界中心**，不是把整幅爆炸贴到一张面上。
- 单团为零厚度的双面像素面，分别朝向当前相机并保留各自roll。整体厚度来自xyz中心分布；没有封闭立方网格、悬空积木或星形杆，也没有新地面圈。
- 旋转中心是每团当前世界中心。表面朝向=`camera quaternion * local Z roll`。换镜头只改每团朝向，绝不旋转整组世界位置去迎合相机。
- 父子层级：场景组 → 单目标示例组 → `FX_26_WORLD_POSITIONED_CAMERA_FACING_PUFFS` → `FX_PUFF_00..25`。假人/地面/文字全以AUX命名，单独编辑，不进运行资源。
- 锚点在一次命中时冻结；示例不跟随目标移动，不绑定宿主旋转。运行候选同样建议世界命中点短时播放，不做持续包身燃烧。
- 所有粒子方向、局部尺寸、错时、生命周期在 `evidence/particle-seeds.json`；每一时刻真实位置在 `captures_*.json`。这些和制作脚本是交接参数，不是Java实现。

## 时间与空间运动

整体候选20tick=1000ms、单次播放。原始GUI捕获每50ms一帧，播放速度1×；这不是法术冷却或施法时长。GIF末尾另留650ms空白阅读停顿后重复展示，游戏不应跟着无限循环。最后两张GUI图像等同无粒子，GIF合并相同帧后为19个编码帧，总时长1650ms。

对序号i=0..23：

- `dy=1-2*(i+0.5)/24`；`angle=i*2.3999632297+0.23*sin(1.7*i)`；`radial=sqrt(1-dy*dy)`；方向为 `(cos(angle)*radial,dy,sin(angle)*radial)`。
- 起始延迟 `i%4` tick；寿命 `13+i%4` tick；局部完整面宽 `16.5+((i*5)%7)` u；roll=`(i*137.5)%360` 度。
- 最大行进参数 `travel=10.6+((i*7)%9)*0.49` u。
- 局部年龄age以tick计。位移=`direction * travel * (1-exp(-0.32*max(age,0))) * scale`；Y另加 `max(age,0)*0.055*scale` u的轻微上浮。
- atlas帧=`floor(clamp(age/(lifetime-1),0,1)*15)`；age<0或age>=lifetime不显示。不按16帧逐帧固定套用所有粒子，错时和寿命产生厚实的交叠云团。
- 中心两团参数以seeds文件为准：span27u/23u，寿命9/10tick，延迟0/1tick，短行程1u/2u。

可见阶段：0–150ms亮芯快速喷开；200–350ms多团膨胀、灰云出现；400–650ms偏心开裂、团间出现空隙；700–900ms少量碎片收束；950ms所有实例失活。

真实S50第7帧（300ms）粒子**中心跨度**为X1.0702b、Y1.1559b、Z1.2063b；第13帧为1.3012b、1.5723b、1.6028b。面宽另计，因此这些不是整个可见外包尺寸，也绝不是伤害半径。主、侧、蓝三个序列的世界中心与局部帧逐项完全一致。

## 强度与变体

- S0：不建引爆粒子，无额外闪光；示例中只留下目标。
- S10：空间位移和每团面宽统一乘0.65；S50乘1.00；S100乘1.35。数量和时序不变。三组是待确认的美术参数，不是当前程序已实现。
- 若后续需要连续映射，建议 `0<S<=10: 0.40+0.025*S`；`10<S<=50: 0.65+0.00875*(S-10)`；`50<S<=100: 1+0.007*(S-50)`。1–9非零；100以上尚未展示，需在接入参数确认时明确外推或视觉上限，不得擅自把视觉上限变成资源消费或伤害上限。
- 蓝变体只换同一版atlas，所有方向、位置、roll、错时、寿命、scale完全相同。顶点颜色保持白色，避免再次乘旧深蓝把亮部压黑。
- 当前代码是否读取spent及如何传递不由本美术回执改变。按父级输入，现有DETONATE仍是单张billboard，不能声称本稿三维发射器或强度缩放已在游戏生效。

## 材质、视口与保存限制

- 二维alpha是0/255的硬边；nearest、clamp、无柔焦光晕。单团双面；仅有一张实际纹理面，避免背面重复混合。
- 本次真实Blockbench材质：`SHADE=false`、`EMISSIVE=false`（避免该应用的emissive行为破坏alpha）、`transparent=true`、`depthTest=true`、`depthWrite=false`。每帧按相机深度后到前排序各个粒子面的renderOrder。
- Forge建议采用保留alpha的明亮粒子材质、FULL_BRIGHT、深度测试，必要时按单团深度排序；不可把全部粒子强制当作一个整体CAMERA_PLANE，也不可关闭所有深度测试让烟云永远盖住实体。具体渲染管线由确认后接入者实现并验证。
- 主视角相机 `[62,46,106]` → `[0,17,0]`，正交zoom0.34。侧上视角 `[98,81,-23]` → `[0,15,0]`，zoom0.34。强度镜头 `[12,60,280]` → `[0,20,0]`，zoom0.135。
- `.bbmodel` 保存第7帧的可编辑姿态、UV、嵌入纹理和场景层级，不包含可直接播放的原生Animation轨道。动态序列由 `build_scene.js` / `capture_sequence.js` 在真实GUI中逐帧更新几何和UV后捕获；不可把保存的单姿态模型说成Forge可直接加载的动画。
- 渲染排序和部分材质override是预览运行状态，不都由bbmodel持久化。`verify_reopened.js`在真实复开工程后恢复同样的预览材质；复开结果另存 `evidence/reopened_native_viewport.png`。贴图本体、UV、几何的可编辑保存已验证。
- 真实源项目UUID：`5f702182-73a6-cd40-964d-2ad6a7c2a361`；复开UUID：`e9eebb72-fed6-5b89-ab9e-3991002b637d`。两个项目均保留，没有关闭任何旧项目。
- 17个既有项目的几何/UV/纹理/saved状态指纹前后完全相同，包含未保存的`daozhang_v1_candidate`和旧`fire_detonate_v2_candidate`；焚天项目保持不变。证据见 `protected-before.json`、`protected-after.json`。

## 接入边界与待办

- 正式资源路径尚未确定；本目录是过程候选，不是正式采用。确认后可讨论把两张atlas映射到独立 `textures/effect/detonate_v2_3d/`，当前没有复制任何文件到src。
- 待替换是现行引爆v1单张billboard表现，不是把其旧64×1024 strip随便换成32×512就能获得三维效果。确认后需要Grok按真实spent和本次命中位置创建多团3D粒子表现，并核对原伤害逻辑不变。
- 所有AUX、地面、假人、对照布局和整个示意bbmodel都不进入运行资源。不能通过辅助模型增加地面伤害或陨石方块。
- 没有修改Java、正式贴图、总README、制作表、批次记录，也未编译、打包、启动游戏。Forge的透明排序、真实相机透视、多人网络同步、性能、实体遮挡和客户端时序均未游戏验证。
- 下一步：父级向用户展示本版实物并记录本版选择；在同版本明确确认之前继续保持`candidate_pending_user_confirmation`。Rawls另行处理共享索引/表记录，本作者不写。

## 真实工具记录与限制

- 本轮重新发现并调用成功：`mcp__magpie.aseprite__status`、`aseprite__run_lua`、`blockbench__get_capabilities`、`blockbench__create_project`、`blockbench__risky_eval`。版本为Aseprite1.3.17、Desktop Blockbench5.2.2/plugin1.10.1。
- 首次20帧捕获返回原文：`The MCP server blockbench couldn't run risky_eval: it didn't answer in 50s`。当时页面`document.visibilityState=hidden`，逐帧70ms等待被实际延长至约60秒；只读观察文件时间戳确认。已阻止旧捕获继续写入，在第10帧后收束该事务；删除等待timer，改为同步原生渲染后截图，四条完整20帧序列随后成功。没有新增连接桥、换模型、启动第二实例或用headless替代。
- 本地类名初查使用不存在的`ExplosionParticle.java.txt`，Get-Content报不存在；随后在同一本地sources.jar定向确认真实类名`HugeExplosionParticle`并读取成功。
- 初次嵌入贴图验证筛选名用了大写构造名，但fromPath实际保存为小写文件名，返回空列表；已改为真实文件名并增加必须恰好2张的断言，最终像素验证成功。不得把初次空列表算验证通过。
- 作者检查中发现侧视文字板穿过云团、强度图边缘裁切；仅隐藏侧视辅助文字并调节强度镜头后重新真实捕获，最终图片已逐张查看。这是同一稿制作修正，不是追加正式复审。
- 既有Carbon/Argon `Unknown model`证据沿用，不重试、不派Grok；本轮没有429、403、额度耗尽或审批拒绝，不把截图超时改写为额度错误。授权请求模型为`gpt-6-astra` max，实际后端身份未另行查证。
