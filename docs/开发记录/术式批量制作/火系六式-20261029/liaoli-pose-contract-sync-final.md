先读这份契约同步任务的授权范围，再按它规定的边界改。旧检查仍要求柄更靠后、尖更靠前。只把这几条改成已确认的站立朝下、柄近尖远和火焰沿新剑延伸，其余约束不动。# 同步燎里布局检查的方向预期

只改了 `src/test/java/com/bslx/XueXiMod/visual/liaoli/LiaoliV6LayoutCheck.java`。生产代码两份哈希仍是上次接入报告里的值。没有运行这份检查，没有跑 Gradle，没有进游戏。本阶段额度设计 2/2、实现 3/3 不变。这不是新增测试阶段，也不是正式复审通过。本任务写入已经停止。

## 同步的旧预期

这些断言还按「柄更靠后、尖更靠前」写，和已确认的站立姿态相反，已改成方向语义，没有把实现里的半径和换算式抄进期望值：

- `checkFan` 的 `hilt.forward() < tip.forward()`（柄在尖后方）。改为：尖比柄低，而且落差大于 0.4 格；相对背后中线，柄比尖更近。剑仍在身后。
- 同一循环补上尖端的左右镜像，和原有柄镜像、相位、扇角和单侧翻转一起保留。
- `checkFlames` 的 `start.forward() < tip.forward()`（火起点比尖更靠后）。改为：火的根比尖更靠近柄。中线、在身后、起点不是尖，这三条还在。
- `checkFlames` 的 `end.forward() > tip.forward()`（火比尖更靠前）。改为：火焰终点在柄到尖的延长线上，并且已经越过剑尖。
- `checkReveal` 的 `early.forward()` 比较（金边沿旧的前后方向铺）。改为：第 3 拍仍落在剑身中间，并且更靠近柄而不是尖。冷、满、逐渐铺开这三条没动。

手持剑、分流、三道环、六张贴图哈希和源码字符串约束没有改。

## 名称门禁

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1。本次检查文件没有新的命名问题。剩下的仍是 `OriginalHumanoid.java:165` 的 `i` 和 `:202` 的 `to`，没有改。

## 哈希

检查文件 `45BC9DB109B6CE2A5F1C8A8C83F5FC2CCB4F0B9F14F1AAC84A6E64C28F2A9173`，11653 字节，2026-10-30 00:53:56.3400226。

生产文件未改：

- `LiaoliV6Layout.java` `98169A38537D34EA19B95DC9482A863D7A85E95562A2BC44AF2C49D599394C35`，2026-10-30 00:46:47.1639184
- `LiaoliV6Renderer.java` `ABB8B26D9696672275CE319F4B55E01050FD7CFCBCFEE3C3E1D9B236C3DC99AA`，2026-10-30 00:47:36.0356603

没有运行 `LiaoliV6LayoutCheck`。本任务写入已经停止。报告在 `docs/开发记录/术式批量制作/火系六式-20261029/liaoli-pose-contract-sync-report.md`，证据在同目录 `liaoli-pose-contract-sync-evidence.json`。
