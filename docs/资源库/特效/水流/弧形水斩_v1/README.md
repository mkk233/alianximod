# 弧形水斩 v1

状态：2026-10-25 已接入水刃弧带视觉。长条版与水弹版均继续保留。伤害、扫角和实体寿命未改。

## 造型与节奏

64×64 透明画布，16帧，每帧50ms，单次动作0.8秒。水刃沿弧线由左下扫过左上、顶部至右侧，中央留空；尾部水丝逐步收细，较厚前端含切向浪钩，弧内有两处小翻浪。

- 帧0–2：短水丝起势。
- 帧3–6：快速扩展水刃，第6帧为峰值。
- 帧7–10：浪头前进、尾部断开，水滴沿切线甩出。
- 帧11–14：水丝与飞沫缩小消散。
- 帧15：全透明，动作结束。

不使用生成式图像。硬边像素与实色色块消散，不做高斯模糊；像素透明度仅0/255。

## 文件

- `water_arc_slash.psd`：64×1024竖向序列，16个可编辑帧图层，图层名包含帧序号及50ms时长。
- `water_arc_slash.png`、`water_arc_slash.png.mcmeta`：竖向透明序列与图集动画配置，64×64帧，frametime=1 tick，无插值。
- `单帧/frame_00.png` 至 `frame_15.png`：原生64×64透明PNG。
- `water_arc_slash_preview.gif`：深色底循环观看预览，8倍最近邻放大；每次动作末尾额外停顿400ms。
- `water_arc_slash_transparent.gif`：同尺寸透明预览，也含400ms观看停顿。
- `water_arc_slash_contact.png`：4×4帧总览。
- `water_arc_slash_keyframe.png`、`water_arc_slash_keyframe_8x.png`：第6帧及放大图。

GIF与总览从Photoshop已导出的帧封装；观看停顿不计入正式16帧序列。

## 游戏接入说明

游戏采用同一份 PNG，路径 `src/main/resources/assets/xueximod/textures/entity/xueximod/water_arc_slash.png`，资源名 `xueximod:textures/entity/xueximod/water_arc_slash.png`。SHA256 `76A1F122661A9BEB4040E980B83A910C5F0BAEF55F58A2E9FA534A5BC4AA5098`，与本目录原件一致。游戏侧故意不放 `.mcmeta`，避免图集自己循环；帧由渲染按年龄选纵向 UV。

水刃实体仍是 6 tick 攻击加 4 tick 消退。`0<=age<=6` 时帧号为 `floor(age)`，峰值帧 6 保持到约 6.4 tick，之后才进入消退帧；`age>=10` 不画。世界扫角仍是三拍的 90°、-90°、140°。贴图按椭圆中心 (32,34)、半轴 (25.5,24) 取样后贴到这条弧上，不把 217° 素材再弯一次，也不扩大命中扇区。

2026-10-25 CPU 检查通过：帧边界、三拍正负扫角、UV 不串帧、无二次平移和缩放、64×1024、帧 15 全透明。初次离线构建缺少插件缓存；随后 `compileJava --no-daemon` 编译成功，补跑 `compileJava --offline --no-daemon` 也通过。独立 Grok 4.7 审查通过。Minecraft MCP 连接失败，游戏内未验证。

制作脚本与色号数据：[过程归档](../../../过程归档/弧形水斩-20261024/)。
