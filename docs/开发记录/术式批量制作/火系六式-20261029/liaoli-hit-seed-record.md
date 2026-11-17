# 燎里命中火环确认与所有权

只补记录。制作表未改，`fire_liaoli` 仍在制作中第 6 行，不迁待验收。最终编译后另单写回。

用户明确“所有带火种实体常驻显示（建议）”，并确认“确认，使用这版火苗”。正式目录 `docs/资源库/特效/火焰/火种常驻火苗_v1/`，图集 `fire_seed_flame.png`。PNG 为 32×256，8 帧各 32×32，每帧 100ms，整轮 0.8 秒。SHA256 `106DD7C62A1E360C859FB598B320EBCF10A40568555C763465023380E5DE800B`。确认的是二维视觉，不是游戏实测通过。美术图像已停写，接入等源码作者完成。

每次合格命中触发现有周期的三道后扩环，不因空挥自动续播，保留现有贴图与方向。S>50 是命中和扣种前的实际共享余额。高速跟随修复按源码作者最终报告归档，目前只是派发。定位报告不是实现完成，也不是审核通过。

源码和运行时资源唯一作者：`1f4155a4-e537-4518-a3f7-893f09c5daff`。工作簿和记录.md：`d6f84d0b-4ea1-4368-a576-8acc20cc2afd`。资源归档和索引：主会话。统一编译者 `b2d5b359-42e3-4a90-9d24-b26acec6d5f8` 尚未派发，相关源码写入未结束前不得编译。

源码可写范围以 `src/main/java/com/bslx/XueXiMod/` 为根：`client/LiaoliV6Renderer.java`、`visual/liaoli/LiaoliV6Layout.java`、`shu/technique/effects/LiaoliEffect.java`、`shu/technique/release/entity/TechniqueHitEntity.java`。`ClientTrack.java` 与 `release/motion/FollowCasterMotion.java` 只在前述范围不够时最小修改。`status/fire/seed/FireSeedState.java` 与 `status/AttributeStateData.java` 只做同步接线。另有 `client/FireVisualClient.java`、`network/FireVisualPacket.java`、`network/ModNetwork.java`，以及必要的新同步、渲染或命中火环类。运行资源只在 `src/main/resources/assets/xueximod/` 新增火苗。语言文案如需修改，只限燎里和火种说明。该作者还写 `liaoli-hit-seed-implement-report.md`、`liaoli-hit-seed-implement-evidence.json` 和必要检查日志。

设计 2/2、实现 3/3 不变。本阶段额度已用完，返修结果未追加复审。

本单停止写入。未改制作表，未读实现中的源码或流式日志，未改 src，未跑 Gradle，未改资源库，未操作游戏，未提交。
