你是独立grok-4.7审核者，只审刚完成的公共输入契约小项。禁止工具、改代码、编译、另派代理。以下是完整带行号源文件和已批准契约；MAGIC/NONE资格与所有路由还没实施，不当成本项缺陷。核对不可变链倍率、BASE、非法值回1、duration ceil/保留<=0/饱和int、int环绕expiry与差值到期、long饱和加法是否正确。考虑record原始组件未归一但sizeMultiplier归一是否构成实际可行动问题；只提确定正确性缺陷，不泛化重构，允许说明使用前置条件。输出简短通过或路径行号+触发+最小修法。
# GPT-6-astra最终公共契约补充

A独占在ReleaseInput实现：
static int expiryTick(int nowTick,int durationTicks)
static boolean hasExpired(int nowTick,int expiryTick)
static long expiryGameTime(long now,long durationTicks)

非法multiplier(非有限或<=0)回1；scaleDuration有限正base以double ceil并钳[1,Integer.MAX_VALUE]，base<=0原样。有限duration范围1..Integer.MAX_VALUE。expiryTick按Java int环绕加法now+duration；hasExpired用now-expiry>=0（int差值）；禁止now>=expiry。调用只针对有限正寿命，不能把无限哨兵送进helper。对象正常逐tick检查到期处理，不能用于超过2^31 tick未检查的历史记录。

B/C必须核对长寿命所有到期入口，包含主tick/living/live/刷新/同步剩余计算，不仅主tick；long游戏时间expiryGameTime在now>Long.MAX_VALUE-duration时返回Long.MAX_VALUE否则相加，比到期保留now>=expiry。实体age到Integer.MAX_VALUE必须先到期退出，不能递增回负。不要顺手改无关维护计时器。

垂丝私有lifetime未同步，C必须增加实例寿命同步使客户端和服务端相位一致。ArcBand剑术固定窗口不改。

# 公共契约 API（A 第一小项）

未做路由、资格、MAGIC/NONE。未改容器。未跑 Gradle。未 commit。

命名门禁：`ReleaseInput` 保持 record，本次两个文件没有新违规。脚本退出码 1，仅旧违规，未改：

- `src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java:165` 局部变量 `i`
- `src/main/java/com/bslx/XueXiMod/animation/runtime/OriginalHumanoid.java:202` 局部变量 `to`

## 文件

- 新增 `src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseInput.java`
- `CastFrame` 只增加字段 `public ReleaseInput releaseInput = ReleaseInput.BASE;`
- 构造不改写倍率。组件里的 NaN、±Inf、≤0 原样保存。

## 类型

```java
public record ReleaseInput(float chainEnergyMultiplier) {
    public static final ReleaseInput BASE = new ReleaseInput(1f);

    public float sizeMultiplier();
    public int scaleDuration(int baseTicks);

    public static int expiryTick(int nowTick, int durationTicks);
    public static boolean hasExpired(int nowTick, int expiryTick);
    public static long expiryGameTime(long now, long durationTicks);
}
```

没有 `energyScale`、附魔半径、操作 ID。`sizeMultiplier()` 只返回有效链聚能，不乘 `CastFrame.energyScale` 或 `radiusMultiplier`。

## sizeMultiplier

`!Float.isFinite(chainEnergyMultiplier) || chainEnergyMultiplier <= 0f` 时返回 `1f`。否则返回保存值。

覆盖 NaN、+Inf、-Inf、+0、-0 和负数。`(0, +Inf)` 内的有限正数原样返回，包含小于 1 和大于 1。

## scaleDuration

`baseTicks <= 0` 时原样返回，含 0、负数和 `Integer.MIN_VALUE`。这条结果不能送进下面三个 helper。

`baseTicks >= 1` 时：

```text
scaled = Math.ceil((double) baseTicks * (double) sizeMultiplier())
```

先转 double 再乘，再 `ceil`。然后：

| scaled | 返回 |
|---|---|
| NaN | 1 |
| ≥ `Integer.MAX_VALUE`（含 +Inf） | `Integer.MAX_VALUE` |
| ≤ 1 | 1 |
| 其余 | `(int) scaled` |

因此有限正寿命的结果闭区间是 `1 .. Integer.MAX_VALUE`。`Integer.MAX_VALUE` 是有限正寿命的上界，不是无限哨兵；乘完超出 int 时停在这个上界。

## expiryTick

```java
return nowTick + durationTicks;
```

Java int 环绕加法，不钳制，不识别哨兵。调用方只能传入已经由 `scaleDuration` 得到的有限正寿命 `1 .. Integer.MAX_VALUE`。`Integer.MAX_VALUE + 1` 环绕成 `Integer.MIN_VALUE`。

## hasExpired

```java
return nowTick - expiryTick >= 0;
```

相等视为已到期。禁止写成 `nowTick >= expiryTick`。只给逐 tick 检查的有限正寿命用；超过 2^31 tick 未检查的历史记录不能用这个差值。

## expiryGameTime

```java
if (durationTicks >= 0L && now > Long.MAX_VALUE - durationTicks) {
    return Long.MAX_VALUE;
}
return now + durationTicks;
```

`durationTicks >= 0` 时与「`now > Long.MAX_VALUE - duration` 则返回 `Long.MAX_VALUE`，否则相加」相同；这个前提避免负寿命把 `Long.MAX_VALUE - durationTicks` 自己算溢出。负寿命不在调用契约内。

长游戏时间的到期比较保持 `now >= expiry`。不要改用 int 那条差值。

三个 helper 都不接受无限哨兵。调用前寿命必须已是有限正值。

<source path="src/main/java/com/bslx/XueXiMod/shu/technique/release/ReleaseInput.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: /**
4:  * 链上聚能的冻结输入。只保存链聚能倍率，不含术式自身能量、附魔半径或操作身份。
5:  */
6: public record ReleaseInput(float chainEnergyMultiplier) {
7:     public static final ReleaseInput BASE = new ReleaseInput(1f);
8: 
9:     /** 有效链聚能。非有限或小于等于 0 时返回 1。 */
10:     public float sizeMultiplier() {
11:         float multiplier = this.chainEnergyMultiplier;
12:         if (!Float.isFinite(multiplier) || multiplier <= 0f) {
13:             return 1f;
14:         }
15:         return multiplier;
16:     }
17: 
18:     /**
19:      * 有限正寿命按有效倍率做 double 乘法后向上取整，再钳到 1 与 Integer.MAX_VALUE。
20:      * 小于等于 0 的原值保持不变。
21:      */
22:     public int scaleDuration(int baseTicks) {
23:         if (baseTicks <= 0) {
24:             return baseTicks;
25:         }
26:         double scaledTicks = Math.ceil((double) baseTicks * (double) this.sizeMultiplier());
27:         if (Double.isNaN(scaledTicks)) {
28:             return 1;
29:         }
30:         if (scaledTicks >= Integer.MAX_VALUE) {
31:             return Integer.MAX_VALUE;
32:         }
33:         if (scaledTicks <= 1d) {
34:             return 1;
35:         }
36:         return (int) scaledTicks;
37:     }
38: 
39:     /** 有限正寿命的 int 到期刻。按 Java int 环绕相加，不钳制。 */
40:     public static int expiryTick(int nowTick, int durationTicks) {
41:         return nowTick + durationTicks;
42:     }
43: 
44:     /** 当前刻是否已到到期刻。用 int 差值，避免直接比较两个可能环绕的刻。 */
45:     public static boolean hasExpired(int nowTick, int expiryTick) {
46:         return nowTick - expiryTick >= 0;
47:     }
48: 
49:     /**
50:      * 长游戏时间的到期点。非负寿命相加会超过 long 上界时返回 Long.MAX_VALUE，否则相加。
51:      */
52:     public static long expiryGameTime(long now, long durationTicks) {
53:         if (durationTicks >= 0L && now > Long.MAX_VALUE - durationTicks) {
54:             return Long.MAX_VALUE;
55:         }
56:         return now + durationTicks;
57:     }
58: }
59: 
</source>

<source path="src/main/java/com/bslx/XueXiMod/shu/technique/release/CastFrame.java">
1: package com.bslx.XueXiMod.shu.technique.release;
2: 
3: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
4: import com.bslx.XueXiMod.shu.technique.enchant.TideMarkSuccess;
5: import net.minecraft.util.RandomSource;
6: import net.minecraft.world.phys.Vec3;
7: 
8: public final class CastFrame {
9:     private static final ThreadLocal<CastFrame> CURRENT = new ThreadLocal<>();
10: 
11:     public float damage;
12:     public EnchantmentResolution enchantments;
13:     public ReleaseBatch.Tally tally;
14:     public int copyIndex;
15:     public Vec3 anchor;
16:     public Vec3 origin;
17:     public Vec3 look;
18:     public Vec3 planeNormal;
19:     public float roll;
20:     public boolean pose3d;
21:     public boolean overrideGeometry;
22:     public boolean drop;
23:     /** 独立追踪修饰，不替换基础路径或生成几何。 */
24:     public boolean tracking;
25:     public Trajectory pathOverride;
26:     public boolean trajectoryMutable;
27:     public float radiusMultiplier = 1f;
28:     public float energyScale = 1f;
29:     /** 链上聚能冻结输入。不含术式自身能量、附魔半径或操作身份。 */
30:     public ReleaseInput releaseInput = ReleaseInput.BASE;
31:     public int burnSeconds;
32:     public int controlTicks;
33:     public boolean extinguish;
34:     public float knockback;
35:     public float speedMultiplier = 1f;
36:     public TideMarkSuccess tideSuccess = TideMarkSuccess.NONE;
37:     /** 玩家身体当前被几块场地包住。0 和 1 都不改伤害。 */
38:     public float fieldDamageScale = 1f;
39:     public float healAmount;
40:     public boolean healCaster;
41:     public float pullStrength;
42:     public int wetnessGrant;
43:     public boolean blocksMovement;
44:     public Vec3 pullAnchor;
45:     public float liftStrength;
46:     public double volumeHalf;
47:     public double groundLevel = Double.NEGATIVE_INFINITY;
48:     public long sampleSeed;
49:     public boolean scatterAim;
50:     public int copyCount = 1;
51:     /** 这一份的生成几何已经按复制序号散开。只给视觉读，释放计算不读。 */
52:     public boolean cohortDispersed;
53:     public boolean heavyWater;
54:     public java.util.UUID releaseGroup;
55: 
56:     public static CastFrame current() {
57:         return CURRENT.get();
58:     }
59: 
60:     /** 能量倍率乘上已经含有元素反应的半径倍率。无效值按 1。 */
61:     public float volumeScale() {
62:         float scale = this.energyScale * this.radiusMultiplier;
63:         if (!Float.isFinite(scale) || scale <= 0f) {
64:             return 1f;
65:         }
66:         return scale;
67:     }
68: 
69:     public static void bind(CastFrame frame) {
70:         if (frame == null) {
71:             CURRENT.remove();
72:         } else {
73:             CURRENT.set(frame);
74:         }
75:     }
76: 
77:     public static void unbind() {
78:         CURRENT.remove();
79:     }
80: 
81:     /** 垂落从落点上方出现，再自由落体。 */
82:     private static final double DROP_LIFT = 8.0;
83:     private static final double GOLDEN_TURN = Math.PI * (3.0 - Math.sqrt(5.0));
84:     private static final double VOLUME_HALF = 1.5;
85: 
86:     public static CastFrame forAttack(float damage, EnchantmentResolution enchantments, ReleaseBatch.Tally tally,
87:                                       int copyIndex, int copyCount, Vec3 center, Vec3 baseLook,
88:                                       boolean spaceLock, boolean scatter, boolean entityLock, Trajectory pathOverride,
89:                                       boolean trajectoryMutable, long seed, double groundLevel) {
90:         CastFrame frame = new CastFrame();
91:         frame.damage = damage;
92:         frame.enchantments = enchantments != null ? enchantments : EnchantmentResolution.empty();
93:         frame.tally = tally;
94:         frame.copyIndex = copyIndex;
95:         frame.copyCount = copyCount < 1 ? 1 : copyCount;
96:         frame.pathOverride = pathOverride;
97:         frame.drop = pathOverride == Trajectory.DROP;
98:         frame.trajectoryMutable = trajectoryMutable;
99:         boolean replacesPath = trajectoryMutable && pathOverride != null && pathOverride != Trajectory.STRAIGHT;
100: 
101:         if (!scatter && !spaceLock && !replacesPath) {
102:             frame.overrideGeometry = false;
103:             return frame;
104:         }
105: 
106:         frame.overrideGeometry = true;
107:         if (!trajectoryMutable) {
108:             return slashGeometry(frame, center, baseLook, spaceLock, scatter, copyIndex, copyCount, seed);
109:         }
110:         return projectileGeometry(frame, center, baseLook, spaceLock, scatter, entityLock, pathOverride, seed, groundLevel);
111:     }
112: 
113:     /** 斩击保持原有落点和刀面散布，不改成弹丸的随机向量。 */
114:     private static CastFrame slashGeometry(CastFrame frame, Vec3 center, Vec3 baseLook, boolean spaceLock, boolean scatter,
115:                                            int copyIndex, int copyCount, long seed) {
116:         double half = spaceLock ? 1.5 : 1.0;
117:         Vec3 origin = (spaceLock || scatter) ? jitter(center, half, seed, copyIndex) : center;
118:         frame.origin = origin;
119:         frame.anchor = spaceLock ? center : null;
120:         frame.look = baseLook.normalize();
121: 
122:         if (!scatter) {
123:             frame.planeNormal = horizontal(baseLook);
124:             frame.roll = 0.0F;
125:             frame.pose3d = false;
126:             frame.cohortDispersed = spaceLock;
127:             return frame;
128:         }
129: 
130:         RandomSource random = RandomSource.create(seed ^ copyIndex);
131:         Vec3 flatLook = horizontal(baseLook);
132:         Vec3 flatRight = new Vec3(-flatLook.z, 0.0, flatLook.x);
133:         double radius = 0.35 + random.nextDouble() * 0.9;
134:         double azimuth = random.nextDouble() * Math.PI * 2.0;
135:         double along = (random.nextDouble() * 2.0 - 1.0) * 0.75;
136:         double verticalOffset = (random.nextDouble() * 2.0 - 1.0) * 0.75;
137:         Vec3 scattered = center
138:                 .add(flatRight.scale(Math.cos(azimuth) * radius))
139:                 .add(flatLook.scale(Math.sin(azimuth) * radius))
140:                 .add(flatLook.scale(along))
141:                 .add(0.0, verticalOffset, 0.0);
142: 
143:         frame.origin = clampToBox(scattered, center, spaceLock ? 1.5 : 1.0);
144:         frame.anchor = spaceLock ? center : null;
145:         frame.look = baseLook.normalize();
146:         frame.roll = (float) ((random.nextDouble() * 2.0 - 1.0) * 35.0);
147:         frame.planeNormal = deflectNormal(random, baseLook, copyIndex, copyCount);
148:         frame.pose3d = Math.abs(frame.roll) > 1.0F || Math.abs(frame.planeNormal.y) > 0.2;
149:         frame.cohortDispersed = true;
150:         return frame;
151:     }
152: 
153:     /** 落点指定只撒生成点。三维交错在没有其他索敌时围着玩家撒点，并改成随机三维方向。 */
154:     private static CastFrame projectileGeometry(CastFrame frame, Vec3 center, Vec3 baseLook, boolean spaceLock, boolean scatter,
155:                                                 boolean entityLock, Trajectory pathOverride, long seed, double groundLevel) {
156:         if (pathOverride == Trajectory.VORTEX) {
157:             frame.anchor = center;
158:             frame.volumeHalf = 0.0;
159:             frame.origin = center;
160:             frame.scatterAim = false;
161:             frame.look = frame.vortexPhase(0);
162:             frame.planeNormal = horizontal(frame.look);
163:             frame.roll = 0.0F;
164:             frame.pose3d = false;
165:             frame.cohortDispersed = true;
166:             return frame;
167:         }
168:         boolean cube = spaceLock || (scatter && !entityLock);
169:         frame.anchor = cube ? center : null;
170:         frame.volumeHalf = cube ? VOLUME_HALF : 0.0;
171:         frame.sampleSeed = seed;
172:         frame.groundLevel = groundLevel;
173:         frame.scatterAim = scatter && pathOverride != Trajectory.DROP;
174:         frame.look = baseLook.normalize();
175:         frame.origin = cube ? frame.volumePoint(0) : (pathOverride == Trajectory.DROP ? center.add(0.0, DROP_LIFT, 0.0) : center);
176:         frame.look = frame.aimFor(0);
177:         frame.planeNormal = horizontal(frame.look);
178:         frame.roll = 0.0F;
179:         frame.pose3d = false;
180:         frame.cohortDispersed = cube || frame.scatterAim;
181:         return frame;
182:     }
183: 
184:     /** 接触点把每一份收到同一个锚点上之后，视觉不能再把覆写当成已经散开。 */
185:     public void adoptSharedAnchor() {
186:         this.cohortDispersed = false;
187:     }
188: 
189:     /** 同一条龙卷风上的起始角。序号连续时仍均匀铺在圆周上。 */
190:     public Vec3 vortexPhase(int spawnIndex) {
191:         double phase = (this.copyIndex * 1024 + spawnIndex) * GOLDEN_TURN;
192:         return new Vec3(Math.cos(phase), 0.0, Math.sin(phase));
193:     }
194:     public Vec3 volumePoint(int spawnIndex) {
195:         Vec3 point = jitter(this.anchor, this.volumeHalf, this.sampleSeed, this.copyIndex * 1024 + spawnIndex);
196:         if (point.y < this.groundLevel) {
197:             point = new Vec3(point.x, this.groundLevel, point.z);
198:         }
199:         if (this.drop) {
200:             point = point.add(0.0, DROP_LIFT, 0.0);
201:         }
202:         return point;
203:     }
204: 
205:     public Vec3 aimFor(int spawnIndex) {
206:         if (this.drop) {
207:             return new Vec3(0.0, -1.0, 0.0);
208:         }
209:         if (this.scatterAim) {
210:             return randomDirection(this.sampleSeed, this.copyIndex * 1024 + spawnIndex);
211:         }
212:         return this.look == null ? new Vec3(0.0, 0.0, 1.0) : this.look;
213:     }
214: 
215:     private static Vec3 randomDirection(long seed, int copyIndex) {
216:         RandomSource random = RandomSource.create(seed ^ (copyIndex * 31L) ^ 0x6C62272EL);
217:         double rise = random.nextDouble() * 2.0 - 1.0;
218:         double spin = random.nextDouble() * Math.PI * 2.0;
219:         double ring = Math.sqrt(Math.max(0.0, 1.0 - rise * rise));
220:         return new Vec3(ring * Math.cos(spin), rise, ring * Math.sin(spin));
221:     }
222: 
223:     private static Vec3 deflectNormal(RandomSource random, Vec3 baseLook, int copyIndex, int copyCount) {
224:         if (copyCount <= 1) {
225:             return yawPitch(baseLook,
226:                     (random.nextDouble() * 2.0 - 1.0) * 25.0,
227:                     (random.nextDouble() * 2.0 - 1.0) * 15.0);
228:         }
229:         double baseYaw = copyIndex * 120.0;
230:         double yaw = baseYaw;
231:         double pitch = (copyIndex - 1) * 15.0;
232:         for (int attempt = 0; attempt < 8; attempt++) {
233:             double jitter = (random.nextDouble() * 2.0 - 1.0) * 25.0;
234:             if (Math.abs(jitter) >= 12.0) {
235:                 yaw = baseYaw + jitter;
236:                 pitch = (random.nextDouble() * 2.0 - 1.0) * 15.0;
237:                 break;
238:             }
239:         }
240:         return yawPitch(baseLook, yaw, pitch);
241:     }
242: 
243:     private static Vec3 yawPitch(Vec3 base, double yawDegrees, double pitchDegrees) {
244:         Vec3 flat = horizontal(base);
245:         Vec3 flatRight = new Vec3(-flat.z, 0.0, flat.x);
246:         double yaw = Math.toRadians(yawDegrees);
247:         double pitch = Math.toRadians(pitchDegrees);
248:         Vec3 yawed = flat.scale(Math.cos(yaw)).add(flatRight.scale(Math.sin(yaw)));
249:         double cosPitch = Math.cos(pitch);
250:         double sinPitch = Math.sin(pitch);
251:         return new Vec3(yawed.x * cosPitch, sinPitch, yawed.z * cosPitch).normalize();
252:     }
253: 
254:     private static Vec3 jitter(Vec3 center, double half, long seed, int sampleIndex) {
255:         long mixed = mix64(seed ^ (sampleIndex * 0x9E3779B97F4A7C15L));
256:         double east = (unit(mixed ^ 0x9E3779B97F4A7C15L) * 2.0 - 1.0) * half;
257:         double rise = (unit(mixed ^ 0xBF58476D1CE4E5B9L) * 2.0 - 1.0) * half;
258:         double south = (unit(mixed ^ 0x94D049BB133111EBL) * 2.0 - 1.0) * half;
259:         return center.add(east, rise, south);
260:     }
261: 
262:     /** 连续序号不能直接丢给 RandomSource，否则第一轴几乎不动，点会排成一条线。 */
263:     private static long mix64(long value) {
264:         long mixed = value;
265:         mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
266:         mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
267:         return mixed ^ (mixed >>> 31);
268:     }
269: 
270:     private static double unit(long mixed) {
271:         return (mix64(mixed) >>> 11) * 0x1.0p-53;
272:     }
273: 
274:     private static Vec3 clampToBox(Vec3 point, Vec3 center, double half) {
275:         return new Vec3(
276:                 clamp(point.x, center.x - half, center.x + half),
277:                 clamp(point.y, center.y - half, center.y + half),
278:                 clamp(point.z, center.z - half, center.z + half));
279:     }
280: 
281:     private static double clamp(double value, double lowerBound, double upperBound) {
282:         return Math.max(lowerBound, Math.min(upperBound, value));
283:     }
284: 
285:     private static Vec3 horizontal(Vec3 vector) {
286:         double length = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
287:         if (length < 1.0E-4) {
288:             return new Vec3(0.0, 0.0, 1.0);
289:         }
290:         return new Vec3(vector.x / length, 0.0, vector.z / length);
291:     }
292: }
293: 
</source>
