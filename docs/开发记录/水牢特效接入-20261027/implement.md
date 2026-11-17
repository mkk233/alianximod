# 当前阶段：实现（已授权执行）
模型固定 Cursor CLI --model grok-4.7-high-fast，不得换档。遵守项目 AGENTS.md、.grok/skills/max-dispatch/SKILL.md。你是唯一实现者。读本目录 design-consolidate-report.md；按其中已消解的方案执行。本项是直接用户授权的水牢视觉接入，不是表队列领取。

## 需求与输入
用户已批准二维v3（96×96每帧，16×100ms）和v4等比公告板：纯2D单张面，始终面向当前客户端渲染相机含俯仰。每客户端独立计算；整体等比变大/小，保持圆形，不分别拉伸宽高。透明蓝色源像素保留、白顶点、不双绘alpha、保留世界遮挡。目标体型自适配，能量/聚能后的实际范围必须体现为视觉放大。现有湿度、玩法、锁定/释放/死亡/维度/超时、碰撞、时长、协议保持。缺失实体和ID复用安全，第一人称/近平面按方案稳定处理。
素材 D:/alianximod/docs/资源库/特效/水流/水牢_等比公告板_v1/water_prison_billboard.png，必须字节复制，SHA256 8ba44798140ec92fc45b826549cec03cf629bcb7bcd1d34dab750462101e5096。不重新绘画或修改PNG。正式目录其余源文件已归档。

## 唯一文件所有权
允许你写：
- D:/alianximod/src/main/java/com/bslx/XueXiMod/client/PrisonShellRenderer.java
- D:/alianximod/src/main/resources/assets/xueximod/textures/entity/xueximod/water_prison_billboard.png
先检查同名PNG是否存在，若存在与源不同先报告，不覆盖非本项资产。接入前渲染器基线在本目录 PrisonShellRenderer.before.java.txt；保留基线已有工作。其他 src 文件、规则、制作表、索引、正式美术、Gradle配置均不可改。若确需helper文件先报告理由，不扩范围。

## 约束与交付
编译授权：无。不得 gradlew、build/jar/reobf、runClient、启动游戏或提交。不要spawn子代理。不要清理工作区，不reset/clean/checkout。不得改Java范围外的既有脏文件。
完成Java改动后运行 powershell -NoProfile -File tools/check-name-gate.ps1，只修你改过文件内发现的命名问题；其他基线报错仅如实记录。做必要的贴图引用/帧数/哈希定向检查。禁止自造游戏已验结论。
交付 ≤1000字：实际修改文件、关键实现及边界、命令退出码、资源一致性、风险/未跑项，并明确“待统一编译；文件写入结束，可释放所有权”。当前阶段只有你一个作者，待你完成后再集中只读审查，不边改边审。

已统一：复用 StillSheetMesh.of 材质，不增加helper。设计报告旧过程源和正式源字节相同，实际复制使用上面明确的正式源。只按设计报告里的有限方法定向读代码；不再次通读全仓或设计辩论。模型固定High Fast。
