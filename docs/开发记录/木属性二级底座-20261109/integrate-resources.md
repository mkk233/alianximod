# 整合C：木系资源与同步接线

仅主会话在原C工作项带入依赖回执并更新TaskFile后执行。本批架构仍为本目录架构裁决.md，设计1/2，不新增术式或花谱。资源A的a202611090913301145ceb已经reported_done/valid/writesSettled=true，session49791fb3-b7e2-4753-8dc4-164d04f2b4b5。读取资源作者交付.md及后续若有的定向返修回执，依当前实际API接线，不修改A私有目录。

你是公共接线唯一作者。在独立WoodResourceRuntimeEvents里接onServerStart/onServerTick/onOwnerDeath/onOwnerLogout/onOwnerChangeDimension/onChunkUnload/onLevelUnload/onServerStop，按A合同在服务端主线程调用。用户死亡双清，其他非自然清场不发奖；必须落实实际事件，不能只加空方法。启动/停止与主世界DataStorage可用期匹配。注册一次SuihuaStatus.PROVIDER，保留既有resourceAtLeast整数度数语义。

新增SuihuaSyncPacket（服务端到客户端唯一方向），载荷只含当前本人UUID及六个整数绝对分量。packet解码边界和方向验证、客户端安全调度、专服不加载Minecraft客户端类要落实。SuihuaClientCache已有A实现，C只接包及断线清理。登录/重生/换维发一次；余额改变经sink或dirtyOwners单发给当前在线本人，无每tick全服广播。未知版本ledgerWritable=false时不得把全零当成真实余额发给玩家。网络注册号追加当前列表末尾，不覆盖旧包；需要同步更新握手协议，避免旧客户端握手成功后收到未知包。不要改变现有包格式或顺序。

必要的counterattack死亡消息在现有zh_cn/en_us末尾添加，保留所有已有键。记录真实注册号、协议和客户端隔离方式。适配器/事件里的异常不得误把已经扣除的资源重复退还或重发奖。

不做地面渲染、美术、HUD、具体术式、花谱MobEffect登记、枝条/花开/侵蚀/岁岁枯荣或生命恢复净化。可准备必要接线检查，不运行Gradle/javac/检查脚本，结束后集中验证。任何A/B私有API缺口交回原作者，不直接改。保留全部已有脏改动，不操作其他任务、启动器、运行记录、制作表或批次额度。

写整合交付.md说明最终公共接线、接口/文件/行号、需要原作者修复的阻塞（若有）、未运行验证。最终回执writesSettled=true，待统一编译；此阶段没有游戏验收。
