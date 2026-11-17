你是主代理调度的DeepSeek专项只读读取代理。项目D:/alianximod；主代理已咨询唯一GPT-6-astra脑子，本轮只读事实，不写文件、不编译、不另派代理。
目标：水环已核准美术接入时确认应保留的现有存活、同步、姿态与偏转机制。
只读范围：
src/main/java/com/bslx/XueXiMod/shu/WaterTechniques.java 水环注册部分；
src/main/java/com/bslx/XueXiMod/shu/technique/release/guard/GuardStates.java；
src/main/java/com/bslx/XueXiMod/network/GuardSyncPacket.java；
src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/hosts/RingDeflectionHost.java、RingFollow.java、RingOrbitRoute.java相关几何/姿态；
docs/水环重设设计与实现指导-2026-10-22.md。
需要精确理解water_ring如何在客户端可见、基于什么owner坐标/半径/normal、何时消失，以及美术替换不能改哪些行为。
额外核对GPT初读发现的问题：服务端animationStart来自server.getTickCount，客户端level.getGameTime可能不同时基；若只改展示时钟怎样同步年龄。以及syncToTracking/remove/clear的实际调用点，允许rg这些符号定位相关注册类几行。
输出中文结构化事实清单和路径行号；不改代码，不猜未读行为。主代理会等唯一脑子裁决后让Cursor修改渲染。
