现在正式执行本验证任务。A/B/C写代码阶段均已结束，目前并行任务均为只读审查，可以单一编译。
先读legacy-entry-decision.md：GPT-6-astra查证SkillFollowCast没有任何外部调用，是未使用旧入口，本次不改也不重新接通。请修正route-report.md中“越界仍需改”表述为未使用遗留入口，不再把它当真实运行阻塞。无属性资格仍未收到用户回答，保持未最终决定并如实报告。
最终编译与定向验证任务，只有主控明确发起本文件才开始。当前本文件只是准备稿。
你是Cursor实现者，按用户要求由你编译、主控核对，不打包、不进游戏。先确认route-report.md和最新审查结论及各组报告，不改无关模块。无属性资格若尚待用户，报告必须明确未最终决定，不能称整个任务全完。
单一Gradle编译：PowerShell设置 $env:GRADLE_USER_HOME='C:\Users\baishang\.gradle' ，项目根运行 .\gradlew.bat compileJava --offline --no-daemon，保存真实完整日志与退出码到本任务目录。禁止build/jar/reobf/runClient和并行Gradle。JDK17为当前项目。若代码编译错误来自本任务，限定修复并报告文件；作者文件归属不确定先报告主控，不能回退工作区已有改动。
已知完整compileTestJava因原有SkinAttachmentChecks中Matrix4f私有字段与Model方法实现错误失败，不要为此次修复这些无关旧测试，也不要再次用全套失败替代定向验证。PrisonQaPacketCheck只有deprecated警告不是主错误。
从实际生产classes编译并执行本次定向main：ReleaseContainerEnergyCheck、WallHostExpiryCheck、ReleaseEntityEnergyCheck、PrisonEnergyCheck、公共路由组新测试，及必要已有回归（如射线/水环受此次修改影响）。优先现有build/tide-classpath.txt与当前编译产物，用javac/java限定源文件，隔离输出目录写本任务目录/checks-integration。不要新建junction，PowerShell带中文脚本需UTF8BOM或明确UTF8读取。保持真正生产类无stubs替代。类加载需要Forge启动的部分如无法隔离，明确静态核对/未执行，不能吞异常之后输出PASS。
ReleaseInput真实生产类77断言已经通过，若该文件本轮没再变不重复跑。
测试失败要判断真实缺陷/旧基线错误/测试环境缺依赖，分别报告，不通过预期改成实现来掩盖；必要修复由主控分派。每组记录assertions（若程序报告）、exit、命令、classpath/实际生产代码来源。没有运行游戏，无需模拟说实测通过。
归档validation-report.md及compileJava.log/check-*.log。更新scope.md的只读初始状态为实际完成/待确认范围。不要git提交，不打包，不启动游戏。