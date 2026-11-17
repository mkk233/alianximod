# 周身水环 · 大浪溅射 v5（待确认）

2026-10-27。根据用户“大水花、参考神奈川冲浪图”的要求制作，继续使用Aseprite绘制浪形，Blockbench负责立体摆放和遮挡。参考原作：https://www.metmuseum.org/art/collection/search/45434 （The Met公开领域藏品图）；`reference-great-wave.jpg`仅供制作观察。

本版为稳定腰间流水＋五处错相大溅射，两侧主浪冠展开得更宽，前方较低，另外两处布在后方。深靛浪底、青蓝翻卷水面托住大面积近白浪冠，粗短分叉间保留透明缺口。8～10像素的大水块先上抛，再沿抛物线落回并收小。所有浪形变化均来自Aseprite帧动画，载体没有旋转动画。

- `great_splash.aseprite`：96×64，16帧，100ms/帧，五层：深靛厚浪底、青蓝翻卷水面、大面积白色浪冠、粗短爪状分叉、外抛大水块。
- `great_splash.png`：96×1024透明竖向序列；0～4副本仅用于Blockbench独立错相播放。
- `splash_ring_flow.aseprite/png`：128×32每帧，16帧；持续流水与断续白沫。
- `great_splash_preview.gif`：Aseprite原生6倍像素导出。
- `周身水环_大浪溅射_v5.bbmodel`：可编辑三维验证场景，嵌入全部纹理。五处水花使用向外张开的弧面，动画相位为0/10/5/13/8帧；无骨骼旋转动画。
- `water_splash_fixed.gif`：固定相机真实Blockbench渲染，720×720，16帧/1.6秒。
- `water_splash_orbit.gif`：48帧/4.8秒三维环绕检查。

制作脚本：`draw-great-splash.lua`通过Aseprite MCP执行；`mount-big-splashes.js`和`refine-big-splashes.js`通过Blockbench MCP顺序执行。预览为水材质临时关闭方向光着色，显示素材本色；重新打开可执行`preview-water-unlit.js`恢复相同显示。此显示设置不属于bbmodel保存项。模型重开后使用纹理播放按钮即可循环。

已核对16帧渲染互不相同、所有贴图内嵌、五处错相、脸部留空和人物前后遮挡。各旧版本保留，本版仅验证，尚未正式采用或接入游戏；无游戏代码修改、无编译打包和游戏测试。
