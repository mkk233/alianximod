# 引爆 v2-3D 正式采用版视觉交接

## 确认状态与来源

- 美术状态：`approved_for_adoption`，2026-11-16用户原话**“可以先采用这一版”**。直接父会话`01a1ce06-b681-7c90-b11d-260d3c169911`转达，绑定真实main/side GIF及版本`fire_detonate_v2_3d_candidate_20261116`。
- 正式归档：`D:/alianximod/docs/资源库/特效/火焰/引爆_三维扩散_v2/`。
- 候选来源：`D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116/`，该目录未改。以下参数全部沿用该确认版，不新画、不改空间参数。
- batch/task：`待制作-20261031` / `fire_detonate_v2_art`；归档attempt：`20261116-art-detonate-3d-archive-02`；原制作attempt：`20261116-art-detonate-3d-01`。
- 实施状态：**接入进行中，未编译、未游戏验证**。Harvey `01a1e377-93fa-7cc3-ac7f-4bd275c32916`独占src接入，本作者仅归档。未取得接入完成或编译回执，不提前迁待验收。
- 本阶段额度已用完，返修结果未追加复审。设计2/2、实现3/3保持不变，本次无正式复审。
- 模型/版本字符串保留历史`candidate`命名以保证字节不变，当前美术采用状态不再是待确认。历史证据中的旧状态不覆盖本次确认。

## 绑定文件与输入

- 两份可编辑源：`source/explosion_puff_orange.aseprite`、`source/explosion_puff_soul.aseprite`。
- 两条导出：`textures/explosion_puff_orange-strip.png`、`textures/explosion_puff_soul-strip.png`。
- 预览：`previews/puff_keyframes.png`、`detonate_v2_3d_main.gif`、`detonate_v2_3d_side.gif`、`detonate_v2_3d_strength.gif`、`detonate_v2_3d_soul.gif`及对应真实GUI关键PNG。
- 模型：`blockbench/fire_detonate_v2_3d_candidate.bbmodel`，SHA256 `000F4370871D97886DAF2BB8411AAAB417847A62D3807E6CAD9786C4D0C26E0E`。
- 确认绑定main GIF SHA256：`054C4B5F8363DA115F0A9472DCADE60F9E7220E869974CE8BBDCED3DEB80E98B`；side GIF：`179EF78C79B65A637CEF58251EBD3C7273BDB553A4099D84DA7B939251B1B971`。
- 橙strip SHA256：`28666B39C8A80E0A18EB04DDAE7F7422A546B179307DB0990F7F8109ADF8D0F7`；蓝strip：`C4B656B506B6CB721BCEF39CFED9BA99AA0AE21F2EDED7839057FC3573835856`。
- N/O/DV输入：`provenance/input-snapshot-used.json`，SHA256 `0229D3A6C5E064E12ED6E144982658D2F9E25B6195AE2D2DF4B5A629C7FA0033`。其原表快照187795字节、SHA256 `1473D5EF076D4B8B19A59CBCC92DF3A394CDD888FB03E4B4025AC250D1F264B1`，制作中第9行`fire_detonate`。
- 全部文件哈希与候选对应关系在正式`manifest.json`和`provenance/archive-verification.json`，复制资产逐字节相同。

N没有变化：本目标即时消费当时共享余额S，资源火伤`0.2*S*(1+F)`；同次真实释放同目标最多一次，S0只有载体自己的命中，1–9照实际余额结算。与燃烧争用同一余额；不增加范围伤害、不复制目标余额、不加总锁。陨石仅中心父段，不自动进入热波；完整边界保留在输入快照。视觉扩散尺寸不是伤害半径。

## 二维与变体

- 每份源32×32、16帧、每帧50ms、2可编辑层；导出32×512竖条，从上到下为局部粒子帧0–15。这不是把整体爆炸画在一张面上。
- 零基UV：`[0,32*k,32,32*(k+1)]`；nearest、clamp、不插值、不柔焦；alpha为0/255，末帧完全透明。
- 灰云壳与亮芯分别可编辑。橙版由淡黄亮芯/橙边转亮灰、偏心开裂、碎片熄灭；蓝版使用相同形状/alpha/几何/节奏，以`E2FFF4/A7FFF0/44EDE7/0795A2`色族替换亮部，灰云偏冷。
- 顶点RGB保持白色，不再乘旧深蓝色。局部alpha像素数仍为29、79、166、259、355、427、434、397、308、242、144、102、46、20、1、0。
- 原制作阶段已通过Aseprite重新打开逐帧比对，32帧零差异，模型嵌入两条贴图与导出逐像素相同；本归档通过字节一致性沿用这些验证，不重新启动软件。

## 三维空间与挂点

Blockbench适用：适用，已经真实GUI制作和展示。确认绑定包括三维分布、深度遮挡和动态预览。

- 换算：16模型单位=1方块；普通MC体型假人高28.8u=1.8b，地面网格16u=1b。
- 轴向：+X目标右侧，+Y向上，+Z目标正前方。预览命中锚点相对脚底`(0,16,1.8)u`。
- 实际挂点取载体真实世界命中点，不统一额外加Y1.0，不沿用旧二维稿0.20b相机推移。锚点命中时冻结，不随目标移动或旋转。
- 每个非零效果26团：24个不等延迟/尺寸外扩云团和2个短寿命中心亮团。每团有独立xyz世界中心，不把整组位置旋转到当前相机平面。
- 每团零厚度双面像素面，旋转中心在自己的当前世界中心，朝向`camera quaternion * local Z roll`。整体厚度来自不同xyz中心，不来自实体立方体。
- 层级：场景组 → 单目标组 → `FX_26_WORLD_POSITIONED_CAMERA_FACING_PUFFS` → `FX_PUFF_00..25`。所有AUX假人、文字、地面和整体示意模型不进入运行资源。
- 粒子方向、延迟、寿命、面宽和roll的权威具体值：`provenance/particle-seeds.json`，与候选同字节。

## 时序和运动

整体20tick=1000ms单次播放；真实GUI序列每50ms一帧，1×播放，不是施法时长或冷却。GIF另有650ms空白展示停顿后循环，游戏不照搬循环。原始20张GUI帧末尾两张同为无粒子，GIF合并后19个编码帧，总1650ms。

外扩团i=0..23沿用：

- `dy=1-2*(i+0.5)/24`；`angle=i*2.3999632297+0.23*sin(1.7*i)`；`radial=sqrt(1-dy*dy)`；方向`(cos(angle)*radial,dy,sin(angle)*radial)`。
- 延迟`i%4` tick，寿命`13+i%4` tick；完整面宽`16.5+((i*5)%7)` u，roll=`(i*137.5)%360`度。
- 行进参数`travel=10.6+((i*7)%9)*0.49` u；位移=`direction*travel*(1-exp(-0.32*max(age,0)))*scale`，Y另加`max(age,0)*0.055*scale` u上浮。
- age以tick计；atlas帧=`floor(clamp(age/(lifetime-1),0,1)*15)`；age<0或age>=lifetime不显示。
- 两个中心团：面宽27u/23u、寿命9/10tick、延迟0/1tick、行进参数1u/2u，具体方向及roll见seeds，不另改参数。

阶段：0–150ms亮芯喷开；200–350ms多团膨胀并出现灰云；400–650ms偏心开裂；700–900ms碎片收束；950ms全部实例失活。S50第7帧粒子中心跨度X/Y/Z为1.0702/1.1559/1.2063b；第13帧为1.3012/1.5723/1.6028b，面宽另计。这些不是伤害范围。

## 消费强度

- S0不建额外引爆粒子。
- 同版示例S10/S50/S100的位移与每团面宽统一乘0.65/1.00/1.35，数量/时序不变。归档没有重定义这些参数。
- 原交接的连续映射建议原样保留：`0<S<=10:0.40+0.025*S`；`10<S<=50:0.65+0.00875*(S-10)`；`50<S<=100:1+0.007*(S-50)`。1–9仍非零。
- 100以上在确认预览中没有展示；用户采用本版不自动产生一个未展示的新上限或外推规则。本次归档不新增该范围参数，更不得把视觉上限变成资源/伤害上限。接入者与父级按原交接边界处理。
- 幽冥蓝仅换同版atlas，所有世界位置、roll、延迟、寿命和强度缩放相同。

## 材质与实际预览

- Blockbench原制作材质：`SHADE=false`、`EMISSIVE=false`、`transparent=true`、`depthTest=true`、`depthWrite=false`，每帧按单团相机深度后到前排序renderOrder。
- Forge交接仍为保留alpha的明亮粒子材质、FULL_BRIGHT、nearest、深度测试，按需单团深度排序；不能整组CAMERA_PLANE，不能让所有烟云永远盖过实体。具体管线接入及游戏验证由接入者负责。
- 主相机`[62,46,106]→[0,17,0]`，正交zoom0.34；侧上`[98,81,-23]→[0,15,0]`，zoom0.34；强度`[12,60,280]→[0,20,0]`，zoom0.135。不改镜头参数。
- 模型保存第7帧姿态、UV、嵌入贴图和可编辑层级，不包含原生Animation轨道。动态GIF来自原制作真实GUI逐帧更新及截图，没有离线重绘。编码仅固定透明边界裁切、量化和时长设置，记录在`provenance/native-gif-encoding.json`。
- 部分材质override/渲染排序不由bbmodel持久化；原制作复开后通过脚本恢复预览状态并截图，证据`provenance/reopen-validation.json`。本次不再次操作Blockbench。
- 原制作源UUID`5f702182-73a6-cd40-964d-2ad6a7c2a361`、复开UUID`e9eebb72-fed6-5b89-ab9e-3991002b637d`仅为历史证据；不关闭、不切换、不保存任何现有用户项目。

## 过程链接与实施边界

以下文件保留在原过程目录，正式归档不复制整套原始帧或制作脚本：

- 原始像素帧：`D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116/frames/`
- 原始GUI帧：`D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116/previews/main/`、同级`side/`、`strength/`、`soul/`。
- 制作、捕获、复开脚本：`D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116/scripts/`
- 完整逐帧位置证据：`D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116/evidence/captures_main.json`，同目录另外三个`captures_*.json`。
- 原始工具成功/超时及修正记录：`D:/alianximod/docs/资源库/过程归档/引爆_三维扩散爆炸_v2_20261116/visual-handoff.md`。这些是制作阶段历史，不表示现在尚未确认。

待替换的是引爆v1单张billboard表现，不能只把旧64×1024贴图替换成32×512就声称三维效果完成。Harvey接入进行中，最终应以正式目录两条导出与实际运行文件逐字节对账，并独立回报引用、占位替换和编译状态；运行路径以接入回执为准，本作者不猜测已写入的src路径。

Forge透明排序、透视、实体遮挡、多人同步、性能和实际客户端时序尚未游戏验证。确认归档不等于游戏效果通过，不改N、不扩大作用目标、不产生地面伤害圈或陨石方块。共享索引/制作表/批次记录由Rawls单独维护。
