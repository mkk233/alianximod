# 留焰与火苗弹体对齐返修回执

- batchId：`待制作-20261031`
- taskId：`flame_trail_spark_alignment`
- attemptId：`20261115-impl-alignment-01`
- 直接父代理：当前主会话
- 阶段：定向返修。设计 2/2、实现 3/3 已耗尽，本回执不是正式复审。
- 写入收束：是。只改下列三个 Java 文件和本回执。

## 根因

火苗 `fire_spark_v3` 的服务端实体仍在弹道坐标，客户端 `drawSparkCross` 按已批准交接把面片几何中心沿发射上方向下移 `SPARK_FIRST_PERSON_DROP = 0.245` 格。该值对应 `火苗_十字穿插直射_v3.1` 中眼高 `25.92` 与弹体中心 `22.0` 的 `3.92` 模型单位。

留焰采样由 `BatchFlameTrail.placeSamples` 直接取 `TechniqueHitEntity.position()`，再由 `drawTrailRibbon` 以实体坐标为面片中心。碰撞扫掠仍用同一宿主坐标。因此火径和伤害没有偏，只有留焰画面停在火苗可见十字上方 `0.245` 格。水平飞行时这个差值正好是世界 `+Y`。贴图质心和碰撞箱中心都不是这次的偏移来源。

## 修复

火苗宿主的隐藏留焰跟踪器设置 `SparkMeshAnchor`，生成的每个采样点同步该标记。渲染留焰时只在该标记为真时调用与火苗完全相同的 `translateSparkMeshAnchor`。火球、陨岩等其它宿主不带标记，采样和碰撞坐标不变。

## 修改

- `src/main/java/com/bslx/XueXiMod/carrier/BatchCarrierVisual.java`：52–53、138、158、276–282、380、539–541、565。SHA256 `856BDD041C56A326267647BF91BD2E6EF9C5AF8DE9FCB7BFCC8489ED662F49D6`。
- `src/main/java/com/bslx/XueXiMod/carrier/BatchFlameTrail.java`：48–50、194。SHA256 `5E55ED27458AB7B673C0141005FD16EAD52E7602752E25281DED015085140510`。
- `src/main/java/com/bslx/XueXiMod/client/BatchCarrierVisualRenderer.java`：177、193–195、295、308–317。SHA256 `CD573DD4C0D4B03B1B59A00B1FA471C8DDBCEF5A3FC849207E6F252B38D2AB0C`。

## 检查

`powershell -NoProfile -File tools/check-name-gate.ps1` 退出码 1，全仓原有 274 处命名问题。过滤本次三个文件没有命中，本次新增局部名未报错。未改全仓旧名。

## 未验证

本任务没有编译授权，未运行 `compileJava`，未启动游戏。画面是否贴合仍需统一编译后由用户实测。
