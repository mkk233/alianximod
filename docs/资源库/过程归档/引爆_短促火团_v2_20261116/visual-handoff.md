# 引爆 v2 · 短促火团候选交接

状态：`candidate_pending_user_confirmation`。本目录为单一候选版本 `fire_detonate_v2_candidate_20261116`；二维源、两色导出、真实 Blockbench 示意、动画及本交接须同版本确认。尚无用户确认，未接入、未编译、未游戏验证，不能凭本回执迁待验收。

关联术式：`fire_detonate`；batchId=`待制作-20261031`；taskId=`fire_detonate_v2_art`；attemptId=`20261116-art-detonate-03`。设计 2/2、实现 3/3。**本阶段额度已用完，返修结果未追加复审**。作者查看图像及导出一致性检查属于本次美术制作收束，不是追加正式复审。

## 输入与来源

现行 O 原文：「命中点一次短促火团，强度随当时花掉的火种增加。没有余额时只有载体自己的命中。」完整 N/O/DV 和表参数使用父级记录者提供的 `docs/开发记录/术式批量制作/待制作-20261031/detonate-v2-input-snapshot-20261116.json`，原样副本在 `evidence/input-snapshot-used.json`。工作簿基线 187795 字节，SHA256 `1473D5EF076D4B8B19A59CBCC92DF3A394CDD888FB03E4B4025AC250D1F264B1`，制作中第9行。

火伤仍为 `0.2×S×(1+F)`，S 为真实结算时目标自己的剩余共享火种。本稿没有改伤害、余额、并发顺序、同次释放限制、载体范围或中心父段绑定。不能将一个目标的火种复制成范围伤害；不能把视觉面片边长当攻击范围。零消耗不生成额外引爆，载体本身的命中照常。

主会话传达的「MC硬边像素、厚实火瓣、亮芯、短促外鼓、少量火屑、避免橙色海带」属于制作方向，不冒充用户逐字原话。旧 `引爆_v1/review-board.png` 是10项合集，第08格才是旧引爆；此事实由父级补充，本稿未将整张合集作为引爆。v1 仍是2026-10-31暂用历史，不算本稿通过。

## 同版产物

根目录：`D:/alianximod/docs/资源库/过程归档/引爆_短促火团_v2_20261116/`。每个文件的字节数、SHA256、图像尺寸见 `manifest.json`。

- `source/detonate_v2_orange.aseprite`、`source/detonate_v2_soul.aseprite`：64×64，三可编辑层，16帧，50ms/帧，单次0.8秒。
- `textures/detonate_v2_orange-strip.png`、`textures/detonate_v2_soul-strip.png`：原尺寸64×1024竖条；`frames/orange/`、`frames/soul/` 为对应原尺寸完整帧序列。
- `previews/detonate_v2_native_complete.gif`：首选完整动画，真实桌面 Blockbench 主视口16帧，橙火/蓝火相同消耗50并排，普通MC体型目标和1格地面网格参照。
- `previews/detonate_v2_native_strengths.gif`：真实视口零/低/中/高实际消耗对照，0/10/50/100；例示尺寸待确认。
- `previews/keyframes.png`：两色同源关键帧板；`previews/strength_comparison.png` 和 `target_comparison_2d.gif` 为 Aseprite 像素源的二维对照。
- `blockbench/fire_detonate_v2_candidate.bbmodel`：真实GUI内建立并保存的90个Cube、20组、22张嵌入贴图的可编辑示意。火团是零厚度公告板，其他多为辅助目标、地面、标签和对照场景。
- `previews/gui_01_burst_threequarter.png`、`gui_02_frozen_side_depth.png`、`gui_03_dissipation_threequarter.png`、`gui_04_strength_comparison.png`：四张实际主视口，分别为爆发、冻结侧向深度、消散、强度对照。原始PNG未重绘/拼贴。`previews/gui_frames/` 与 `strength_frames/` 保存完整原始视口序列。
- `evidence/reopened_native_viewport.png`、`reopen-validation.json`：原生保存后复开的视口和贴图/模型记录。模型中两套嵌入PNG解码像素与原尺寸导出完全相同。

绑定哈希：橙色源 `320D780DCE01F880B92B2E998417AC1093A4F236048CC8A3CA87C85C60CBBA26`；蓝色源 `273A73FC09DAE28D9FBB3E7B28DC7CEE91A6A18E681035CD80F85CD9FF2C4D9B`；橙条 `85D32359C435FFF08395CFF7CB3B1017661349451B3C6A7E8265406D9DDD48A8`；蓝条 `83359A9FDCC7ADF14841F737E40B0CC5787D8BFFCC869CC50846E99B58CB2C5B`；模型 `EED1A2BBED81BAC96F2FC6AC73A728AD99EF89D95FBDE3D878300851C1885236`。

## 空间参数（候选，待同版确认）

Blockbench 适用：涉及目标命中、朝相机面片、尺寸、深度与遮挡，已完成真实空间示意。

换算：16模型单位=1游戏方块；参照目标高28.8u=1.8格。目标按常规MC头/身/四肢比例搭建，肩宽14.4u=0.9格；此网格不是游戏碰撞箱或新实体模型。

轴向：+X为参照目标右侧，+Y向上，+Z为初始正面镜头方向；贴图U向右、V向下。火团原点/旋转中心为整个64×64面片中心，纹理从像素左上开始。模型南面贴图，材质双面；实际运行采用同向公告板四边形。

本示意的目标脚底为局部原点，示例真实命中点为 `(0,16,1.8)u`，即躯干正表面。候选运行挂点为**载体真实命中世界位置**，再沿当前相机朝向偏移0.20格（3.2u）；不在真实命中点上额外加固定Y=1.0。0.20格是本稿明确可见的视觉偏置，待用户确认。它解决此示意中面片切入躯干的观感，不证明所有目标体型或相机角度已在Forge里验证。

面片零厚度；层级为 `01_MAIN_TWO_VARIANTS_SPENT50 / ORANGE_SPENT50或SOUL_SPENT50 / FX_CAMERA_PLANE_* / FX_*`。本模型保存第5帧爆发姿态。运行建议只在命中时记录一次世界点，16tick内不跟随目标位移（与当前host=-1方向一致），每次绘制继续 `CAMERA_PLANE` 面向当前观察相机。模型本身不自动执行相机跟随或UV动画；归档脚本逐帧设置真实GUI场景状态并截图。

整体视角：正交相机 `(58,44,165)u`，看向 `(0,17,0)u`。深度视角：`(68,39,45)u` 看向 `(-20,16,1.8)u`，**故意冻结正面面片朝向**，用于显示0厚度和0.20格偏移；不能据侧图误解运行公告板也冻结朝向。完整实测相机和帧记录在 `evidence/native-captures.json`。

## 强度与颜色映射（只提议视觉，不改玩法）

实际消耗 S=0：无额外引爆面片。S=10/50/100 的示例面片边长分别为0.80/1.20/1.60格，半边长0.40/0.60/0.80格。火团有透明边缘，最大阶段的可见像素范围为46×47/64，所以可见火团约为上述面片边长的72%×73%，不等同面片边长或伤害范围。

建议在这些取值间连续插值：`0<S≤10: L=0.60+0.02S`；`10<S≤50: L=0.80+0.01(S−10)`；`S>50: L=1.20+0.008(S−50)`。1至9点也有非零视觉，不取整为档位伤害。100以上延续末段只是未展示的外推建议，最终接入以用户确认映射为准；本稿不设资源或伤害上限。所有尺寸例子、连续映射和偏移均是美术候选参数，不是表内已经批准的数值。持续时间、亮度和帧数保持一致，通过尺寸表现强度。

普通六色：`#D2310D #F5540D #FF8B14 #FFC129 #FFE367 #FFF8B7`。幽冥逐色对应：`#085C66 #0795A2 #0EC4CC #44EDE7 #A7FFF0 #E2FFF4`，依据现有 `幽冥蓝火重配色-20261116/visual-handoff.md` 的六色映射。两套图形、alpha和时间完全相同，使用白色顶点RGB，蓝图不能再次乘暗蓝 `(90,160,255)`；领域判定沿用现行语义，不由本稿新增。

## UV、材质与时序

16帧竖排，f=1在顶端；UV为 `u=0..1`、`v=(f−1)/16..f/16`。每帧50ms，1tick一帧，总16tick/0.8秒，只播放一次。F1–F5迅速鼓开，F6–F7外鼓峰值，F8开始裂开，F9–F12不对称火瓣收缩，F13–F15减少至零星余烬，F16全透明。最后可见像素数82→22→17→11→2→0，未用整团突断作收尾。

源图6色，边缘硬透明，没有柔焦、渐变辉光、平滑环或新地面圈。最近邻采样，建议 `blur=false, clamp=true`。运行材质延续 `entityTranslucent/FULL_BRIGHT`，深度测试保留。Blockbench示意为默认透明材质，`SHADE=false, EMISSIVE=false, transparent=true, depthWrite=false, depthTest=true`，面片绘制顺序10。这里关闭BB的EMISSIVE分支是避免它强制alpha产生不透明面，不表示游戏放弃FULL_BRIGHT。

完整GIF播放的是0.8秒效果，加0.6秒清空停顿后循环，便于观看；停顿和循环都不进入运行寿命，也不是施法时长。GIF由原始真实视口PNG合成，仅填深色透明背景和统一256色无抖动调色板；原视口PNG保留。二维对照亦包含同样的观看停顿。

## 交给 Grok 的接入待办

当前代码边界来自父级已定位事实：`BatchTriggerVisuals.java:26,40,75,80` 的DETONATE仍为固定 `halfSpan=0.6`、命中点+Y1.0、host=-1、16tick/16帧，只判断`load.detonate`，没有读取spent。本美术任务未再次审查或改写Java，不能声称消耗缩放已实现。

用户同版本确认之后，由Grok取得本次目标结算实际花掉的S，S=0不额外生成引爆，S>0按最终确认映射发出一次视觉；不要从施法前余额、预付池、另一目标或热波子段复制S。载体伤害、共享燃烧扣除次序、同次释放最多一次等现有N约束继续保持。

建议运行路径（尚未创建）：`src/main/resources/assets/xueximod/textures/effect/detonate_v2/detonate.png` 对应本橙条，`detonate_soul.png` 对应本蓝条；由Grok核对项目现有蓝火模式和注册方式后落实，避免二次乘色。待替换占位为现有 `textures/effect/detonate_v1/detonate.png`，SHA256 `F3F0FCEB4A8AAECBA5EAC2B300C1FD5036B0E9EEAF1CD04B1A4F38FC1C79D858`。旧资源保留，本作者未修改。

`AUX_*`、假人、地面、网格、标签、测量杆、比较场景及整份 `.bbmodel` 均不进入运行资源。Blockbench不能证明Forge透明排序、多人相机、快速移动目标、各种碰撞体、世界光照/深度或实际spent传递；这些尚未游戏验证。

接入回执：**未接入、未编译**。用户尚未确认本版。记录者Rawls之后接收本产物更新总索引/批次记录/表；本作者没有这些文件的写权限。本交付只结束美术候选制作，不等于正式采用或制作表迁页。

## 工具与保护证据

本会话实际成功：`mcp__magpie__aseprite__status`（Aseprite1.3.17，batch可用，live.connected=false）、`run_lua`（绘制/导出/只读源核验）；`mcp__magpie__blockbench__get_capabilities`（Desktop5.2.2/plugin1.10.1）、`create_project`、`risky_eval`（真实GUI建模、截图、保存、原生复开）。Aseprite批处理没有占用用户实时编辑文档；Blockbench从未用headless替代，未用桌面computer-use或ImageGen。

建模原UUID=`07be6674-6249-0d6c-f677-8fc0b139903d`，复开UUID=`e0d306f9-0f12-bc19-9636-0c56f9f1ce88`。保护15个原有项目，含未保存 `daozhang_v1_candidate` UUID=`4f04348e-9d85-4d4f-0c95-9e5e50c4dbe2`；焚天UUID=`2ddaf358-56ef-cd78-263a-4e6afb143ef0` 保留，所有原标签未关闭。前后几何/贴图/保存状态快照一致，见 `evidence/protected-projects-*.json`。

实际错误：第一次建模调用在写入前读取保护快照时遇到 `TypeError: Converting circular structure to JSON`，改为显式序列化字段后成功。制作中发现组隐藏未作用到子物体、透明绘制顺序/偏移导致遮挡，均在本候选场景修正并重新截图；没有改其他项目。第一次原生复开后立即截图为空，随后等待GUI稳定并重新原生截图成功；最终证据为后者。一次可选xlsx读取遇到缺少sharedStrings入口的非致命读取错误，但N/O/DV inline文本读取成功；父级随后提供完整JSON，后续不再读取xlsx。上述均不属于额度耗尽、403或审批拒绝。
