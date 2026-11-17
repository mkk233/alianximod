# 作者B：炉心状态

完整读取 implementation-contract.md 和主设计后按B归属实现。B唯一写 status/fire/furnace/**、同包tests、ModMobEffects.java、xueximod.mixins.json、炉心新增mixin与中英文炉心语言。不要改A/C文件。

实现普通/过载MobEffect与持久F，状态转换、稳定属性modifier、在线爬坡、无离线补涨、登录/登出/跨维/非死亡clone/死亡清理；效果有/运行态无与反向修复；牛奶不可清除但命令可清除；普通→过载保F原子更新、不重复修饰、不被MobEffect隐藏效果机制悄悄降级；正常heal缩放；仅炉心玩家的Resistance减伤失效但Protection正常（核本地Forge真实钩子，不根据旧报告猜事件顺序）。不修改全局抗性，不移除抗性效果。cool调用C的SteamBurst.release(Player)，其它结束不蒸汽。

FurnaceService的fireBonus/seedMultiplier/active/waterIceMultiplier固定契约。无限MobEffect不用Wetness有限时长replace。不做任何术式入口、技能冷却/魔力/配方。实现必要参数校验、状态机数学检查和定向结构检查，作者阶段不Gradle、不独立正式复审。最终说明哪些需要游戏验证。
