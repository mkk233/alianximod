# 水幻形 D 第 2 轮返修报告

日期：2026-11-02。只去掉已收下快照在绘制时的 160 格相机裁剪。命名门禁已通过。未跑 Gradle，未进游戏。本文件写完后停止写入，不自行复审。

## 删除

`src/main/java/com/bslx/XueXiMod/client/CloneAfterimagePlayback.java` 的 `draw`（423–455 行）。

删掉的条件是：相机到快照锚点的距离平方大于 `TRACKING_REACH_BLOCKS`（160）时 `continue`。一并删掉只为这道判断存在的 `reachSquared`、`offsetX`、`offsetY`、`offsetZ` 和 `squared`。

相机位置仍用于把姿态平移到视点，这不是距离拒绝。

## 保留

绘制仍跳过维度不符或已经到期的快照。会话校验、256 条快照上限、256 条终止标记、阶段去重、序号、实体离开只结束维持、世界切换和断线清空都没有改。

服务端 160 格附近接收者和同维主人补发没有改。包布局、通知和接收者选择没有改。实体与维持快照之间的 2.5 格关联仍在 `nearestMaintain`，那是链接而不是绘制距离。没有新增可视距离开关，没有强制加载区块，没有放宽其它渲染器。

## 验证

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 0。

未执行 `gradlew compileJava`。未启动游戏。
