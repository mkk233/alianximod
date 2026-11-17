# GPT-6-astra最终公共契约补充

A独占在ReleaseInput实现：
static int expiryTick(int nowTick,int durationTicks)
static boolean hasExpired(int nowTick,int expiryTick)
static long expiryGameTime(long now,long durationTicks)

非法multiplier(非有限或<=0)回1；scaleDuration有限正base以double ceil并钳[1,Integer.MAX_VALUE]，base<=0原样。有限duration范围1..Integer.MAX_VALUE。expiryTick按Java int环绕加法now+duration；hasExpired用now-expiry>=0（int差值）；禁止now>=expiry。调用只针对有限正寿命，不能把无限哨兵送进helper。对象正常逐tick检查到期处理，不能用于超过2^31 tick未检查的历史记录。

B/C必须核对长寿命所有到期入口，包含主tick/living/live/刷新/同步剩余计算，不仅主tick；long游戏时间expiryGameTime在now>Long.MAX_VALUE-duration时返回Long.MAX_VALUE否则相加，比到期保留now>=expiry。实体age到Integer.MAX_VALUE必须先到期退出，不能递增回负。不要顺手改无关维护计时器。

垂丝私有lifetime未同步，C必须增加实例寿命同步使客户端和服务端相位一致。ArcBand剑术固定窗口不改。
