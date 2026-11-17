你是主代理调度的DeepSeek专项只读读取代理。项目D:/alianximod；已由主代理咨询唯一GPT-6-astra脑子，现阶段只收集事实，不裁决、不改任何文件、不编译、不另派代理。
目标：确认“用户核准的水环v8素材接入现有水环渲染”最小改动入口。
只读范围：
1. src/main/java/com/bslx/XueXiMod/client/GuardRingRenderer.java（完整）
2. 必要时对client/WaterArcRenderer.java、DomainMistRingRenderer.java仅看RenderType/VertexConsumer/贴图动画和资源引用写法。
3. docs/资源库/过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/README.md，模型只提取mesh与UV，不输出base64。
输出中文事实摘要：渲染事件、坐标系及radius/normal来源、现有贴图/alpha/renderstate、客户端帧时钟，建议接入需要动的最少文件、风险点。路径+行号；不要泛化重构。别读整仓，别改游戏逻辑。主代理另行整合，等脑子拍板后Cursor再实现。若git ownership问题仅用git -c safe.directory=D:/alianximod，不改全局配置。

当前是离线只读分析，以下已提供全部必要源文件。禁止调用任何工具，无须再查文件。若给定材料不含某个调用点，明确指出待查，不要因未提供的事实臆测。不要写文件或启动任何进程。

<source_file path="src/main/java/com/bslx/XueXiMod/client/GuardRingRenderer.java">
1: package com.bslx.XueXiMod.client;
2: 
3: import com.bslx.XueXiMod.XueXiMod;
4: import com.bslx.XueXiMod.network.GuardSyncPacket;
5: import com.mojang.blaze3d.vertex.PoseStack;
6: import com.mojang.blaze3d.vertex.VertexConsumer;
7: import net.minecraft.client.Minecraft;
8: import net.minecraft.client.renderer.LightTexture;
9: import net.minecraft.client.renderer.MultiBufferSource;
10: import net.minecraft.client.renderer.RenderType;
11: import net.minecraft.client.renderer.texture.OverlayTexture;
12: import net.minecraft.resources.ResourceLocation;
13: import net.minecraft.util.Mth;
14: import net.minecraft.world.entity.player.Player;
15: import net.minecraft.world.phys.Vec3;
16: import net.minecraftforge.api.distmarker.Dist;
17: import net.minecraftforge.client.event.RenderLevelStageEvent;
18: import net.minecraftforge.eventbus.api.SubscribeEvent;
19: import net.minecraftforge.fml.common.Mod;
20: import org.joml.Matrix4f;
21: 
22: /** 水环跟着身体画。气泡图只贴在环带上，不生成气泡。 */
23: @Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, value = Dist.CLIENT)
24: public final class GuardRingRenderer {
25:     private static final ResourceLocation BUBBLE = new ResourceLocation("minecraft", "textures/particle/bubble.png");
26:     private static final int SEGMENTS = 32;
27: 
28:     private GuardRingRenderer() {
29:     }
30: 
31:     @SubscribeEvent
32:     public static void onRender(RenderLevelStageEvent event) {
33:         if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
34:             return;
35:         }
36:         Minecraft minecraft = Minecraft.getInstance();
37:         if (minecraft.level == null || minecraft.player == null) {
38:             return;
39:         }
40:         if (GuardSyncPacket.ClientRings.all().isEmpty()) {
41:             return;
42:         }
43:         float partialTick = event.getPartialTick();
44:         Vec3 camera = event.getCamera().getPosition();
45:         boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
46:         PoseStack pose = event.getPoseStack();
47:         MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
48:         VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(BUBBLE));
49:         pose.pushPose();
50:         pose.translate(-camera.x, -camera.y, -camera.z);
51:         Matrix4f matrix = pose.last().pose();
52:         for (GuardSyncPacket.ClientRings.View view : GuardSyncPacket.ClientRings.all()) {
53:             Player wearer = minecraft.level.getPlayerByUUID(view.ownerId);
54:             if (wearer == null) {
55:                 continue;
56:             }
57:             double centerX = Mth.lerp(partialTick, wearer.xo, wearer.getX());
58:             double centerY = Mth.lerp(partialTick, wearer.yo, wearer.getY()) + wearer.getBbHeight() * view.bodyHeight;
59:             double centerZ = Mth.lerp(partialTick, wearer.zo, wearer.getZ());
60:             Vec3 normal = blendedNormal(view);
61:             boolean fadeNearEye = firstPerson && wearer == minecraft.player;
62:             ring(consumer, matrix, camera, centerX, centerY, centerZ, normal, view.radius, view.band, fadeNearEye);
63:         }
64:         pose.popPose();
65:         buffer.endBatch(RenderType.entityTranslucent(BUBBLE));
66:     }
67: 
68:     private static Vec3 blendedNormal(GuardSyncPacket.ClientRings.View view) {
69:         float span = Math.max(1, view.poseTicks) * 50f;
70:         float progress = Mth.clamp((System.currentTimeMillis() - view.poseStartMillis) / span, 0f, 1f);
71:         double normalX = Mth.lerp(progress, view.fromX, view.towardX);
72:         double normalY = Mth.lerp(progress, view.fromY, view.towardY);
73:         double normalZ = Mth.lerp(progress, view.fromZ, view.towardZ);
74:         Vec3 normal = new Vec3(normalX, normalY, normalZ);
75:         if (normal.lengthSqr() < 1.0E-6) {
76:             return new Vec3(0.0, 1.0, 0.0);
77:         }
78:         return normal.normalize();
79:     }
80: 
81:     private static void ring(VertexConsumer consumer, Matrix4f matrix, Vec3 camera,
82:                              double centerX, double centerY, double centerZ, Vec3 normal,
83:                              float radius, float band, boolean fadeNearEye) {
84:         Vec3 helper = Math.abs(normal.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
85:         Vec3 side = helper.cross(normal);
86:         if (side.lengthSqr() < 1.0E-6) {
87:             side = new Vec3(1.0, 0.0, 0.0);
88:         }
89:         side = side.normalize();
90:         Vec3 other = normal.cross(side).normalize();
91:         float outer = radius + band * 0.5f;
92:         float inner = Math.max(0.05f, radius - band * 0.5f);
93:         int packedLight = LightTexture.FULL_BRIGHT;
94:         for (int segmentIndex = 0; segmentIndex < SEGMENTS; segmentIndex++) {
95:             double startAngle = segmentIndex * Math.PI * 2.0 / SEGMENTS;
96:             double endAngle = (segmentIndex + 1) * Math.PI * 2.0 / SEGMENTS;
97:             Vec3 startOuter = place(centerX, centerY, centerZ, side, other, startAngle, outer);
98:             Vec3 endOuter = place(centerX, centerY, centerZ, side, other, endAngle, outer);
99:             Vec3 endInner = place(centerX, centerY, centerZ, side, other, endAngle, inner);
100:             Vec3 startInner = place(centerX, centerY, centerZ, side, other, startAngle, inner);
101:             int alpha = 170;
102:             if (fadeNearEye) {
103:                 Vec3 middle = startOuter.add(endInner).scale(0.5);
104:                 double distance = middle.distanceTo(camera);
105:                 if (distance < 0.35) {
106:                     continue;
107:                 }
108:                 if (distance < 0.7) {
109:                     alpha = (int) (170 * (distance - 0.35) / 0.35);
110:                 }
111:             }
112:             float textureU = segmentIndex / (float) SEGMENTS;
113:             float nextU = (segmentIndex + 1) / (float) SEGMENTS;
114:             quad(consumer, matrix, packedLight, startOuter, endOuter, endInner, startInner,
115:                     (float) normal.x, (float) normal.y, (float) normal.z, alpha, textureU, nextU);
116:             Vec3 offset = normal.scale(0.002);
117:             quad(consumer, matrix, packedLight,
118:                     endOuter.subtract(offset), startOuter.subtract(offset), startInner.subtract(offset), endInner.subtract(offset),
119:                     (float) -normal.x, (float) -normal.y, (float) -normal.z, alpha, nextU, textureU);
120:         }
121:     }
122: 
123:     private static Vec3 place(double centerX, double centerY, double centerZ, Vec3 side, Vec3 other, double angle, float reach) {
124:         double cosine = Math.cos(angle);
125:         double sine = Math.sin(angle);
126:         return new Vec3(
127:                 centerX + side.x * cosine * reach + other.x * sine * reach,
128:                 centerY + side.y * cosine * reach + other.y * sine * reach,
129:                 centerZ + side.z * cosine * reach + other.z * sine * reach);
130:     }
131: 
132:     private static void quad(VertexConsumer consumer, Matrix4f matrix, int packedLight,
133:                              Vec3 first, Vec3 second, Vec3 third, Vec3 fourth,
134:                              float normalX, float normalY, float normalZ, int alpha, float leftU, float rightU) {
135:         vertex(consumer, matrix, packedLight, first, leftU, 0f, normalX, normalY, normalZ, alpha);
136:         vertex(consumer, matrix, packedLight, second, rightU, 0f, normalX, normalY, normalZ, alpha);
137:         vertex(consumer, matrix, packedLight, third, rightU, 1f, normalX, normalY, normalZ, alpha);
138:         vertex(consumer, matrix, packedLight, fourth, leftU, 1f, normalX, normalY, normalZ, alpha);
139:     }
140: 
141:     private static void vertex(VertexConsumer consumer, Matrix4f matrix, int packedLight, Vec3 point,
142:                                float textureU, float textureV, float normalX, float normalY, float normalZ, int alpha) {
143:         consumer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z)
144:                 .color(90, 180, 220, alpha)
145:                 .uv(textureU, textureV)
146:                 .overlayCoords(OverlayTexture.NO_OVERLAY)
147:                 .uv2(packedLight)
148:                 .normal(normalX, normalY, normalZ)
149:                 .endVertex();
150:     }
151: }
152: 
</source_file>

<source_file path="src/main/java/com/bslx/XueXiMod/client/DomainMistRingRenderer.java">
1: package com.bslx.XueXiMod.client;
2: 
3: import com.bslx.XueXiMod.XueXiMod;
4: import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
5: import com.bslx.XueXiMod.shu.technique.release.domain.DomainVisual;
6: import net.minecraft.client.renderer.RenderType;
7: import net.minecraft.resources.ResourceLocation;
8: import net.minecraft.world.phys.AABB;
9: import net.minecraft.world.phys.Vec3;
10: 
11: /** 雾雨领域的竖直雾环。顶点保持白色，只提交一份外向面，不画顶底，也不另画反向面。 */
12: public final class DomainMistRingRenderer {
13:     private static final ResourceLocation TEXTURE = new ResourceLocation(XueXiMod.MOD_ID, "textures/domain/mist_panel.png");
14:     private static final RenderType MESH = DomainSheetMesh.mistCurtain(TEXTURE);
15: 
16:     private DomainMistRingRenderer() {
17:     }
18: 
19:     public static void collect(DomainSightStore.Sight sight, Vec3 camera, long gameTime, DomainVolumeBatch batch) {
20:         if (sight.visual() != DomainVisual.VOLUME) {
21:             return;
22:         }
23:         if (gameTime < sight.visualOrigin() || gameTime >= sight.visualEnd()) {
24:             return;
25:         }
26:         AABB box = sight.bounds();
27:         DomainMistRingLayout layout = DomainMistRingLayout.tryBuild(
28:                 box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
29:         if (layout == null) {
30:             return;
31:         }
32:         int frame = (int) Math.floorMod(
33:                 gameTime - sight.visualOrigin() + sight.animSeed(), (long) DomainMistRingLayout.FRAME_COUNT);
34:         float topV = DomainMistRingLayout.frameV(frame, DomainMistRingLayout.FRAME_EDGE_PAD);
35:         float bottomV = DomainMistRingLayout.frameV(frame, 1.0f - DomainMistRingLayout.FRAME_EDGE_PAD);
36:         double cameraX = camera.x;
37:         double cameraY = camera.y;
38:         double cameraZ = camera.z;
39:         float upperY = layout.relativeUpperY(cameraY);
40:         float lowerY = layout.relativeLowerY(cameraY);
41:         for (int side = 0; side < layout.quadCount(); side++) {
42:             int next = (side + 1) % layout.quadCount();
43:             float startX = layout.relativeX(side, cameraX);
44:             float startZ = layout.relativeZ(side, cameraZ);
45:             float nextX = layout.relativeX(next, cameraX);
46:             float nextZ = layout.relativeZ(next, cameraZ);
47:             batch.addQuad(
48:                     MESH,
49:                     startX, upperY, startZ, layout.uAt(side), topV,
50:                     nextX, upperY, nextZ, layout.uAt(side + 1), topV,
51:                     nextX, lowerY, nextZ, layout.uAt(side + 1), bottomV,
52:                     startX, lowerY, startZ, layout.uAt(side), bottomV,
53:                     layout.outwardX(side), 0.0f, layout.outwardZ(side));
54:         }
55:     }
56: }
57: 
</source_file>

<source_file path="docs/资源库/过程归档/周身水环3D验证_v8_细碎水膜毛边-20261027/README.md">
1: # 周身水环 v8：细碎水膜毛边
2: 
3: 状态：美术验证草稿，待用户确认，未接入游戏。
4: 
5: 依据用户提供的水环照片，仅提取毛边特征：薄水膜撕开的圆钝水舌、细叉、小弯钩，以及断离的小珠滴。继续采用像素画法，保留v7完整内外双缘结构、中央破碎水体、共同逆时针旋转；没有照参考改成C形开口。
6: 
7: ## 预览
8: 
9: - [固定斜视动画](water_ragged_fixed.gif)
10: - [固定俯视动画](water_ragged_top.gif)
11: - [外缘二维动画](ragged_outer_preview.gif) / [内缘二维动画](ragged_inner_preview.gif)
12: - [静态预览](water_ragged_preview.png)
13: 
14: 三维预览由Blockbench真实场景逐帧截图生成。两个GIF均720×720、96帧、20fps、4.8秒循环，深色底仅用于展示。原贴图与原生截图背景透明。
15: 
16: ## 源文件
17: 
18: - `ragged_splash_outer.aseprite` / `.png`：外缘水膜毛边，256×40，16帧，每帧100ms；图集256×640。
19: - `ragged_splash_inner.aseprite` / `.png`：内缘，独立错相和较短水舌，同规格。
20: - 两个Aseprite文件均四层：连续薄水膜浪根、不规则圆钝短舌与细叉、水膜局部高光、断离珠滴与细碎水团。
21: - `broken_saw_flow.aseprite` / `.png`：从v7原样保留的破碎环体。
22: - `周身水环_细碎水膜毛边_v8.bbmodel`：可编辑Blockbench工程，全部贴图内嵌。
23: - `draw-ragged-water.lua`：实际通过Aseprite MCP执行的绘制脚本。
24: - `用户参考_水环毛边.png`：用户提供，仅供本次视觉参考。
25: - `frames-fixed/`、`frames-top/`、`export-previews.ps1`：原生透明帧与GIF导出脚本。
26: 
27: ## 保留结构的检查
28: 
29: 与v7模型逐项比对，网格顶点、面、UV、层级、组、动画均一致；主体贴图逐字节一致。只替换两张浪缘贴图，结果见 `validation.json`。
30: 
31: 根组仍2.4秒逆时针一圈；纹理仍1.6秒局部水舌伸长、珠滴断离、回缩循环，不增加整周UV滚动。0与4.8秒的根变换、贴图帧完全一致。斜视和俯视各96帧均有不同画面。
32: 
33: 为严格保留v7结构，部分网格名称仍包含旧称“锯齿”；实际贴图已改为本版水膜毛边。
34: 
35: 重开Blockbench后，运行 `preview-water-unlit.js` 可复现本次水体原色展示；只关闭水体材质的会话明暗衰减，人物仍正常受明暗影响。该会话设置不存入模型。
36: 
37: 旧版保留。此次未修改游戏代码，未编译、打包或进行游戏内验证。
38: 
39: 
</source_file>
