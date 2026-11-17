# 领域绑定规则：最终 compileJava 报告

结论：`compileJava` 已实际执行并通过。恢复后的 Gradle 退出码是 0，外层 Shell 退出码也是 0。该任务不是 UP-TO-DATE。编译期间 `src/main/java` 无变化。编译槽已释放。游戏、Mixin 运行时和测试都没有做。

本单没有改源码，没有改制作表，没有重跑命名门，没有做 `build`、`jar`、`reobf`、`test` 或 `runClient`。没有新增正确性审查轮次。

## 派发状态

`记录.md` 写明相关源码作者已经收束，`review-decision-2.md` 允许对收束后的源码做统一 `compileJava`。该裁决明确这不等于编译或游戏验证通过。候选查询 A/B 与统计说明的证据均为 `released=true`，本次没有替该任务追加审查。

## 占用

预检时间 `2026-10-30T13:46:50.6654690+08:00`。

- 活跃 `GradleWrapperMain`：0。没有正在进行的构建。
- 既有 Java daemon：PID 53016，父进程 72260，创建于 2026-10-30 13:09:22，命令行是 Gradle 8.8 daemon，使用本机 `C:\Users\baishang\.gradle`，堆参数 `-Xmx3G`。这是后台 daemon，不是当前构建。
- 预检前 5 分钟内 `src/main/java` 没有文件被改写。
- 编译结束后再次清点：`GradleWrapperMain` 仍为 0。PID 53016 仍在，属于编译前就存在的 daemon。本次 `--no-daemon` 的一次性 daemon 已结束。编译槽已释放。

## 环境

项目 `gradle.properties` 保持 `org.gradle.jvmargs=-Xmx3G` 和 `org.gradle.daemon=false`，本单未改。

当前进程继承到的环境是：

- `JAVA_HOME` 为空。
- `GRADLE_USER_HOME=C:\Users\baishang\AppData\Local\Temp\cursor-sandbox-cache\da9014e6892998c9b90f6055eb2faa2f\gradle`。
- PATH 上的 `java.exe` 是 `C:\Program Files\Common Files\Oracle\Java\javapath\java.exe`。
- 该沙箱目录没有 `wrapper\dists\gradle-8.8-bin`，没有 `caches`，也没有 ForgeGradle 缓存。

本机可用位置存在：`JAVA_HOME=C:\Program Files\Java\jdk-17`，`GRADLE_USER_HOME=C:\Users\baishang\.gradle`。后者有 Gradle 8.8 发行包、caches 和 `net.minecraftforge.gradle` 缓存。

## 两次命令

工作目录都是 `D:\alianximod`。唯一命令是 `.\gradlew.bat compileJava --offline --no-daemon`。Shell 没有被审批拒绝。

### 第 1 次：未启动

时间 `2026-10-30T13:49:55.4394675+08:00`。没有 Gradle 退出码。

原因：继承的 `GRADLE_USER_HOME` 是空沙箱，里面没有已有的 Gradle 8.8 发行包。此时启动 wrapper 会去下载发行包，本单禁止网络拉取，所以没有启动。这是记录下来的首轮失败，不是编译错误。

### 第 2 次：通过

同一进程内改用已存在的本机环境后执行同一命令：

- `JAVA_HOME=C:\Program Files\Java\jdk-17`
- `GRADLE_USER_HOME=C:\Users\baishang\.gradle`
- 开始 `2026-10-30T13:49:55.4584153+08:00`
- 结束 `2026-10-30T13:50:23.5243287+08:00`
- Gradle 进程退出码：0
- 外层 Shell 退出码：0
- Gradle 自身报告 `BUILD SUCCESSFUL in 25s`

配置输出：`Java: 17.0.6, JVM: 17.0.6+9-LTS-190 (Oracle Corporation), Arch: amd64`。

任务结果：

- `:downloadMcpConfig` 执行
- `:extractSrg` UP-TO-DATE
- `:createMcpToSrg` UP-TO-DATE
- `:compileJava` 执行，不是 UP-TO-DATE

`4 actionable tasks: 2 executed, 2 up-to-date`。执行的两个任务是 `downloadMcpConfig` 和 `compileJava`。命令带 `--offline`，构建成功，输出里没有离线缺包或下载失败。没有另发网络命令，也没有改项目配置。

完整 stdout：

```
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.8/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

> Configure project :
Java: 17.0.6, JVM: 17.0.6+9-LTS-190 (Oracle Corporation), Arch: amd64

> Task :downloadMcpConfig
> Task :extractSrg UP-TO-DATE
> Task :createMcpToSrg UP-TO-DATE

> Task :compileJava

BUILD SUCCESSFUL in 25s
4 actionable tasks: 2 executed, 2 up-to-date
```

stderr 全文在 `compile-evidence/attempt-2-stderr.log`。Mixin 注解处理器 0.8.5 加载了 searge 映射并写出 refmap。随后是 34 个弃用警告，没有错误。警告集中在既有的 `ResourceLocation` 构造器，以及 `HitVolumes.java` 的 `ProjectileImpactEvent.setCanceled`。输出没有指向候选查询新增服务或 mixin，也没有指向本次领域绑定接口。

## 源码身份

快照范围是 `src/main/java` 下的全部文件。编译前 `2026-10-30T13:49:55.1611716+08:00`，编译后 `2026-10-30T13:50:23.7500534+08:00`。

- 前：687 个文件，其中 Java 687 个。
- 后：687 个文件，其中 Java 687 个。
- 前后树 SHA-256 都是 `25e6db62231e5a4c2582b9ceb0fe47327aeadac815411156f198d9c6d818a458`。
- 树散列是按路径排序的 `文件SHA256<TAB>相对路径` 行，用换行拼接后再做 SHA-256。
- `source-diff.txt` 为 `UNCHANGED`。

逐文件前后清单在 `compile-evidence/pre-sources.txt` 和 `compile-evidence/post-sources.txt`。这两个文本少了一行 `tree_sha256`，因为脚本对固定长度集合调用 `Insert` 失败；散列本身来自同一批文件行，并已记在本报告和 `result.json`。

本任务各 evidence 列出的 27 个源码路径全部存在，没有缺失。编译前散列如下。

| 路径 | 字节 | SHA-256 |
| --- | ---: | --- |
| `src/main/java/com/bslx/XueXiMod/network/AutomaticRound.java` | 22205 | `8345ef7abac42c3a0ba308ccd212b5b50d50194344789308c5acd2b5484c51e4` |
| `src/main/java/com/bslx/XueXiMod/network/SkillSegmentRunner.java` | 55483 | `1069151b0e9b82980cb032bccc28e43d476b314d2fdb9a43b89dc58c5eb2c9de` |
| `src/main/java/com/bslx/XueXiMod/network/GroupBudget.java` | 1472 | `d99a4b7c9fc505c17ffec95cf0f52fdcc4919543597bfe5d579d001d7e08e0f6` |
| `src/main/java/com/bslx/XueXiMod/shu/ModTechniques.java` | 18220 | `0bd58f7c5bb905f6f8f1f55165d824c31f2f1f96ac5c3c573de983809ec2d6df` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseDefinitions.java` | 18541 | `8d6b41f016a166dbd33c53228fcfc3a25f4e3f7efacde01407433826b4a54c00` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseBatch.java` | 13695 | `16d4d0ecdd7412119d42a2796f2cc79ac3fe58d1a877dfdd2a484f40c4bed2ee` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/CastFrame.java` | 14149 | `631272eeefd7432996fe8d515a0d30bc731cad14b386d77e062498b933381ba9` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/ChainRole.java` | 355 | `5d7b9d8b5e9456d468a6e81530062f65dc9f5b9ce9b0511e0278cfd6dcee0ba6` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/aim/LaunchAim.java` | 6179 | `130e0df323dcca4092d52a40bd07309d3ffc40951050b3b4fb707cddbb08a28c` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/bind/ReleaseExecutionContext.java` | 5271 | `b3d7f4fe819922c7c53a93ec8e8f4c82b46d117f61d1f027d7065422952d3ce5` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitSpec.java` | 27487 | `befc8746ab3bdb822b96989d4b0aada3aba1969372bb0b5167307ff275a0c4ce` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/entity/TechniqueHitEntity.java` | 41339 | `e107255cedfcbafd6d040bf47941c2ccdb271a4f053900c4e61e6d2d34989070` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/motion/ProjectileMotion.java` | 1492 | `0581d12db146f568d7644357e53c1dab16e28aed46c392e27d7ab224fd31cc43` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/motion/FollowCasterMotion.java` | 3132 | `a7e0a8972f2808c81c2dd3f032538d60a93d36a81e4d37309568598d25b9b97c` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/motion/TrackingSupport.java` | 4021 | `9d18674882412496db5c0567e5e4f5a893d1d5602e3a5d543b76d653f169828d` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainGeometry.java` | 1034 | `1d3e5abe09df92b30063b3515e5310346118c55bb70301e43e1e305b1e84b285` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/AxisBoxGeometry.java` | 7452 | `e138a5d95e8cc3b6291a06bdc0b31d2e97938c2ba2dd783a4b9a5cfd61e61540` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainBindingRuntime.java` | 13956 | `1883cb567ee31bda502045afc019f7e67a95f19ea8ef7e2acf7cd2254db0b2cc` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainInstance.java` | 8000 | `dbe5befda11bc09a1a45914a05a734df542943525062964aa334a13c1d385667` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainService.java` | 11121 | `1306c4e1977bf18c86ec428f1300112b327e77ba01b26d235d62eb763bffe323` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/effects/PhantomSwordEffect.java` | 5492 | `39513c3a2246a7ea1d043186621fe7b076df99d9611627deb6772036f5cbc8d2` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/effects/SwordReturnAllEffect.java` | 1736 | `09b7c42c3327c8954a3a4caf6d7a086cb36bbc08a6e20db38c87651e038ebb9f` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/effects/WeepingCurtainEffect.java` | 4207 | `af5eff58741f7800d2a2b0c157ce9f7dab520a91d05ff7d9cd39a7bd25cd7428` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/effects/LiaoliEffect.java` | 9350 | `a217e1e25261890d295f6e4aa3d62b8b63ee386501d1087aab675065eada8896` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/effects/SpaceCutEffect.java` | 4114 | `aa694d1a200836a1c19a766664819674fb34b9a2218815e8a886eab40bd1a7e0` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/sword/SwordEntity.java` | 26485 | `2cbf193e9591d1ef10c6017114a3ca1769441e3ad443397b90dc77c35f2fd1ee` |
| `src/main/java/com/bslx/XueXiMod/shu/technique/sword/SwordVortex.java` | 11352 | `a724c55b28f211e861451846b850e1654f4524857f81706802a0210a4bb04f70` |

## 未验证

- 没有启动游戏，没有验证领域绑定的释放、取消、退款或复制布局。
- Mixin 只在编译期由注解处理器写出 refmap。没有验证 Mixin 运行时注入，包括候选查询的两个 LivingCandidate mixin。
- 没有独立测试。命名门沿用此前作者结果，本单未重跑；已知旧违规仍是未改的 `OriginalHumanoid` 两处，本单没有处理。

## 证据

- `compile-evidence/preflight.txt`
- `compile-evidence/pre-sources.txt`
- `compile-evidence/post-sources.txt`
- `compile-evidence/source-diff.txt`
- `compile-evidence/evidence-paths.txt`
- `compile-evidence/attempt-1-meta.txt`
- `compile-evidence/attempt-2-stdout.log`
- `compile-evidence/attempt-2-stderr.log`
- `compile-evidence/attempt-2-combined.log`
- `compile-evidence/attempt-2-meta.txt`
- `compile-evidence/result.json`

`compile-evidence/facts.json` 的内容是 `null`。脚本在组装对象时出错，没有写成有效事实。结构化结果以 `result.json` 为准。该记账失败不改变 Gradle 退出码。
