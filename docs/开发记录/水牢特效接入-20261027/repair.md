# 当前阶段：实现返修
Cursor CLI --model grok-4.7-high-fast，原唯一作者。集中审查全部读写已收束，允许返修。读 review-implementation-report.md；只动 src/main/java/com/bslx/XueXiMod/client/PrisonShellRenderer.java，PNG保持不变。绝不改其他src、正式源、索引、表、配置。不编译、不游戏、不spawn。
集中复核结论需要两项最小修复：
1. overlayCoords(0) 改 OverlayTexture.NO_OVERLAY，保留原始蓝色；复用现有同材质绘制先例，不调整共享材质。
2. 顶点提交先double坐标减camera，再cast float，使用原始rotation视图矩阵；消除pose.translate(-camera)和绝对世界坐标float相互抵消。不要二次减相机或二次转视图。其余投影、正方形尺寸、聚能bounds、UV生命周期保持。
3. Codex实现第1轮发现另一个待证实API：当前约266行 rotation.transformTranspose(worldOffset) 的 worldOffset 是Vector3f。用真实项目JOML jar javap核对是否有 Matrix4f.transformTranspose(Vector3f)；若无，使用项目版本支持的逆旋转等价最小修复，例如 new Matrix3f(rotation).transpose().transform(worldOffset)，只处理rotation旋转部分。明确回报实际jar路径与签名证据，不能凭记忆。
完成运行 powershell -NoProfile -File tools/check-name-gate.ps1。仅本文件问题可修，OriginalHumanoid旧问题保持。不要自行跑Gradle。无需重复全仓通读或再做设计讨论，依照集中结论返修。
交付≤800字：准确改动、API证据、门禁退出码、本次文件检查、剩余风险、未编译；明确“写入结束，待统一编译”。
