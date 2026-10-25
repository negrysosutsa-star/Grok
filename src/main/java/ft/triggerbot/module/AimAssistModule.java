package ft.triggerbot.module;

import ft.triggerbot.util.CritChecks;
import ft.triggerbot.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssistModule extends Module {

    // ─── Настройки ───────────────────────────────────────────────
    /** Максимальная дистанция до цели */
    public double  range          = 4.5;
    /** FOV захвата цели (градусы, полный угол) */
    public double  fov            = 360.0;
    /** Скорость ротации (чем выше — тем быстрее наводка) */
    public double  speed          = 3.0;
    /** Удержание цели после удара (секунды) */
    public double  holdAfterHit   = 2.0;
    /** Только игроки */
    public boolean onlyPlayers    = true;
    /** Только с оружием */
    public boolean weaponOnly     = true;
    /** Проверка стен */
    public boolean wallCheck      = false;
    /** Только по оси X (horizontal only) */
    public boolean onlyOnX        = false;
    /** Только видимые цели */
    public boolean visibleOnly    = false;
    /** Не целиться когда едим */
    public boolean notWhileEating = true;
    /** Не целиться в инвентаре */
    public boolean notInInventory = true;

    // ─── Bind ─────────────────────────────────────────────────────
    public int keyCode = 45;

    // ─── Состояние ────────────────────────────────────────────────
    private LivingEntity lastTarget;
    private final RotationUtil rotation = new RotationUtil();
    private int holdTicks = 0;

    public AimAssistModule() {
        super("AimAssist", 1);
    }

    @Override
    public void onDisable() {
        lastTarget = null;
        holdTicks = 0;
    }

    public void onAttack(LivingEntity target) {
        if (!enabled) return;
        lastTarget = target;
        holdTicks = (int)(holdAfterHit * 20);
    }

    public void onTick(MinecraftClient mc) {
        if (!enabled) return;
        if (mc.player == null || mc.world == null) return;

        ClientPlayerEntity player = mc.player;

        // Условия отключения
        if (notWhileEating && player.isUsingItem()) {
            lastTarget = null;
            return;
        }
        if (notInInventory && mc.currentScreen != null) return;
        if (weaponOnly && !CritChecks.check6(mc)) return;

        // Удержание после удара
        LivingEntity target = null;
        if (holdTicks > 0 && lastTarget != null && isValidTarget(player, lastTarget)) {
            target = lastTarget;
            holdTicks--;
        } else {
            target = findBestTarget(mc, player);
        }

        if (target == null) return;
        if (!isValidTarget(player, target)) return;

        Vec3d targetPos = getTargetPoint(target);
        lookAt(player, targetPos);
    }

    private LivingEntity findBestTarget(MinecraftClient mc, ClientPlayerEntity player) {
        double halfFov = Math.toRadians(fov / 2.0);
        LivingEntity best = null;
        double bestAngle  = Double.MAX_VALUE;

        Box searchBox = player.getBoundingBox().expand(range);
        Vec3d eye  = player.getEyePos();
        float yaw  = player.getYaw();
        float pitch = player.getPitch();

        for (LivingEntity entity : mc.world.getEntitiesByClass(
                LivingEntity.class, searchBox, e -> isValidTarget(player, e))) {

            double dist = player.distanceTo(entity);
            if (dist > range || dist < 0.5) continue;
            if (visibleOnly && !player.canSee(entity)) continue;

            // Угол до цели
            Vec3d toTarget = getTargetPoint(entity).subtract(eye).normalize();
            Vec3d lookVec  = Vec3d.fromPolar(pitch, yaw).normalize();
            double dot = toTarget.dotProduct(lookVec);
            double angle = Math.acos(MathHelper.clamp(dot, -1.0, 1.0));

            if (angle < halfFov && angle < bestAngle) {
                bestAngle = angle;
                best = entity;
            }
        }
        return best;
    }

    private Vec3d getTargetPoint(LivingEntity entity) {
        return entity.getPos().add(0, entity.getHeight() * 0.85, 0);
    }

    private void lookAt(ClientPlayerEntity player, Vec3d target) {
        Vec3d eye = player.getEyePos();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double d  = Math.sqrt(dx * dx + dz * dz);

        double targetYaw   = Math.toDegrees(Math.atan2(-dx, dz));
        double targetPitch = -Math.toDegrees(Math.atan2(dy, d));

        float cYaw   = player.getYaw();
        float cPitch = player.getPitch();

        double dYaw   = wrapDeg(targetYaw   - cYaw);
        double dPitch = targetPitch - cPitch;

        double factor = 1.0 - Math.pow(0.75, speed);

        double moveYaw   = dYaw   * factor;
        double movePitch = onlyOnX ? 0 : dPitch * factor;

        if (Math.abs(dYaw)   < 0.02) moveYaw   = dYaw;
        if (Math.abs(dPitch) < 0.02) movePitch = dPitch;

        player.setYaw(cYaw   + (float) moveYaw);
        player.setPitch(MathHelper.clamp(cPitch + (float) movePitch, -90f, 90f));
    }

    private boolean isValidTarget(ClientPlayerEntity player, Entity entity) {
        if (entity == player) return false;
        if (!(entity instanceof LivingEntity living)) return false;
        if (!living.isAlive()) return false;
        if (living.isSpectator()) return false;
        if (!living.canHit()) return false;
        if (onlyPlayers && !(entity instanceof PlayerEntity)) return false;
        return true;
    }

    private static double wrapDeg(double deg) {
        deg %= 360.0;
        if (deg > 180.0) deg -= 360.0;
        if (deg < -180.0) deg += 360.0;
        return deg;
    }
}
