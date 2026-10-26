package ft.triggerbot.aim;

import ft.triggerbot.util.RandomUtil;
import java.security.SecureRandom;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Порт LegitAim из System клиента.
 * Плавная легит-наводка с фазовыми циклами и GCD-снаппингом.
 */
public class LegitAim extends AimMode {

    // ─── Настройки ────────────────────────────────────────────────
    public double speed       = 1.0;   // базовая скорость (множитель лерпа)
    public double smoothing   = 1.0;   // сглаживание
    public double maxLerp     = 1.5;   // верхний предел доли угла за тик
    public double minLerp     = 0.05;  // нижний предел
    public boolean randomize  = true;  // фазовые случайные колебания

    // ─── Состояние ────────────────────────────────────────────────
    private final SecureRandom rng = new SecureRandom();
    private final double[] phaseTable = new double[15]; // 5 фаз × 3 компонента
    private final float[]  lerp5      = new float[5];   // скользящее среднее лерпа
    private int    phase      = 0;
    private double phasePos   = 0.0;
    private long   phaseStart = 0L;
    private long   phaseNano  = 0L;
    private float  lastLerp   = 0f;
    private long   lerp5Time  = 0L;
    private int    lerp5Idx   = 0;

    public LegitAim() { super("Легит"); }

    // ─── AimMode API ──────────────────────────────────────────────

    @Override
    public void onNewTarget() {
        phaseNano  = System.nanoTime();
        phase      = 0;
        phasePos   = 0.0;
        phaseStart = System.currentTimeMillis();
        lastLerp   = 0f;
        lerp5Time  = System.currentTimeMillis();
        lerp5Idx   = 0;
        Arrays.fill(lerp5, 0f);
        rebuildPhaseTable();
    }

    @Override
    public void onReset() {
        phase      = 0;
        phasePos   = 0.0;
        phaseStart = System.currentTimeMillis();
        phaseNano  = System.nanoTime();
        lastLerp   = 0f;
        lerp5Time  = System.currentTimeMillis();
        lerp5Idx   = 0;
        Arrays.fill(lerp5, 0f);
        rebuildPhaseTable();
    }

    @Override
    public void tick(long nowMs) {
        // Продвигаем фазу
        long elapsed = nowMs - phaseStart;
        double cycleDur = 3000.0 + phaseTable[0] * 2000.0;
        phasePos = elapsed / cycleDur;
        if (phasePos >= 1.0) {
            phase      = (phase + 1) % 5;
            phasePos   = 0.0;
            phaseStart = nowMs;
        }
    }

    /**
     * Основной метод — вычисляет дельты yaw/pitch для данного тика.
     * @param dYaw    угловая дельта до цели по yaw
     * @param dPitch  угловая дельта до цели по pitch
     * @param totalAngle суммарный угол до цели (градусы)
     * @param frameFactor frame-factor из RotationUtil
     * @param nowMs   текущий System.currentTimeMillis()
     * @param targetStartMs когда взяли цель
     * @return float[]{moveYaw, movePitch}
     */
    @Override
    public float[] compute(double dYaw, double dPitch,
                           double totalAngle, double frameFactor,
                           long nowMs, long targetStartMs) {
        float angle = (float) totalAngle;

        // Адаптивный лерп-фактор на основе угла и фазы
        float lerpFactor = computeLerpFactor(angle);
        lerp5[lerp5Idx] = lerpFactor;
        lerp5Idx = (lerp5Idx + 1) % lerp5.length;

        float avgLerp = avgLerp5();
        float angleMod = angleModifier(angle);

        double d = smoothing > 0 ? smoothing : 1.0;
        double step = (frameFactor / 3.0) * speed / d;
        double mn   = minLerp, mx = maxLerp;
        if (mn > mx) mn = mx;

        double lerp = MathHelper.clamp(step * avgLerp * angleMod, mn, mx);

        double moveYaw   = dYaw   * lerp;
        double movePitch = dPitch * lerp; // LegitAim двигает оба угла

        // GCD-снаппинг
        double gcd = RandomUtil.mouseGcd();
        moveYaw   = RandomUtil.snap(gcd, moveYaw);
        movePitch = RandomUtil.snap(gcd, movePitch);

        // Если снаппинг скушал всё — минимальный шаг
        if (moveYaw == 0.0 && Math.abs(dYaw) > gcd)
            moveYaw = Math.signum(dYaw) * gcd;
        if (movePitch == 0.0 && Math.abs(dPitch) > gcd)
            movePitch = Math.signum(dPitch) * gcd;

        return new float[]{ (float) moveYaw, (float) movePitch };
    }

    // ─── Приватные хелперы ────────────────────────────────────────

    private float computeLerpFactor(float totalAngle) {
        boolean rand = randomize;
        long dt = System.currentTimeMillis() - lerp5Time;
        lerp5Time = System.currentTimeMillis();

        float f;
        if (rand) {
            f = switch (phase) {
                case 0 -> 0.6f + rng.nextFloat() * 0.4f;
                case 1 -> 0.8f + rng.nextFloat() * 0.3f;
                case 2 -> 0.5f + rng.nextFloat() * 0.3f;
                case 3 -> 0.7f + rng.nextFloat() * 0.5f;
                case 4 -> 0.4f + rng.nextFloat() * 0.4f;
                default -> 0.7f + rng.nextFloat() * 0.4f;
            } * (0.9f + rng.nextFloat() * 0.3f);

            if (dt < 45L)        f *= 0.7f + rng.nextFloat() * 0.4f;
            else if (dt > 300L)  f *= 1.1f + rng.nextFloat() * 0.4f;

            if (totalAngle > 120f)     f *= 1.3f;
            else if (totalAngle < 15f) f *= 0.5f + rng.nextFloat() * 0.4f;

            if (lastLerp > 0f) {
                float diff = f - lastLerp;
                if (Math.abs(diff) > 0.35f) f = lastLerp + Math.signum(diff) * 0.35f;
            }
        } else {
            f = 1.0f;
            if (totalAngle > 120f)     f *= 1.3f;
            else if (totalAngle < 15f) f *= 0.7f;
        }

        f = MathHelper.clamp(f, 0.15f, 1.8f);
        lastLerp = f;
        return f;
    }

    private float avgLerp5() {
        float sum = 0f; int cnt = 0;
        for (float v : lerp5) if (v > 0f) { sum += v; cnt++; }
        return cnt > 0 ? sum / cnt : (lastLerp > 0f ? lastLerp : 1.0f);
    }

    private float angleModifier(float angle) {
        if (!randomize)
            return angle < 20f ? 1.0f : (angle > 90f ? 0.95f : 0.97f);
        return angle < 20f
            ? 0.95f + rng.nextFloat() * 0.1f
            : (angle > 90f
                ? 0.8f  + rng.nextFloat() * 0.3f
                : 0.85f + rng.nextFloat() * 0.25f);
    }

    private void rebuildPhaseTable() {
        long base = phaseNano;
        for (int i = 0; i < 5; i++) {
            int j = i * 3;
            long k = base + (long) i * 7919L;
            phaseTable[j]     = sinRand(k,     0.3, 0.7);
            phaseTable[j + 1] = sinRand(k + 1, 0.2, 0.9);
            phaseTable[j + 2] = sinRand(k + 2, 0.1, 0.8);
        }
    }

    private static double sinRand(long seed, double lo, double hi) {
        double v = Math.abs(Math.sin(seed * 0.001)) % 1.0;
        return lo + v * (hi - lo);
    }
}
