B继续：GPT-6-astra已明确批准你把两个消费端纳入范围。只补下列改动，不扩展其余模块：
1. WallDeflectionHost 的屏障有限正寿命到期判断必须采用 ReleaseInput.hasExpired(now, expiry)，与BarrierBody统一。保持既有无限/非正语义，不能直接>=比较int绝对时刻。
2. DomainPressureRenderer 的游戏时间到期边界从 gameTime > visualEnd 改为 >=，与服务器和其他领域渲染一致。只改边界，不调渲染流程。
3. 你报告的同tick去重不含倍率暂不改。若已有明确证据两次不同操作身份同tick实际被吞，仅记录最小证据及来源，交公共路由组统一裁决，不要给key加倍率避免破坏幂等。
先保存这两文件任务开始前快照到本目录before/对应相对路径（若已有绝不可覆盖）。修改后更新containers-report.md，并加合适定向检查覆盖屏障宿主到期；不要扩写无关测试。所有更改交给本Cursor会话完成。
禁止改公共路由、ReleaseInput/CastFrame、实体/水牢组或美术；其他组正写公共路由，别碰。此阶段仍不跑Gradle，不打包，不启动游戏，不提交。完成后停止等待主控审查。