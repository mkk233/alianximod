# 幽冥摇曳确认版编译回执

2026-11-16。batchId `待制作-20261031`；taskId `fire_nether_gloom_v2_domain_preview`；attemptId `20261116-compile-gloom-sway-01`。父方 `01a1ce06-b681-7c90-b11d-260d3c169911`。

本阶段额度已用完，返修结果未追加复审。设计 2/2、实现 3/3 不变。本次只是编译，不是复审。无子代理。

## 已跑

- 命令：`D:/alianximod` 下 `.\gradlew.bat compileJava --offline --no-daemon`。只跑这一次。
- 开始 `2026-11-16 21:15:12.347 +08:00`，结束 `2026-11-16 21:15:38.292 +08:00`。Gradle 写 `BUILD FAILED in 25s`。
- 退出码：`1`。
- Java `17.0.6`，Gradle `8.8`。`--no-daemon` 仍会临时 fork 一次单次 Daemon，日志写明结束时停掉。没有第二次编译。
- 没有 `build`、`jar`、`reobf`、`runClient`，没有打包，没有启动游戏，没有游戏内验证。

日志：`D:/alianximod/docs/开发记录/术式批量制作/待制作-20261031/gloom-v2-sway-compile-20261116.log`。248583 字节，SHA256 `B2615A62DB5563BD01F648133432C3381E047587395CA9A4687263853F24A6A2`。里面含编译前 `src/main` 清单、Gradle 输出和编译后树摘要。

## 失败原因

编译器总结：2 个错误，58 个警告。两个错误都不在本项文件：

- `src/main/java/com/bslx/XueXiMod/status/wood/vine/RoseVineProjectile.java:73`：找不到 `VineMarkService`。该行是 `VineMarkService.upgradeFromProjectile(owner, target);`。
- 同文件第 109 行：`instanceof ServerPlayer player` 后的模式变量不能用在同一条 `||` 右侧。

这个文件编译前就是 4225 字节，SHA256 `6C7E89A3A9F4EFB637EF1E3F047A251193E6BAA9C46EE9D80FB451F4CB008A7C`。本次没改它，也没有因为这两个错误再编译。

58 个警告是既有的 `ResourceLocation` 等过时 API，其中包括 `NetherDomainWisps.java:34` 的旧构造器。警告不是失败原因，没有改。本项新类 `NetherDomainAtmosphere`、`ClientLevelGloomSkyMixin` 没有编译错误。

## 指纹

编译前 6 个运行文件与接入回执一致，编译后仍一致：

- `NetherDomainWisps.java`：15383，`9DF4B74F3440B42254A6534CE388592EEFA5BF77148EBB6D5B6C0A65B57F0E54`
- `NetherDomainAtmosphere.java`：5910，`910A91A7C80CAEB7E93773A3554EB5FA0F4FD21B813138FC25BBAC368C4FBA6B`
- `ClientLevelGloomSkyMixin.java`：883，`3B32271479CAE7FCAD779DA119B0C2B5FC37C982619D0DC4071641BEFA779B94`
- `xueximod.mixins.json`：2578，`11906A0CFB81886DE4560FACECA0654AD482692A7FDE127F2C585EDF276D8BDD`
- `nether_gloom.png`：5310，`7699297D207AC3CCACEC25E9CF7226C5B3765C0E16F620F3E1D717C1E23AD0FB`
- `nether_gloom.png.mcmeta`：61，`A2313FCC609089214681B434F5959499F068679AA7DFEBDF5B3F0E7551ACC2F9`

`src/main` 编译前后都是 1629 个文件，树 SHA256 `7CF4B725A17297991EF933F94DEF352B9329F59E4B6B5E326FCADC82D78CC177`，逐文件对比变更数为 0。源码没有在编译期间被改。这次失败不能当成覆盖最终源码的成功编译。

## 并发

开编前只看到当时自己的快照命令行里带着 `gradlew` 这个字，没有另一个工作区 Java/Gradle 进程。父方说明聊天 `01a1e39f-f155-7e73-b171-59bd929e26f8` 仍可能在写；本回执不把工作区说成无人写。没有改共享类，也没有覆盖 `xueximod.mixins.json`。

## 收束

只写本回执和上面的日志。没有写制作表、批次记录、资源索引或美术源。没有编译期修复。写入已收束。周期 5 与批准 20 仍未决，本项仍留制作中。
