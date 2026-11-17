# Cursor 只读信息采集：空闲客户端

固定 Cursor CLI --model grok-4.7-high-fast。你已是被授权的 Cursor 执行者，不再派发 Cursor 或其他子代理。
工作区 D:/alianximod；遵守 AGENTS.md 与 .grok/skills/max-dispatch/SKILL.md。只读，不改任何文件，不编译、不测试、不启动/操控游戏、不打包、不提交。不读凭据。Git 如需要使用 git -c safe.directory=D:/alianximod。
用户现象：24区块，原生Forge无优化mod，刚进世界没施法也只有40–50FPS，死亡复活更明显。硬件i7-12700KF、RTX4080SUPER、32GB。不要以硬件或源码猜测代替测量。
允许读取：D:/alianximod/src/main/java/com/bslx/XueXiMod/client、event、gui、mixin（若存在）、visual，以及这些客户端入口直接调用的本项目类、src/main/resources/xueximod.mixins.json、必要局部文档。不要通读无关术式。若名称不同，用 rg 定位对应目录。
重点：渲染/HUD、客户端tick、输入和目标判定、粒子/着色器、全实体/区块扫描、NBT解析、重复分配、每帧I/O；无施法短路；死亡/复活的客户端清理与重复注册。对资源只读元数据。其他会话排查服务端/网络/复活公共路径，不重复该范围。
交付一份完整最终报告：最多8个有证据热点，每项具体路径与行号、调用频率、空闲/复活触发条件、循环范围/分配、已存在的缓存与失效、保持语义的最小优化边界。明确区分确定的多余工作与运行采样才能确认的耗时。无足够证据就明确写未证实。优先报告能影响空闲的项目；不要把仅施法时运行的问题当作空闲主因。列出必要但尚未跑的对照实测。不要作本批正式正确性结论，等主会话集中裁决。
