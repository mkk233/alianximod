# 燎里命中火环与火种常驻显示：最终统一编译

固定Cursor CLI --model grok-4.7-high-fast。你是本批指定编译者b2d5b359-42e3-4a90-9d24-b26acec6d5f8。此任务文件只作为派发时授权，主会话在相关源码作者已经返回最终报告/停写、完成最小并发状态采集后才会实际调用你。

新增并发门槛：另有性能优化批次正在实施，见liaoli-hit-seed-coordination.md及docs/开发记录/空闲与复活性能优化-20261030/记录.md。必须确认该批全部作者已停写，且其聊天没有已经承担本次统一编译；不能仅依据本批作者停写就启动Gradle。若对方已有覆盖双方最终字节的成功编译，则只采集证据，不重复编译。

本单现已获得实际派发授权：性能聊天已明确发送最终ready，三作者与其独立只读复核均返回停止，11文件证据在docs/开发记录/空闲与复活性能优化-20261030/frozen-source-metadata.json，所有权/停止声明在同批记录.md，复核最终反馈review-1-final.md。对方明确由你负责本次共享工作区统一编译，自己不另开Gradle。本批源码23ef4660-da4b-47a8-9341-8ec236ec5cc3及交付记录者也已停写。请将性能11文件与本批10Java+1PNG及src/main全部文件的冻结证据一并核对，编译报告供双方引用；不重审任何代码。

不做额外审查，不改源码、不改制作表、不改记录.md、不做测试、不打包、不runClient、不启动游戏、不提交。设计2/2实现3/3不重置，没有独立测试阶段。编译失败只交回作者，不边编边改。

输入：本批liaoli-hit-seed-implement-report.md与-evidence.json（以最终实现作者实际交回文件为准），liaoli-hit-seed-source-handoff.md、liaoli-hit-seed-record.md、liaoli-hit-seed-art-approval.md、主会话保存的liaoli-hit-seed-thread-status-before-compile.json；另看既有领域/共享火种外部作者停写记录与当前线程摘要，若发现真正未收束的占用则交回不启动。

文件所有权：原作者1f4155a4-e537-4518-a3f7-893f09c5daff已在只读模式返回且未写源码；接任唯一源码作者23ef4660-da4b-47a8-9341-8ec236ec5cc3负责本次全部源码及运行时火苗资源，以其最终清单/停止声明为准。表格作者d6f84d0b-4ea1-4368-a576-8acc20cc2afd只写工作簿和记录，记录任务已返回停止。主会话只负责归档和资源README，已停止PNG/原生文件写入。编译不得在任一相关源码占用未收束时开始。

执行：先记输入报告列出的实际文件最后写入时间和SHA256，与作者证据匹配；记录src/main完整哈希清单（只用于冻结检测，不读源码评审）。核对当前没有另一活跃Gradle编译；不杀掉他人或历史空闲daemon。只在项目根执行 `.\gradlew.bat compileJava --offline --no-daemon`，沿用JDK17与org.gradle.jvmargs=-Xmx3G，不改配置。编译前后同范围hash一致才说明覆盖最终字节；有差异或失败就真实报告不能迁验收。运行资源应核对原PNG与正式归档SHA256一致；不对Java进行第二次正确性审查。

输出liaoli-hit-seed-compile-report.md、liaoli-hit-seed-compile-results.json、liaoli-hit-seed-gradle.log及src-main前后哈希文本，记录命令、起止时间、exit、success/fail、warning概要、是否实际compileJava或up-to-date、所有权/最后写入/停写三类证据。明确未跑测试、未游戏实测、本阶段额度已用完返修结果未追加复审。最后返回结果与停止占用声明。
