package com.bslx.XueXiMod.shu.technique.release.deflection;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 按维度登记的偏转宿主。只保留显式注册的对象，不扫描护体或弧斩。
 * 强引用只在注册期间存在，停服和维度卸载会丢掉，不清其他系统。
 */
public final class DeflectionHosts {
    static final double SEGMENT_TIE = 1.0E-9;

    private static final HashMap<HostKey, DeflectionHost> HOSTS = new HashMap<>();

    private DeflectionHosts() {
    }

    /** 在主服务器线程登记宿主。资料不齐时直接返回。 */
    public static void register(DeflectionHost host) {
        if (!usable(host)) {
            return;
        }
        demandServerThread(host.level());
        HOSTS.put(host.key(), host);
    }

    /** 注销宿主，并忘掉它尚未关闭的占用。 */
    public static void unregister(HostKey key) {
        if (key == null) {
            return;
        }
        DeflectionHost host = HOSTS.get(key);
        if (host == null) {
            DeflectionService.forgetHosts(List.of(key));
            return;
        }
        ServerLevel level = readLevel(host);
        if (level != null && level.getServer() != null && !level.getServer().isSameThread()) {
            throw new IllegalStateException("偏转宿主只能在主服务器线程注销");
        }
        HOSTS.remove(key, host);
        DeflectionService.forgetHosts(List.of(key));
    }

    /**
     * 这个维度里已注册宿主的最近接触。没有宿主时为空，不表示偏转成功。
     * 平局取较小的 {@link HostKey}。
     */
    public static Optional<HostContact> contacts(ServerLevel level, Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
        List<HostContact> ordered = contactsOrdered(level, from, toward, projectileRadius, gameTick);
        if (ordered.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(ordered.get(0));
    }

    /**
     * 与 {@link #contacts} 同一套过滤，按线段参数和宿主身份排好。
     * 最近一项不合格时，调用方还可以看后面的接触。
     */
    public static List<HostContact> contactsOrdered(ServerLevel level, Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
        if (level == null || from == null || toward == null || !Double.isFinite(projectileRadius) || projectileRadius < 0.0) {
            return List.of();
        }
        if (level.getServer() != null && !level.getServer().isSameThread()) {
            return List.of();
        }
        ArrayList<HostContact> found = new ArrayList<>();
        for (DeflectionHost host : HOSTS.values()) {
            if (host == null || readLevel(host) != level || !readActive(host)) {
                continue;
            }
            Optional<HostContact> hit;
            try {
                hit = host.intersect(from, toward, projectileRadius, gameTick);
            } catch (RuntimeException failure) {
                continue;
            }
            if (hit == null || hit.isEmpty()) {
                continue;
            }
            HostContact contact = hit.get();
            if (contact.host() != host || !admissible(contact)) {
                continue;
            }
            found.add(contact);
        }
        found.sort(DeflectionHosts::orderContacts);
        return List.copyOf(found);
    }

    /**
     * 近战候选。过滤、排序和平局回退与 {@link #contactsOrdered} 相同。
     * 不在这里丢掉零长度线段，交由宿主的 {@link DeflectionHost#intersectMelee} 决定。
     */
    public static List<HostContact> meleeOrdered(ServerLevel level, DeflectionAttack attack, Vec3 from, Vec3 toward, long gameTick) {
        if (level == null || attack == null || from == null || toward == null) {
            return List.of();
        }
        if (!finitePoint(from) || !finitePoint(toward)) {
            return List.of();
        }
        if (level.getServer() != null && !level.getServer().isSameThread()) {
            return List.of();
        }
        ArrayList<HostContact> found = new ArrayList<>();
        for (DeflectionHost host : HOSTS.values()) {
            if (host == null || readLevel(host) != level || !readActive(host)) {
                continue;
            }
            Optional<HostContact> hit;
            try {
                hit = host.intersectMelee(attack, from, toward, gameTick);
            } catch (RuntimeException failure) {
                continue;
            }
            if (hit == null || hit.isEmpty()) {
                continue;
            }
            HostContact contact = hit.get();
            if (contact.host() != host || !admissible(contact)) {
                continue;
            }
            found.add(contact);
        }
        found.sort(DeflectionHosts::orderContacts);
        return List.copyOf(found);
    }

    /**
     * 这把宿主身份现在是否还包住该点。宿主已注销或不活跃时为假，调用方可以结束退出闩。
     */
    public static boolean covers(HostKey key, Vec3 point, double projectileRadius, long gameTick) {
        DeflectionHost host = find(key);
        if (host == null || !readActive(host) || point == null || !Double.isFinite(projectileRadius) || projectileRadius < 0.0) {
            return false;
        }
        try {
            return host.covers(point, projectileRadius, gameTick);
        } catch (RuntimeException failure) {
            return false;
        }
    }

    /** 停服时清空宿主和偏转账。 */
    public static void onServerStopping() {
        HOSTS.clear();
        DeflectionService.clearBook();
    }

    /** 维度卸载时丢掉该世界的宿主。 */
    public static void onLevelUnload(LevelAccessor level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ArrayList<HostKey> removed = new ArrayList<>();
        Iterator<Map.Entry<HostKey, DeflectionHost>> iterator = HOSTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<HostKey, DeflectionHost> entry = iterator.next();
            if (readLevel(entry.getValue()) == serverLevel) {
                removed.add(entry.getKey());
                iterator.remove();
            }
        }
        DeflectionService.forgetHosts(removed);
    }

    static void registerUnchecked(DeflectionHost host) {
        if (host == null || host.key() == null) {
            return;
        }
        HOSTS.put(host.key(), host);
    }

    static DeflectionHost find(HostKey key) {
        if (key == null) {
            return null;
        }
        return HOSTS.get(key);
    }

    /** 按宿主身份查找当前登记。已注销时为空。 */
    public static DeflectionHost findRegistered(HostKey key) {
        return find(key);
    }

    static Optional<HostContact> choose(List<HostContact> candidates) {
        HostContact best = null;
        if (candidates == null) {
            return Optional.empty();
        }
        for (HostContact candidate : candidates) {
            if (!admissible(candidate)) {
                continue;
            }
            if (best == null || prefers(candidate, best)) {
                best = candidate;
            }
        }
        return Optional.ofNullable(best);
    }

    static boolean admissible(HostContact contact) {
        if (contact == null || contact.host() == null || contact.host().key() == null || !readActive(contact.host())) {
            return false;
        }
        double segment = contact.segmentT();
        if (!Double.isFinite(segment) || segment < 0.0 || segment > 1.0) {
            return false;
        }
        return finitePoint(contact.point()) && finitePoint(contact.unitNormal());
    }

    private static int orderContacts(HostContact left, HostContact right) {
        double delta = left.segmentT() - right.segmentT();
        if (delta < -SEGMENT_TIE) {
            return -1;
        }
        if (delta > SEGMENT_TIE) {
            return 1;
        }
        return left.host().key().compareTo(right.host().key());
    }

    private static boolean prefers(HostContact candidate, HostContact incumbent) {
        double delta = candidate.segmentT() - incumbent.segmentT();
        if (delta < -SEGMENT_TIE) {
            return true;
        }
        if (delta > SEGMENT_TIE) {
            return false;
        }
        return candidate.host().key().compareTo(incumbent.host().key()) < 0;
    }

    private static boolean usable(DeflectionHost host) {
        return host != null && host.key() != null && host.ownerId() != null && host.level() != null;
    }

    private static void demandServerThread(ServerLevel level) {
        if (level.getServer() == null || !level.getServer().isSameThread()) {
            throw new IllegalStateException("偏转宿主只能在主服务器线程登记");
        }
    }

    private static ServerLevel readLevel(DeflectionHost host) {
        if (host == null) {
            return null;
        }
        try {
            return host.level();
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private static boolean readActive(DeflectionHost host) {
        try {
            return host.active();
        } catch (RuntimeException failure) {
            return false;
        }
    }

    private static boolean finitePoint(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}
