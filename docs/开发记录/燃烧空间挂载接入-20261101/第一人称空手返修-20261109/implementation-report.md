# 第一人称空手枢轴返修

源码唯一作者是任务 `burn-spatial-implementation` 的当前执行渠道，承接提示给出的 Cursor 会话 `2450561c-3d9a-4398-b2de-82ee839f6270`。源码已停写。游戏效果未测，不能当成通过。

## 根因

`BurnFirstPersonPose.applyEmptyArm` 已与原版 `renderPlayerArm` 一致，手焰网格也在手臂枢轴局部空间。第三人称用真实 `ModelPart.translateAndRotate`，用户已确认与预览一致。空手和地图手臂在复刻链之后调用 `applyArmPivot`。改前 X 是 `sideSign * 5/16`：右臂 `+5/16`，左臂 `-5/16`。原版 `renderHand` 在年龄 0 且强制 `xRot=0` 后，右臂枢轴是 `x=-5`，左臂是 `x=+5`。差 10/16 格的局部 X，被后面的 Z120°、X200°、Y-135° 转成镜头里的下移。Y=`2/16` 和 `rotationZYX(±0.1, 0, 0)` 本来就对，没有改。

## 精确改动

文件 `src/main/java/com/bslx/XueXiMod/client/furnace/BurnFirstPersonPose.java`，方法 `applyArmPivot`，第 110 行。

改前：

```java
poseStack.translate(sideSign * 5.0F * BurnSpatialMount.MODEL_TO_BLOCK,
        2.0F * BurnSpatialMount.MODEL_TO_BLOCK, 0.0F);
```

改后：

```java
poseStack.translate(-sideSign * 5.0F * BurnSpatialMount.MODEL_TO_BLOCK,
        2.0F * BurnSpatialMount.MODEL_TO_BLOCK, 0.0F);
```

第 111–112 行的 Y、Z 和 `rotationZYX(sideSign * 0.1F, 0.0F, 0.0F)` 未动。`sideSign` 仍是右 `+1`、左 `-1`（第 354–356 行）。因此右臂 X 为 `-5/16`，左臂 X 为 `+5/16`。

## 本次最小差异

只增加一个负号。文件字节从 18733 变为 18734。

- 改前 SHA-256：`3B7622358032F0C2D57F81C8BFC9A939FD52870AB83549330512CA90EA11FA96`
- 改后 SHA-256：`413801BED36F6B620B317CE00BB3A4F9D5E8569B4860D78BA2276D3F0A66D017`

待编译源文件就是这一份，哈希为改后值。写入已停止，之后没有再改该文件。

`applyArmPivot` 的三个调用都吃到同一符号：

- 空手主手第 44–46 行：`armSide` 是 `player.getMainArm()`，右手玩家走右臂，左手玩家走左臂。
- 单手地图第 66–68 行：同一 `armSide`，先 `applyOneHandedMap` 再 `applyArmPivot`。
- 双手地图第 60–61 行进入 `drawMapHand`，第 100 行对 `HumanoidArm.RIGHT` 和 `HumanoidArm.LEFT` 各调用一次。

持物分支第 77–78 行仍是 `applyHeldItem` 然后 `BurnSpatialMount.translateGrip`，不经过 `applyArmPivot`。`applyEmptyArm` 第 132 行的 `sideSign * 5.6F` 是原版 `renderPlayerArm` 末段平移，不是枢轴，保持原样。

未改 `FurnaceBodyLayer`、`BurnSpatialMount.translateGrip`、网格、纹理、游戏逻辑和配置。

## 自查与门禁

作者核对了上述调用和符号，没有用镜头 Y 补偿。命名门禁在项目根执行 `powershell -NoProfile -File tools/check-name-gate.ps1`，退出码 0，输出：`源码命名可读性检查通过：未发现单字母变量或禁止缩写`。

## 未运行

没有 `compileJava`，没有 `build`、`jar`、`reobf`，没有 `runClient`，没有进游戏，没有桌面控制，没有改制作表或资源库索引。

## 用户测试卡

编号/名称：fire_burn 燃烧
前置：启用燃烧过载且主手空手
步骤：第一人称左右主手站立、走动、挥手，再看持物与第三人称
预期：火焰随拳头无下移，已批准第三人称保持
