# 作者A：修复唯一追加轮的R1/R2

固定Cursor CLI --model grok-4.7-high-fast，继续原A Agent157c03a0-8753-46c8-bfd4-42136cdeb189。读extra-review-final.md，这是已完成的用户唯一追加轮（累计实现4轮），不得再开复审/代理/设计裁决。只实现报告前两项具体修复，不扩查。

R1：ComputeSelection路径解析捕获InvalidPathException，提供明确配置回退原因，并让一次解析正常收束到Java/resolved，后续不重复异常。不扩大为吞掉所有运行异常或Error，不改变计算业务异常传播。

R2：保留原TrajectorySampler的VORTEX停驻分支可返回null位置的行为：VORTEX、origin=null、forward=(0,0,1)、lifetime=12、age=8时Sample.position应为null、direction应为(0,0,1)。旋转未结束时origin=null仍保留原异常；不要把null位置变成原点。两后端的现有活调用都应沿同一适配语义；不改合法输入公式/阈值、Java整数溢出/除法等。优先在现有live façade/适配处理这个原本允许的对象空值，不为此静默修改已冻结C数字ABI或制造Java/C不一致。

独占可写shu/compute/ComputeSelection.java、ComputeCalls.java、必要ComputeLiveDispatch/ComputeOutput，以及TrajectorySampler.java的最小适配；docs/纯计算后端使用说明.md、abi-contract.md中相应语义/状态说明；本目录repair-review-A-final.md、evidence-A-review-fix.json、backup-A-review-fix/。如果必须改JavaReference或C协议才能保留语义，先最终返回具体必要改动及未完成项，不能碰作者B独占C文件。B同时只修Windows构建脚本，不改公式。

不改其它网络/释放/领域/性能枚举/Gradle/制作表。不编译、运行测试、加载DLL、游戏或打包。报告里明确此为按追加轮结论修复，修后未再审，待最终编译。对已知原始输入路径可作实现自查，但不得另给一次正式审查结论。

保留修改前副本和实际文件元数据，不补做之前被拒绝的hash。审批拒绝记录原始原因，不取消auto-review/加force/换模型或工具绕过。最终交回具体R1/R2修改与native ABI/C源是否完全不变、文档状态、未完成项，released=true。
