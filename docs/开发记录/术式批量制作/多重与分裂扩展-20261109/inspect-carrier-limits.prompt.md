模型：Cursor CLI --model grok-4.7-high-fast。原inspect-base同task/session80cde5fe-4edd-4070-8a46-322a2ae7490c的范围内只读补事实，不作新架构裁决/正式复审，不写任何文件，不编译，不跟踪运行中内容。
用户最新纠正：测试法杖32装配格；8格上限只用于剑术，剑术有四个释放入口、四条独立8格链。不要扩容量。剑术不允许使用八重及以上多重；本批按spec能力fixedCount>=8或remaining禁止剑术，三/四重与2/5/8份复制不额外禁止。
请集中采集现行代码的最小落点：
1. 测试法杖32格与剑四入口每链8格的真实字段/容量签名，原review提Skill.CHAIN_LIMIT=8为何只适用于剑链。SpellAssemblyScreen/SkillAssemblyScreen等UI分别如何关联载具。
2. 剑术装配服务端验证入口SkillAssemblyPacket及任何法杖装配入口，现有共用Technique/ReleaseDefinition/载具资格规则如何拒绝不支持术式；客户端可装配列表是否同用资格函数。只查这次限制需要的直接引用，不整仓。
3. 已保存链/NBT/命令或旧装配链施放时现行服务端资格检查位于何处（四入口分别在哪解码/分链），能否复用现有公共资格点检查multicastSpec.fixedCount>=8/remaining，防绕过。不要只靠UI隐藏。
4. 给最小必改文件及准确方法/路径/行号，列可复用现有拒绝文案/本地化键；如需新增最少提示，列语言路径。按spec类属性不按三个ID特判，保持法杖与其他规则原资格。
5. 当前外部wood verify-delivery仅编译/检查不写src，其他实际源码作者刚有完成回执；你只读未被新作者占用的代码。原读取避开的ReleaseDefinition/SkillUsePacket如必须读，先仅核对其当前输入权限/最终状态，若仍有活跃写入则交回所需单符号而不读实时变更。
输出事实与最小已有机制落点，不替用户重设架构、不重新评价旧底座；本单是新输入信息采集，设计额度已经2/2不能再开架构裁决。最终标准报告changedFiles=[]、writesSettled=true，列未实施/编译/游戏。将容量事实给主会话修正文档与制作表DV。
