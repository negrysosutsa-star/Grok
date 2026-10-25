package ft.triggerbot.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationUtil {
    private long time = 0L;

    public void update() { time = System.nanoTime(); }
    public void setTime() { time = 0L; }

    public double frameFactor() {
        long now = System.nanoTime();
        double f = time > 0L ? (now - time) / 1.66666667E7 : 1.0;
        time = now;
        return MathHelper.clamp(f, 0.05, 3.0);
    }

    public void softLook(double speed, float targetPitch, float targetYaw) {
        var p = MinecraftClient.getInstance().player;
        if (p == null) return;
        double ff = frameFactor();
        float yaw = p.getYaw();
        float pitch = p.getPitch();
        double dy = RandomUtil.wrapYaw(targetYaw - yaw);
        double dp = targetPitch - pitch;
        if (Math.sqrt(dy * dy + dp * dp) < 0.05) return;

        double step = MathHelper.clamp(speed * 0.04 * ff, 0.01, 0.95);
        double rdx = RandomUtil.snap(RandomUtil.mouseGcd(), dy * step);
        double rdp = RandomUtil.snap(RandomUtil.mouseGcd(), dp * step);
        double gcd = RandomUtil.mouseGcd();
        if (rdx == 0.0 && Math.abs(dy) > gcd) rdx = Math.signum(dy) * gcd;
        if (rdp == 0.0 && Math.abs(dp) > gcd) rdp = Math.signum(dp) * gcd;

        p.setYaw(yaw + (float) rdx);
        p.setPitch(MathHelper.clamp(pitch + (float) rdp, -90f, 90f));
    }

    public void softLookAt(double speed, Vec3d point) {
        float[] r = angles(point);
        softLook(speed, r[1], r[0]);
    }

    public static float[] angles(Vec3d target) {
        var p = MinecraftClient.getInstance().player;
        Vec3d eye = p.getEyePos();
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
        float pitch = (float) -(Math.atan2(dy, dist) * 180.0 / Math.PI);
        return new float[]{yaw, pitch};
    }
}