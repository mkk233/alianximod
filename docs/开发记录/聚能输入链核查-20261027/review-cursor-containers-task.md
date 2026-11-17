独立Grok4.7只读审查当前容器聚能改动，不修改代码、不运行游戏、不调用子代理/brain。架构已由GPT-6-astra审定，现请你审查实际实现；主控在并行处理实体和公共路由，不要读整仓。

先读本任务目录 shared-contract.md、containers-report.md。本次差异基线在 before/对应相对路径，不要拿git HEAD的旧改动当本次内容。只审查：
release/domain/AxisBoxGeometry、DomainInstance、DomainService；
release/guard/GuardDefinition、GuardStates；
release/barrier/BarrierDefinition、BarrierService、BarrierBody；
release/field/FieldSpawner、FieldZone、FieldZones；
WallDeflectionHost、DomainPressureRenderer的新增到期边界；
src/test/java/com/bslx/XueXiMod/shu/technique/release/ReleaseContainerEnergyCheck.java。
以上生产根为src/main/java/com/bslx/XueXiMod/shu/technique/，客户端文件按rg精确定位。

重点：倍率BASE2/4在实例冻结仅乘一次、定义不污染、领域从落地点底面锚点放大、屏障放大后仍安全预检、水环普通→聚能→普通刷新真实参数替换且重绑偏转，动画与维护节拍不变，服务端碰撞与同步视觉用同一个尺寸寿命，场地子范围不重复乘，有限正寿命int回绕所有消费端一致，long结束饱和，非正特殊寿命兼容；费用/单次伤害/维护周期/卸力系数/放置前向距离不能乘。定向检查是否确实测生产分支，没执行不可称通过。

公共路由的显式input接线正在另组实施，旧调用BASE此时不算本组缺陷。同tick去重key不含倍率暂不改，除非有两次独立操作身份同tick实际被吞的确证；不能简单把倍率塞key破坏幂等。无属性资格待用户，不属你本次裁决。
最终输出PASS或者可复现有依据的缺陷，每项提供严重性、路径行号、触发条件、建议。不要输出猜测问题，不运行Gradle、不打包、不写报告文件，最终文字由主控归档。