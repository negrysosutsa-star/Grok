package ft.triggerbot.module;

import ft.triggerbot.aim.AimMode;
import ft.triggerbot.aim.FreeAim;
import ft.triggerbot.aim.LegitAim;
import ft.triggerbot.util.CritChecks;
import ft.triggerbot.util.RotationUtil;
import ft.triggerbot.util.TpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssistModule extends Module {

    // ─── Настройки ────────────────────────────────────────────────
    public double  range          = 4.5;
    public double  fov            = 360.0;
    public double  holdAfterHit   = 2.0;
    public boolean onlyPlayers    = true;
    public boolean weaponOnly     = true;
    public boolean wallCheck      = false;
    public boolean onlyOnX        = false;
    public boolean visibleOnly    = false;
    public boolean notWhileEating = true;
    public boolean notInInventory = true;

    /** Режим: "legit" или "free" */
    public String aimModeStr = "legit";

    public int keyCode = 0;

    // ─── Режимы ───────────────────────────────────────────────────
    public final LegitAim legitAim = new LegitAim();
    public final FreeAim  freeAim  = new FreeAim();

    // ─── Состояние ────────────────────────────────────────────────
    private LivingEntity lastTarget;
    private long         targetStartMs = 0L;
    private int          holdTicks     = 0;

    // Антителепорт: позиция цели в прошлом тике
    private Vec3d lastTargetPos  = null;
    private long  teleportIgnoreUntil = 0L;

    private final RotationUtil rotation = new RotationUtil();

    public AimAssistModule() { super("AimAssist", 1); }

    @Override
    public void onDisable() {
        clearTarget();
        rotation.setTime();
    }

    public void onAttack(LivingEntity target) {
        if (!enabled) return;
        if (lastTarget != target) {
            lastTarget    = target;
            targetStartMs = System.currentTimeMillis();
            getMode().onNewTarget();
        }
        holdTicks = (int)(holdAfterHit * 20);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!enabled) return;
        if (mc.player == null || mc.world == null) return;
        ClientPlayerEntity player = mc.player;

        if (notWhileEating && player.isUsingItem()) { clearTarget(); return; }
        if (notInInventory && mc.currentScreen != null) return;
        if (weaponOnly && !CritChecks.check6()) return;

        LivingEntity target = resolveTarget(player);
        if (target == null) { clearTarget(); return; }

        // Антителепорт
        Vec3d tPos = target.getPos();
        if (lastTargetPos != null) {
            double jumped = lastTargetPos.squaredDistanceTo(tPos);
            if (jumped > 144.0) { // 12 блоков²
                teleportIgnoreUntil = System.currentTimeMillis() + 250;
            }
        }
        lastTargetPos = tPos;
        if (System.currentTimeMillis() < teleportIgnoreUntil) return;

        // Новая цель
        if (target != lastTarget) {
            lastTarget    = target;
            targetStartMs = System.currentTimeMillis();
            getMode().onNewTarget();
        }

        long nowMs = System.currentTimeMillis();
        getMode().tick(nowMs);

        // frame-factor + TPS-поправка
        rotation.update();
        double ff = rotation.frameFactor();
        float  tps = TpsTracker.getInstance().getFloat();
        double tpsFactor = tps / 20.0; // < 1 на лагающем сервере
        ff *= tpsFactor;

        // Целевая точка
        Vec3d point = aimPoint(target);

        // Вычислить углы
        float[] targetAngles = RotationUtil.angles(point);
        float tYaw   = targetAngles[0];
        float tPitch = targetAngles[1];

        float cYaw   = player.getYaw();
        float cPitch = player.getPitch();

        double dYaw   = RandomUtil_wrapDeg(tYaw   - cYaw);
        double dPitch = tPitch - cPitch;
        double totalAngle = Math.sqrt(dYaw * dYaw + dPitch * dPitch);

        if (totalAngle < 0.02) return;

        // FreeAim: продвинуть осциллятор
        if (getMode() instanceof FreeAim fa) fa.advanceOscillator(ff);

        float[] move = getMode().compute(dYaw, dPitch, totalAngle, ff, nowMs, targetStartMs);

        float moveYaw   = move[0];
        float movePitch = onlyOnX ? 0f : move[1];

        player.setYaw(cYaw + moveYaw);
        player.setPitch(MathHelper.clamp(cPitch + movePitch, -90f, 90f));
    }

    // ─── Приватные хелперы ────────────────────────────────────────

    private LivingEntity resolveTarget(ClientPlayerEntity player) {
        // Удержание после удара
        if (holdTicks > 0 && lastTarget != null && isValid(player, lastTarget)) {
            holdTicks--;
            return lastTarget;
        }
        return findBest(player);
    }

    private LivingEntity findBest(ClientPlayerEntity player) {
        double halfFovRad = Math.toRadians(fov / 2.0);
        LivingEntity best = null;
        double bestAngle  = Double.MAX_VALUE;

        Vec3d eye  = player.getEyePos();
        Vec3d look = Vec3d.fromPolar(player.getPitch(), player.getYaw()).normalize();
        Box   box  = player.getBoundingBox().expand(range);

        var mc = MinecraftClient.getInstance();
        for (LivingEntity e : mc.world.getEntitiesByClass(
                LivingEntity.class, box, en -> isValid(player, en))) {
            double dist = player.distanceTo(e);
            if (dist > range || dist < 0.5) continue;
            if (visibleOnly && !player.canSee(e)) continue;
            if (wallCheck   && !player.canSee(e)) continue;

            Vec3d toTarget = aimPoint(e).subtract(eye).normalize();
            double dot   = MathHelper.clamp(toTarget.dotProduct(look), -1.0, 1.0);
            double angle = Math.acos(dot);
            if (angle < halfFovRad && angle < bestAngle) {
                bestAngle = angle;
                best = e;
            }
        }
        return best;
    }

    private Vec3d aimPoint(LivingEntity e) {
        return e.getPos().add(0, e.getHeight() * 0.85, 0);
    }

    private boolean isValid(ClientPlayerEntity player, Entity e) {
        if (e == player) return false;
        if (!(e instanceof LivingEntity l)) return false;
        if (!l.isAlive() || l.isSpectator() || !l.canHit()) return false;
        if (onlyPlayers && !(e instanceof PlayerEntity)) return false;
        return true;
    }

    private void clearTarget() {
        if (lastTarget != null) getMode().onReset();
        lastTarget     = null;
        lastTargetPos  = null;
        holdTicks      = 0;
        rotation.setTime();
    }

    private AimMode getMode() {
        return "free".equals(aimModeStr) ? freeAim : legitAim;
    }

    private static double RandomUtil_wrapDeg(double d) {
        d %= 360.0;
        if (d > 180.0)  d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }
}
