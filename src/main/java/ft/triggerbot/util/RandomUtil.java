package ft.triggerbot.util;

import net.minecraft.client.MinecraftClient;

public final class RandomUtil {
    private RandomUtil() {}

    public static double mouseGcd() {
        double sens = MinecraftClient.getInstance().options.getMouseSensitivity().getValue();
        double d = sens * 0.6 + 0.2;
        return d * d * d * 1.2;
    }

    public static double wrapYaw(double v) {
        v %= 360.0;
        if (v > 180.0) v -= 360.0;
        if (v < -180.0) v += 360.0;
        return v;
    }

    public static double snap(double gcd, double delta) {
        return gcd <= 1e-4 ? delta : delta - delta % gcd;
    }

    /** Случайное целое от 0 (включительно) до bound (не включая) */
    public static int nextInt(int bound) {
        if (bound <= 0) return 0;
        return (int)(Math.random() * bound);
    }
}
