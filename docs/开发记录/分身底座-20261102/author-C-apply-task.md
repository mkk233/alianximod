模型：Cursor CLI --model grok-4.7-high-fast。阶段：候选接入与整合，非正式复审，无编译授权。只在主会话派发后执行；届时A/B/C都已最终交回并停止候选写入，另一聊天当前修复/编译窗口已释放。你仍是唯一整合者 C。设计1/2已定，实现正式复核仍0/3。

完整读设计、冻结合同、记录.md、A/B/C最终报告和evidence/manifest。保留所有原有未提交修改，不reset/clean/checkout、不提交、不改表、不测试、不打包、不运行Gradle、不启动游戏。其他作者不在写候选；不阅读其他聊天实时工作。

本单授权：
- 把已收束的A core、B action/CloneBalance候选按原字节复制到其正式src/main/java目标。你仅作为机械接入者，不改A/B逻辑；目标若已有未知内容，不能覆盖，精确报告。
- 按implementation-contract第2节C归属文件，将自己的候选增量接入正式src/main及对应语言文件；同一共享文件只由你写。你可以修正自己范围内的机械整合/签名引用/名称问题，但不得越过A/B所有权或改已定语义。
- 本批before-apply/保留现行真实目标字节；author-C-apply-report.md、author-C-apply-evidence.json、final-source-manifest.json、naming-gate.log。
- 未列共享文件不得扩修。真实source路径映射以冻结合同的校正为准，不照旧设计中不适用路径新增文件。

安全应用顺序：
1. 确认本批作者均已返回最终报告，正式src/main未有本批之外新增写入/Gradle正在执行；若有新冲突只报状态，不继续覆盖。
2. 读取integration-apply-manifest与before-C，对所有目标比对现行sha。若修改目标sha相同可按候选应用；若已变化，保留当前新版本，只定向合并本批相对before-C的增量，不整文件覆盖，不还原其他聊天修复。新文件目标必须不存在或内容与本候选完全一致。
3. 先保存before-apply真实当前字节及所有目标sha/mtime，再应用。A/B新候选按作者evidence hash核对，并只复制已宣布完成的文件。
4. 全部接入后仅做必要静态整合检查，不能调用编译器。核对冻结公开接口的引用、注册器和配置入口/默认值、命令/能力过滤挂接、专服客户端隔离。发现A/B接口/实现问题列清精确文件/符号给对应作者，不直接改他们文件。普通实现自检不是额外正式复审。
5. 按docs/统一开发与术式批量制作流程.md第6节执行现有命名门禁。如合同签名与门禁冲突，现有override不擅改接口，交回精确问题；只修你自己的新增命名，A/B问题派原作者。保留无关历史问题，不扩修OriginalHumanoid等。不得通过禁用门禁假装通过。
6. 交回本次真实接入文件、前后sha/mtime、命名结果、所有未决接口与缺漏、停止写入声明和“待统一编译”。当前不能编译。报告以真实source为准，不以候选替代。

本单不修改设计/合同或重审旧批次，不把未游戏实测写成通过。完成后你释放写入，供集中只读复核；后续编译必须另行明确派发。

## 三方最终报告里的机械交接

A最终报告明确提供 CloneService.handleFor(ServerPlayer, UUID)，命令不能自编session/代次。请在C-owned CloneCommands中先用这个正式方法取本人真实handle；找不到明确拒绝，再按实际handle调用request。不要保留你候选报告的session0/代次1占位约定，也不能放宽A的generation/session校验。此为A交付的机械接口补充，不变更设计。

B报告未有CloneEntity代次访问口、prepare只检查帧generation>0；A报告真实handle及服务重验。请在接入报告中准确交回这一衔接现状供集中复核；不要越权改A/B。B取消原因码BUSY附可读文字也作为现状记录，不自行扩改枚举。A提到没有PreparedAction射程getter，范围由B同一次prepare判断，按已冻结分工接入。
