# 同步已有燎里布局检查的过时方向预期

固定 Cursor CLI `--model grok-4.7-high-fast`，原作者1f4155a4-e537-4518-a3f7-893f09c5daff。你刚交回的 `liaoli-pose-implement-report.md` 明确报告现存 `LiaoliV6LayoutCheck` 仍断言相反方向。这次只同步该已有检查与用户已确认的方向，不新增测试框架、不运行独立测试、不做额外复审。

本单新增且唯一可写源码路径：`src/test/java/com/bslx/XueXiMod/visual/liaoli/LiaoliV6LayoutCheck.java`。只把与本次姿态变更直接冲突的旧“柄更靠后、尖更靠前、火焰旧方向”预期改为已确认的站立剑尖朝下/柄近尖远/左右镜像和附着新剑方向的预期，保留其余未受影响的有效约束。依据批准模型与既有几何语义，不把所有失败条件简单删除，也不机械照抄实现计算。不要扩大为新一轮完整审查。

只读需要的两份已完成生产代码与已确认归档。不得再改生产文件、资源、制作表、记录.md、索引。不要启动Gradle、游戏或本检查，不新增测试文件。名称门禁可按流程执行，并说明本次路径是否新增问题；既有OriginalHumanoid的问题不改。

结果写 `liaoli-pose-contract-sync-report.md` 和 `liaoli-pose-contract-sync-evidence.json`，列哪些既有预期被同步，明确没运行该检查，不写复审通过。交回该文件最终SHA256/LastWriteTime、确认两生产文件仍为上次报告哈希，并再次明确所有本任务写入已经停止。阶段额度设计2/2、实现3/3不变；这是作者已报告问题的定向收尾，不新增测试阶段或正式复审。
