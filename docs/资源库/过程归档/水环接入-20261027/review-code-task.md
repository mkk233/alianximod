你是独立Grok4.7代码审核代理。Cursor已完成水环v8游戏接入，用户要求主代理核对设计且编译通过，不打包，用户游戏内实测。只读本次已完成变更，不修改文件、不运行写入命令、不另派代理。
先读D:/alianximod/docs/资源库/过程归档/水环接入-20261027/implementation-task.md（含唯一Astra最终裁决）和implementation-report.md。检查实际源文件与before/备份的本次差异；git仓库大量既有未提交改动，不能把全局diff都归本次。不要读任何raw .jsonl里的thinking内容。
目标：找会影响用户核准的3D双缘毛边、破碎流转、共同逆时针、游戏正确性的可证实问题。正式模型是docs/资源库/特效/水流/周身水环_细碎毛边_v8/周身水环_细碎水膜毛边_v8.bbmodel；不要输出base64。核对runtime只取三个水mesh，准确源顶点/UV，非平环替代，不含人物；三层同变换当地Ry→静态Z3°→法线对齐，统一scale=radius/11.775；48tick圈、32tick纹理、96tick闭环，U无额外整周滚动；帧高度body24外内40，合并atlas768x640布局与源图逐像素一致；单同材质可排序透明批次、depth test且不写depth、no cull、不重复反面、白色顶点色、无二次染色、远坐标double先减camera、F3T重载不每帧IO。
同步核对：wire只加animationAgeTicks服务端now-animationStart，客户端game tick锚点，重复FULL/POSE及refresh不断相/不倒退，新的animationStart身份能替代旧高revision，旧FULL/POSE/REMOVE不破坏新环；FULL当前法线+剩余姿态时长而非重播旧完整pose。客户端level unload清理；已有StartTracking在ReleaseBaseEvents，不应重复。旧维度REMOVE覆盖已有guard.serverLevel观察者。玩法持续时间、魔力、减伤、偏转和碰撞band不改。
检查编译log和定向测试是否真实通过及覆盖关键行为，不做游戏内测试。验证是不是真实调用生产代码，别把仅复算常量当证明。
输出按严重性排列可行动问题：文件绝对路径+行号、触发条件、实际错误与必要修正；没有则明确审核通过并简要说明覆盖。不要用未证实的推测提缺陷，不扩大无关范围。主代理会独立核对。

