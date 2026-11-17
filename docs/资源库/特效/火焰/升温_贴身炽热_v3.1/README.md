# 升温 · 贴身炽热 v3.1（用户确认接入）

用户于 2026-11-11 先确认二维手腿比例，再要求去掉脖子横杠、在身体上加少量热纹。随后确认按这版做三维预览，并明确说“就用这版接入”。

Blockbench 适用性：适用。
版本：heat-up-v3.1。
确认：用户，2026-11-11，确认本目录对应的二维稿和真实 Blockbench 预览后授权接入。
二维源：`heat_up_v3_1.aseprite`、`heat_up_v3_1_entry.aseprite`。96×112，7 个独立图层。循环 20 帧×80 ms；入场 8 帧×60 ms，只播一次后接循环。
二维导出：`runtime/` 下 14 张按图层拆开的透明纵向图集；循环图高 2240，入场图高 896，宽均为 96。整图条为 `heat_up_v3_1_strip.png` 与 `heat_up_v3_1_entry_strip.png`。
二维预览：`heat_up_v3_1_preview.gif`、`heat_up_v3_1_preview.png`。人偶只存在于预览。
素材：`runtime/loop_*.png` 与 `runtime/entry_*.png`，图层名为 warmth、hand_rear、leg_rear、torso、hand_front、leg_front、embers。
.bbmodel：`spatial/heat_up_v3_1_spatial.bbmodel`。
实际预览：`spatial/heat_up_v3_1_front_entry_loop.gif`、`spatial/heat_up_v3_1_rear_loop.gif`，以及 front、rear、side 三张静帧。均由真实 Blockbench `Preview.screenshot` 取得。

换算：3 个贴图像素对应 1 个模型单位，与现有 `BODY_SCALE = 8/24` 相同。模型单位与游戏方块的最终关系沿用玩家模型，没有另做游戏实测标定。
轴向与前向：Blockbench 预览以 +Y 向上、+Z 朝向预览相机。Minecraft 玩家渲染层在 `scale(-1,-1,1)` 之后，+Y 为视觉向下，-Z 为玩家正面。接入时把预览坐标转换为部件局部坐标：`localX = bbX - boneX`，`localY = boneY - bbY`，`localZ = -bbZ`。
原点 / 锚点 / 旋转中心：贴图像素 `(48,104)` 是脚底中心。预览骨骼原点为躯干 `(0,24,0)`、右臂 `(-5,22,0)`、左臂 `(5,22,0)`、右腿 `(-2,12,0)`、左腿 `(2,12,0)`。
父子层级：每张卡片跟随对应玩家骨骼，随手臂和腿部姿态转动。
尺寸 / 偏移 / 朝向：每张卡片的包含式像素矩形、绝对起止坐标、深度和归属见 `spatial/placement.json`。正面与背面使用对应矩形的完整范围；侧面只使用记录中的窄外侧矩形，不把整幅手臂或腿部窗口压成居中厚片。卡片双面可见。
UV / 帧布局：横向 `left/96` 到 `(right+1)/96`；纵向按帧计算 `(frame + top/112) / frameCount` 到 `(frame + (bottom+1)/112) / frameCount`。
材质 / 透明：沿用现有自发光透明渲染。空像素保持透明，不能为了裁边而移动火焰像素。
时间 / 循环 / 相位：入场 8×60 ms，循环 20×80 ms。预览里入场后重复播放两遍循环，只为看稿，游戏里循环持续到状态结束。
跟随对象与运动绑定：第三人称挂玩家身体、左右臂、左右腿。第一人称仍只在现有空手/地图手臂事件中画左右手的短焰，不改成每次持物都画，也不把腿部或躯干火焰画到手部。
预览速度：上述毫秒值就是确认的动画帧时长，不是术式施法时长或炉心状态时长。

运行资源：把 `runtime/` 原样复制到 `src/main/resources/assets/xueximod/textures/effect/furnace_v3/`。不要覆盖 `furnace_v2` 中的燃烧贴图。v2 升温四层贴图退出升温路径，文件可保留。
示意辅助：灰色人偶、眼睛、地面格，只在 `.bbmodel` 和预览中，不进运行资源。
Blockbench 不能代表的运行效果：游戏光照、透明排序、行走摆臂和第一人称手臂透视尚未实测。纤细手臂不是本次确认模型；保持同一贴图窗口，只把局部 X 按现有宽臂与纤细臂中心差向内侧平移。

玩法、炉心状态、法力、持续时间和冷却蒸汽都不改。背部没有单独绘制，复用 torso 图层。

过程原稿留在 `docs/资源库/过程归档/升温_躯干少量热纹_v3.1-20261111/` 与 `docs/资源库/过程归档/升温_立体查看_v3.1-20261111/`。旧正式版 [升温 v2](../升温_贴身炽热_v2/README.md) 保留。
