package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.mixin.client.LocalPlayerAnchorAccess;
import com.bslx.XueXiMod.network.FlowCorrectionAckC2SPacket;
import com.bslx.XueXiMod.network.FlowMoveC2SPacket;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowClientBridge;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowMotionIntents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 本地玩家和驾驶根在原生 move 前消费服务端指令。其他实体只显示服务器位置。 */
public final class FlowClientMotion implements FlowClientBridge.Source {
    private static final FlowClientMotion INSTANCE = new FlowClientMotion();
    private final Map<UUID, Command> commands = new HashMap<>();
    private final Map<UUID, FlowClientBridge.Receipt> receipts = new HashMap<>();
    private final Map<UUID, Boolean> consumed = new HashMap<>();

    private FlowClientMotion() {
    }

    public static void bind() {
        FlowClientBridge.bind(INSTANCE);
    }

    @Override
    public Vec3 combine(Entity mover, Vec3 nativeMove) {
        if (!drives(mover) || nativeMove == null || consumed.containsKey(mover.getUUID())) {
            return nativeMove;
        }
        Command command = live(mover);
        if (command == null) {
            return nativeMove;
        }
        consumed.put(mover.getUUID(), Boolean.TRUE);
        boolean jumped = mover instanceof LocalPlayer localPlayer && localPlayer.input.jumping && localPlayer.onGround();
        receipts.put(mover.getUUID(), new FlowClientBridge.Receipt(command.instanceId, command.rootId, command.epoch,
                command.step, command.rising, nativeMove.y, jumped));
        double nextY = nativeMove.y;
        if (command.rising && nativeMove.y < command.riseFloor) {
            nextY = command.riseFloor;
        }
        return new Vec3(nativeMove.x + command.horizontalX, nextY, nativeMove.z + command.horizontalZ);
    }

    @Override
    public void beginStep(Entity mover) {
        if (mover != null) {
            consumed.remove(mover.getUUID());
        }
    }

    @Override
    public void finishStep(Entity mover) {
        if (!drives(mover) || consumed.containsKey(mover.getUUID()) || !mover.isAlive() || mover.isRemoved()) {
            return;
        }
        if (mover instanceof Player player && player.isSleeping()) {
            return;
        }
        if (live(mover) == null) {
            return;
        }
        mover.move(MoverType.SELF, mover.getDeltaMovement());
    }

    @Override
    public void acceptIntent(boolean removed, boolean clearInstance, UUID instanceId, UUID rootId, long epoch, long step,
                             double horizontalX, double horizontalZ, boolean rising, double riseFloor) {
        if (clearInstance) {
            commands.entrySet().removeIf(entry -> instanceId.equals(entry.getValue().instanceId));
            return;
        }
        if (removed) {
            Command current = commands.get(rootId);
            if (current != null && instanceId.equals(current.instanceId)) {
                commands.remove(rootId);
                receipts.remove(rootId);
            }
            return;
        }
        Command previous = commands.get(rootId);
        if (previous != null && previous.instanceId.equals(instanceId) && previous.epoch == epoch && step < previous.step) {
            return;
        }
        long seenTick = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        commands.put(rootId, new Command(instanceId, rootId, epoch, step, horizontalX, horizontalZ, rising, riseFloor, seenTick));
    }

    @Override
    public void acceptCorrection(UUID rootId, int sequence, double worldX, double worldY, double worldZ, float yaw,
                                 float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || rootId == null) {
            return;
        }
        Entity root = player.getRootVehicle();
        if (!rootId.equals(player.getUUID()) && !rootId.equals(root.getUUID())) {
            return;
        }
        Vec3 kept = root.getDeltaMovement();
        boolean flying = player.getAbilities().flying;
        root.absMoveTo(worldX, worldY, worldZ, yaw, pitch);
        root.setDeltaMovement(kept);
        root.xo = worldX;
        root.yo = worldY;
        root.zo = worldZ;
        player.getAbilities().flying = flying;
        LocalPlayerAnchorAccess access = (LocalPlayerAnchorAccess) player;
        access.xuexiAnchorX(worldX);
        access.xuexiAnchorY(worldY);
        access.xuexiAnchorZ(worldZ);
        access.xuexiAnchorYaw(yaw);
        access.xuexiAnchorPitch(pitch);
        access.xuexiAnchorGround(root.onGround());
        receipts.remove(player.getUUID());
        receipts.remove(root.getUUID());
        FlowCorrectionAckC2SPacket.send(sequence);
        sendCurrent(player, root);
    }

    @Override
    public FlowClientBridge.Receipt takeReceipt(Entity mover) {
        if (mover == null) {
            return null;
        }
        return receipts.get(mover.getUUID());
    }

    @Override
    public double riseFloor(Entity mover) {
        Command command = mover == null ? null : live(mover);
        if (command == null || !command.rising) {
            return 0.0D;
        }
        return command.riseFloor;
    }

    public static boolean wrapSend(Entity mover, net.minecraft.network.protocol.Packet<?> packet) {
        if (!(mover instanceof LocalPlayer player) || packet == null) {
            return false;
        }
        Entity root = player.getRootVehicle();
        FlowClientBridge.Receipt receipt = INSTANCE.receipts.get(root.getUUID());
        if (receipt == null) {
            return false;
        }
        if (packet instanceof ServerboundMoveVehiclePacket) {
            FlowMoveC2SPacket.send(new FlowMoveC2SPacket(FlowMoveC2SPacket.VEHICLE, root.getX(), root.getY(), root.getZ(),
                    root.getYRot(), root.getXRot(), root.onGround(), receipt.instanceId, receipt.rootId, receipt.epoch,
                    receipt.step, receipt.rising, receipt.nativeVertical, receipt.playerJumped));
            return true;
        }
        if (packet instanceof ServerboundMovePlayerPacket.PosRot) {
            sendPlayer(FlowMoveC2SPacket.PLAYER_PLACE_TURN, player, receipt);
            return true;
        }
        if (packet instanceof ServerboundMovePlayerPacket.Pos) {
            sendPlayer(FlowMoveC2SPacket.PLAYER_PLACE, player, receipt);
            return true;
        }
        if (packet instanceof ServerboundMovePlayerPacket.Rot) {
            sendPlayer(FlowMoveC2SPacket.PLAYER_FACING, player, receipt);
            return true;
        }
        if (packet instanceof ServerboundMovePlayerPacket.StatusOnly) {
            sendPlayer(FlowMoveC2SPacket.PLAYER_STATUS, player, receipt);
            return true;
        }
        return false;
    }

    private FlowClientBridge.Receipt receiptFor(LocalPlayer player, Entity root) {
        FlowClientBridge.Receipt receipt = receipts.get(root.getUUID());
        if (receipt == null && root != player) {
            receipt = receipts.get(player.getUUID());
        }
        if (receipt != null) {
            return receipt;
        }
        Command command = live(root);
        if (command == null && root != player) {
            command = live(player);
        }
        if (command == null) {
            return null;
        }
        return new FlowClientBridge.Receipt(command.instanceId, command.rootId, command.epoch, command.step,
                command.rising, root.getDeltaMovement().y, false);
    }

    private static void sendCurrent(LocalPlayer player, Entity root) {
        FlowClientBridge.Receipt receipt = INSTANCE.receiptFor(player, root);
        if (receipt == null) {
            player.connection.send(new ServerboundMovePlayerPacket.PosRot(player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.onGround()));
            return;
        }
        if (root != player) {
            FlowMoveC2SPacket.send(new FlowMoveC2SPacket(FlowMoveC2SPacket.VEHICLE, root.getX(), root.getY(), root.getZ(),
                    root.getYRot(), root.getXRot(), root.onGround(), receipt.instanceId, receipt.rootId, receipt.epoch,
                    receipt.step, receipt.rising, receipt.nativeVertical, false));
            return;
        }
        sendPlayer(FlowMoveC2SPacket.PLAYER_PLACE_TURN, player, receipt);
    }

    private static void sendPlayer(int kind, LocalPlayer player, FlowClientBridge.Receipt receipt) {
        FlowMoveC2SPacket.send(new FlowMoveC2SPacket(kind, player.getX(), player.getY(), player.getZ(), player.getYRot(),
                player.getXRot(), player.onGround(), receipt.instanceId, receipt.rootId, receipt.epoch, receipt.step,
                receipt.rising, receipt.nativeVertical, receipt.playerJumped));
    }

    private Command live(Entity mover) {
        Command command = commands.get(mover.getUUID());
        if (command == null) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getGameTime() > command.seenTick + 40L) {
            commands.remove(mover.getUUID());
            return null;
        }
        return command;
    }

    private static boolean drives(Entity mover) {
        return FlowMotionIntents.clientDriven(mover) && !(mover instanceof AbstractMinecart);
    }

    private static final class Command {
        private final UUID instanceId;
        private final UUID rootId;
        private final long epoch;
        private final long step;
        private final double horizontalX;
        private final double horizontalZ;
        private final boolean rising;
        private final double riseFloor;
        private final long seenTick;

        private Command(UUID instanceId, UUID rootId, long epoch, long step, double horizontalX, double horizontalZ,
                        boolean rising, double riseFloor, long seenTick) {
            this.instanceId = instanceId;
            this.rootId = rootId;
            this.epoch = epoch;
            this.step = step;
            this.horizontalX = horizontalX;
            this.horizontalZ = horizontalZ;
            this.rising = rising;
            this.riseFloor = riseFloor;
            this.seenTick = seenTick;
        }
    }
}
