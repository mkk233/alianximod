# 空间切割左腰右拔 v5：实现复核返修

继续你已完成的用户授权实现，仅修实际缺陷。先读本目录 review-r1-report.md、implementation-report.md 和 implementation-ownership.json。当前 Cursor 必须 grok-4.7-high-fast；你是可写作者 044b78d1-5a7e-4853-ab82-af2512815f4b。

## 明确返修范围
只修改 src/main/java/com/bslx/XueXiMod/client/action/spacecut/SpaceCutItemMount.java（若证明必要的编译依赖在其他文件，先返回具体原因，不越界）。
1. 按复核指出的完整矩阵修正真实剑的展示旋转抵消：挂接线性变换必须满足 M * R展示 = T，而不是 T * R展示²。保留真实物品自身模型、缩放与正常 ItemDisplayContext，不修改 SpaceCutSampler 四元数。让源模型刀轴与物品最终刀轴、扁平面法线一致。
2. 抵消当前展示平移，保证握持点处于掌心。复核给出第三人称 (0,4,0.5)/16，第一人称 (1.13,3.2,1.13)/16；请以实际 renderItem 完整变换验证，不能仅套数值后宣称正确。实际剑长度沿用真实物品。
3. 第一人称进入 drawFirstPerson 前父栈已乘两个 bob 相关 0.1 旋转，按实际顺序取逆，消除额外俯仰/偏航；保留原有身体进相机坐标的正确变换。不改全局视角、用户输入或其他术式。

## 自查和交付
这是返修作者实现自查，不是新的独立复核轮次。做有意义的小范围数值检查：起始、0.48、0.60、1.60 秒的第一/第三人称最终刀轴及法线偏差、握持点误差；第一人称不同俯仰/yaw/bob下验证外层旋转抵消（不要只检查父栈）。可在过程目录保留检查脚本和输出，不写镜像实现测试充数。检查主手左右/副手剑分支未被破坏。运行命名门禁 powershell -NoProfile -File tools/check-name-gate.ps1。
不运行 Gradle，当前仍未授权编译；不打包、不 runClient、不启动游戏。保留工作区所有既有改动，不 reset/clean，不碰其他任务所有文件。
不改已确认的 Aseprite/Blockbench/预览字节。更新正式资源目录 runtime-mapping.md/json 中本次实际映射，以及本任务 implementation-report.md、implementation-evidence.json；追加 repair-r1-report.md，包含实际文件哈希/最后写入时间、自查证据、限制。不要改主会话所有的资源 README/manifest/根索引和调度记录。
最终完整返回结果并明确停止全部源码写入；我只等最终反馈，不需要实时过程。