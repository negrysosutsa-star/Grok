package ft.triggerbot.aim;

import ft.triggerbot.util.RandomUtil;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Порт FreeAim из System клиента.
 * Магнит-стиль: тянет прицел к хитбоксу с инерцией, шумом и замедлением вблизи.
 */
public class FreeAim extends AimMode {

    // ─── Настройки ────────────────────────────────────────────────
    public double  speed           = 15.0;  // базовая скорость (5–20)
    public double  corrStrength    = 0.5;   // сила коррекции вне хитбокса
    public double  freedomDown     = 0.0;   // расширение мёртвой зоны вниз (-1..1)
    public double  slowThreshold   = 6.0;   // угол (°) начала замедления вблизи

    public boolean smoothEntry     = true;  // плавный вход
    public boolean useSens         = true;  // учитывать сенсу
    public boolean slowNear        = true;  // замедление вблизи
    public boolean noise           = true;  // шум при наводке

    // ─── Состояние ────────────────────────────────────────────────
    // Фазы осциллятора (обновляются при onNewTarget)
    private double phaseYaw, phasePitch;
    private double freqYaw, freqPitch;
    private double baseNoise;
    private double scaleMul;         // случайный масштаб [0.85, 1.15]

    // Плавный вход [0,1]
    private double entryProgress;

    // Инерция «выход за хитбокс»
    private double outsideMomentum;

    // Smoothstep на вход в цель
    private double smoothIn;

    // Флаг «большой угол» (> 80°) — режим размаха
    private boolean swingMode;
    private double  swingAmp, swingFreq, swingAtten;
    private double  swingPhase;

    // Дрейф offset (медленное смещение)
    private double driftOffset;

    public FreeAim() { super("Свободная"); }

    // ─── AimMode API ──────────────────────────────────────────────

    @Override
    public void onNewTarget() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        phaseYaw   = r.nextDouble(Math.PI * 2);
        phasePitch = r.nextDouble(Math.PI * 2);
        freqYaw    = r.nextDouble(0.06, 0.18);
        freqPitch  = r.nextDouble(0.04, 0.14);
        baseNoise  = r.nextDouble(-0.08, 0.08);
        scaleMul   = r.nextDouble(0.85, 1.15);
        entryProgress  = 0.0;
        outsideMomentum = 0.0;
        smoothIn   = 0.0;
        swingMode  = false;
        swingPhase = 0.0;
        driftOffset = 0.0;
    }

    @Override
    public void onReset() {
        phaseYaw = phasePitch = freqYaw = freqPitch = baseNoise = 0.0;
        scaleMul = 1.0;
        entryProgress = outsideMomentum = smoothIn = 0.0;
        swingMode = false;
        swingPhase = driftOffset = 0.0;
    }

    /**
     * Вызывать каждый тик пока цель активна — продвигает осциллятор.
     * @param frameFactor frame-factor из RotationUtil
     */
    public void advanceOscillator(double frameFactor) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        phaseYaw   += freqYaw   * frameFactor;
        phasePitch += freqPitch * frameFactor;

        if (smoothEntry) {
            entryProgress = Math.min(1.0, entryProgress + 0.035 * frameFactor);
        } else {
            entryProgress = 1.0;
        }

        if (r.nextDouble() < 0.02)  scaleMul   = r.nextDouble(0.8, 1.2);
        if (r.nextDouble() < 0.015) {
            driftOffset += r.nextDouble(-0.04, 0.04);
            driftOffset  = MathHelper.clamp(driftOffset, -0.15, 0.1);
        }

        if (swingMode) swingPhase += swingFreq * frameFactor * 0.02;
    }

    @Override
    public float[] compute(double dYaw, double dPitch,
                           double totalAngle, double frameFactor,
                           long nowMs, long targetStartMs) {
        ThreadLocalRandom r = ThreadLocalRandom.current();

        boolean onTarget = totalAngle <= 45.0 && isLookingAtHitbox();

        // Инерция выхода за хитбокс
        double outsideDelta = onTarget ? 0.0 : 1.0;
        double inertiaRate  = onTarget
            ? Math.max(0.0, 1.0 - 0.15 * frameFactor)
            : Math.min(outsideMomentum + 0.06 * frameFactor, 3.0);
        if (!onTarget) outsideMomentum = inertiaRate;
        else           outsideMomentum *= inertiaRate;

        double lerpRate = onTarget ? 0.08 : 0.08 + outsideMomentum * 0.04;
        smoothIn += (outsideDelta - smoothIn) * lerpRate * frameFactor;
        smoothIn  = MathHelper.clamp(smoothIn, 0.0, 1.0);

        // Коэффициент коррекции
        double corr     = corrStrength * entryProgress * scaleMul;
        double sensMul  = 1.0;
        if (useSens) {
            double sens = MinecraftClient.getInstance().options.getMouseSensitivity().getValue();
            sensMul = MathHelper.clamp(sens * 2.0, 0.3, 3.0);
        }

        // Smoothstep от времени захвата цели
        double tRaw    = Math.clamp((nowMs - targetStartMs) / (450.0 + r.nextDouble(150.0)), 0.0, 1.0);
        double tSmooth = tRaw * tRaw * (3.0 - 2.0 * tRaw);

        double corrStep = frameFactor * corr * sensMul * tSmooth;

        // Замедление вблизи
        if (slowNear && totalAngle < slowThreshold) {
            double ratio = Math.max(0.2, totalAngle / slowThreshold);
            corrStep *= ratio;
        }

        // Скейл по дистанции угла
        corrStep *= angleScale(totalAngle);

        // Инерция разгона
        corrStep *= 1.0 + outsideMomentum * 0.5;

        // Swing-режим (большой угол)
        if (totalAngle > 80.0 && !swingMode) {
            swingMode  = true;
            swingAmp   = r.nextDouble(0.15, 0.4);
            swingFreq  = r.nextDouble(5.0, 12.0);
            swingAtten = r.nextDouble(2.0, 3.5);
            swingPhase = r.nextDouble(Math.PI * 2);
        }
        if (totalAngle < 6.0 && swingMode) swingMode = false;

        if (swingMode) {
            double sw = Math.sin(swingPhase) * swingAmp;
            corrStep *= swingAtten * (1.0 + sw);
        }

        // Скорость → итоговый lerpStep
        double speedNorm = frameFactor / 15.0;
        double noiseBase = r.nextDouble(-0.002, 0.002);
        double lerpStep  = MathHelper.clamp(corrStep * 0.03 * speedNorm + noiseBase, 0.003, 0.85);

        double moveYaw   = dYaw   * lerpStep;
        double movePitch = dPitch * lerpStep * (0.85 + r.nextDouble(0.3));

        // Шум при наводке
        if (noise) {
            double n = 0.05 + r.nextDouble(0.07);
            moveYaw   += Math.sin(phaseYaw)   * n * (0.5 + r.nextDouble(0.6));
            movePitch += Math.cos(phasePitch) * n * 0.45 * (0.4 + r.nextDouble(0.5));
        }

        // Дополнительный толчок от инерции
        if (outsideMomentum > 0.3 && !onTarget) {
            double boost = MathHelper.clamp(outsideMomentum * 0.07, 0.02, 0.2);
            double by = dYaw   * boost;
            double bp = dPitch * boost * 0.85;
            if (Math.abs(moveYaw)   < Math.abs(by)) moveYaw   = by;
            if (Math.abs(movePitch) < Math.abs(bp)) movePitch = bp;
        }

        // Применить smoothIn (плавный вход)
        moveYaw   *= smoothIn;
        movePitch *= smoothIn;

        // Ограничение скорости изменения (anti-snap)
        double angleBoost = 1.0 + Math.min(totalAngle / 30.0, 4.0) * speedNorm;
        if (swingMode) angleBoost *= 2.0 + r.nextDouble(1.5);

        double maxDY = (1.8 + r.nextDouble(1.2)) * frameFactor * angleBoost;
        double maxDP = (1.3 + r.nextDouble(0.8)) * frameFactor * angleBoost;
        if (Math.abs(moveYaw - dYaw)   > maxDY) moveYaw   = dYaw   + Math.signum(moveYaw - dYaw)   * maxDY;
        if (Math.abs(movePitch - dPitch) > maxDP) movePitch = dPitch + Math.signum(movePitch - dPitch) * maxDP;

        // GCD-снаппинг
        double gcd = RandomUtil.mouseGcd();
        moveYaw   = RandomUtil.snap(gcd, moveYaw);
        movePitch = RandomUtil.snap(gcd, movePitch);

        return new float[]{ (float) moveYaw, (float) movePitch };
    }

    // ─── Приватные хелперы ────────────────────────────────────────

    /** Смотрим ли мы в хитбокс ближайшего врага */
    private boolean isLookingAtHitbox() {
        var mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return false;
        Vec3d eye  = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVec(1f);
        double reach = 6.0;
        Vec3d end = eye.add(look.multiply(reach));

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive()) continue;
            if (mc.player.squaredDistanceTo(e) > reach * reach) continue;
            Box box = e.getBoundingBox();
            if (freedomDown != 0.0)
                box = new Box(box.minX, box.minY - freedomDown, box.minZ, box.maxX, box.maxY, box.maxZ);
            if (box.raycast(eye, end).isPresent() || box.contains(end)) return true;
        }
        return false;
    }

    /** Нелинейный масштаб силы коррекции по углу */
    private static double angleScale(double angle) {
        if (angle < 1.5)       return 0.12 + angle * 0.12;
        else if (angle < 8.0)  return 0.3  + ((angle - 1.5) / 6.5) * 0.4;
        else if (angle < 40.0) {
            double t = (angle - 8.0) / 32.0;
            return 0.7 + t * t * (3.0 - 2.0 * t) * 1.8;
        } else {
            double t = Math.min((angle - 40.0) / 140.0, 1.0);
            return 2.5 + t * 2.5;
        }
    }
}
