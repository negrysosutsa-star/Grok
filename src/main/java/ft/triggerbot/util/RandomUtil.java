package ft.triggerbot.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class RandomUtil {
    private static final Random random = Random.create();
    private RandomUtil() {}

    /** GCD мыши — из сенсы игрока */
    public static double mouseGcd() {
        double sens = MinecraftClient.getInstance().options.getMouseSensitivity().getValue();
        double d = sens * 0.6 + 0.2;
        return d * d * d * 1.2;
    }

    /** Алиас для совместимости с портом System (getDouble = mouseGcd) */
    public static double getDouble() { return mouseGcd(); }

    /** Wrap yaw в диапазон [-180, 180] */
    public static double wrapYaw(double v) {
        v %= 360.0;
        if (v > 180.0)  v -= 360.0;
        if (v < -180.0) v += 360.0;
        return v;
    }

    /** Алиас для System-совместимости */
    public static double getDoubleByDouble(double v) { return wrapYaw(v); }

    /** GCD-снаппинг: округлить delta до ближайшего кратного gcd */
    public static double snap(double gcd, double delta) {
        return gcd <= 1e-4 ? delta : delta - delta % gcd;
    }

    /** Алиас для System-совместимости */
    public static double getDoubleByDoubleDouble(double gcd, double delta) {
        return snap(gcd, delta);
    }

    /** clamp float (из System TpsTracker) */
    public static float getFloatByFloatFloatFloat(float min, float max, float val) {
        return Math.max(min, Math.min(max, val));
    }

    /** Случайное целое [0, bound) */
    public static int nextInt(int bound) {
        if (bound <= 0) return 0;
        return (int)(Math.random() * bound);
    }

    /** Случайное double [min, max) через MathHelper */
    public static double nextDouble(double min, double max) {
        return MathHelper.nextDouble(random, min, max);
    }

    /** Случайное bool с вероятностью prob [0,1] */
    public static boolean chance(double prob) {
        return Math.random() < prob;
    }
}
