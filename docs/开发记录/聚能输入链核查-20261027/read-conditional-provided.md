你是deepseek-flash专项只读补查，唯一GPT-6-astra要求以下事实。不调用工具、不改代码、不编译、不派代理；只按给定完整带行号源码核实，1000-1800字中文路径行号报告。核心聚能已确认只进入实体CastFrame；公共输入快照后续由brain裁决，此轮不要自行给架构修法。用户MAGIC/NONE资格边界正在确认，不自行假定。
补查条件释放：ConditionalEffect→会话/接触/预约→PrisonService.prepare/Reservation/实例。列准确接口、几何及寿命如何冻结、所有给定调用点、准备与提交/退款语义、何处能带输入量而不破坏预约。生成水真实effect是否长期世界改变、是否有有限范围寿命可缩放，不能按NONE判断。

<source_file path="src/main/java/com/bslx/XueXiMod/network/ContactCast.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.shu.Technique;
4: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
5: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
7: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
8: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
9: import com.bslx.XueXiMod.skill.Skill;
10: import net.minecraft.server.level.ServerPlayer;
11: 
12: import java.util.List;
13: 
14: /** 附魔能不能绑到这一发零伤害宿主上。不能绑就在扣费和推进之前拒绝。 */
15: final class ContactCast {
16:     private ContactCast() {
17:     }
18: 
19:     static String refusal(Skill skill, List<ChainSegment> segments) {
20:         List<Technique> techniques = skill.getTechniques();
21:         for (ChainSegment segment : segments) {
22:             if (segment.enchantIndexes().isEmpty()) {
23:                 continue;
24:             }
25:             Technique host = techniques.get(segment.actionIndex());
26:             if (host.getBaseDamage() > 0f || host.isEnchantment()) {
27:                 continue;
28:             }
29:             ReleaseDefinition definition = ReleaseRegistry.get(host.getId());
30:             if (!carriesContact(definition)) {
31:                 return host.getName() + "还不能承载附魔，这次没有释放";
32:             }
33:             if (EnchantmentSnapshot.damageOf(skill, segment.enchantIndexes()) <= 0f) {
34:                 return "没有附加伤害的接触附魔暂不支持，这次没有释放";
35:             }
36:         }
37:         return null;
38:     }
39: 
40:     static boolean keptForContact(List<Technique> techniques, List<ChainSegment> segments, int from) {
41:         for (int index = from; index < segments.size(); index++) {
42:             ChainSegment segment = segments.get(index);
43:             Technique host = techniques.get(segment.actionIndex());
44:             if (host.isEnchantment() || SkillSegmentMath.isChainModifier(host)) {
45:                 continue;
46:             }
47:             if (host.getBaseDamage() > 0f || segment.enchantIndexes().isEmpty()) {
48:                 return false;
49:             }
50:             return carriesContact(ReleaseRegistry.get(host.getId()));
51:         }
52:         return false;
53:     }
54: 
55:     static EnchantmentSnapshot snapshot(Skill skill, ChainSegment segment, ServerPlayer player) {
56:         if (segment.enchantIndexes().isEmpty()) {
57:             return null;
58:         }
59:         Technique host = skill.getTechniques().get(segment.actionIndex());
60:         if (host.getBaseDamage() > 0f) {
61:             return null;
62:         }
63:         if (!carriesContact(ReleaseRegistry.get(host.getId()))) {
64:             return null;
65:         }
66:         EnchantmentSnapshot built = EnchantmentSnapshot.from(skill, segment, player);
67:         if (built == null || built.contactDamage <= 0f) {
68:             return null;
69:         }
70:         return built;
71:     }
72: 
73:     private static boolean carriesContact(ReleaseDefinition definition) {
74:         return definition != null && (definition.barrier() != null || definition.domain() != null
75:                 || definition.fieldRadius() > 0f || definition.guard() != null
76:                 || ConditionalEffects.supportsContact(definition));
77:     }
78: }
79: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/SkillFollowCast.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.player.MagicPool;
4: import com.bslx.XueXiMod.shu.Technique;
5: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
6: import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
7: import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
8: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
9: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
10: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
11: import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
12: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
13: import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
14: import com.bslx.XueXiMod.skill.Skill;
15: import net.minecraft.server.level.ServerPlayer;
16: import net.minecraft.world.item.ItemStack;
17: import net.minecraft.world.phys.Vec3;
18: 
19: import java.util.ArrayList;
20: import java.util.List;
21: import java.util.function.Consumer;
22: 
23: final class SkillFollowCast {
24:     static Consumer<Vec3> buildRuleTrigger(ServerPlayer player, Skill skill,
25:                                             List<Technique> techniques, ItemStack weapon,
26:                                             List<ChainSegment> segments, int ruleIndex, float weaponBonus, int slot,
27:                                             double cooldownReduction, Double attributeDamageBonus) {
28:         ReleaseBatch pressed = ReleaseBatch.current();
29:         List<ChainSegment> released = SkillChainPlan.releasedSegments(segments, ruleIndex, techniques);
30:         if (released.isEmpty()) {
31:             return position -> {};
32:         }
33:         ChainSegment aSegment = released.get(0);
34:         List<ChainSegment> follow = released.subList(1, released.size());
35: 
36:         Technique aAction = techniques.get(aSegment.actionIndex());
37:         EnchantmentResolution aEnchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, aSegment);
38:         EnchantBonus aBonus = EnchantBonus.resolve(skill, aSegment.enchantIndexes(), aAction.getBaseDamage(), 1);
39:         boolean aCarries = aAction.getBaseDamage() > 0 && !aSegment.enchantIndexes().isEmpty();
40:         float aDamage = hostedDamage(skill, aSegment, aAction, weaponBonus, attributeDamageBonus, aBonus);
41:         int totalCooldown = SkillSegmentMath.scaleCooldown(SkillSegmentMath.baseCooldownSum(released, techniques), cooldownReduction);
42: 
43:         if (SkillSegmentMath.releaseKind(aAction.getId()) == ReleaseKind.ACTION) {
44:             Consumer<Vec3> followTrigger = buildFollowTrigger(player, skill, techniques, weapon, segments, follow, weaponBonus, slot, aAction, cooldownReduction, attributeDamageBonus);
45:             Runnable aRunner = SkillSegmentRunner.buildSegmentRunner(player, aAction, techniques, aSegment.actionIndex(), weapon, aDamage, aCarries, aEnchantments, followTrigger,
46:                     SkillChainPlan.lockThisCast(techniques, aSegment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, aSegment.actionIndex()),
47:                     null, 0, 1, ReleaseBatch.current(), aBonus, 0, null);
48:             return parentPosition -> ChainScheduler.schedule(player.getServer(), player.getServer().getTickCount(), aRunner);
49:         }
50: 
51:         return parentPosition -> {
52:             Vec3 parent = parentPosition != null ? parentPosition : player.position();
53:             SkillSegmentRunner.buildSegmentRunnerAt(player, aAction, techniques, aSegment.actionIndex(), weapon, aDamage, aCarries, aEnchantments, null,
54:                     player.getLookAngle(), parent, SkillChainPlan.lockThisCast(techniques, aSegment.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, aSegment.actionIndex()),
55:                     null, 0, 1, pressed, aBonus, 0, null).run();
56: 
57:             int lastTriggerIndex = -1;
58:             for (int followIndex = 0; followIndex < follow.size(); followIndex++) {
59:                 if (SkillSegmentMath.isTriggerRelease(techniques.get(follow.get(followIndex).actionIndex()).getId())) {
60:                     lastTriggerIndex = followIndex;
61:                 }
62:             }
63: 
64:             int tick = player.getServer().getTickCount();
65:             for (int followIndex = 0; followIndex < follow.size(); followIndex++) {
66:                 ChainSegment seg = follow.get(followIndex);
67:                 Technique tech = techniques.get(seg.actionIndex());
68:                 EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, seg);
69:                 EnchantBonus bonus = EnchantBonus.resolve(skill, seg.enchantIndexes(), tech.getBaseDamage(), 1);
70:                 boolean carries = tech.getBaseDamage() > 0 && !seg.enchantIndexes().isEmpty();
71:                 float damage = hostedDamage(skill, seg, tech, weaponBonus, attributeDamageBonus, bonus);
72: 
73:                 Runnable runner;
74:                 if (followIndex == lastTriggerIndex) {
75:                     runner = () -> {
76:                         Vec3 look = player.getLookAngle();
77:                         Vec3 position = player.position();
78:                         SkillSegmentRunner.buildSegmentRunnerAt(player, tech, techniques, seg.actionIndex(), weapon, damage, carries, enchantments, null, look, position,
79:                                 SkillChainPlan.lockThisCast(techniques, seg.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, seg.actionIndex()),
80:                                 null, 0, 1, pressed, bonus, 0, null).run();
81:                     };
82:                 } else {
83:                     runner = () -> {
84:                         Vec3 look = player.getLookAngle();
85:                         SkillSegmentRunner.buildSegmentRunnerAt(player, tech, techniques, seg.actionIndex(), weapon, damage, carries, enchantments, null, look, parent,
86:                                 SkillChainPlan.lockThisCast(techniques, seg.actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, seg.actionIndex()),
87:                                 null, 0, 1, pressed, bonus, 0, null).run();
88:                     };
89:                 }
90:                 ChainScheduler.schedule(player.getServer(), tick, runner);
91:             }
92: 
93:             CastCooldown.mark(player, weapon, slot, tick + totalCooldown);
94:         };
95:     }
96: 
97:     static Consumer<Vec3> buildFollowTrigger(ServerPlayer player, Skill skill,
98:                                               List<Technique> techniques, ItemStack weapon,
99:                                               List<ChainSegment> segments, List<ChainSegment> follow,
100:                                               float weaponBonus, int slot, Technique action,
101:                                               double cooldownReduction, Double attributeDamageBonus) {
102:         ReleaseBatch pressed = ReleaseBatch.current();
103:         List<ChainSegment> triggerSegments = new ArrayList<>();
104:         List<Technique> triggerActions = new ArrayList<>();
105:         List<Float> triggerDamages = new ArrayList<>();
106:         List<Boolean> triggerCarries = new ArrayList<>();
107:         List<EnchantmentResolution> triggerEnchantments = new ArrayList<>();
108:         List<EnchantBonus> triggerBonuses = new ArrayList<>();
109: 
110:         List<ChainSegment> sustainedSegments = new ArrayList<>();
111:         List<Technique> sustainedActions = new ArrayList<>();
112:         List<Float> sustainedDamages = new ArrayList<>();
113:         List<Boolean> sustainedCarries = new ArrayList<>();
114:         List<EnchantmentResolution> sustainedEnchantments = new ArrayList<>();
115:         List<EnchantBonus> sustainedBonuses = new ArrayList<>();
116: 
117:         for (ChainSegment seg : follow) {
118:             Technique technique = techniques.get(seg.actionIndex());
119:             ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
120:             if (definition == null) {
121:                 continue;
122:             }
123:             EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, seg);
124:             EnchantBonus bonus = EnchantBonus.resolve(skill, seg.enchantIndexes(), technique.getBaseDamage(), 1);
125:             boolean carries = technique.getBaseDamage() > 0 && !seg.enchantIndexes().isEmpty();
126:             float damage = hostedDamage(skill, seg, technique, weaponBonus, attributeDamageBonus, bonus);
127:             if (definition.tag() == ReleaseTag.TRIGGER) {
128:                 triggerSegments.add(seg);
129:                 triggerActions.add(technique);
130:                 triggerDamages.add(damage);
131:                 triggerCarries.add(carries);
132:                 triggerEnchantments.add(enchantments);
133:                 triggerBonuses.add(bonus);
134:             } else if (definition.tag() == ReleaseTag.SUSTAINED) {
135:                 sustainedSegments.add(seg);
136:                 sustainedActions.add(technique);
137:                 sustainedDamages.add(damage);
138:                 sustainedCarries.add(carries);
139:                 sustainedEnchantments.add(enchantments);
140:                 sustainedBonuses.add(bonus);
141:             }
142:         }
143: 
144:         int triggerManaCostSum = 0;
145:         for (ChainSegment seg : triggerSegments) {
146:             triggerManaCostSum += SkillSegmentMath.segmentManaCost(techniques, seg);
147:         }
148:         int sustainedManaCostSum = 0;
149:         for (ChainSegment seg : sustainedSegments) {
150:             sustainedManaCostSum += SkillSegmentMath.segmentManaCost(techniques, seg);
151:         }
152:         final int triggerMana = triggerManaCostSum;
153:         final int sustainedMana = sustainedManaCostSum;
154: 
155:         int triggerCooldown = SkillSegmentMath.scaleCooldown(SkillSegmentMath.baseCooldownSum(triggerSegments, techniques), cooldownReduction);
156:         int sustainedCooldown = SkillSegmentMath.baseCooldownSum(sustainedSegments, techniques);
157:         int interval = SkillSegmentMath.scaleCooldown(Math.max(1, action.getBaseCooldown() + sustainedCooldown), cooldownReduction);
158:         ReleaseDefinition actionDefinition = ReleaseRegistry.get(action.getId());
159:         int lifetime = actionDefinition != null ? actionDefinition.lifetimeTicks() : 10;
160: 
161:         return position -> {
162:             int tick = player.getServer().getTickCount();
163: 
164:             int firstCost = triggerMana + sustainedMana;
165:             boolean firstPaid = MagicPool.trySpend(player, firstCost);
166:             if (firstPaid && firstCost > 0) {
167:                 MagicPool.showBalance(player);
168:             }
169: 
170:             if (firstPaid && !triggerSegments.isEmpty()) {
171:                 Vec3 hitPosition = position != null ? position : player.position();
172:                 Vec3 look = player.getLookAngle();
173:                 for (int triggerIndex = 0; triggerIndex < triggerSegments.size(); triggerIndex++) {
174:                     Technique technique = triggerActions.get(triggerIndex);
175:                     float damage = triggerDamages.get(triggerIndex);
176:                     boolean carries = triggerCarries.get(triggerIndex);
177:                     EnchantmentResolution enchantments = triggerEnchantments.get(triggerIndex);
178:                     EnchantBonus bonus = triggerBonuses.get(triggerIndex);
179:                     final int index = triggerIndex;
180:                     Runnable runner = () -> SkillSegmentRunner.buildSegmentRunnerAt(player, technique, techniques, triggerSegments.get(index).actionIndex(), weapon, damage, carries, enchantments, null,
181:                             look, hitPosition,
182:                             SkillChainPlan.lockThisCast(techniques, triggerSegments.get(index).actionIndex()),
183:                             SkillChainPlan.dropThisCast(techniques, segments, triggerSegments.get(index).actionIndex()),
184:                             null, 0, 1, pressed, bonus, 0, null).run();
185:                     if (triggerIndex == 0) {
186:                         runner.run();
187:                     } else {
188:                         ChainScheduler.schedule(player.getServer(), tick, runner);
189:                     }
190:                 }
191:                 CastCooldown.mark(player, weapon, slot, tick + triggerCooldown);
192:             }
193: 
194:             if (!sustainedActions.isEmpty()) {
195:                 Runnable round = () -> {
196:                     for (int sustainedIndex = 0; sustainedIndex < sustainedActions.size(); sustainedIndex++) {
197:                         SkillSegmentRunner.buildSegmentRunner(player, sustainedActions.get(sustainedIndex), techniques, sustainedSegments.get(sustainedIndex).actionIndex(), weapon,
198:                                 sustainedDamages.get(sustainedIndex), sustainedCarries.get(sustainedIndex),
199:                                 sustainedEnchantments.get(sustainedIndex), null,
200:                                 SkillChainPlan.lockThisCast(techniques, sustainedSegments.get(sustainedIndex).actionIndex()), SkillChainPlan.dropThisCast(techniques, segments, sustainedSegments.get(sustainedIndex).actionIndex()),
201:                                 null, 0, 1, pressed, sustainedBonuses.get(sustainedIndex), 0, null).run();
202:                     }
203:                 };
204:                 if (firstPaid) {
205:                     round.run();
206:                 }
207:                 int elapsed = interval;
208:                 while (elapsed < lifetime) {
209:                     int delay = elapsed;
210:                     ChainScheduler.schedule(player.getServer(), tick + delay, () -> {
211:                         int pulseCost = sustainedMana;
212:                         if (!MagicPool.trySpend(player, pulseCost)) {
213:                             return;
214:                         }
215:                         if (pulseCost > 0) {
216:                             MagicPool.showBalance(player);
217:                         }
218:                         round.run();
219:                     });
220:                     elapsed += interval;
221:                 }
222:             }
223:         };
224:     }
225: 
226:     private static float hostedDamage(Skill skill, ChainSegment segment, Technique technique,
227:                                        float weaponBonus, Double attributeScale, EnchantBonus bonus) {
228:         if (segment.enchantIndexes().isEmpty()) {
229:             int magicDamage = SkillSegmentMath.segmentMagicDamage(skill, segment);
230:             return SkillSegmentMath.segmentDamage(technique, segment, weaponBonus, magicDamage, attributeScale);
231:         }
232:         EnchantBonus resolved = bonus == null ? EnchantBonus.NONE : bonus;
233:         return resolved.carriedDamage(weaponBonus);
234:     }
235: }
236: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/CompiledReleaseStep.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.shu.Technique;
4: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
5: import com.bslx.XueXiMod.shu.technique.release.ChainRole;
6: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
7: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
8: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
9: import com.bslx.XueXiMod.skill.Skill;
10: 
11: import java.util.ArrayList;
12: import java.util.List;
13: 
14: /**
15:  * 同一次按键的选段、费用和冷却都读这一份。双重只编译两个终结成员，C 留到下一步。
16:  */
17: final class CompiledReleaseStep {
18:     final int actionSegment;
19:     final int dualSegment;
20:     final List<ChainSegment> members;
21:     final List<Integer> memberShares;
22:     final int ruleShare;
23:     final int actionShare;
24:     final int fullCooldown;
25:     final int actionCooldown;
26:     final boolean actionLed;
27:     final String chargeRefusal;
28: 
29:     private CompiledReleaseStep(int actionSegment, int dualSegment, List<ChainSegment> members,
30:                                 List<Integer> memberShares, int ruleShare, int actionShare,
31:                                 int fullCooldown, int actionCooldown, boolean actionLed, String chargeRefusal) {
32:         this.actionSegment = actionSegment;
33:         this.dualSegment = dualSegment;
34:         this.members = members;
35:         this.memberShares = memberShares;
36:         this.ruleShare = ruleShare;
37:         this.actionShare = actionShare;
38:         this.fullCooldown = fullCooldown;
39:         this.actionCooldown = actionCooldown;
40:         this.actionLed = actionLed;
41:         this.chargeRefusal = chargeRefusal;
42:     }
43: 
44:     static CompiledReleaseStep compile(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
45:                                        double cooldownReduction) {
46:         int dualSegment = -1;
47:         int actionSegment = -1;
48:         for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
49:             Technique technique = techniques.get(segments.get(segmentIndex).actionIndex());
50:             ChainRole role = SkillSegmentMath.roleOf(technique);
51:             if (role == ChainRole.RULE_DUAL && dualSegment < 0) {
52:                 dualSegment = segmentIndex;
53:             }
54:             if (role == ChainRole.ACTION && actionSegment < 0 && dualSegment < 0) {
55:                 actionSegment = segmentIndex;
56:             }
57:         }
58:         if (dualSegment < 0) {
59:             return actionConditional(skill, techniques, segments, cooldownReduction, actionSegment);
60:         }
61:         List<ChainSegment> released = SkillChainPlan.releasedSegments(segments, dualSegment, techniques);
62:         List<ChainSegment> members = new ArrayList<>();
63:         for (int memberIndex = 0; memberIndex < released.size() && members.size() < 2; memberIndex++) {
64:             members.add(released.get(memberIndex));
65:         }
66:         List<Integer> memberShares = new ArrayList<>();
67:         for (ChainSegment member : members) {
68:             memberShares.add(memberShare(techniques, segments, member));
69:         }
70:         int ruleShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(dualSegment));
71:         int actionShare = actionSegment < 0
72:                 ? 0
73:                 : SkillSegmentMath.segmentManaCost(techniques, segments.get(actionSegment));
74:         List<Integer> everyIndex = new ArrayList<>();
75:         for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
76:             everyIndex.add(techniqueIndex);
77:         }
78:         int fullCooldown = ChainSteps.cooldown(skill, everyIndex, cooldownReduction);
79:         int actionCooldown = 0;
80:         if (actionSegment >= 0) {
81:             ChainSegment action = segments.get(actionSegment);
82:             List<Integer> actionIndexes = new ArrayList<>();
83:             actionIndexes.add(action.actionIndex());
84:             actionIndexes.addAll(action.enchantIndexes());
85:             actionCooldown = ChainSteps.cooldown(skill, actionIndexes, cooldownReduction);
86:         }
87:         String chargeRefusal = chargeRefusal(techniques, members);
88:         return new CompiledReleaseStep(actionSegment, dualSegment, members, memberShares, ruleShare, actionShare,
89:                 fullCooldown, actionCooldown, actionSegment >= 0, chargeRefusal);
90:     }
91: 
92:     /**
93:      * 没有双重、动作后面只有一个条件释放时，沿用动作会话。
94:      * 前面或后面还有别的可释放段时返回空，留给原来的按键路径。
95:      */
96:     private static CompiledReleaseStep actionConditional(Skill skill, List<Technique> techniques,
97:                                                          List<ChainSegment> segments, double cooldownReduction,
98:                                                          int actionSegment) {
99:         if (actionSegment < 0) {
100:             return null;
101:         }
102:         for (int earlierIndex = 0; earlierIndex < actionSegment; earlierIndex++) {
103:             Technique earlier = techniques.get(segments.get(earlierIndex).actionIndex());
104:             if (SkillSegmentMath.isRemovedRule(earlier.getId()) || SkillSegmentMath.isChainModifier(earlier)) {
105:                 continue;
106:             }
107:             return null;
108:         }
109:         ChainSegment conditionalMember = null;
110:         for (int segmentIndex = actionSegment + 1; segmentIndex < segments.size(); segmentIndex++) {
111:             ChainSegment candidate = segments.get(segmentIndex);
112:             Technique technique = techniques.get(candidate.actionIndex());
113:             if (SkillSegmentMath.isRemovedRule(technique.getId()) || SkillSegmentMath.isChainModifier(technique)) {
114:                 continue;
115:             }
116:             ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
117:             if (technique.getBaseDamage() > 0f || definition == null || definition.conditionalEffect() == null) {
118:                 return null;
119:             }
120:             if (conditionalMember != null) {
121:                 return null;
122:             }
123:             conditionalMember = candidate;
124:         }
125:         if (conditionalMember == null) {
126:             return null;
127:         }
128:         List<ChainSegment> members = new ArrayList<>();
129:         members.add(conditionalMember);
130:         List<Integer> memberShares = new ArrayList<>();
131:         memberShares.add(memberShare(techniques, segments, conditionalMember));
132:         int actionShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(actionSegment));
133:         List<Integer> everyIndex = new ArrayList<>();
134:         for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
135:             everyIndex.add(techniqueIndex);
136:         }
137:         int fullCooldown = ChainSteps.cooldown(skill, everyIndex, cooldownReduction);
138:         ChainSegment action = segments.get(actionSegment);
139:         List<Integer> actionIndexes = new ArrayList<>();
140:         actionIndexes.add(action.actionIndex());
141:         actionIndexes.addAll(action.enchantIndexes());
142:         int actionCooldown = ChainSteps.cooldown(skill, actionIndexes, cooldownReduction);
143:         String refusal = chargeRefusal(techniques, members);
144:         return new CompiledReleaseStep(actionSegment, -1, members, memberShares, 0, actionShare,
145:                 fullCooldown, actionCooldown, true, refusal);
146:     }
147: 
148:     private static String chargeRefusal(List<Technique> techniques, List<ChainSegment> members) {
149:         for (ChainSegment member : members) {
150:             Technique technique = techniques.get(member.actionIndex());
151:             ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
152:             if (definition != null && definition.chargeTicks() > 0) {
153:                 return "这次组合里有还不能按时提交的蓄力，没有释放";
154:             }
155:         }
156:         return null;
157:     }
158: 
159:     int groupBill() {
160:         int bill = Math.max(0, ruleShare);
161:         for (int share : memberShares) {
162:             bill += Math.max(0, share);
163:         }
164:         return bill;
165:     }
166: 
167:     private static int memberShare(List<Technique> techniques, List<ChainSegment> segments, ChainSegment member) {
168:         int memberIndex = segments.indexOf(member);
169:         Technique technique = techniques.get(member.actionIndex());
170:         int copies = Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, memberIndex));
171:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
172:         int enchantAndHost = SkillSegmentMath.segmentManaCost(techniques, member);
173:         if (definition != null && technique.getBaseDamage() <= 0f && definition.domain() != null) {
174:             return enchantAndHost + definition.domain().initialCost() * copies;
175:         }
176:         if (definition != null && technique.getBaseDamage() <= 0f && definition.barrier() != null) {
177:             return enchantAndHost + definition.barrier().spawnCost() * copies;
178:         }
179:         if (definition != null && technique.getBaseDamage() <= 0f && definition.guard() != null) {
180:             return enchantAndHost + definition.guard().spawnCost() * copies;
181:         }
182:         if (technique.getBaseDamage() <= 0f && ConditionalEffects.copiesCoalesce(definition)) {
183:             return enchantAndHost;
184:         }
185:         int share = enchantAndHost * copies;
186:         if (definition != null && definition.fieldRadius() > 0f && definition.fieldSpawnCost() > 0) {
187:             share += definition.fieldSpawnCost() * copies;
188:         }
189:         return share;
190:     }
191: }
192: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/network/CastExecutionSession.java">
1: package com.bslx.XueXiMod.network;
2: 
3: import com.bslx.XueXiMod.debug.DebugLog;
4: import com.bslx.XueXiMod.player.ManaPaymentReceipt;
5: import com.bslx.XueXiMod.shu.Technique;
6: import com.bslx.XueXiMod.shu.technique.ActionWindow;
7: import com.bslx.XueXiMod.shu.technique.EnchantmentResolution;
8: import com.bslx.XueXiMod.shu.technique.chain.ChainScheduler;
9: import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
10: import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
11: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
12: import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
13: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
14: import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
15: import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalIntent;
16: import com.bslx.XueXiMod.shu.technique.release.entity.HostCast;
17: import com.bslx.XueXiMod.shu.technique.release.water.CastSource;
18: import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupIds;
19: import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService;
20: import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
21: import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
22: import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
23: import com.bslx.XueXiMod.shu.technique.release.momentum.ChargeGate;
24: import com.bslx.XueXiMod.skill.Skill;
25: import net.minecraft.network.chat.Component;
26: import net.minecraft.server.level.ServerLevel;
27: import net.minecraft.server.level.ServerPlayer;
28: import net.minecraft.world.entity.Entity;
29: import net.minecraft.world.entity.LivingEntity;
30: import net.minecraft.world.item.ItemStack;
31: import net.minecraft.world.phys.Vec3;
32: 
33: import java.util.ArrayList;
34: import java.util.HashSet;
35: import java.util.List;
36: import java.util.Set;
37: import java.util.UUID;
38: import java.util.concurrent.ConcurrentHashMap;
39: 
40: /**
41:  * 双重释放的父会话。执行锁和正式冷却分开。游标和冷却只在终结时各做一次。
42:  */
43: public final class CastExecutionSession implements ActionWindow.Sink {
44:     static final int STEP_GAP = 5;
45: 
46:     private static final ConcurrentHashMap<String, CastExecutionSession> OPEN = new ConcurrentHashMap<>();
47: 
48:     private final ServerPlayer owner;
49:     private final ItemStack carrier;
50:     private final int slot;
51:     private final CastSource castSource;
52:     private final String carrierKey;
53:     private final Skill skill;
54:     private final List<Technique> techniques;
55:     private final List<ChainSegment> segments;
56:     private final CompiledReleaseStep plan;
57:     private final float weaponBonus;
58:     private final String signature;
59:     private final int stepCount;
60:     private final ServerLevel castLevel;
61:     private final int castEpoch;
62:     private final Set<UUID> processedTargets = new HashSet<>();
63:     private final List<Group> groups = new ArrayList<>();
64: 
65:     private ManaPaymentReceipt actionReceipt;
66:     private boolean accepted;
67:     private boolean actionEnded;
68:     private boolean legalCollision;
69:     private boolean terminalAlready;
70:     private boolean closed;
71:     private boolean reserveFailed;
72:     private int pending;
73:     private HostCast hostCast;
74:     private final Set<String> conditionalUsage = new HashSet<>();
75: 
76:     private CastExecutionSession(ServerPlayer owner, ItemStack carrier, int slot, CastSource castSource, String carrierKey, Skill skill,
77:                                  List<Technique> techniques, List<ChainSegment> segments, CompiledReleaseStep plan,
78:                                  float weaponBonus, String signature, int stepCount) {
79:         this.owner = owner;
80:         this.carrier = carrier;
81:         this.slot = slot;
82:         this.castSource = castSource;
83:         this.carrierKey = carrierKey;
84:         this.skill = skill;
85:         this.techniques = techniques;
86:         this.segments = segments;
87:         this.plan = plan;
88:         this.weaponBonus = weaponBonus;
89:         this.signature = signature;
90:         this.stepCount = stepCount;
91:         this.castLevel = owner.serverLevel();
92:         this.castEpoch = ChargeGate.epoch(owner.getUUID());
93:     }
94: 
95:     static CastTiming open(ServerPlayer owner, ItemStack carrier, int slot, CastSource castSource, Skill skill, List<Technique> techniques,
96:                            List<ChainSegment> segments, CompiledReleaseStep plan, float weaponBonus,
97:                            String signature, int stepCount) {
98:         if (plan.members.isEmpty()) {
99:             owner.sendSystemMessage(Component.literal("§c双重释放后面没有可放的术式，这次没有释放"));
100:             return CastTiming.rejected();
101:         }
102:         String carrierKey = CastCooldown.carrierKey(owner, carrier, slot);
103:         if (!CastCooldown.acquire(carrierKey)) {
104:             return CastTiming.rejected();
105:         }
106:         CastExecutionSession session = new CastExecutionSession(owner, carrier, slot, castSource, carrierKey, skill, techniques,
107:                 segments, plan, weaponBonus, signature, stepCount);
108:         OPEN.put(carrierKey, session);
109:         if (plan.actionLed) {
110:             return session.startAction();
111:         }
112:         return session.startOrdinary();
113:     }
114: 
115:     public static void cancelOwned(ServerPlayer owner) {
116:         if (owner == null) {
117:             return;
118:         }
119:         List<CastExecutionSession> living = new ArrayList<>(OPEN.values());
120:         for (CastExecutionSession session : living) {
121:             if (owner.getUUID().equals(session.owner.getUUID())) {
122:                 session.cancel();
123:             }
124:         }
125:     }
126: 
127:     public static void cancelAll() {
128:         List<CastExecutionSession> living = new ArrayList<>(OPEN.values());
129:         for (CastExecutionSession session : living) {
130:             session.cancel();
131:         }
132:     }
133: 
134:     private CastTiming startAction() {
135:         actionReceipt = ManaPaymentReceipt.charge(owner, plan.actionShare, true);
136:         if (actionReceipt == null) {
137:             dropUnaccepted();
138:             return CastTiming.insufficient();
139:         }
140:         ChainSegment action = segments.get(plan.actionSegment);
141:         Technique technique = techniques.get(action.actionIndex());
142:         if (!SkillSegmentRunner.playAction(owner, technique, techniques, action.actionIndex(), carrier, this)) {
143:             dropUnaccepted();
144:             owner.sendSystemMessage(Component.literal("§c动作没有开始，这次没有释放"));
145:             return CastTiming.rejected();
146:         }
147:         accepted = true;
148:         return CastTiming.deferred(owner.getServer().getTickCount(), proficiencyTechniques(techniques));
149:     }
150: 
151:     private CastTiming startOrdinary() {
152:         int nowTick = owner.getServer().getTickCount();
153:         UUID groupId = UUID.randomUUID();
154:         ActionContact contact = new ActionContact(owner.getUUID(), nowTick, owner.position(), owner.getLookAngle(), groupId);
155:         beginGroup(contact);
156:         if (!accepted) {
157:             boolean unpaid = reserveFailed;
158:             dropUnaccepted();
159:             return unpaid ? CastTiming.insufficient() : CastTiming.rejected();
160:         }
161:         return CastTiming.deferred(nowTick, proficiencyTechniques(techniques));
162:     }
163: 
164:     @Override
165:     public void onContact(UUID targetId, Vec3 point, Vec3 direction) {
166:         if (closed || terminalAlready || !plan.actionLed || actionEnded) {
167:             return;
168:         }
169:         Entity found = castLevel.getEntity(targetId);
170:         if (!(found instanceof LivingEntity living) || !ActionWindow.hostileTo(owner, living)) {
171:             return;
172:         }
173:         if (!processedTargets.add(targetId)) {
174:             return;
175:         }
176:         legalCollision = true;
177:         Vec3 aimed = direction == null || direction.lengthSqr() < 1.0E-8 ? owner.getLookAngle() : direction.normalize();
178:         Vec3 anchor = point == null ? living.position() : point;
179:         beginGroup(new ActionContact(targetId, owner.getServer().getTickCount(), anchor, aimed, UUID.randomUUID()));
180:     }
181: 
182:     @Override
183:     public void onEnded() {
184:         if (actionEnded || terminalAlready) {
185:             return;
186:         }
187:         actionEnded = true;
188:         tryFinish();
189:     }
190: 
191:     @Override
192:     public boolean closed() {
193:         return closed || terminalAlready;
194:     }
195: 
196:     private void cancel() {
197:         if (terminalAlready) {
198:             return;
199:         }
200:         closed = true;
201:         if (!accepted) {
202:             dropUnaccepted();
203:             return;
204:         }
205:         actionEnded = true;
206:         List<Group> pendingGroups = new ArrayList<>(groups);
207:         for (Group group : pendingGroups) {
208:             finishGroup(group);
209:         }
210:         tryFinish();
211:         closeHostLedger();
212:     }
213: 
214:     private boolean beginGroup(ActionContact contact) {
215:         if (closed || terminalAlready) {
216:             return false;
217:         }
218:         Group group = new Group(contact);
219:         groups.add(group);
220:         pending++;
221:         GroupBudget budget = GroupBudget.reserve(owner, plan.groupBill());
222:         if (budget == null) {
223:             reserveFailed = true;
224:             finishGroup(group);
225:             return false;
226:         }
227:         group.budget = budget;
228:         group.batch = ReleaseBatch.open(owner, skill);
229:         ReleaseBatch.unbind();
230:         ReleaseOutcome first = commitMember(group, 0);
231:         if (!first.committed()) {
232:             if (!first.reason().isEmpty()) {
233:                 owner.sendSystemMessage(Component.literal("§c" + first.reason()));
234:             }
235:             finishGroup(group);
236:             return plan.actionLed && accepted;
237:         }
238:         budget.commit(plan.ruleShare + plan.memberShares.get(0));
239:         accepted = true;
240:         if (!plan.actionLed) {
241:             legalCollision = true;
242:             actionEnded = true;
243:         }
244:         if (plan.members.size() < 2) {
245:             finishGroup(group);
246:             return true;
247:         }
248:         int secondTick = first.tick() + STEP_GAP;
249:         ChainScheduler.schedule(owner.getServer(), secondTick, () -> runSecond(group));
250:         return true;
251:     }
252: 
253:     private void runSecond(Group group) {
254:         if (group.closed || terminalAlready) {
255:             return;
256:         }
257:         if (closed || !continues()) {
258:             finishGroup(group);
259:             return;
260:         }
261:         ReleaseOutcome second = commitMember(group, 1);
262:         if (second.committed() && group.budget != null) {
263:             group.budget.commit(plan.memberShares.get(1));
264:         }
265:         finishGroup(group);
266:     }
267: 
268:     private ReleaseOutcome commitMember(Group group, int memberIndex) {
269:         int nowTick = owner.getServer() == null ? 0 : owner.getServer().getTickCount();
270:         if (!continues()) {
271:             return ReleaseOutcome.cancelled(nowTick, "施法已经中断");
272:         }
273:         ChainSegment member = plan.members.get(memberIndex);
274:         Technique technique = techniques.get(member.actionIndex());
275:         Vec3 point = group.contact.point();
276:         Vec3 look = group.contact.direction();
277:         if (technique.isEnchantment()) {
278:             SkillChainPlan.imprintSelection(owner, skill, List.of(member.actionIndex()), 1);
279:             return ReleaseOutcome.committed(nowTick);
280:         }
281:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
282:         int copies = Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, segments.indexOf(member)));
283:         EnchantmentSnapshot snapshot = ContactCast.snapshot(skill, member, owner);
284:         String claimSuffix = group.contact.groupId().toString();
285:         if (definition != null && technique.getBaseDamage() <= 0f && definition.conditionalEffect() != null) {
286:             HostCast parentCast = hostLedger();
287:             UUID operationId = ConditionalIntent.operationId(parentCast.tokenId(), memberIndex, 0, group.contact.groupId());
288:             ReleaseOutcome outcome = SkillSegmentRunner.commitConditional(owner, definition, operationId, snapshot);
289:             if (outcome.committed()) {
290:                 recordConditionalUse(technique);
291:             }
292:             return outcome;
293:         }
294:         if (definition != null && technique.getBaseDamage() <= 0f && definition.domain() != null) {
295:             boolean placed = DomainService.tryWaveReserved(owner, technique, definition.domain(), copies, snapshot, point, claimSuffix);
296:             return placed ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
297:         }
298:         if (definition != null && technique.getBaseDamage() <= 0f && definition.barrier() != null) {
299:             boolean placed = BarrierService.tryPlaceReserved(owner, definition.barrier(), copies, snapshot, point, look, claimSuffix);
300:             return placed ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
301:         }
302:         if (definition != null && technique.getBaseDamage() <= 0f && definition.guard() != null) {
303:             long waveKey = group.contact.groupId().getLeastSignificantBits() ^ member.actionIndex();
304:             boolean placed = GuardStates.activateReserved(owner, definition.guard(), waveKey);
305:             if (placed && snapshot != null) {
306:                 GuardStates.bindContact(owner, snapshot);
307:             }
308:             return placed ? ReleaseOutcome.committed(nowTick) : ReleaseOutcome.rejected(nowTick, "");
309:         }
310:         ReleaseBatch previous = ReleaseBatch.current();
311:         ReleaseBatch.bind(group.batch);
312:         try {
313:             if (definition != null && definition.submitsHostGroup()) {
314:                 ReleaseBatch.Tally tally = copies > 1 ? group.batch.openGroupTally() : null;
315:                 HostCast parentCast = hostLedger();
316:                 UUID hostGroup = HostGroupIds.sessionHost(group.contact.groupId(), memberIndex);
317:                 EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, member);
318:                 EnchantBonus bonus = EnchantBonus.resolve(skill, member.enchantIndexes(), technique.getBaseDamage(), 1);
319:                 float damage = member.enchantIndexes().isEmpty()
320:                         ? SkillSegmentMath.segmentDamage(technique, member, weaponBonus, SkillSegmentMath.segmentMagicDamage(skill, member), null)
321:                         : bonus.carriedDamage(weaponBonus);
322:                 boolean fieldHost = definition.fieldRadius() > 0f;
323:                 boolean carries = !fieldHost && technique.getBaseDamage() > 0f && !member.enchantIndexes().isEmpty();
324:                 ReleaseOutcome outcome = SkillSegmentRunner.commitHostGroup(owner, technique, techniques, member.actionIndex(), carrier,
325:                         damage, carries, enchantments,
326:                         SkillChainPlan.lockThisCast(techniques, member.actionIndex()),
327:                         SkillChainPlan.dropThisCast(techniques, segments, member.actionIndex()),
328:                         tally, copies, group.batch, bonus, memberIndex, look, point, point, look, parentCast, hostGroup, null);
329:                 if (outcome.committed() && fieldHost && snapshot != null) {
330:                     FieldZones.bindBorn(owner.getUUID(), snapshot, outcome.tick());
331:                 }
332:                 return outcome;
333:             }
334:             ReleaseBatch.Tally tally = copies > 1 ? group.batch.openGroupTally() : null;
335:             EnchantmentResolution enchantments = SkillSegmentMath.resolveSegmentEnchantments(skill, member);
336:             EnchantBonus bonus = EnchantBonus.resolve(skill, member.enchantIndexes(), technique.getBaseDamage(), 1);
337:             float damage = member.enchantIndexes().isEmpty()
338:                     ? SkillSegmentMath.segmentDamage(technique, member, weaponBonus, SkillSegmentMath.segmentMagicDamage(skill, member), null)
339:                     : bonus.carriedDamage(weaponBonus);
340:             boolean fieldHost = definition != null && definition.fieldRadius() > 0f;
341:             boolean carries = !fieldHost && technique.getBaseDamage() > 0f && !member.enchantIndexes().isEmpty();
342:             boolean anyCommitted = false;
343:             int commitTick = nowTick;
344:             for (int copyIndex = 0; copyIndex < copies; copyIndex++) {
345:                 ReleaseOutcome outcome = SkillSegmentRunner.commitNow(owner, technique, techniques, member.actionIndex(), carrier,
346:                         damage, carries, enchantments,
347:                         SkillChainPlan.lockThisCast(techniques, member.actionIndex()),
348:                         SkillChainPlan.dropThisCast(techniques, segments, member.actionIndex()),
349:                         tally, copyIndex, copies, group.batch, bonus, memberIndex, look, point);
350:                 if (outcome.committed()) {
351:                     anyCommitted = true;
352:                     commitTick = outcome.tick();
353:                 }
354:             }
355:             if (!anyCommitted) {
356:                 return ReleaseOutcome.rejected(nowTick, "没有可提交的释放");
357:             }
358:             if (fieldHost && snapshot != null) {
359:                 FieldZones.bindBorn(owner.getUUID(), snapshot, commitTick);
360:             }
361:             return ReleaseOutcome.committed(commitTick);
362:         } finally {
363:             ReleaseBatch.bind(previous);
364:         }
365:     }
366: 
367:     private void finishGroup(Group group) {
368:         if (group.closed) {
369:             return;
370:         }
371:         group.closed = true;
372:         if (group.budget != null) {
373:             group.budget.settle(owner);
374:         }
375:         pending = Math.max(0, pending - 1);
376:         tryFinish();
377:     }
378: 
379:     private void tryFinish() {
380:         if (!actionEnded || pending != 0 || terminalAlready || !accepted) {
381:             return;
382:         }
383:         terminalAlready = true;
384:         closed = true;
385:         int nowTick = owner.getServer().getTickCount();
386:         int cooldown = legalCollision ? plan.fullCooldown : plan.actionCooldown;
387:         int readyTick = nowTick + Math.max(cooldown, 1);
388:         CastCooldown.mark(owner, carrier, slot, readyTick);
389:         if (stepCount > 0) {
390:             ChainProgress.advance(owner, carrier, slot, signature, stepCount);
391:         }
392:         CastCooldown.release(carrierKey);
393:         OPEN.remove(carrierKey);
394:         closeHostLedger();
395:         DebugLog.log("双重释放结算 碰撞=" + legalCollision + " 冷却=" + cooldown + " 重放=" + readyTick);
396:     }
397: 
398:     private void dropUnaccepted() {
399:         if (terminalAlready) {
400:             return;
401:         }
402:         terminalAlready = true;
403:         closed = true;
404:         for (Group group : groups) {
405:             if (!group.closed && group.budget != null) {
406:                 group.budget.settle(owner);
407:             }
408:             group.closed = true;
409:         }
410:         if (actionReceipt != null) {
411:             actionReceipt.refund(owner);
412:         }
413:         CastCooldown.release(carrierKey);
414:         OPEN.remove(carrierKey);
415:         closeHostLedger();
416:     }
417: 
418:     private HostCast hostLedger() {
419:         if (hostCast == null) {
420:             hostCast = HostCast.open(owner.getUUID(), castSource);
421:         }
422:         return hostCast;
423:     }
424: 
425:     private void closeHostLedger() {
426:         HostCast current = hostCast;
427:         if (current != null) {
428:             current.close();
429:         }
430:     }
431: 
432:     private boolean continues() {
433:         return owner.isAlive() && owner.level() == castLevel && ChargeGate.same(owner.getUUID(), castEpoch);
434:     }
435: 
436:     private void recordConditionalUse(Technique technique) {
437:         SkillUsePacket.recordTechniqueUse(owner, technique, conditionalUsage);
438:     }
439: 
440:     private static boolean isConditionalTechnique(Technique technique) {
441:         if (technique == null || technique.getBaseDamage() > 0f) {
442:             return false;
443:         }
444:         ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
445:         return definition != null && definition.conditionalEffect() != null;
446:     }
447: 
448:     /** 没有条件术式时原样交回，延后释放的熟练度名单不变。 */
449:     private static List<Technique> proficiencyTechniques(List<Technique> techniques) {
450:         boolean conditionalPresent = false;
451:         for (Technique technique : techniques) {
452:             if (isConditionalTechnique(technique)) {
453:                 conditionalPresent = true;
454:                 break;
455:             }
456:         }
457:         if (!conditionalPresent) {
458:             return techniques;
459:         }
460:         List<Technique> visible = new ArrayList<>();
461:         for (Technique technique : techniques) {
462:             if (!isConditionalTechnique(technique)) {
463:                 visible.add(technique);
464:             }
465:         }
466:         return visible;
467:     }
468: 
469:     private static final class Group {
470:         final ActionContact contact;
471:         GroupBudget budget;
472:         ReleaseBatch batch;
473:         boolean closed;
474: 
475:         Group(ActionContact contact) {
476:             this.contact = contact;
477:         }
478:     }
479: 
480:     private static final class GroupBudget {
481:         final ManaPaymentReceipt receipt;
482:         int openShare;
483:         boolean settled;
484: 
485:         GroupBudget(ManaPaymentReceipt receipt, int openShare) {
486:             this.receipt = receipt;
487:             this.openShare = openShare;
488:         }
489: 
490:         static GroupBudget reserve(ServerPlayer owner, int cost) {
491:             ManaPaymentReceipt receipt = ManaPaymentReceipt.charge(owner, Math.max(0, cost), true);
492:             if (receipt == null) {
493:                 return null;
494:             }
495:             return new GroupBudget(receipt, Math.max(0, cost));
496:         }
497: 
498:         void commit(int share) {
499:             if (settled || share <= 0) {
500:                 return;
501:             }
502:             int keeping = Math.min(share, openShare);
503:             openShare -= keeping;
504:             if (keeping > 0 && receipt != null) {
505:                 receipt.retainShare(keeping);
506:             }
507:         }
508: 
509:         void settle(ServerPlayer owner) {
510:             if (settled) {
511:                 return;
512:             }
513:             settled = true;
514:             receipt.refundShare(owner, openShare);
515:             openShare = 0;
516:         }
517:     }
518: }
519: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/ConditionalEffect.java">
1: package com.bslx.XueXiMod.shu.technique.release.conditional;
2: 
3: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
4: import net.minecraft.server.level.ServerPlayer;
5: 
6: import java.util.UUID;
7: 
8: /**
9:  * 零伤条件释放。旧术式不实现它，释放定义默认没有。
10:  * 成功只认 {@link #attempt} 的提交结果，不靠正伤害、领域、护体或命中实体凑回执。
11:  */
12: public interface ConditionalEffect {
13:     ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact);
14: 
15:     /** 同一意图的复制份只尝试一次，基费也不乘份数。默认关闭。 */
16:     default boolean copiesCoalesce() {
17:         return false;
18:     }
19: 
20:     /** 零伤宿主可以承接有接触伤害的附魔。默认关闭。 */
21:     default boolean supportsContact() {
22:         return false;
23:     }
24: }
25: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/ConditionalIntent.java">
1: package com.bslx.XueXiMod.shu.technique.release.conditional;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.entity.HostGroupIds;
4: 
5: import java.util.UUID;
6: 
7: /**
8:  * 同一次父释放里的条件意图。
9:  * 父令牌、成员、波次和动作接触决定身份，复制序号不进入编号。
10:  */
11: public final class ConditionalIntent {
12:     private ConditionalIntent() {
13:     }
14: 
15:     public static UUID operationId(UUID castToken, int memberIndex, int volley, UUID contactId) {
16:         if (castToken == null) {
17:             return null;
18:         }
19:         UUID castScoped = HostGroupIds.castHost(castToken, memberIndex, volley);
20:         if (contactId == null) {
21:             return castScoped;
22:         }
23:         return new UUID(
24:                 castScoped.getMostSignificantBits() ^ contactId.getMostSignificantBits(),
25:                 castScoped.getLeastSignificantBits() ^ contactId.getLeastSignificantBits());
26:     }
27: }
28: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/ConditionalEffects.java">
1: package com.bslx.XueXiMod.shu.technique.release.conditional;
2: 
3: import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
4: 
5: /** 读取条件释放能力。没有该能力时两个开关都是关。 */
6: public final class ConditionalEffects {
7:     private ConditionalEffects() {
8:     }
9: 
10:     public static boolean copiesCoalesce(ReleaseDefinition definition) {
11:         ConditionalEffect effect = definition == null ? null : definition.conditionalEffect();
12:         return effect != null && effect.copiesCoalesce();
13:     }
14: 
15:     public static boolean supportsContact(ReleaseDefinition definition) {
16:         ConditionalEffect effect = definition == null ? null : definition.conditionalEffect();
17:         return effect != null && effect.supportsContact();
18:     }
19: }
20: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/conditional/WaterPrisonEffect.java">
1: package com.bslx.XueXiMod.shu.technique.release.conditional;
2: 
3: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
4: import com.bslx.XueXiMod.shu.technique.release.prison.PrisonCommitResult;
5: import com.bslx.XueXiMod.shu.technique.release.prison.PrisonReservation;
6: import com.bslx.XueXiMod.shu.technique.release.prison.PrisonService;
7: import net.minecraft.server.level.ServerPlayer;
8: import net.minecraft.world.entity.LivingEntity;
9: 
10: import java.util.UUID;
11: 
12: /**
13:  * 水牢只适配牢服务的选择、准备和提交。牢的状态、禁锢和结算不在这里。
14:  * 目标始终是施术者准星上的单个潮湿生物，不用动作撞到的那一个代替。
15:  */
16: public final class WaterPrisonEffect implements ConditionalEffect {
17:     public static final WaterPrisonEffect INSTANCE = new WaterPrisonEffect();
18:     private static final double TARGET_RANGE = 20.0D;
19: 
20:     private WaterPrisonEffect() {
21:     }
22: 
23:     @Override
24:     public boolean copiesCoalesce() {
25:         return true;
26:     }
27: 
28:     @Override
29:     public boolean supportsContact() {
30:         return true;
31:     }
32: 
33:     @Override
34:     public ConditionalResult attempt(ServerPlayer caster, UUID operationId, EnchantmentSnapshot contact) {
35:         if (caster == null || operationId == null) {
36:             return ConditionalResult.rejected("没有可禁锢的潮湿目标");
37:         }
38:         LivingEntity target = PrisonService.selectTarget(caster, TARGET_RANGE);
39:         if (target == null) {
40:             return ConditionalResult.rejected("没有可禁锢的潮湿目标");
41:         }
42:         PrisonReservation reservation = PrisonService.prepare(caster, target, operationId);
43:         if (reservation == null) {
44:             return ConditionalResult.rejected("水牢没有成立");
45:         }
46:         boolean established = false;
47:         String because = "";
48:         try {
49:             if (!reservation.acquired()) {
50:                 because = reservation.reason();
51:                 return ConditionalResult.rejected(because == null || because.isEmpty() ? "水牢没有成立" : because);
52:             }
53:             PrisonCommitResult committed = reservation.commit(contact);
54:             established = committed != null && committed.committed();
55:             if (!established) {
56:                 because = committed == null ? "" : committed.reason();
57:             }
58:         } finally {
59:             reservation.close();
60:         }
61:         if (established) {
62:             return ConditionalResult.committed();
63:         }
64:         return ConditionalResult.rejected(because == null || because.isEmpty() ? "水牢没有成立" : because);
65:     }
66: }
67: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonService.java">
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
4: import net.minecraft.server.level.ServerPlayer;
5: import net.minecraft.world.entity.LivingEntity;
6: 
7: import java.util.Optional;
8: import java.util.UUID;
9: 
10: /** 水牢服务端入口。不引用客户端类型，也不扣魔力。 */
11: public final class PrisonService {
12:     private PrisonService() {
13:     }
14: 
15:     public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId) {
16:         return PrisonRuntime.prepare(caster, target, operationId);
17:     }
18: 
19:     public static Optional<PrisonView> view(UUID targetId) {
20:         return PrisonRuntime.view(targetId);
21:     }
22: 
23:     public static boolean end(UUID prisonId, EndReason reason) {
24:         return PrisonRuntime.end(prisonId, reason);
25:     }
26: 
27:     public static boolean suppressesEnhancedSuffocation(LivingEntity entity) {
28:         return PrisonRuntime.suppressesEnhancedSuffocation(entity);
29:     }
30: 
31:     public static LivingEntity selectTarget(ServerPlayer caster, double range) {
32:         return PrisonAim.select(caster, range);
33:     }
34: 
35:     public static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact) {
36:         if (reservation == null) {
37:             return PrisonCommitResult.fail("missing");
38:         }
39:         return reservation.commit(contact);
40:     }
41: }
42: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonReservation.java">
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
4: import net.minecraft.server.level.ServerPlayer;
5: import net.minecraft.world.entity.LivingEntity;
6: 
7: import java.util.UUID;
8: 
9: /** 一次释放会话里的目标占用。未提交时 close 放人；已提交时 close 不破牢。 */
10: public final class PrisonReservation implements AutoCloseable {
11:     private final UUID operationId;
12:     private final ServerPlayer caster;
13:     private final LivingEntity target;
14:     private final boolean acquired;
15:     private final String reason;
16:     private boolean closed;
17:     private boolean committed;
18:     private PrisonCommitResult outcome;
19: 
20:     PrisonReservation(UUID operationId, ServerPlayer caster, LivingEntity target, boolean acquired, String reason) {
21:         this.operationId = operationId;
22:         this.caster = caster;
23:         this.target = target;
24:         this.acquired = acquired;
25:         this.reason = reason == null ? "" : reason;
26:     }
27: 
28:     public boolean acquired() {
29:         return this.acquired;
30:     }
31: 
32:     public String reason() {
33:         return this.reason;
34:     }
35: 
36:     public PrisonCommitResult commit(EnchantmentSnapshot contact) {
37:         if (this.outcome != null) {
38:             return this.outcome;
39:         }
40:         if (!this.acquired) {
41:             this.outcome = PrisonCommitResult.fail(this.reason.isEmpty() ? "rejected" : this.reason);
42:             return this.outcome;
43:         }
44:         if (this.closed) {
45:             this.outcome = PrisonCommitResult.fail("closed");
46:             return this.outcome;
47:         }
48:         this.outcome = PrisonRuntime.commit(this, contact);
49:         return this.outcome;
50:     }
51: 
52:     @Override
53:     public void close() {
54:         if (this.closed) {
55:             return;
56:         }
57:         this.closed = true;
58:         PrisonRuntime.close(this);
59:     }
60: 
61:     UUID operationId() {
62:         return this.operationId;
63:     }
64: 
65:     ServerPlayer caster() {
66:         return this.caster;
67:     }
68: 
69:     LivingEntity target() {
70:         return this.target;
71:     }
72: 
73:     boolean wasCommitted() {
74:         return this.committed;
75:     }
76: 
77:     void markCommitted() {
78:         this.committed = true;
79:     }
80: }
81: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonRuntime.java">
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import com.bslx.XueXiMod.registry.ModMobEffects;
4: import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
5: import com.bslx.XueXiMod.shu.technique.release.contact.ContactCarrier;
6: import com.bslx.XueXiMod.shu.technique.release.contact.ContactEnchantState;
7: import com.bslx.XueXiMod.status.WetnessState;
8: import com.mojang.logging.LogUtils;
9: import net.minecraft.server.MinecraftServer;
10: import net.minecraft.server.level.ServerLevel;
11: import net.minecraft.server.level.ServerPlayer;
12: import net.minecraft.world.effect.MobEffectInstance;
13: import net.minecraft.world.entity.Entity;
14: import net.minecraft.world.entity.LivingEntity;
15: import net.minecraft.world.phys.AABB;
16: import net.minecraft.world.phys.Vec3;
17: import org.slf4j.Logger;
18: 
19: import java.util.ArrayList;
20: import java.util.HashMap;
21: import java.util.IdentityHashMap;
22: import java.util.Map;
23: import java.util.Optional;
24: import java.util.UUID;
25: 
26: /** 占用、提交、结束和耗湿。操作号只留在本次预约里，关闭后丢掉。 */
27: public final class PrisonRuntime {
28:     private static final Logger LOGGER = LogUtils.getLogger();
29:     private static final int SHORT_TICKS = 20;
30:     private static final int LONG_TICKS = 100;
31:     private static final long REVISION = 1L;
32: 
33:     private static final Map<UUID, PrisonReservation> SESSIONS = new HashMap<>();
34:     private static final Map<UUID, UUID> PREPARED_TARGETS = new HashMap<>();
35:     private static final Map<UUID, PrisonRecord> PRISONS = new HashMap<>();
36:     private static final Map<UUID, UUID> PRISON_BY_TARGET = new HashMap<>();
37:     private static final IdentityHashMap<LivingEntity, PrisonRecord> BY_ENTITY = new IdentityHashMap<>();
38: 
39:     private PrisonRuntime() {
40:     }
41: 
42:     public static PrisonReservation prepare(ServerPlayer caster, LivingEntity target, UUID operationId) {
43:         if (operationId == null || caster == null || target == null) {
44:             return rejected(null, caster, target, "missing");
45:         }
46:         if (!target.level().isClientSide()) {
47:             PrisonWetDue.settleLoaded(target);
48:         }
49:         PrisonReservation existing = SESSIONS.get(operationId);
50:         if (existing != null) {
51:             return existing;
52:         }
53:         if (caster.level().isClientSide() || target.level().isClientSide()) {
54:             return rejected(operationId, caster, target, "client");
55:         }
56:         if (PRISON_BY_TARGET.containsKey(target.getUUID()) || PREPARED_TARGETS.containsKey(target.getUUID())) {
57:             return rejected(operationId, caster, target, "occupied");
58:         }
59:         PrisonReservation reservation = new PrisonReservation(operationId, caster, target, true, "");
60:         PREPARED_TARGETS.put(target.getUUID(), operationId);
61:         SESSIONS.put(operationId, reservation);
62:         return reservation;
63:     }
64: 
65:     static PrisonCommitResult commit(PrisonReservation reservation, EnchantmentSnapshot contact) {
66:         LivingEntity target = reservation.target();
67:         if (target == null || !target.isAlive() || target.isRemoved() || target.level().isClientSide()) {
68:             return PrisonCommitResult.fail("dead");
69:         }
70:         if (!(target.level() instanceof ServerLevel level)) {
71:             return PrisonCommitResult.fail("dimension");
72:         }
73:         MobEffectInstance wet = target.getEffect(ModMobEffects.WETNESS.get());
74:         int current = WetnessState.read(target);
75:         int tier = WetnessState.tierOf(current);
76:         if (wet == null || current <= 0 || tier <= 0) {
77:             return PrisonCommitResult.fail("not_wet");
78:         }
79:         int durationTicks = prisonTicks(tier, wet.getDuration());
80:         if (durationTicks <= 0) {
81:             return PrisonCommitResult.fail("duration");
82:         }
83:         dismount(target);
84:         Vec3 anchor = target.position();
85:         AABB bounds = PrisonShell.expand(target.getBoundingBox());
86:         if (anchor == null || bounds == null || reservation.caster() == null) {
87:             return PrisonCommitResult.fail("rejected");
88:         }
89:         long birthTick = level.getGameTime();
90:         PrisonView view = new PrisonView(
91:                 UUID.randomUUID(),
92:                 REVISION,
93:                 reservation.caster().getUUID(),
94:                 target.getUUID(),
95:                 level.dimension(),
96:                 anchor,
97:                 bounds,
98:                 tier,
99:                 birthTick,
100:                 birthTick + durationTicks);
101:         PrisonRecord prison = new PrisonRecord(view, PrisonShell.innerOf(bounds), target, tier);
102:         reservation.markCommitted();
103:         PREPARED_TARGETS.remove(target.getUUID());
104:         PRISONS.put(view.prisonId(), prison);
105:         PRISON_BY_TARGET.put(view.targetId(), view.prisonId());
106:         BY_ENTITY.put(target, prison);
107:         try {
108:             if (contact != null) {
109:                 prison.contact = ContactEnchantState.open(prison, contact);
110:             }
111:             PrisonEvents.created(view);
112:             if (prison.phase == Phase.COMMITTED && tier >= 4 && target.isAlive()) {
113:                 PrisonPressure.pulse(target);
114:             }
115:         } catch (RuntimeException failure) {
116:             LOGGER.error("water prison commit follow-up failed for {}", view.prisonId(), failure);
117:         }
118:         return PrisonCommitResult.success(view);
119:     }
120: 
121:     static void close(PrisonReservation reservation) {
122:         if (reservation.operationId() != null) {
123:             SESSIONS.remove(reservation.operationId(), reservation);
124:         }
125:         if (reservation.wasCommitted() || reservation.target() == null || reservation.operationId() == null) {
126:             return;
127:         }
128:         UUID holder = PREPARED_TARGETS.get(reservation.target().getUUID());
129:         if (reservation.operationId().equals(holder)) {
130:             PREPARED_TARGETS.remove(reservation.target().getUUID());
131:         }
132:     }
133: 
134:     public static Optional<PrisonView> view(UUID targetId) {
135:         PrisonRecord prison = prisonOf(targetId);
136:         if (prison == null || prison.phase != Phase.COMMITTED) {
137:             return Optional.empty();
138:         }
139:         return Optional.of(prison.view);
140:     }
141: 
142:     public static boolean end(UUID prisonId, EndReason reason) {
143:         if (prisonId == null) {
144:             return false;
145:         }
146:         PrisonRecord prison = PRISONS.get(prisonId);
147:         if (prison == null) {
148:             return false;
149:         }
150:         return prison.finish(reason == null ? EndReason.REMOVED : reason, prison.carrier());
151:     }
152: 
153:     public static boolean suppressesEnhancedSuffocation(LivingEntity entity) {
154:         PrisonRecord prison = find(entity);
155:         return prison != null && prison.phase == Phase.COMMITTED && prison.view.lockedTier() >= 4;
156:     }
157: 
158:     public static boolean locks(Entity entity) {
159:         return entity instanceof LivingEntity living && find(living) != null;
160:     }
161: 
162:     public static Vec3 anchor(Entity entity) {
163:         if (!(entity instanceof LivingEntity living)) {
164:             return null;
165:         }
166:         PrisonRecord prison = find(living);
167:         if (prison == null) {
168:             return null;
169:         }
170:         return prison.view.anchor();
171:     }
172: 
173:     public static boolean endTarget(LivingEntity locked, EndReason reason, LivingEntity wetCarrier) {
174:         PrisonRecord prison = find(locked);
175:         if (prison == null) {
176:             return false;
177:         }
178:         LivingEntity carrier = wetCarrier == null ? prison.carrier() : wetCarrier;
179:         return prison.finish(reason == null ? EndReason.REMOVED : reason, carrier);
180:     }
181: 
182:     public static void crossDimension(Entity original, Entity arrived) {
183:         if (!(original instanceof LivingEntity living) || arrived == null) {
184:             return;
185:         }
186:         LivingEntity carrier = arrived instanceof LivingEntity survivor && !survivor.isRemoved() ? survivor : living;
187:         endTarget(living, EndReason.DIMENSION, carrier);
188:     }
189: 
190:     public static void onConfirmedDeath(LivingEntity entity) {
191:         if (entity == null) {
192:             return;
193:         }
194:         endTarget(entity, EndReason.DEATH, entity);
195:         if (entity instanceof ServerPlayer player) {
196:             endCastBy(player, EndReason.CASTER_DEATH);
197:         }
198:     }
199: 
200:     public static void onLogout(ServerPlayer player) {
201:         if (player == null) {
202:             return;
203:         }
204:         endTarget(player, EndReason.LOGOUT, player);
205:         endCastBy(player, EndReason.CASTER_LOGOUT);
206:     }
207: 
208:     public static void onDimension(ServerPlayer player) {
209:         if (player == null) {
210:             return;
211:         }
212:         endTarget(player, EndReason.DIMENSION, player);
213:         endCastBy(player, EndReason.CASTER_DIMENSION);
214:     }
215: 
216:     /** 原版末地回主世界已经接受、玩家即将被丢掉之前的那次返回。普通换维事件不到这里。 */
217:     public static void onVanillaEndReturn(ServerPlayer player) {
218:         if (player == null) {
219:             return;
220:         }
221:         boolean wroteOwn = false;
222:         PrisonRecord own = find(player);
223:         if (own != null) {
224:             own.finish(EndReason.DIMENSION, player);
225:             wroteOwn = own.settled;
226:         }
227:         endCastBy(player, EndReason.CASTER_DIMENSION);
228:         if (wroteOwn) {
229:             PrisonWetTransfer.capture(player);
230:         }
231:     }
232: 
233:     public static void shutdown(MinecraftServer server) {
234:         if (server == null || PRISONS.isEmpty()) {
235:             return;
236:         }
237:         ArrayList<PrisonRecord> snapshot = new ArrayList<>(PRISONS.values());
238:         for (PrisonRecord prison : snapshot) {
239:             if (prison.phase != Phase.COMMITTED) {
240:                 continue;
241:             }
242:             LivingEntity carrier = prison.carrier();
243:             if (!holdsWritableWet(carrier)) {
244:                 LivingEntity loaded = findLive(server, prison.view.targetId());
245:                 if (holdsWritableWet(loaded)) {
246:                     carrier = loaded;
247:                 }
248:             }
249:             prison.finish(EndReason.REMOVED, carrier);
250:         }
251:     }
252: 
253:     public static void onWetRemoved(LivingEntity entity, MobEffectInstance instance) {
254:         if (entity == null || instance == null || com.bslx.XueXiMod.status.AttributeStateService.isDirectEdit()) {
255:             return;
256:         }
257:         if (instance.getEffect() != ModMobEffects.WETNESS.get()) {
258:             return;
259:         }
260:         endTarget(entity, EndReason.WET_CLEARED, entity);
261:     }
262: 
263:     public static void rebind(LivingEntity living) {
264:         if (living == null || living.level().isClientSide()) {
265:             return;
266:         }
267:         PrisonWetDue.settleLoaded(living);
268:         PrisonRecord prison = prisonOf(living.getUUID());
269:         if (prison == null || prison.phase != Phase.COMMITTED) {
270:             return;
271:         }
272:         long gameTime = living.level().getGameTime();
273:         if (prison.expiredWhileAway || gameTime >= prison.view.endTick()) {
274:             prison.finish(EndReason.DURATION, living);
275:             return;
276:         }
277:         prison.attach(living);
278:     }
279: 
280:     public static void tick(MinecraftServer server) {
281:         if (server == null || PRISONS.isEmpty()) {
282:             return;
283:         }
284:         ArrayList<PrisonRecord> snapshot = new ArrayList<>(PRISONS.values());
285:         for (PrisonRecord prison : snapshot) {
286:             prison.tick(server);
287:         }
288:     }
289: 
290:     private static void endCastBy(ServerPlayer player, EndReason reason) {
291:         ArrayList<PrisonRecord> owned = new ArrayList<>();
292:         for (PrisonRecord prison : PRISONS.values()) {
293:             if (player.getUUID().equals(prison.view.casterId())) {
294:                 owned.add(prison);
295:             }
296:         }
297:         for (PrisonRecord prison : owned) {
298:             prison.finish(reason, prison.carrier());
299:         }
300:     }
301: 
302:     private static PrisonRecord find(LivingEntity entity) {
303:         if (entity == null) {
304:             return null;
305:         }
306:         PrisonRecord direct = BY_ENTITY.get(entity);
307:         if (direct != null && direct.phase == Phase.COMMITTED && direct.prisoner == entity) {
308:             return direct;
309:         }
310:         return null;
311:     }
312: 
313:     private static PrisonRecord prisonOf(UUID targetId) {
314:         if (targetId == null) {
315:             return null;
316:         }
317:         UUID prisonId = PRISON_BY_TARGET.get(targetId);
318:         if (prisonId == null) {
319:             return null;
320:         }
321:         return PRISONS.get(prisonId);
322:     }
323: 
324:     private static PrisonReservation rejected(UUID operationId, ServerPlayer caster, LivingEntity target, String reason) {
325:         PrisonReservation reservation = new PrisonReservation(operationId, caster, target, false, reason);
326:         if (operationId != null) {
327:             SESSIONS.put(operationId, reservation);
328:         }
329:         return reservation;
330:     }
331: 
332:     private static void dismount(LivingEntity target) {
333:         if (target.isPassenger()) {
334:             target.stopRiding();
335:         }
336:         if (target.isVehicle()) {
337:             target.ejectPassengers();
338:         }
339:     }
340: 
341:     private static int prisonTicks(int tier, int remaining) {
342:         if (tier == 1) {
343:             return SHORT_TICKS;
344:         }
345:         if (tier == 2) {
346:             return LONG_TICKS;
347:         }
348:         if (tier == 3) {
349:             if (remaining <= 0) {
350:                 return 0;
351:             }
352:             return Math.max(1, remaining / 2);
353:         }
354:         if (tier >= 4) {
355:             return remaining;
356:         }
357:         return 0;
358:     }
359: 
360:     static boolean settleCarrier(LivingEntity carrier, int lockedTier, EndReason reason) {
361:         if (!holdsWritableWet(carrier)) {
362:             return false;
363:         }
364:         if (reason == EndReason.WET_CLEARED) {
365:             WetnessState.clear(carrier);
366:             return true;
367:         }
368:         int current = WetnessState.read(carrier);
369:         if (current <= 0) {
370:             if (carrier.getEffect(ModMobEffects.WETNESS.get()) != null) {
371:                 WetnessState.clear(carrier);
372:             }
373:             return true;
374:         }
375:         if (lockedTier == 2) {
376:             WetnessState.replacePreservingDuration(carrier, current / 2);
377:             return true;
378:         }
379:         WetnessState.clear(carrier);
380:         return true;
381:     }
382: 
383:     private static boolean holdsWritableWet(LivingEntity entity) {
384:         if (entity == null || entity.level() == null || entity.level().isClientSide()) {
385:             return false;
386:         }
387:         if (!entity.isRemoved()) {
388:             return true;
389:         }
390:         return entity instanceof ServerPlayer
391:                 && entity.getRemovalReason() == Entity.RemovalReason.CHANGED_DIMENSION;
392:     }
393: 
394:     private static LivingEntity findLive(MinecraftServer server, UUID targetId) {
395:         if (server == null || targetId == null) {
396:             return null;
397:         }
398:         for (ServerLevel level : server.getAllLevels()) {
399:             Entity found = level.getEntity(targetId);
400:             if (found instanceof LivingEntity living && !living.isRemoved()) {
401:                 return living;
402:             }
403:         }
404:         return null;
405:     }
406: 
407:     private enum Phase {
408:         COMMITTED,
409:         ENDING,
410:         ENDED
411:     }
412: 
413:     private static final class PrisonRecord implements ContactCarrier {
414:         private final PrisonView view;
415:         private final AABB inner;
416:         private final int tier;
417:         private LivingEntity prisoner;
418:         private ContactEnchantState contact;
419:         private Phase phase = Phase.COMMITTED;
420:         private boolean settled;
421:         private boolean expiredWhileAway;
422:         private long nextPulse;
423: 
424:         private PrisonRecord(PrisonView view, AABB inner, LivingEntity prisoner, int tier) {
425:             this.view = view;
426:             this.inner = inner;
427:             this.prisoner = prisoner;
428:             this.tier = tier;
429:             this.nextPulse = tier >= 4 ? view.birthTick() + PrisonPressure.INTERVAL_TICKS : Long.MAX_VALUE;
430:         }
431: 
432:         private void tick(MinecraftServer server) {
433:             if (this.phase != Phase.COMMITTED) {
434:                 return;
435:             }
436:             ServerLevel level = server.getLevel(this.view.dimension());
437:             if (level == null) {
438:                 return;
439:             }
440:             long gameTime = level.getGameTime();
441:             LivingEntity live = resolve(level);
442:             if (live == null) {
443:                 if (this.prisoner != null && this.prisoner.isRemoved()) {
444:                     handleRemoval(server);
445:                     return;
446:                 }
447:                 if (gameTime >= this.view.endTick()) {
448:                     this.expiredWhileAway = true;
449:                 }
450:                 return;
451:             }
452:             if (live != this.prisoner) {
453:                 attach(live);
454:             }
455:             if (this.expiredWhileAway || gameTime >= this.view.endTick()) {
456:                 finish(EndReason.DURATION, live);
457:                 return;
458:             }
459:             if (this.tier >= 4 && gameTime >= this.nextPulse) {
460:                 this.nextPulse = gameTime + PrisonPressure.INTERVAL_TICKS;
461:                 PrisonPressure.pulse(live);
462:             }
463:         }
464: 
465:         private LivingEntity resolve(ServerLevel level) {
466:             Entity found = level.getEntity(this.view.targetId());
467:             if (found instanceof LivingEntity living && !living.isRemoved()) {
468:                 return living;
469:             }
470:             if (this.prisoner != null && !this.prisoner.isRemoved() && this.prisoner.level() == level) {
471:                 return this.prisoner;
472:             }
473:             return null;
474:         }
475: 
476:         private void handleRemoval(MinecraftServer server) {
477:             Entity.RemovalReason removal = this.prisoner.getRemovalReason();
478:             if (removal == Entity.RemovalReason.UNLOADED_TO_CHUNK) {
479:                 BY_ENTITY.remove(this.prisoner);
480:                 this.prisoner = null;
481:                 return;
482:             }
483:             if (removal == Entity.RemovalReason.CHANGED_DIMENSION) {
484:                 LivingEntity survivor = findLive(server, this.view.targetId());
485:                 finish(EndReason.DIMENSION, survivor == null ? this.prisoner : survivor);
486:                 return;
487:             }
488:             if (removal == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
489:                 finish(EndReason.LOGOUT, this.prisoner);
490:                 return;
491:             }
492:             if (removal == Entity.RemovalReason.KILLED) {
493:                 finish(EndReason.DEATH, this.prisoner);
494:                 return;
495:             }
496:             finish(EndReason.REMOVED, this.prisoner);
497:         }
498: 
499:         private void attach(LivingEntity living) {
500:             if (this.prisoner != null && this.prisoner != living) {
501:                 BY_ENTITY.remove(this.prisoner);
502:             }
503:             this.prisoner = living;
504:             BY_ENTITY.put(living, this);
505:         }
506: 
507:         private LivingEntity carrier() {
508:             if (this.prisoner != null) {
509:                 return this.prisoner;
510:             }
511:             return null;
512:         }
513: 
514:         private boolean finish(EndReason reason, LivingEntity wetCarrier) {
515:             if (this.phase != Phase.COMMITTED) {
516:                 return false;
517:             }
518:             this.phase = Phase.ENDING;
519:             if (this.contact != null) {
520:                 this.contact.close();
521:                 this.contact = null;
522:             }
523:             if (this.prisoner != null) {
524:                 BY_ENTITY.remove(this.prisoner);
525:             }
526:             PRISONS.remove(this.view.prisonId());
527:             UUID mapped = PRISON_BY_TARGET.get(this.view.targetId());
528:             if (this.view.prisonId().equals(mapped)) {
529:                 PRISON_BY_TARGET.remove(this.view.targetId());
530:             }
531:             if (!this.settled) {
532:                 LivingEntity carrier = wetCarrier;
533:                 if (!holdsWritableWet(carrier)) {
534:                     MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
535:                     if (server != null) {
536:                         LivingEntity loaded = findLive(server, this.view.targetId());
537:                         if (holdsWritableWet(loaded)) {
538:                             carrier = loaded;
539:                         }
540:                     }
541:                 }
542:                 if (holdsWritableWet(carrier)) {
543:                     try {
544:                         if (settleCarrier(carrier, this.tier, reason)) {
545:                             this.settled = true;
546:                             PrisonWetDue.drop(this.view.prisonId(), this.view.dimension());
547:                         } else {
548:                             PrisonWetDue.keep(this.view.prisonId(), this.view.targetId(), this.view.dimension(), this.tier, reason);
549:                         }
550:                     } catch (RuntimeException failure) {
551:                         LOGGER.error("water prison wet settlement failed for {}", this.view.prisonId(), failure);
552:                         PrisonWetDue.keep(this.view.prisonId(), this.view.targetId(), this.view.dimension(), this.tier, reason);
553:                     }
554:                 } else {
555:                     PrisonWetDue.keep(this.view.prisonId(), this.view.targetId(), this.view.dimension(), this.tier, reason);
556:                 }
557:             }
558:             this.phase = Phase.ENDED;
559:             PrisonEvents.ended(this.view, reason);
560:             return true;
561:         }
562: 
563:         @Override
564:         public UUID hostId() {
565:             return this.view.prisonId();
566:         }
567: 
568:         @Override
569:         public int revision() {
570:             return (int) this.view.revision();
571:         }
572: 
573:         @Override
574:         public UUID ownerId() {
575:             return this.view.casterId();
576:         }
577: 
578:         @Override
579:         public boolean active() {
580:             return this.phase == Phase.COMMITTED;
581:         }
582: 
583:         @Override
584:         public boolean touches(LivingEntity target) {
585:             return PrisonShell.touches(this.view.bounds(), this.inner, target, this.view.targetId());
586:         }
587: 
588:         @Override
589:         public ServerLevel hostLevel() {
590:             if (this.prisoner != null && this.prisoner.level() instanceof ServerLevel level
591:                     && this.view.dimension().equals(level.dimension())) {
592:                 return level;
593:             }
594:             return null;
595:         }
596: 
597:         @Override
598:         public AABB queryBox() {
599:             return this.view.bounds();
600:         }
601:     }
602: }
603: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonLifecycle.java">
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import com.bslx.XueXiMod.XueXiMod;
4: import net.minecraft.server.level.ServerPlayer;
5: import net.minecraft.world.entity.LivingEntity;
6: import net.minecraftforge.event.TickEvent;
7: import net.minecraftforge.event.entity.EntityJoinLevelEvent;
8: import net.minecraftforge.event.entity.player.PlayerEvent;
9: import net.minecraftforge.event.server.ServerStoppingEvent;
10: import net.minecraftforge.eventbus.api.EventPriority;
11: import net.minecraftforge.eventbus.api.SubscribeEvent;
12: import net.minecraftforge.fml.common.Mod;
13: 
14: /** 水牢自己的生命周期。登出在存档事件里同步结算，不写进别的订阅者。 */
15: @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
16: public final class PrisonLifecycle {
17:     private PrisonLifecycle() {
18:     }
19: 
20:     @SubscribeEvent
21:     public static void onServerTick(TickEvent.ServerTickEvent event) {
22:         if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
23:             return;
24:         }
25:         PrisonRuntime.tick(event.getServer());
26:     }
27: 
28:     @SubscribeEvent
29:     public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
30:         if (event.getEntity() instanceof ServerPlayer player) {
31:             PrisonRuntime.onLogout(player);
32:             PrisonWetTransfer.discard(player);
33:         }
34:     }
35: 
36:     @SubscribeEvent
37:     public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
38:         if (event.getEntity() instanceof ServerPlayer player) {
39:             PrisonWetTransfer.apply(player);
40:         }
41:     }
42: 
43:     @SubscribeEvent(priority = EventPriority.LOW)
44:     public static void onClone(PlayerEvent.Clone event) {
45:         if (event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer fresh)) {
46:             return;
47:         }
48:         PrisonWetTransfer.apply(fresh);
49:     }
50: 
51:     @SubscribeEvent
52:     public static void onStopping(ServerStoppingEvent event) {
53:         PrisonRuntime.shutdown(event.getServer());
54:     }
55: 
56:     @SubscribeEvent
57:     public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
58:         if (event.getEntity() instanceof ServerPlayer player) {
59:             PrisonRuntime.onDimension(player);
60:         }
61:     }
62: 
63:     @SubscribeEvent
64:     public static void onJoin(EntityJoinLevelEvent event) {
65:         if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) {
66:             return;
67:         }
68:         PrisonRuntime.rebind(living);
69:     }
70: }
71: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonView.java">
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import net.minecraft.resources.ResourceKey;
4: import net.minecraft.world.level.Level;
5: import net.minecraft.world.phys.AABB;
6: import net.minecraft.world.phys.Vec3;
7: 
8: import java.util.UUID;
9: 
10: /**
11:  * 服务端水牢快照。bounds 是创建时目标真实包围盒外扩后的壳，身份只用 UUID。
12:  * 同步用的网络编号不在这里。
13:  */
14: public record PrisonView(
15:         UUID prisonId,
16:         long revision,
17:         UUID casterId,
18:         UUID targetId,
19:         ResourceKey<Level> dimension,
20:         Vec3 anchor,
21:         AABB bounds,
22:         int lockedTier,
23:         long birthTick,
24:         long endTick) {
25: }
26: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/release/prison/PrisonShell.java">
1: package com.bslx.XueXiMod.shu.technique.release.prison;
2: 
3: import net.minecraft.world.entity.LivingEntity;
4: import net.minecraft.world.phys.AABB;
5: 
6: import java.util.UUID;
7: 
8: /** 创建时把目标真实包围盒外扩 0.20 格作为壳。接触只算这层壳，不算盒子内部。 */
9: public final class PrisonShell {
10:     public static final double PADDING = 0.20D;
11: 
12:     private PrisonShell() {
13:     }
14: 
15:     public static AABB expand(AABB body) {
16:         if (body == null) {
17:             return null;
18:         }
19:         return body.inflate(PADDING);
20:     }
21: 
22:     public static AABB innerOf(AABB outer) {
23:         if (outer == null) {
24:             return null;
25:         }
26:         AABB inner = outer.inflate(-PADDING);
27:         if (inner.maxX <= inner.minX || inner.maxY <= inner.minY || inner.maxZ <= inner.minZ) {
28:             return null;
29:         }
30:         return inner;
31:     }
32: 
33:     public static boolean touches(AABB outer, AABB inner, LivingEntity candidate, UUID prisonerId) {
34:         if (outer == null || candidate == null || prisonerId == null || prisonerId.equals(candidate.getUUID())) {
35:             return false;
36:         }
37:         AABB body = candidate.getBoundingBox();
38:         if (body == null || !body.intersects(outer)) {
39:             return false;
40:         }
41:         if (inner == null) {
42:             return true;
43:         }
44:         return body.minX < inner.minX || body.maxX > inner.maxX
45:                 || body.minY < inner.minY || body.maxY > inner.maxY
46:                 || body.minZ < inner.minZ || body.maxZ > inner.maxZ;
47:     }
48: }
49: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/shu/technique/effects/MagicCreateWaterEffect.java">
1: package com.bslx.XueXiMod.shu.technique.effects;
2: 
3: import com.bslx.XueXiMod.shu.Technique;
4: import com.bslx.XueXiMod.shu.technique.TechniqueCastContext;
5: import com.bslx.XueXiMod.shu.technique.TechniqueEffect;
6: import net.minecraft.server.level.ServerPlayer;
7: import net.minecraft.world.item.ItemStack;
8: import net.minecraft.world.phys.Vec3;
9: 
10: public class MagicCreateWaterEffect implements TechniqueEffect {
11:     @Override
12:     public void execute(ServerPlayer player, Technique technique, ItemStack weapon, TechniqueCastContext cast) {
13:         Vec3 look = cast.getLook();
14:         Vec3 castPosition = cast.getPos();
15:         Vec3 target = castPosition.add(look.scale(5));
16:         net.minecraft.core.BlockPos targetBlock = new net.minecraft.core.BlockPos((int) target.x, (int) target.y, (int) target.z);
17:         if (player.level().isEmptyBlock(targetBlock)) {
18:             player.level().setBlock(targetBlock,
19:                     net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
20:         }
21:     }
22: }
23: 
</source_file>
