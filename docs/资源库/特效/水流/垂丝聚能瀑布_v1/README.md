# 垂丝＋聚能 · 竖直瀑布 v1

2026-10-26 在 Photoshop 重新绘制，非横向长带旋转。用户要求瀑布自上而下流动，随聚能时间整条左右扩宽。横向旧版保留但不作为此组合的采用方向。

## 采用状态

2026-10-26 已接入垂丝。游戏只加载下面四张原样复制的 PNG，目录是 `assets/xueximod/textures/entity/xueximod/`：

- `waterfall_runtime_left.png`、`waterfall_runtime_middle.png`、`waterfall_runtime_right.png`：各 4×2048，16 帧，每帧 4×128。左右边缘各 1/6 格，中段按 4px 周期重复或裁切。最细是 4+4+4。
- `waterfall_thin.png`：64×2048，16 帧，每帧 64×128。只在水帘宽放不下两条 4px 边缘时整幅回退。

`waterfall_charge.png` 和 `waterfall_charge.gif` 只是 12→48 的美术演示，游戏不引用、不循环。旧 8px 的 `waterfall_left/middle/right.png` 和 48px 的 `waterfall_flow.png` 也只留在资源库。像素没有改。`compileJava --offline --no-daemon` 已通过，游戏内画面尚未实测。

## 动画

- `waterfall_flow.psd/png`：64×128每帧，16帧竖排64×2048。固定48像素宽的流水参考。每帧100ms，纹理每帧向下8像素，纵向可连续接段；16帧内包含两次0.8秒水纹循环。
- `waterfall_charge.psd/png`：同规格聚能演示，宽度从12增加到48像素，前12帧展开，末4帧保持。左右同步扩展，长度固定，不把纵向流线拉胖。GIF回到窄态仅为演示重播。游戏不使用这份贴图，也不按它的时间把水帘从窄放到宽。
- `waterfall_flow.gif`、`waterfall_charge.gif`：4倍最近邻深底预览；底色不在正式PNG中。
- `单帧/flow`、`单帧/charge`：PS原尺寸透明导出。

## 分件

`waterfall_left.psd/png`、`waterfall_middle.psd/png`、`waterfall_right.psd/png` 为从固定宽流水源中分出的左水边、中水体、右水边。每帧分别8×128、16×128、8×128，都是16帧同步序列。中段横向16像素重复，填充扩宽区域；边缘保持原像素厚度。左右分件来自源图x=[6,14)、[50,58)，中段来自[14,30)。这三份是归档，游戏不加载。正式运行时用的是后补的 4px `waterfall_runtime_left/middle/right.png`，宽度跟已经结算的水帘宽走，不按聚能进度换贴图。细于两条边缘合计时回退整幅 `waterfall_thin.png`，不把边缘叠到一起。

水色沿用青蓝六色硬边像素，深蓝不规则边、浅青长流线及少量白沫均向下移动。未加落地水花，方便之后由实际落点处理。`验证记录.json`检查向下位移与导出序列一致性。

流水相位与聚能宽度须独立：流水循环播放，宽度由蓄力值决定。自定义渲染直绑序列使用当前帧UV，不能把整张序列纵向当作单帧拉伸。
