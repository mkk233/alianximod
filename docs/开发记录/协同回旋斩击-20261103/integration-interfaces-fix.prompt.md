沿impl-integration原task/session进行接口作者返修，不新开任务或额度。公共接口阶段最终回执已valid/reported_done/writesSettled=true，C/E/V尚未Start，没有消费者正在写。文件范围仍是原TaskFile，业务/旧共享代码/编译禁止。

主会话在本批实现第1/3集中逻辑复核中只检查了你的最终接口交付文档，发现稳定API交付阻塞：interfaces-delivery.md第4节同时列出public static TechniqueExecutionCapabilities timedRelease()与public boolean timedRelease()，standaloneDeployment()/cooperativeRule()也同样重名同参；Java不能仅依返回值或static区别重载。请你定向核对自己新建的TechniqueExecutionCapabilities实际源码和交付文档；如果真实代码也有冲突，修为不冲突的稳定名字（优先保留static工厂名，把实例判断改为isTimedRelease/isStandaloneDeployment/isCooperativeRule之类一致命名或统一has）。同步你范围内直接引用及interfaces-delivery.md，明确最终签名。若仅文档写错，则只改文档并给真实精确方法。不扩成全库审查。

同份文档ExecutionFamilyPlan说明“取消令牌和完成令牌不同，并和每个AttackPreparation.cancelId相同”语义矛盾：请明确是每个攻击的cancelId等于取消tokenId，而完成token独立；只核对此处新类型实际检查和文档，按已冻结合同修正，不能让两枚令牌同时等于同一个cancelId。

这是已记录发现项的作者修复和必要定向自检，不进行新正式复审，不跑gradlew，不改记录.md/制作表/启动器，不新增美术。当前视觉已获用户确认，但本单仍不接资源。保留所有既有修改。原工厂/接口清单必须对消费者清楚；完成停止写入后才派C/E/V。主会话不会在本次返修后再单独消耗一轮正式接口复审，后续集中实现复核时再审整体。

最终报告修了什么、实际精确签名、涉及文件、未编译，唯一标记回执：
<<<CURSOR_TASK_REPORT>>>
{"workStatus":"reported_done","summary":"接口定向返修结果","changedFiles":[],"checks":[],"notRun":["未编译","未游戏验证","未新增正式复审"],"writesSettled":true,"blocker":null,"nextAction":"主会话转交接口修订后的最终清单给C/E/V，不另开逐项正式复审"}
<<<END_CURSOR_TASK_REPORT>>>
