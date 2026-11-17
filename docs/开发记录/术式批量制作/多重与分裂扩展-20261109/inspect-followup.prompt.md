模型：Cursor CLI --model grok-4.7-high-fast。本单为 inspect-base 范围内只读补事实，精确Resume原session 80cde5fe-4edd-4070-8a46-322a2ae7490c；不新增实现、不做正式代码验收、不重新复审旧底座。完整读取TaskFile与本Prompt。
前次回执 reported_done/valid/writesSettled=true，进程已结束、无文件写入；本次输入沿用户八项新规则要求，已有唯一gpt-6-astra设计1/2方案裁决。
八项：rule_triple_release/fixed3、rule_quadruple_release/fixed4、rule_octuple_release/fixed8、rule_sixteen_release/fixed16、rule_exhaustive_release/remaining；rule_double_cast/of2、rule_quintuple_cast/of5、rule_octuple_cast/of8。仅数量声明不同，角色保留RULE_DUAL/RULE_TRIPLE。尚未实施、尚未入表。
只在前次发现的相关调用链集中补齐下列事实，不要通读整仓，不改文件：
1. SkillChainPlan.java:243、680–681：所在方法、上游调用者及八个新声明是否经过；当地能否取得ReleaseDefinition/spec；copies乘积是否会被其他阶段再次展开。报告数据流及准确行号，勿重新判定旧实现正确性。
2. CooperativeChainCompiler.java:179–182、251：两处复制3具体用途；恰好2攻击是双重固定术式适用门槛还是通用多重截断/展开逻辑；不满足回退何处，新项能否走既有通用路径；实际成员间隔/支付是否由通用展开接管。
3. BatchReleaseCompiler.java:61–65、102：dualRemaining是原始格数还是攻击数，declaresBatch来源与调用链；新项同角色是否实际可到达；是否有既有通用回退。禁止为了让一行生效而把这两角色加进batchLifecycle。
4. 仅上述编译入口到MulticastParser/MemberCastView.copyProduct的spec传递，是否另有默认2/3回填。列需要本次新项最小改动的文件、可不动兼容入口和原因，不作旧底座验收结论。
5. 复制生成几何：实际份数是否任意枚举，还是仅三个位置；2/5/8是否已有通用分布；精准定位空间算法、相关调用者，列新项必须触及的文件。若有用户可见布局新设计需求，报告事实不要自行改变。
6. 核对拟新增八ID未占用，列准确metadata注册签名与提供description的位置。末尾给最小安全扩展建议（仅基于事实，不替代主会话的剩余设计裁决）。
当前木属性整合作者占ReleaseDefinition.java/SkillUsePacket.java，其他木系作者占status/wood/resource/**、combat/counter/**、MagicPool.java等，禁止修改和读取其运行中改动。本单只读既有与新项相关、且未由上述作者写入的文件；如必须触及占用路径先报告阻塞。
输出简明但完整事实、真实绝对路径/行号，最终附标准 <<<CURSOR_TASK_REPORT>>>；changedFiles=[]、writesSettled=true，未跑实施/编译/表格/游戏。摘要截断则原session补全，不新开task。
