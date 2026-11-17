# 聚能输入链核查

用户要求：修复聚能对雾雨、水环等术式的范围、特效和持续时间传递；剑术不能获得聚能。Cursor 修改并编译，主控审查，不打包，游戏由用户测试。

当前只支持元素法术消费聚能。无属性法术不支持。这是按较窄原需求收口，不是用户从选项里点过一项。剑术排除已确定。

已落地：公共 `ReleaseInput` 与到期辅助；释放计划在建立时按成员冻结链输入，并沿计划、会话和延迟入口显式传递。剑技槽拒绝新装聚能，服务端对剑技和剑术成员冻结为基准倍率。费用、单次伤害、冷却、速度、索敌距离、维护不乘这份额外倍率。

已验证：释放边界改完后 `compileJava --offline --no-daemon` 退出码 0，日志 `compileJava-boundary.log`。路由定向检查 68 条，失败 0，日志 `check-route-boundary.log`。实体、水牢和 `ReleaseInput` 契约的生产代码这次没改，没有重跑。容器和墙宿主仍因缺少 ModLauncher 跑不完，没有再试 Bootstrap。真实药水和游戏内都没跑。

工作区最终编译：菜单两处修改已在源码里，`staff == false` 时 `SpellAssemblyMenu.rejectReason` 直接拒绝法术，`writeGear` 直接跳过。`compileJava --offline --no-daemon` 真实退出码 0，日志 `compileJava-workspace-final.log`，退出码文件 `compileJava-workspace-final.exit.txt`。没有打包，没有进游戏。先前已执行的逻辑断言合计 `ReleaseInput` 77、实体 56、水牢 56、路由 68，共 257，失败 0。这轮没有重跑它们。容器和墙宿主检查仍未跑完，实际世界回调没有运行，不能当成全套测试通过。

未完成：容器和墙宿主的完整执行、游戏内复核。没有打包。基准倍率不要求对象单例。

调查范围只涉及公共释放输入与各载体消费者；不得按 water_mist、water_ring 名称单独乘倍率，不改变费用、伤害、冷却、索敌，不改已批准美术。
