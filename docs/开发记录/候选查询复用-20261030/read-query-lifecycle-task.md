# 步骤3：只读定位目标查询及实体查询生命周期事实

固定Cursor CLI --model grok-4.7-high-fast，工作区D:/alianximod。只读事实定位，不设计裁决、不正确性复审、不派代理/其他CLI，不写文件，不编译/测试/游戏。累计设计2/2已用完、实现2/3，不追加复审。最终result由包装器保存。

用户授权减少重复候选实体查询。读本目录记录.md和docs/术式链计划缓存说明.md。源码范围只限src/main/java/com/bslx/XueXiMod下LockTargetService、SkillSegmentRunner中目标查询/上下文传递位置、SwordEntity中ownedBy/anyOwnedBy、SwordVortex相关调用，以及当前PerfCounter/PerfMetric/PerformanceRecorder计数接口、现有mixins配置与实体生命周期相关mixin/事件。先rg精确定位，只读相关方法和直接依赖，不通读整个仓库。

还可只读本机已有Forge1.20.1/Minecraft映射源码缓存里验证以下API所需的文件（不下载、不修改）：ServerLevel/Level.getEntitiesOfClass、LevelEntityGetterAdapter、EntitySectionStorage/EntitySection、Entity.setBoundingBox/setPos和levelCallback/ServerLevel实体加入移除回调。只核对查询遍历顺序、按AABB过滤时机、哪些现成回调在实体移动/扩缩/跨section/新增/移除时触发及顺序、包括LivingEntity大小变化和非Living命中体移动的区别。如缓存源码不可读，报告缺口，不猜。

报告：目标查询是否同一次释放重复且中间可有任何世界副作用；距离相等时原有顺序决定谁；看向/位置是否随复制体或段而变；现有缓存/索引是否已存在、其失效触发事实；查询API/事件精确签名和直接证据行号；服务端绑定/停服清理可复用的模式；本仓mixins命名和服务端安全放置方式。只描述事实及未知，不提出新架构或判定缓存正确性。

外部定义/Forge事件可能修改世界，未证实调用不能当成纯函数。不要把仅tick过期说成实时失效；仅重新过滤旧候选不能补上新进入区域的实体，这一点只用于保证报告覆盖新增/移动路径事实。最后列实际读文件及缺口，确认只读无写入无后台任务，完整最终返回。
