固定Cursor CLI --model grok-4.7-high-fast，原C作者。设计2/2、实现3/3已用尽，不追加任何正确性复核。这是编译唯一报错的明确定点修复，不设计/审查/测试/编译/游戏/改表/提交。
唯一生产授权文件：src/main/java/com/bslx/XueXiMod/shu/technique/release/fire/SteamBurst.java。compile-final.md记录javac报错第55行：float steamDamage = (float) settled.get();，settled.get返回java.lang.Double，不能这样直接转float。请将这里改为正确拆箱/floatValue转换，保持现有已结算伤害和边界语义，不扩改其他代码。
保存修前字节pre-compile-fix/C/，保留所有既有基线和返修备份，更新author-C-manifest.json。必要命名门禁只报告已知OriginalHumanoid旧问题，不修。最终一次完整报告具体修复、写入释放，尚待重新统一编译；不得把本单说成正式复审。
