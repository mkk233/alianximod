# 补齐缓存失效的最后几个事实

继续只读定位，固定Cursor --model grok-4.7-high-fast；不是正式复审，不写文件、不编译、不测试、不再委托。

只补上前份read-invalidation-final.md的未知，不重读所有报告或重复输出已有结论。根D:/alianximod/src/main/java/com/bslx/XueXiMod。
1. 精确定位WaterReleaseDefinition.java，只列可变字段和会改变影响链结构/费用/冷却/输入倍率的fluent/setter方法；用rg查这些方法的实际调用，判断本仓是否存在“登记以后就地改这个定义实例”的路径，以及哪些getter依赖非final字段。不要展开所有水系效果。
2. ChainSegment.java的字段/访问器/equals语义，是否真正不可变；CompiledReleaseStep.members是否被外部修改，成员身份与segments.indexOf依赖应怎样描述成事实。
3. ReleaseDefinition / EnchantmentDefinition接口的结构读取方法中，哪些返回不可变配置、哪些可能是行为回调；本仓注册实现是否有在getter里读取世界/玩家/时钟的实例（只定向搜索getter实现，不整仓分析）。不保证未知第三方实现。

最终尽量1500字内，带精确路径/行号、实证和未确定范围。结尾写明无改动。禁止给出一整套新方案或正式正确性结论。
