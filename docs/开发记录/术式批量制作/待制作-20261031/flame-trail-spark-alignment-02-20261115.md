# 留焰锁定发射坐标系补齐回执

- batchId：`待制作-20261031`
- taskId：`flame_trail_spark_alignment`
- attemptId：`20261115-impl-alignment-02`
- 直接父代理：当前主会话
- 阶段：定向补齐。设计 2/2、实现 3/3 已耗尽，本回执不是正式复审，也不再派作者。
- 原作者交接：`01a1db6e-43ac-7c22-8a84-56c2a5d6c4ac` 已 final 并 close，写入已收束。其回执为 `docs/开发记录/术式批量制作/待制作-20261031/flame-trail-spark-alignment-20261115.md`。
- 写入收束：是。只改下列三个 Java 文件和本回执。

## 原交接遗留

原作者已给火苗留焰加 `SparkMeshAnchor`，并让火苗与留焰共用 `translateSparkMeshAnchor`，偏移仍是已批准的 `0.245`。但传入的 `upAxis` 不是同一套方向：火苗取生成时锁定的发射方向，留焰取每段当前运动切线。喷涌、追踪等改变方向后，留焰偏移会跟着当前切线偏走。

## 本次补齐

火苗跟踪器在挂接时读取宿主 `TechniqueHitEntity.launchDirection()`。该方法返回生成当刻的单位发射方向，不推进运动、碰撞或伤害。该方向写入 `SparkLaunchX/Y/Z`，每个留焰采样同步同一组值。

采样位置和 `VisualTangentX/Y/Z` 仍由当前段 `segmentEnd - segmentStart` 决定，所以火径形状继续跟随真实运动。渲染留焰时，当前切线只用于面片朝向和沿火径展开；只有 `SparkMeshAnchor` 为真时，才用锁定发射方向重建与火苗完全相同的上轴，再沿该上轴反向平移 `0.245`。火球、陨岩等其它宿主不写标记，也不写这组方向，采样和碰撞坐标不变。

## 偏移方向

偏移向量固定为 `-sparkAnchorUp(lockedLaunch) * 0.245`。`sparkAnchorUp` 与火苗原公式相同：发射方向未接近竖直时取世界 `+Y` 在垂直于发射方向的平面上的投影，接近竖直时改取世界 `+X` 的投影。

- 直射：锁定发射方向为水平 `+Z (0,0,1)` 时，上轴为 `(0,1,0)`，偏移为 `(0,-0.245,0)`。当前运动切线即使后来改向，偏移仍是这个向量。
- 斜射：锁定发射方向为 `normalize(0,1,1)` 时，上轴为 `normalize(0,0.7071067811865476,-0.7071067811865475)`，偏移为 `(0,-0.1732411865052174,0.1732411865052174)`。
- 变向：喷涌或追踪只改变后续采样的位置和当前切线。跟踪器上的锁定方向在挂接时写一次，采样只复制它，所以偏移方向不变；火径折线仍由各段当前位置决定。

## 修改

- `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java`：增加 `SparkLaunchX/Y/Z` 同步与存档，采样生成时复制锁定方向。SHA256 `8330A16E9A6796FE074584B61EB4F7F1E4A12CEB5DDCD80336597ACD6FCA8220`。
- `src/main/java/com/bslx/XueXiMod/carrier/BatchFlameTrail.java`：仅火苗宿主调用 `assignSparkLaunch(record.body.launchDirection())`，采样继续传当前切线。SHA256 `6ADC888B59782CF03DDDD8040FBB4A26FD7ACD33B3E5C3379B3409A634DBAA52`。
- `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java`：抽出与火苗相同的 `sparkAnchorUp`；火苗用锁定切线，留焰偏移用锁定发射方向，面片仍用当前切线。移除上一轮 `translateSparkMeshAnchor` 的 Javadoc，本轮未新增注释。SHA256 `692911AE7727A2A4BED3001DB30F03400F7344B532B7997A81A173A1C8A5E798`。

## 检查

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1，全仓原有 274 处命名问题。过滤本次三个文件没有命中，本次新增局部名未报错。未改全仓旧名。

## 未验证

本任务没有编译授权，未运行 `compileJava`，未启动游戏，未写制作表，未改共享记录。画面是否贴合仍需统一编译后由用户实测。
