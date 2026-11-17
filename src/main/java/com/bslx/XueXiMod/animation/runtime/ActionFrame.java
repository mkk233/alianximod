package com.bslx.XueXiMod.animation.runtime;

/**
 * 冻结挥斩帧。前向 F 是弧中心的世界前向，法线 N 是带符号的单位法线。
 * 右手系里 R = N × F。零向量、平行和非法数没有备用轴。
 * 长度平方、点积、投影和归一化先转成 double，写回 float 后再核对有限、单位和正交。
 */
public final class ActionFrame {
    public static final float EXTENT_FLOOR = 1.0E-5F;
    private static final double STORED_BAND = 1.0E-4D;

    private static final Basis ABSENT = new Basis(false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    private ActionFrame() {
    }

    public static Basis canonicalize(float forwardX, float forwardY, float forwardZ, float normalX, float normalY, float normalZ) {
        if (!finite(forwardX) || !finite(forwardY) || !finite(forwardZ) || !finite(normalX) || !finite(normalY) || !finite(normalZ)) {
            return null;
        }
        double forwardWideX = forwardX;
        double forwardWideY = forwardY;
        double forwardWideZ = forwardZ;
        double normalWideX = normalX;
        double normalWideY = normalY;
        double normalWideZ = normalZ;
        double forwardExtent = extent(forwardWideX, forwardWideY, forwardWideZ);
        if (!(forwardExtent > EXTENT_FLOOR)) {
            return null;
        }
        double unitForwardX = forwardWideX / forwardExtent;
        double unitForwardY = forwardWideY / forwardExtent;
        double unitForwardZ = forwardWideZ / forwardExtent;
        double along = normalWideX * unitForwardX + normalWideY * unitForwardY + normalWideZ * unitForwardZ;
        double perpendicularX = normalWideX - along * unitForwardX;
        double perpendicularY = normalWideY - along * unitForwardY;
        double perpendicularZ = normalWideZ - along * unitForwardZ;
        double normalExtent = extent(perpendicularX, perpendicularY, perpendicularZ);
        if (!(normalExtent > EXTENT_FLOOR)) {
            return null;
        }
        float storedForwardX = (float) (unitForwardX);
        float storedForwardY = (float) (unitForwardY);
        float storedForwardZ = (float) (unitForwardZ);
        float storedNormalX = (float) (perpendicularX / normalExtent);
        float storedNormalY = (float) (perpendicularY / normalExtent);
        float storedNormalZ = (float) (perpendicularZ / normalExtent);
        if (!storedFrame(storedForwardX, storedForwardY, storedForwardZ, storedNormalX, storedNormalY, storedNormalZ)) {
            return null;
        }
        return new Basis(true, storedForwardX, storedForwardY, storedForwardZ, storedNormalX, storedNormalY, storedNormalZ);
    }

    public static Basis resolve(boolean present, float forwardX, float forwardY, float forwardZ, float normalX, float normalY, float normalZ) {
        if (!present) {
            return ABSENT;
        }
        Basis basis = canonicalize(forwardX, forwardY, forwardZ, normalX, normalY, normalZ);
        return basis == null ? ABSENT : basis;
    }

    public static float crossX(float leftX, float leftY, float leftZ, float rightX, float rightY, float rightZ) {
        return leftY * rightZ - leftZ * rightY;
    }

    public static float crossY(float leftX, float leftY, float leftZ, float rightX, float rightY, float rightZ) {
        return leftZ * rightX - leftX * rightZ;
    }

    public static float crossZ(float leftX, float leftY, float leftZ, float rightX, float rightY, float rightZ) {
        return leftX * rightY - leftY * rightX;
    }

    private static boolean finite(float component) {
        return Float.isFinite(component);
    }

    private static double extent(double componentX, double componentY, double componentZ) {
        return Math.sqrt(componentX * componentX + componentY * componentY + componentZ * componentZ);
    }

    private static boolean storedFrame(float forwardX, float forwardY, float forwardZ, float normalX, float normalY, float normalZ) {
        if (!finite(forwardX) || !finite(forwardY) || !finite(forwardZ)
                || !finite(normalX) || !finite(normalY) || !finite(normalZ)) {
            return false;
        }
        double forwardExtent = extent(forwardX, forwardY, forwardZ);
        double normalExtent = extent(normalX, normalY, normalZ);
        double alignment = (double) forwardX * (double) normalX
                + (double) forwardY * (double) normalY
                + (double) forwardZ * (double) normalZ;
        return Math.abs(forwardExtent - 1.0D) <= STORED_BAND
                && Math.abs(normalExtent - 1.0D) <= STORED_BAND
                && Math.abs(alignment) <= STORED_BAND;
    }

    public static final class Basis {
        private final boolean present;
        private final float forwardX;
        private final float forwardY;
        private final float forwardZ;
        private final float normalX;
        private final float normalY;
        private final float normalZ;

        private Basis(boolean present, float forwardX, float forwardY, float forwardZ, float normalX, float normalY, float normalZ) {
            this.present = present;
            this.forwardX = forwardX;
            this.forwardY = forwardY;
            this.forwardZ = forwardZ;
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
        }

        public boolean present() {
            return this.present;
        }

        public float forwardX() {
            return this.forwardX;
        }

        public float forwardY() {
            return this.forwardY;
        }

        public float forwardZ() {
            return this.forwardZ;
        }

        public float normalX() {
            return this.normalX;
        }

        public float normalY() {
            return this.normalY;
        }

        public float normalZ() {
            return this.normalZ;
        }
    }
}
