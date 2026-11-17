# 水牢泡泡 v3 · 斜向错层（2D 草稿）

状态：2D 原画已获用户认可。游戏采用单张二维动画平面，保持原图比例，只整体等比放大或缩小，始终朝向当前客户端镜头；对应 [等比公告板验证 v4](../水牢公告板验证_v4_等比缩放-20261027/README.md)。未接入游戏，旧版均保留。

## 本次调整

用户认可 v2 的厚重水壁与积水设计，但希望交错水流参考《女神异闻录5》UI 的线条排布。

查看了 [P5R 战斗 UI 宣传截图（4Gamer）](https://www.4gamer.net/games/446/G044617/20190803013/screenshot.html?num=009)，借鉴斜向展开、不对称长短与粗细反差。绘制的是原创蓝青色像素水流，参考图未嵌入素材。

- 主水流从左下宽段向右上尖收，穿过腰腹。
- 左上短回流在主流上方停止，不再居中交叉成 X。
- 下方增加更暗、更短的回流，形成错层留白。
- 水流宽窄渐变、分叉短尾与滑动高光维持液体感。
- v2 的透明水体、厚水壁、底部积水及水壁高光逐帧保持一致。

## 文件

- `water_prison_bubble_v3.aseprite`：96×96、RGBA、6 个可编辑图层、16 帧 ×100ms。
- `water_prison_bubble_v3.png`：原尺寸透明首帧。
- `water_prison_bubble_v3_strip.png`：96×1536 竖向序列帧。
- `water_prison_bubble_v3_4x.png`：四倍像素预览。
- `water_prison_bubble_preview.aseprite`：深浅背景与目标占位人预览工程。
- `water_prison_bubble_preview_4x.gif`：896×448、1.6 秒循环动画。
- `water_prison_bubble_preview_4x.png`、`preview_frame09.png`：首帧和中段帧检查图。
- `draw-water-prison.lua`：通过 Aseprite MCP 执行的原生绘制脚本。
- `validate-bubble.lua`、`validation.txt`：原生检查脚本和结果。

## 验证

已验证尺寸、图层、帧数、100ms 帧时长、16 个不同动画帧、固定轮廓、透明角落与 GIF 帧数尺寸。逐帧对比 v2 的第 1/2/3/5 图层，像素和位置一致。已查看深浅底首帧和中段帧。

仅 2D 美术迭代，未进行三维验证、代码改动、编译、打包、游戏接入或游戏内测试。
