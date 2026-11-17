# A最终整合续接说明

先读本单，再读author-a-final-integration-task.md。本单更新前置时序，覆盖旧任务“所有D写入先停止”的泛化要求：A domain-bridge与C content/tint均已最终停写，D第一段接口稳定且已交回；D正在独立完成author-d-scorch-bridge-task.md，只改自己的ContactEnchantState/SurfaceFireHosts/ScorchEffect/NetherDomain几何同步，保持对外sample和已有loaded签名，不改A公共入口。你可以并行完成不依赖该在写源码的C最终注册和A侧公共实现；禁止读D中间源码或流式日志，等D最终交回后若有新请求主会话另传。设计2/2、实现正式0/3。

额外必读已收束文档：author-a-domain-bridge-report.md/author-a-interface-v3.md、author-c-report.md、author-c-tint-report.md、author-c-interface-final.md、最新author-c-registration/ownership、D第一段final report/interface/registration。旧C注册清单里的“残骸/地形配置未接”已经由你的terrain-registration完成，不能重复注册。C动态色调也已经自己接好，不再抢其渲染文件。

在原final-integration任务范围里一次补齐：
- BatchCarrierTypes总线、BatchCarrierBootstrap.install调用时机、视觉渲染/全局重绑/语言，真实启动入口只一次。
- C交回的TechniqueHitSettle intercept接缝，不能与原扫掠在同次接触重复直接命中/资源/中心扩圈；严格依完整intake N/O/DV+approved-v1+合同的最早合法实体/地形/超时区别、接续与地形顺序。C片段只示例，不把与合同矛盾的行为当新授权；明确剩余C实现请求。
- FireOrbEffect publishedHook接真实BatchFlameTrail/BirthLayout/HitClock/root/load。在A ownership中明确加入这份原来没有作者写的旧效果文件；只加本批兼容留焰，不改旧火球伤害/产种/爆炸。
- 喷涌规则使最终可变宿主轨迹实际采用UPWELL，而非只检查宿主definition默认轨迹；按已批准速度/重力/复制锥面/冻结/寿命与同分支冲突处理。
- fire_nether_gloom必须有真实登记的附魔提交效果，不能因为C bootstrap没登记就整段拒绝；其效果是向已支持领域传载荷，绝非新建领域或假攻击。可用与你现有公共注册契合的共享附魔提交实现，不要求D去抢registry。D的领域处理/网络已接。
- 普通武器仅处理合同支持的新资源命中，留焰明确仅火苗/火球/陨岩移动宿主，不新增武器/剑实体/光束火径或武器陨岩。没有真实消费方的adopt必须释放，不能仅因载荷标志就泄漏static根引用。

全部写权限仍为A共享所有权，不修改C/D/B独占文件。对C内部尚需适配的问题交具体最小请求；不要用反射静默忽略缺接口或返回成功占位来绕过。D后续结果未收到不影响你完成本单，报告清楚这一等待边界。禁止编译/游戏/打包/独立测试/正式复审/表写。最终交原任务要求的author-a-final-integration-report.md/json与interface-final/ownership并停写。
