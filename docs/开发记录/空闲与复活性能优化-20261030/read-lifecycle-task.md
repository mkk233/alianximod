# Cursor 只读信息采集：复活与常驻服务

固定 Cursor CLI --model grok-4.7-high-fast。你已是被授权的 Cursor 执行者，不再派发 Cursor 或其他子代理。
工作区 D:/alianximod；遵守 AGENTS.md 与 .grok/skills/max-dispatch/SKILL.md。只读，不改任何文件，不编译、不测试、不启动/操控游戏、不打包、不提交。不读凭据。Git 如需要使用 git -c safe.directory=D:/alianximod。
用户现象：24区块，原生Forge无优化mod，刚进世界没施法也只有40–50FPS，死亡复活更明显。硬件i7-12700KF、RTX4080SUPER、32GB。不要以硬件或源码猜测代替测量。
允许读取：D:/alianximod/src/main/java/com/bslx/XueXiMod/event、player、network、status、debug、block、mcp（若存在）、XueXiMod.java及这些常驻事件和死亡/复活直接调用的类。可用rg定位入口，避免通读无关术式。可只读 build.gradle、gradle.properties、run/options.txt、run/logs/latest.log 的必要行/末尾（不要整日志输出），区分开发启动配置与游戏堆配置。其他会话负责client/gui/渲染，避免重复。
重点：PlayerEvent.Clone/Respawn/死亡/登录，存储NBT复制与同步包，持续tick、实体扫描、全世界循环、日志洪泛、定时器/监听重复注册、旧玩家/世界缓存未释放。判断没施法时是否仍执行。对24区块相关开销指出证据。不要杀进程、改配置或采集运行中的JFR。
交付一份完整最终报告：最多8个有证据热点，每项具体路径与行号、频率、空闲/复活触发条件、数据规模、现有短路与生命周期、保持功能的最小优化建议；列运行参数或日志直接证据，若未发现异常也说明。明确源码事实与尚未采样假设，不能宣称根因或帧数收益。指出改动可能涉及的共享文件，留给主会话分配所有权。不要作本批正式正确性结论，等主会话集中裁决。
