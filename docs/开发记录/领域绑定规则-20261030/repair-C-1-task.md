# C返修：关闭与轮次句柄交接

固定Cursor CLI --model grok-4.7-high-fast，继续C运行作者可写会话。读review-decision-1.md和自己的implementation-C-runtime-final.md。实现1/3轮后授权返修，不另审；设计2/2耗尽不重设计。

只修R3的C部分：DomainBindingRuntime执行executeAutomatic返回后，以及DomainInstance.trackRound接点，重新确认实例active/寿命/主人在线存活同维条件；已失效立即cancel、不挂回关闭实例，不能只等延迟回调。保留现有close清空、取消全部句柄、异常收尾；不改预算/收费，不改其他周期、采样、目标或注册。写权限仅D:/alianximod/src/main/java/com/bslx/XueXiMod/shu/technique/release/domain/DomainBindingRuntime.java、DomainInstance.java。

先用获准原生文件读写把这两文件现行完整内容保存backup-C-repair-1/相对路径，并确认落盘后再改。这个备份是本轮返修前，不伪装成首次实施前。Shell若Rejected不得绕过；如果不能完整备份就停该文件报告，不重复上轮未留备份仍修改的问题。保留所有既有未提交改动，禁止reset/clean/checkout。

A/B将修发布观察/预算，不读写其进行中代码，按RoundHandle既有接口接入。禁止编译、测试、打包、游戏、改表、提交、权限配置或模型更换。完成全部写入后写repair-C-1-final.md与evidence-C-repair-1.json，released=true，列精确接点及未验证，待统一编译。不自行追加正式复审。
