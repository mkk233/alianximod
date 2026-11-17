# 水雾领域 · 80°细雨 v1

状态：2026-10-25 已接入水雾领域 VOLUME 视觉。游戏路径 `assets/xueximod/textures/domain/mist_rain_80.png`。`compileJava` 已通过，尚未游戏内实测。雾盒与细雨共用只写颜色、保留深度测试的透明网格。

通过 Photoshop MCP 在透明画布中绘制像素雨丝，无生成式图像。与水平地面夹角约 80.54°，方向为右下；1 像素宽，长度 5–12 像素，淡青灰蓝三档透明度。用于水雾领域的细雨叠加层，不包含雾体本身。

## 动画与文件

- 每帧 64×64，共 32 帧，每帧 50ms，循环 1.6 秒。
- `mist_rain_80.psd`：64×2048 竖向序列，32 个可编辑帧图层。
- `mist_rain_80.png`：原尺寸透明序列。游戏副本为 `src/main/resources/assets/xueximod/textures/domain/mist_rain_80.png`，旁附只声明 `blur: false`、`clamp: false` 的 mcmeta。不复制 GIF、预览底色或 PSD。
- `mist_rain_80.png.mcmeta`：归档内的 64×64、frametime 1、无插值循环动画配置；仅在支持动画的纹理图集内生效。游戏侧不使用这份动画配置，改为显式选帧。
- `单帧/frame_00.png` 至 `frame_31.png`：Photoshop 导出的透明单帧。
- `mist_rain_80_preview.gif`：8 倍最近邻深色底循环预览，深色底不在正式贴图中。
- `mist_rain_80_keyframe_8x.png`：透明放大关键帧；`mist_rain_80_preview.png` 为深色底版本。
- `mist_rain_80_contact.png`：32 帧总览。
- `验证记录.json`：尺寸、透明度、序列一致性与循环位移检查。

## 循环与接入

基础单元为 32×64，横向重复两次。每帧位移 (+1,+6) 像素，边界环绕；32 帧后位移 (32,192)，恢复相同布局。每帧含 184 个非透明像素，实际导出 alpha 为 0、46、99、168。所有相邻帧（含最后到第一帧）逐像素位移验证通过，序列图与单帧完全一致。

领域渲染直绑整张 64×2048 PNG，不使用上面的动画 mcmeta 播帧。`frame = floorMod(gameTime - visualOrigin + 稳定相位, 32)`，帧内纵向 UV 为 `(frame + localV) / 32`。相位来自 animSeed 和固定雨面序号。最近邻、透明混合，关闭模糊和 mipmap。不把放大预览用于游戏。各雨面相位可以不同，下落方向保持一致。

制作脚本见 [过程归档](../../../过程归档/水雾细雨80度-20261025/)。现有水弹、长带、水斩素材均保留。
