# 第一人称空手火焰下移定位

日期 2026-11-09。只定位，不改 Java、资源、配置，不编译，不进游戏。第三人称已由用户确认与预览一致，这里不重审。

## 结论

静止空手主手的火焰整团比原版手臂低约 0.46 格，同时略向身体中线、略远离镜头。这不是网格写低了，也不是缺一次固定上移。

`BurnFirstPersonPose.applyEmptyArm` 与本机 `ItemInHandRenderer.renderPlayerArm` 逐项一致。手焰网格已在手臂枢轴局部空间，第三人称用真实 `ModelPart.translateAndRotate` 挂上后位置正确。第一人称空手在这条复刻链之后多了 `applyArmPivot`，X 平移用了 `sideSign * 5/16`。右臂因此是 `+5/16`，左臂是 `-5/16`。原版这一帧的枢轴是右臂 `x=-5`、左臂 `x=+5`，也就是相反的符号。差 10/16 格的局部 +X，被 `renderPlayerArm` 末段的 Z120°、X200°、Y-135° 转成镜头空间的下移。

最小修复只改 `BurnFirstPersonPose.applyArmPivot` 的 X 符号，使右臂为 `-5/16`、左臂为 `+5/16`。Y 仍是 `+2/16`，`rotationZYX(±0.1, 0, 0)` 仍保留。不要把 0.46 格当成补丁加回 Y。不改持物分支，不改 `FurnaceBodyLayer`，不改 `mount.json` 和贴图。不需要新的手部渲染钩子。

## 空手主手的当前调用顺序

过载燃烧时，第一人称手焰只从 `RenderHandEvent` 进来。升温的 `RenderArmEvent` 在过载时直接返回，不会叠第二层火。

1. `ItemInHandRenderer.renderHandsWithItems` 先乘视角残留：`XP (viewXRot - xBob) * 0.1`，`YP (viewYRot - yBob) * 0.1`。本机源码第 314–315 行。
2. 主手、副手各自调用 `ForgeHooksClient.renderSpecificFirstPersonHand`。该方法只 `post(RenderHandEvent)`，返回值表示事件是否被取消。本机 `ForgeHooksClient` 第 300–302 行。事件发生在 `renderArmWithItem` 之前，姿态栈上还没有手臂变换。
3. `FurnaceArmFlame.onHand` 订阅该事件，不取消。玩家非空、`playback.present()` 且 `playback.overdrive()` 时调用 `BurnFirstPersonPose.draw`。`FurnaceArmFlame` 第 33–46 行。
4. 主手物品为空、玩家可见、未开镜：`applyEmptyArm`，然后 `applyArmPivot`，然后 `BurnSpatialMount.draw(..., "right_arm"|"left_arm")`。`BurnFirstPersonPose` 第 38–49、107–132 行。
5. 事件未取消，原版继续 `renderArmWithItem`。空手且是主手且未隐身时调用 `renderPlayerArm`，再进 `PlayerRenderer.renderRightHand` 或 `renderLeftHand`。本机 `ItemInHandRenderer` 第 362–370、220–245 行，`PlayerRenderer` 第 177–198 行。
6. `renderRightHand` / `renderLeftHand` 先投递 `RenderArmEvent`。燃烧过载时 `FurnaceArmFlame.onArm` 第 61–62 行直接返回，原版接着 `renderHand`。`renderHand` 把 `attackTime`、`crouching`、`swimAmount` 写成 0，`setupAnim(player, 0, 0, 0, 0, 0)`，再把该手臂 `xRot` 写成 0，然后 `arm.render`。`render` 内部是 `translateAndRotate` 再画立方体。袖子同样把 `xRot` 写成 0 后绘制，枢轴与手臂一致。

`ItemInHandRendererActionMixin` 替换的是第 5 步的 `renderArmWithItem` 调用，替换点在事件之后。火焰使用的是事件里的原始 `swingProgress` 和 `equipProgress`，看不到 mixin 随后是否改挥砍或换模型。

## 原版这一帧的骨骼

宽臂网格定义：右臂 `PartPose.offset(-5, 2, 0)`，立方体 `addBox(-3, -2, -2, 4, 12, 4)`；左臂 `offset(5, 2, 0)`，立方体 `addBox(-1, -2, -2, 4, 12, 4)`。`HumanoidModel.createMesh` 第 70–71 行。局部 +Y 从肩指向手，手端在局部 y=10。苗条臂定义枢轴 y 是 2.5，但下面的 `setupAnim` 会盖掉。

`PlayerRenderer.renderHand` 强制非下蹲、非游泳、攻击时间为 0，年龄参数为 0。`HumanoidModel.setupAnim` 第 110–113、188–189 行把右臂写成 `x=-5, z=0, y=2`，左臂 `x=5, z=0, y=2`。空手对应 `ArmPose.EMPTY`，`poseRightArm` / `poseLeftArm` 只把 `yRot` 写成 0，第 243–244 行。`setupAttackAnimation` 在 `attackTime <= 0` 时整段不跑，第 334 行，不会改 x。

年龄 0 的摆动在 `AnimationUtils.bobModelPart` 第 58–60 行：

`zRot += multiplier * (cos(age * 0.09) * 0.05 + 0.05)`

`xRot += multiplier * sin(age * 0.067) * 0.05`

年龄为 0 时，右臂乘数 1，`zRot` 增加 0.1，`xRot` 增加 0；左臂乘数 -1，`zRot` 增加 -0.1。随后 `renderHand` 再把 `xRot` 写成 0，不改 `yRot` 和 `zRot`。

`ModelPart.translateAndRotate` 第 153–157 行，公开方法：先 `translate(x/16, y/16, z/16)`，再在旋转非全 0 时 `mulPose(rotationZYX(zRot, yRot, xRot))`。比例不是 1 才缩放；玩家手臂比例是 1。第一人称这条路径没有第三人称 `PlayerRenderer.scale` 的 0.9375，也没有实体渲染的 `scale(-1,-1,1)`。`renderPlayerArm` 末段的旋转负责把模型坐标转进镜头。

因此原版空手手臂在 `renderPlayerArm` 之后、立方体之前的变换是：

- 右臂：`translate(-5/16, 2/16, 0)`，然后 `rotationZYX(0.1, 0, 0)`
- 左臂：`translate(+5/16, 2/16, 0)`，然后 `rotationZYX(-0.1, 0, 0)`

骑乘会给 `xRot` 加 `-π/5`，但 `renderHand` 随后把 `xRot` 清零。下蹲被 `renderHand` 写成 false，第一人称不用第三人称的手臂 y=5.2。

## 复刻差在哪

`applyEmptyArm` 第 115–132 行与 `renderPlayerArm` 第 220–239 行相同：挥空的横向、抬起、前后，Y45°，挥击的 Y70° 与 Z-20°，`translate(±-1, 3.6, 3.5)`，Z120°，X200°，Y-135°，`translate(±5.6, 0, 0)`。静止、装备进度 0 时，中间挥击旋转为单位。这里没有多一次或漏一次变换。

`applyArmPivot` 第 108–112 行：

- Y=`2/16`，与上面的 y=2 一致
- Z=0，一致
- `rotationZYX(sideSign * 0.1, 0, 0)` 与年龄 0 的 `zRot` 一致，顺序也是 ZYX
- X=`sideSign * 5/16`。`sideSign` 右为 +1、左为 -1，所以右臂得到 `+5/16`，左臂得到 `-5/16`

和原版相比，只多了局部 X 上的 `+10/16`（右）或 `-10/16`（左）。旋转在平移之后，这个差值不被 0.1 弧度的 `zRot` 再转一次。它被前面整段 `renderPlayerArm` 旋转带进镜头。

镜头坐标按本机姿态栈的后乘、+X 向右、+Y 向上、+Z 朝向镜头。静止、装备进度 0 时，错误枢轴减正确枢轴的镜头位移长度正好是 0.625，方向约为：

- 右手：`(-0.230, -0.458, -0.357)`，最大分量向下
- 左手主手：`(0.230, -0.458, -0.357)`，同样向下，横向改到另一侧

正确右手原点约在 `(0.504, -0.819, -0.663)`，即画面右下方，符合原版手的位置。错误原点约在 `(0.274, -1.277, -1.021)`，整团火焰相对真实拳头下移约 0.46 格。拳心锚 `(-1, 8.8, 0)` 和手端局部 y=10 一起被同一刚体平移带走，所以看起来是整团落到手下面，不是形状被拉长。

网格不需要再减一次枢轴。`mount.json` 的 `anchors.right_arm` 是 `[-1, 8.8, 0]`，`left_arm` 是 `[1, 8.8, 0]`。正式归档 `native-mount-parameters.json` 里 Blockbench 右臂原点 `[-5, 22, 0]`，拳心 `[-6, 13.2, 0]`。交接换算 `x=xBB`、`y=24-yBB`、`z=-zBB` 再减骨骼枢轴后，右拳局部正好是 `(-1, 8.8, 0)`。右臂火壳局部 x 约 -5 到 3.4、y 约 -0.4 到 11.6，包住宽臂立方体局部 x -3 到 1、y -2 到 10，拳心亮核在 y=10.15。这与第三人称「挂在当前手臂骨骼上且与预览一致」相符。若网格还停在模型绝对坐标，第三人称也会偏一截。

## 各路径

空手主手，含左撇子主手：走上面的空手臂。符号错误就在这里。玩家隐身或开镜时 `draw` 直接返回；原版开镜也不画手。

空手副手：`draw` 在 `stack.isEmpty()` 且不是主手时返回。原版 `renderArmWithItem` 同样不给空手副手画手臂。这里没有悬空火，不要为副手补画。

主手有物品、副手空：副手事件走上面的返回。主手走持物分支，不用 `applyArmPivot`。

单手地图：`applyOneHandedMap` 先 `translate(±0.125, -0.125, 0)` 和 Z±10°，再 `applyEmptyArm`，再 `applyArmPivot`。这与 `renderOneHandedMap` 第 154–160 行加上随后的 `translateAndRotate` 同一结构，差的仍是枢轴 X 符号。用户没有报告地图错。改这一个符号会让地图手焰跟着原版地图手臂一起归位，不要另加地图偏移。

双手地图：主手持地图且副手空。`applyTwoHandedMap` 对齐 `renderTwoHandedMap` 第 178–185 行的挥击下压和地图倾角；随后 Y90°，`drawMapHand` 对齐 `renderMapHand` 第 136–144 行的 Y92°、X45°、Z∓41°、`translate(±0.3, -1.1, 0.45)`，然后再 `applyArmPivot`。差的仍是同一个 X 符号。地图纸本身的缩放和后仰只作用在地图上，手焰不加那些变换。现状已经如此。

剑、法杖和其他非地图物品：`applyHeldItem` 复刻弩、`applyForgeHandTransform`、使用、激流、挥击和 `applyItemArmTransform`，然后 `BurnSpatialMount.translateGrip`，不调用 `applyArmPivot`。握点是把局部锚点取负后乘 `1/16`，把拳心放到 `renderItem` 之前的手部原点。这条路径没有这次的 X 符号错误。用户没有说持物错，修复时保持该分支字节不变，也不要改默认耗魔或伤害。

`IClientItemExtensions.applyForgeHandTransform` 本机签名是 `(PoseStack, LocalPlayer, HumanoidArm, ItemStack, float partialTick, float equipProcess, float swingProcess)`，默认返回 false。返回 true 时原版和复刻都停在自定义变换上，然后才 `translateGrip`。法杖若未自定义手部变换，继续走普通持物。不要在这次改这条分支。

动作接管在火焰提交之后：

- 空间切割 `SpaceCutScope.push` 为真时，mixin 改画 `SpaceCutItemMount.drawFirstPerson`，不再走 `renderPlayerArm`。火焰已经按原版空手链画完。
- 协同 `CooperativeActionViews.holds` 为真时，主手改画 `CooperativeFirstPerson.draw`，同样不再走原版手臂。
- 没有播放记录且不在已有动作栈里时，`xuexiBypassesHandAction` 为真，左右手各走一次原版，挥砍进度保持传入值。普通空手实测落在这条。
- `takeFirstPerson` 只在 `swordHand` 为真时成立，而 `swordHand` 要求物品非空且是剑。空手进不了 `FirstPersonSkinDraw`。有播放记录但手是空的，仍会回到 `renderArmWithItem`；`renderHand` 自己重跑 `setupAnim`，可见手臂仍是上面的原版枢轴。

所以这次下移不靠动作替换解释。空间切割和协同会换成另一只手，火焰仍停在事件里的原版复刻上；那是另一条挂接问题，不要为这次下移加钩子或改那些文件。

## 最小修复步骤

只改一个文件：`src/main/java/com/bslx/XueXiMod/client/furnace/BurnFirstPersonPose.java`。

只改 `applyArmPivot` 的 X 平移：把 `sideSign * 5.0F * MODEL_TO_BLOCK` 改成 `-sideSign * 5.0F * MODEL_TO_BLOCK`。右臂变为 `-5/16`，左臂变为 `+5/16`。Y、Z 和 `rotationZYX(sideSign * 0.1F, 0, 0)` 不动。`applyEmptyArm`、`applyOneHandedMap`、`applyTwoHandedMap`、`drawMapHand`、`applyHeldItem`、`translateGrip` 的调用都不改。

不新增渲染钩子。`RenderHandEvent` 时 `renderHand` 还没跑，事件栈上的手臂部件仍是上一帧残留，不能直接读它的 `x/y/zRot`。现有复刻已经对齐 `renderPlayerArm`，缺的只有这个符号。把绘制挪进 `PlayerRenderer.renderHand` 会多一个绘制点，而且空间切割和协同根本不调用 `renderHand`，解决不了那两条路径，也不是这次下移所需要的。

不改：

- `FurnaceBodyLayer`。没有名为 ThirdPerson 的类；第三人称就是这里 `drawBurn` 对 `body`、两条腿、两条臂调用 `onPart`，内部是真实 `translateAndRotate` 再 `BurnSpatialMount.draw`。用户已确认披风和手焰与预览一致。
- `BurnSpatialMount.draw`、`translateGrip`、`mount.json`、贴图。网格保持手臂局部轴向。
- `FurnaceArmFlame.onArm`。那是非过载升温，过载时返回。它已在清零旋转后调用真实 `translateAndRotate`，与这次燃烧空手无关。
- 手焰外形、大小、色彩、披风和玩法。

## 作者自查

改完后对照，不必进游戏也能核对接线：

1. 右臂 X 系数是 `-5/16`，左臂是 `+5/16`，Y 仍是 `2/16`，Z 仍是 0，四元数仍是 `rotationZYX(±0.1, 0, 0)`。
2. 静止、装备进度 0 时，错误符号相对正确符号的镜头位移应消失。不要另加约 `+0.458` 的镜头 Y。那个数是 10/16 局部 X 被 Z120°、X200°、Y-135° 转出来的结果，挥击或装备进度一变方向就变。
3. `applyHeldItem` 到 `translateGrip` 的持物路径与改前一致。
4. 空手副手仍然直接返回。
5. `FurnaceBodyLayer`、`BurnSpatialMount` 和 `mount.json` 无差异。
6. 改了 Java 才在项目根跑 `powershell -NoProfile -File tools/check-name-gate.ps1`。这次诊断不改 Java，不跑。随后若只改这一处平移系数，不新增短名。

游戏画面仍由用户看。编译不在本次授权内。

## 读过的声明和上下文

本机 Forge `1.20.1-47.3.33`，Parchment `2023.06.26-1.20.1` 源码包：`forge-1.20.1-47.3.33_mapped_parchment_2023.06.26-1.20.1-sources.jar`。

- `ItemInHandRenderer.renderHandsWithItems`、`renderArmWithItem`、`renderPlayerArm`、`renderMapHand`、`renderOneHandedMap`、`renderTwoHandedMap`、`applyItemArmTransform`、`applyItemArmAttackTransform`：私有或公有见各方法，调用关系如上。
- `PlayerRenderer.renderRightHand`、`renderLeftHand` 公有；`renderHand`、`setModelProperties`、`getArmPose` 私有。`scale` 只在第三人称实体渲染。
- `HumanoidModel.createMesh`、`setupAnim`、`poseRightArm` 公有或私有见源码；`ModelPart.translateAndRotate` 公有，字段 `x/y/z/xRot/yRot/zRot` 公有。
- `AnimationUtils.bobModelPart` 公有。
- `ForgeHooksClient.renderSpecificFirstPersonHand`、`renderSpecificFirstPersonArm` 公有。`RenderHandEvent`、`RenderArmEvent` 可取消；本模组燃烧订阅不取消。全仓 Java 里没有第二个 `RenderHandEvent` 订阅。
- `IClientItemExtensions.applyForgeHandTransform`、`getArmPose` 为 default 方法。

模组侧只读了炉心目录五个类，以及第一人称手部 mixin 和动作查询里判断空手会不会被换手的条件。没有通读全仓。

正式归档只核对了枢轴和拳心：`docs/资源库/特效/火焰/燃烧_空间挂载_v1/native-mount-parameters.json` 的 `right_arm` 原点 `[-5,22,0]`、`fx_right_fist` 原点 `[-6,13.2,0]`，以及 `视觉交接_待确认.md` 的 Blockbench 轴向说明。运行网格仍用已接入的 `burn_spatial_v1/mount.json`。没有重画，没有操作桌面。
