package com.bslx.XueXiMod.client.action.skinned;

import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.runtime.ActionSampleContext;
import com.bslx.XueXiMod.client.action.ActionRenderQueries;
import com.bslx.XueXiMod.client.action.ActionVisualCatalog;
import com.bslx.XueXiMod.client.action.RenderPoseScope;
import com.bslx.XueXiMod.client.action.RenderSpaceCapture;
import com.bslx.XueXiMod.client.action.SkinBinding;
import com.bslx.XueXiMod.client.action.SlashPlaneAlign;
import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.PlayerSkinMesh;
import com.bslx.XueXiMod.client.action.mesh.SkinLease;
import com.bslx.XueXiMod.client.action.mesh.SkinPalette;
import com.bslx.XueXiMod.client.action.mesh.SkinWorkspacePool;
import com.bslx.XueXiMod.client.action.skinned.attachments.AttachmentSupport;
import com.bslx.XueXiMod.client.action.skinned.attachments.SkinAttachmentBridge;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 挂在当前 RenderPoseScope 帧上的一次玩家绘制。采样只发生在打开时。
 * 租约由本对象关闭，附件不能关主租约。
 */
public final class SkinSession implements SkinBinding {
    private final RenderPoseScope.View view;
    private final Object model;
    private final Object player;
    private ActionSampleContext sample;
    private SkinAssets assets;
    private PlayerMeshKind kind;
    private RenderPlan plan;
    private AttachmentSupport support;
    private PlayerSkinMesh mesh;
    private SkinPalette palette;
    private ImmutableSkinFrame.Clock clock;
    private float frozenPartial;
    private Vec3f frozenForward;
    private Vec3f frozenNormal;
    private String alignmentNote = "";
    private SkinLease lease;
    private ImmutableSkinFrame frame;
    private boolean committed;
    private boolean closed;

    private SkinSession(RenderPoseScope.View view, Object model, Object player) {
        this.view = view;
        this.model = model;
        this.player = player;
    }

    public static void openThirdPerson(Object model, Player player, PlayerRenderer renderer, float partialTick) {
        SkinSession session = new SkinSession(RenderPoseScope.View.THIRD_PERSON, model, player);
        try {
            session.chooseThird(player, renderer, partialTick);
        } catch (RuntimeException failure) {
            session.releaseLease();
            session.plan = session.fallback(player, RenderPlan.Mode.RIGID, "皮肤准备失败，改为刚性回退");
        }
        if (session.plan == null) {
            session.plan = session.fallback(player, RenderPlan.Mode.VANILLA, "没有活动动作");
        }
        RenderPoseScope.attachSkin(session);
    }

    public static void openFirstPerson(Object model, Player player, float partialTick) {
        SkinSession session = new SkinSession(RenderPoseScope.View.FIRST_PERSON, model, player);
        try {
            SkinAssets.recheckRegistry();
            session.sample = ActionRenderQueries.inspectLive(player, partialTick);
            session.assets = SkinAssets.current();
            session.kind = kindOf(player);
        } catch (RuntimeException failure) {
            java.util.UUID ownerId = player == null ? new java.util.UUID(0L, 0L) : player.getUUID();
            session.sample = ActionSampleContext.inactive(ownerId);
            session.assets = SkinAssets.current();
            session.kind = kindOf(player);
        }
        RenderPoseScope.attachSkin(session);
    }

    public static SkinSession top() {
        SkinBinding binding = RenderPoseScope.skinBinding();
        if (binding instanceof SkinSession session) {
            return session;
        }
        return null;
    }

    @Override
    public ActionSampleContext sample() {
        return this.sample;
    }

    @Override
    public boolean skipRigidWriter() {
        return this.view == RenderPoseScope.View.THIRD_PERSON
                && this.plan != null
                && this.plan.mode() == RenderPlan.Mode.SKINNED;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.releaseLease();
    }

    public boolean drawsBody(Object rendered) {
        return !this.closed
                && this.view == RenderPoseScope.View.THIRD_PERSON
                && this.plan != null
                && this.plan.mode() == RenderPlan.Mode.SKINNED
                && this.model == rendered
                && this.mesh != null
                && this.palette != null
                && this.lease != null
                && this.lease.isOpen();
    }

    public void installBody(PlayerModel<?> playerModel) {
        if (this.frame != null || playerModel == null || playerModel.body == null) {
            return;
        }
        this.installAt(
                finite(playerModel.body.x) / 16.0F,
                finite(playerModel.body.y) / 16.0F,
                finite(playerModel.body.z) / 16.0F);
    }

    /**
     * 身体绘制点才读当前 worldToModel。计划阶段只留采样、参考架和局部帧。
     */
    void installAt(float pivotAcross, float pivotUpward, float pivotAhead) {
        if (this.frame != null || this.mesh == null || this.palette == null || this.plan == null || this.clock == null) {
            return;
        }
        OpenMatrix4f spin = this.resolveSpin();
        OpenMatrix4f alignment = SkinnedBodyDraw.bodyAlignment(spin, pivotAcross, pivotUpward, pivotAhead);
        this.frame = ImmutableSkinFrame.captureThirdPerson(this.plan, this.mesh, this.palette, alignment, this.clock);
        this.committed = true;
    }

    public void hold(SkinLease opened, ImmutableSkinFrame installed) {
        this.lease = opened;
        this.frame = installed;
        this.committed = true;
    }

    public boolean bodyStarted() {
        return this.committed;
    }

    public boolean owns(Object candidate) {
        return candidate != null && candidate == this.player;
    }

    public Object player() {
        return this.player;
    }

    public Object model() {
        return this.model;
    }

    public RenderPoseScope.View view() {
        return this.view;
    }

    public RenderPlan plan() {
        return this.plan;
    }

    public AttachmentSupport support() {
        return this.support;
    }

    public SkinAssets assets() {
        return this.assets;
    }

    public PlayerMeshKind kind() {
        return this.kind;
    }

    public ImmutableSkinFrame frame() {
        return this.frame;
    }

    String alignmentNote() {
        return this.alignmentNote;
    }

    void recordPlane(String reason) {
        if (reason != null && !reason.isBlank()) {
            this.alignmentNote = reason;
        }
    }

    public SkinLease lease() {
        return this.lease;
    }

    private void chooseThird(Player player, PlayerRenderer renderer, float partialTick) {
        SkinAssets.recheckRegistry();
        this.sample = ActionRenderQueries.inspectLive(player, partialTick);
        this.assets = SkinAssets.current();
        this.kind = kindOf(player);
        long generation = this.assets.resourceGeneration();
        if (this.sample == null || !this.sample.active()) {
            this.plan = this.fallback(player, RenderPlan.Mode.VANILLA, "没有活动动作");
            return;
        }
        if (!this.assets.usable(this.kind) || !this.assets.matches(this.sample.armature())) {
            String reason = this.assets.usable(this.kind)
                    ? "动作骨架和已发布皮肤骨架不一致，改为刚性回退"
                    : this.assets.diagnostic();
            this.plan = this.fallback(player, RenderPlan.Mode.RIGID, reason);
            return;
        }
        if (!(player instanceof AbstractClientPlayer clientPlayer) || renderer == null) {
            this.plan = this.fallback(player, RenderPlan.Mode.RIGID, "玩家渲染器不能做装备预检");
            return;
        }
        try {
            this.support = SkinAttachmentBridge.preflight(clientPlayer, renderer, this.kind, generation);
        } catch (RuntimeException failure) {
            this.plan = this.fallback(player, RenderPlan.Mode.RIGID, "附件预检失败，改为刚性回退");
            return;
        }
        if (this.support == null || !this.support.supported()) {
            String reason = this.support == null ? "" : this.support.reason();
            if (reason == null || reason.isBlank()) {
                reason = "附件预检不支持，改为刚性回退";
            }
            this.plan = this.fallback(player, RenderPlan.Mode.RIGID, reason);
            return;
        }
        try {
            Pose pose = this.sample.pose();
            if (pose == null || this.sample.armature() == null) {
                throw new IllegalArgumentException("pose missing");
            }
            this.mesh = this.assets.mesh(this.kind);
            this.palette = SkinPalette.prepare(this.sample.armature(), pose);
            String reason = SkinAttachmentBridge.armorPaletteReason(this.support, this.palette);
            if (reason != null) {
                this.plan = this.fallback(player, RenderPlan.Mode.RIGID, "护甲调色板不兼容：" + reason);
                return;
            }
            this.clock = clock(this.sample);
            this.frozenPartial = Float.isFinite(partialTick) ? partialTick : 0.0F;
            this.freezePlane(this.sample);
            this.lease = SkinWorkspacePool.currentThread().open(this.mesh);
            this.plan = this.fallback(player, RenderPlan.Mode.SKINNED, "授权网格和骨架匹配");
        } catch (RuntimeException failure) {
            this.releaseLease();
            this.plan = this.fallback(player, RenderPlan.Mode.RIGID, "蒙皮准备失败，改为刚性回退");
        }
    }

    private void freezePlane(ActionSampleContext context) {
        this.frozenForward = null;
        this.frozenNormal = null;
        if (context == null || !context.hasSlashFrame()) {
            return;
        }
        ResourceLocation visualKey = context.visualActionId();
        Vec3f forward = visualKey == null ? null : ActionVisualCatalog.referenceForward(visualKey);
        Vec3f normal = visualKey == null ? null : ActionVisualCatalog.referenceNormal(visualKey);
        if (visualKey == null || forward == null || normal == null) {
            this.alignmentNote = "有挥斩帧但没有平面校准配方，身体保持普通蒙皮";
            return;
        }
        this.frozenForward = new Vec3f(forward.coordinateX, forward.coordinateY, forward.coordinateZ);
        this.frozenNormal = new Vec3f(normal.coordinateX, normal.coordinateY, normal.coordinateZ);
    }

    private OpenMatrix4f resolveSpin() {
        if (this.sample == null || !this.sample.hasSlashFrame()) {
            return new OpenMatrix4f();
        }
        if (this.frozenForward == null || this.frozenNormal == null) {
            if (this.alignmentNote.isBlank()) {
                this.alignmentNote = "有挥斩帧但没有平面校准配方，身体保持普通蒙皮";
            }
            return new OpenMatrix4f();
        }
        Player body = this.player instanceof Player player ? player : null;
        OpenMatrix4f spin = SlashPlaneAlign.modelAlignment(
                this.sample.directionX(), this.sample.directionY(), this.sample.directionZ(),
                this.sample.planeX(), this.sample.planeY(), this.sample.planeZ(),
                bodyYaw(body, this.frozenPartial),
                RenderSpaceCapture.worldToModel(),
                this.frozenForward,
                this.frozenNormal);
        if (spin == null) {
            this.alignmentNote = "挥斩对齐在身体绘制时不可用";
            return new OpenMatrix4f();
        }
        return spin;
    }

    static SkinSession readyForBodyCheck(
            ActionSampleContext sample,
            Vec3f referenceForward,
            Vec3f referenceNormal,
            float partialTick,
            PlayerSkinMesh mesh,
            SkinPalette palette,
            ImmutableSkinFrame.Clock clock,
            RenderPlan plan) {
        SkinSession session = new SkinSession(RenderPoseScope.View.THIRD_PERSON, new Object(), new Object());
        session.sample = sample;
        session.frozenPartial = partialTick;
        session.frozenForward = referenceForward == null
                ? null
                : new Vec3f(referenceForward.coordinateX, referenceForward.coordinateY, referenceForward.coordinateZ);
        session.frozenNormal = referenceNormal == null
                ? null
                : new Vec3f(referenceNormal.coordinateX, referenceNormal.coordinateY, referenceNormal.coordinateZ);
        session.mesh = mesh;
        session.palette = palette;
        session.clock = clock;
        session.plan = plan;
        session.kind = plan.bodyKind();
        return session;
    }

    private RenderPlan fallback(Player player, RenderPlan.Mode mode, String reason) {
        PlayerMeshKind bodyKind = this.kind == null ? PlayerMeshKind.NORMAL : this.kind;
        long generation = this.assets == null ? 0L : this.assets.resourceGeneration();
        java.util.UUID ownerId = player == null ? new java.util.UUID(0L, 0L) : player.getUUID();
        int entityId = player == null ? -1 : player.getId();
        return RenderPlan.of(
                mode,
                ownerId,
                entityId,
                RenderPlan.Perspective.THIRD_PERSON,
                RenderPlan.Hand.NONE,
                bodyKind,
                generation,
                reason);
    }

    private static ImmutableSkinFrame.Clock clock(ActionSampleContext context) {
        String actionName = "active-action";
        if (context.visualActionId() != null) {
            actionName = context.visualActionId().toString();
        } else if (context.actionId() != null) {
            actionName = context.actionId().toString();
        }
        float elapsed = context.elapsedSeconds();
        float progress = context.normalizedProgress();
        int revision = context.revision();
        if (!Float.isFinite(elapsed) || elapsed < 0.0F || !Float.isFinite(progress) || progress < 0.0F || progress > 1.0F || revision < 0) {
            throw new IllegalArgumentException("skin clock is not usable");
        }
        return ImmutableSkinFrame.Clock.of(actionName, context.startGameTick(), elapsed, progress, revision);
    }

    private static float bodyYaw(Player player, float partialTick) {
        if (player == null) {
            return 0.0F;
        }
        float safePartial = Float.isFinite(partialTick) ? Mth.clamp(partialTick, 0.0F, 1.0F) : 0.0F;
        return Mth.rotLerp(safePartial, player.yBodyRotO, player.yBodyRot);
    }

    private static float finite(float coordinate) {
        return Float.isFinite(coordinate) ? coordinate : 0.0F;
    }

    private static PlayerMeshKind kindOf(Player player) {
        if (player instanceof AbstractClientPlayer clientPlayer && "slim".equals(clientPlayer.getModelName())) {
            return PlayerMeshKind.SLIM;
        }
        return PlayerMeshKind.NORMAL;
    }

    private void releaseLease() {
        SkinLease opened = this.lease;
        this.lease = null;
        if (opened != null) {
            opened.close();
        }
    }
}
