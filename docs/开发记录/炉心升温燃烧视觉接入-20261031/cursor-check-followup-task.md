# 只解决本机 Forge API 的检查争议
固定 --model grok-4.7-high-fast。用户要求检查完全由 Cursor 承担，Codex 不审代码。恢复原只读检查会话 8b959d9c-7d22-4411-957a-2a1dfc36e5bf。

先读本目录 repair-report.md、repair-result.json。原实现作者按实际本机 Forge1.20.1-47.3.33 查证：RenderArmEvent 只有单个类、没有 Pre/Post，事件在 renderHand 之前投递，因而拒绝按上轮检查结论改为 Post，源码未改。这是检查证据冲突，不是扩展设计，不得凭熟悉其他版本API下判断。

你只读本机实际依赖与打补丁源码/patch/必要反编译（不下载别的版本替代，不凭外网新版本API），明确 Forge1.20.1-47.3.33 的 RenderArmEvent 定义、ForgeHooksClient.renderSpecificFirstPersonHand/实际事件投递点、ItemInHandRenderer 调用路径、PlayerRenderer.renderHand/setupAnim/arm.render 的顺序。再检查 FurnaceArmFlame.onArm 在实际调用顺序下是正确、重复、还是姿态错误。只聚焦上轮 NEEDS_FIX 的这个方法，不重复检查全部已查范围，不另起子代理，不运行Gradle/javac，不改任何文件。

最终用很短报告明确 RETRACT_FINDING 或 NEEDS_FIX，并给实际本机文件路径/相关行号或反编译签名与调用顺序证据。若确有缺陷，给适用于当前真实API的最小修复指令；若原结论混淆版本则明确撤回并给本次检查最终状态。不声称完成编译或游戏验证。允许 Ask 模式最终直接报告，调度者原样保存。
