# Cursor 实现 A：静止斩面空闲扫描

固定 Cursor CLI --model grok-4.7-high-fast。你已是被授权实现者，不再派发代理。先读 AGENTS.md、.grok/skills/max-dispatch/SKILL.md 和本目录记录.md。设计1/2已批准，实现0/3，当前是实现而非独立复审。不创建/运行测试，不编译、不打包、不启动或操控游戏、不提交。大量既有修改必须原样保留，禁止reset/clean/checkout。Git用单次 -c safe.directory=D:/alianximod。
本项目根 D:/alianximod。你唯一可写源码：src/main/java/com/bslx/XueXiMod/client/StillSheetPlayback.java、src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/StillSheetCache.java；如确实必要可新增命名明确的专用客户端事件监听文件。不要改 TechniqueHitEntity、ClientTrack 或其他文件，那里有另一聊天作者正在写。
可读取上述及实现所需直接调用类，输入报告read-client-final.md第1项，不扩展整仓审查。
已批准设计：
1. 客户端实体加入/离开事件维护 TechniqueHitEntity 候选集合，tick/每帧只迭代候选，取消当前全entitiesForRendering扫描。不要在加入时只判一次斩面类型：数据可能尚未同步齐；候选继续用现有offer逻辑确认。不得漏掉晚到数据。
2. 无候选且无保留快照直接返回。空缓存不要为copyRetained创建空列表；不要重构播放机制。
3. 保持快照更新、UUID去重、时间/partialTick、实体消失后播放完毕。候选移除不能删仍有效快照。同维复活不清世界效果。
4. 候选绑定客户端世界身份，登出、卸载、切换清理。说明首次启用和已加载实体覆盖，确需初始扫描只能每世界初始化一次，禁止逐帧/逐tick全表兜底。客户端事件可能比client.level更新早，不能因初始化覆盖而漏候选。
5. 不增加副作用、不降低视觉/距离，不修改服务端行为。
实现前为每个将修改的现存文件保存当前字节到本目录before-a/下保留包路径，作为这次增量基线（不是git HEAD，已有未提交改动很多）。仅你写before-a、implement-a-report.md、implement-a-evidence.json和name-gate-a.log，不写记录.md。
做必要自查和 powershell -NoProfile -File tools/check-name-gate.ps1，只修自己修改文件的命名，不扩修其他文件。最后报告实际文件、行号、实现前后SHA256/最后写入时间、如何保持上述边界、未完成项；明确停止所有源码写入并释放文件占用、待统一编译、未游戏验证。不写帧数收益。若设计前提不成立，停止对应改动并报告，不扩大范围。
