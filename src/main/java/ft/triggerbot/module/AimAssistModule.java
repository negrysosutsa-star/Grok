package ft.triggerbot.module;

import ft.triggerbot.util.CritChecks;
import ft.triggerbot.util.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimAssistModule extends Module {

    public double  range          = 4.5;
    public double  fov            = 360.0;
    public double  speed          = 3.0;
    public double  holdAfterHit   = 2.0;
    public boolean onlyPlayers    = true;
    public boolean weaponOnly     = true;
    public boolean wallCheck      = false;
    public boolean onlyOnX        = false;
    public boolean visibleOnly    = false;
    public boolean notWhileEating = true;
    public boolean notInInventory = true;
    public int     keyCode        = 0;

    private LivingEntity lastTarget;
    private int holdTicks = 0;
    private final RotationUtil rotation = new RotationUtil();

    public AimAssistModule() { super("AimAssist", 1); }

    @Override
    public void onDisable() {
        lastTarget = null;
        holdTicks  = 0;
        rotation.setTime();
    }

    public void onAttack(LivingEntity target) {
        if (!enabled) return;
        lastTarget = target;
        holdTicks  = (int)(holdAfterHit * 20);
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (!enabled) return;
        if (mc.player == null || mc.world == null) return;
        ClientPlayerEntity player = mc.player;

        if (notWhileEating && player.isUsingItem()) { lastTarget = null; return; }
        if (notInInventory && mc.currentScreen != null) return;
        if (weaponOnly && !CritChecks.check6()) return;

        LivingEntity target;
        if (holdTicks > 0 && lastTarget != null && isValid(player, lastTarget)) {
            target = lastTarget;
            holdTicks--;
        } else {
            target = findBest(mc, player);
        }

        if (target == null) {
            rotation.setTime();
            return;
        }

        Vec3d point = getTargetPoint(target);

        if (onlyOnX) {
            // Только горизонталь — целимся softLook но pitch не двигаем
            float[] angles = RotationUtil.angles(point);
            float currentPitch = player.getPitch();
            rotation.softLook(speed, currentPitch, angles[0]);
        } else {
            rotation.softLookAt(speed, point);
        }
    }

    private LivingEntity findBest(MinecraftClient mc, ClientPlayerEntity player) {
        double halfFovRad = Math.toRadians(fov / 2.0);
        LivingEntity best = null;
        double bestAngle  = Double.MAX_VALUE;

        Vec3d eye  = player.getEyePos();
        Vec3d look = Vec3d.fromPolar(player.getPitch(), player.getYaw()).normalize();
        Box   box  = player.getBoundingBox().expand(range);

        for (LivingEntity e : mc.world.getEntitiesByClass(
                LivingEntity.class, box, en -> isValid(player, en))) {
            double dist = player.distanceTo(e);
            if (dist > range || dist < 0.5) continue;
            if (visibleOnly && !player.canSee(e)) continue;
            if (wallCheck   && !player.canSee(e)) continue;

            Vec3d toTarget = getTargetPoint(e).subtract(eye).normalize();
            double dot     = MathHelper.clamp(toTarget.dotProduct(look), -1.0, 1.0);
            double angle   = Math.acos(dot);

            if (angle < halfFovRad && angle < bestAngle) {
                bestAngle = angle;
                best = e;
            }
        }
        return best;
    }

    private Vec3d getTargetPoint(LivingEntity e) {
        return e.getPos().add(0, e.getHeight() * 0.85, 0);
    }

    private boolean isValid(ClientPlayerEntity player, Entity e) {
        if (e == player) return false;
        if (!(e instanceof LivingEntity l)) return false;
        if (!l.isAlive() || l.isSpectator() || !l.canHit()) return false;
        if (onlyPlayers && !(e instanceof PlayerEntity)) return false;
        return true;
    }
}
