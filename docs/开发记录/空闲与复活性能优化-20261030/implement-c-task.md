# Cursor 实现 C：领域移动空结果

固定 Cursor CLI --model grok-4.7-high-fast。你已是被授权实现者，不再派发代理。先读 AGENTS.md、.grok/skills/max-dispatch/SKILL.md 和本目录记录.md。设计1/2已批准，实现0/3，当前是实现而非独立复审。不创建/运行测试，不编译、不打包、不启动或操控游戏、不提交。保留全部已有未提交修改，禁止reset/clean/checkout。Git用单次 -c safe.directory=D:/alianximod。
根D:/alianximod。唯一可写源码：src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainMotion.java、src/main/java/com/bslx/XueXiMod/client/DomainClientState.java、DomainClientBridge.java（位于该项目包内，用rg定位）。不改EntityMoveMixin或其他源码。可读取直接调用者判断返回列表是否可变。输入read-client-final.md第2项。
已批准设计：客户端领域集合为空或移动者不符合原有本地预测条件时返回共享空结果，避免DomainMotion.relevant再复制空结果。如果直接返回桥接列表，必须确认调用者不修改它，否则只为空结果短路。服务器路径、已有领域按轴裁切、本地玩家/根载具条件、领域进入离开即时生效、移动前后配对恢复全部不变。不要缓存有内容的列表跨tick，不重构领域体系，不改变同维复活行为。
修改前将每个待改现存文件当前字节备份至本目录before-c/下保留包路径（不是git HEAD）。仅你写before-c、implement-c-report.md、implement-c-evidence.json和name-gate-c.log，不写记录.md。其他作者写动作/蒙皮与斩面，另一个聊天写火种/燎里，不能写其文件。
做必要自查和 powershell -NoProfile -File tools/check-name-gate.ps1，仅修本次自己文件命名，保留历史问题。最终报告实际修改文件与行号、前后SHA256/最后写入时间、列表是否被修改的证据、服务器未变/状态配对保持的理由、未完成项；声明源码全停写并释放占用、待统一编译、未游戏验证。不要估计FPS。前提不成立停对应改动，不扩大设计。
