# 用户授权的最终模组JAR打包

固定Cursor CLI --model grok-4.7-high-fast，沿用指定编译者b2d5b359-42e3-4a90-9d24-b26acec6d5f8。用户已明确“这完成后打个包我看看”，见liaoli-tip-ring-burst-package-authorization.md，当前用户授权覆盖默认不打包。此单只在主会话实际调用时生效，调用前须已收到表格写回最终反馈。

目标：生成能放入Forge1.20.1 mods目录的生产模组JAR（按本仓库ForgeGradle现有重混淆流程），交出本次独立命名的真实最终JAR。

先读取现有build.gradle/gradle.properties和mods.toml，确认产物命名/版本与jar/reobfJar配置。不要改配置、版本、源码、纹理或环境。最新源码由23ef4660-da4b-47a8-9341-8ec236ec5cc3停写，最新compileJava03:16:05至03:16:56exit0覆盖777文件，证据liaoli-tip-ring-burst-compile-report.md/results.json以及src-main-before/after.txt。表格记录者写回已完成后不占src。实际状态和最后表迁移见liaoli-tip-ring-burst-writeback-report.md/results.json。

打包前复核与上一编译src/main冻结清单一致、无人新写src、没有其他活跃Gradle。只用当前仓库Gradle已有生产jar与reobfJar任务组合（或仓库既有专用生产打包任务），默认--offline --no-daemon，沿用JDK17/-Xmx3G，确保获得最终重混淆产物；不能只把开发classes压成zip。读取配置后确定准确命令，不需要为了生成JAR强制触发test/check阶段。旧检查保持未运行，不额外新增测试。若必须的打包步骤失败则如实交回，不拿旧JAR冒充成功，不边编边改源码，也不擅自换模型或降档。

打包前后采样src/main完整hash，与777文件基线核对，报告最终字节覆盖。核对实际JAR中的META-INF/mods.toml/modId、Forge目标、manifest、必要资源、LiaoliV6Layout/LiaoliV6Renderer/LiaoliEffect/FireConsumerPlans、三新类LiaoliRingTrail/LiaoliRingPacket/LiaoliRingBridge，以及新火种火苗PNG/原rear_ring等。对PNG核对归档hash；重混淆class字节不同于开发classes是正常映射过程，按真实生产任务完成状态和ZIP条目作包核对，不扩成源码复审或游戏验证。

保存实际最终产物到build/libs既有路径，并另复制到本批交付包/目录，使用唯一ASCII名称如xueximod-liaoli-tip-ring-burst-20261030-HHmmss.jar，禁止覆盖既有同名交付包。交出确切绝对路径、字节、SHA256、时间以及Gradle命令和exit/任务执行情况。无需复制到游戏目录。

允许写：Gradle默认build/.gradle产物、本批交付包/、liaoli-tip-ring-burst-package-report.md、-package-results.json、-package-gradle.log和打包前后src-main hash元数据。禁止改工作簿/记录.md（由唯一写表者最后补记录）、src/开发资源、提交/上传/签名/发布、runClient/游戏或install。不要杀掉其他历史空闲daemon。

设计2/2实现3/3不重置，不追加复审。本阶段额度已用完，返修结果未追加复审。完成时明确没有游戏实测、没有运行旧定向检查。最后报告打包进程已结束并停止产物写入，供主会话交付与表记录者补打包证据。
