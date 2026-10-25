package ft.triggerbot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ft.triggerbot.util.CritChecks;

public class AimAssist {
    public boolean enabled = false;
    public int key = 0;

    public float range = 4.5f;
    public float fov = 90f;
    public float speed = 8f;          // 1..30
    public float holdAfterHit = 5f;   // ticks to keep aim after hit
    public boolean onlyPlayers = true;
    public boolean weaponOnly = true;
    public boolean visibleOnly = true;
    public boolean notWhileEating = true;
    public boolean notInInventory = true;
    public boolean wallCheck = true;
    public boolean onlyOnX = false;   // horizontal only

    private int holdTicks = 0;
    private LivingEntity lastTarget = null;

    public void tick(MinecraftClient mc) {
        if (!enabled || mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null && notInInventory) return;

        if (notWhileEating && mc.player.isUsingItem()) {
            var action = mc.player.getActiveItem().getUseAction();
            if (action == net.minecraft.item.consume.UseAction.EAT
                    || action == net.minecraft.item.consume.UseAction.DRINK) return;
        }

        if (weaponOnly && !CritChecks.check6()) return;

        if (holdTicks > 0 && lastTarget != null && lastTarget.isAlive()) {
            holdTicks--;
            aimAt(mc, lastTarget);
            return;
        }

        LivingEntity best = findTarget(mc);
        if (best == null) {
            lastTarget = null;
            return;
        }
        lastTarget = best;
        aimAt(mc, best);
    }

    public void onAttack() {
        holdTicks = (int) holdAfterHit;
    }

    private LivingEntity findTarget(MinecraftClient mc) {
        LivingEntity best = null;
        double bestAng = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (living == mc.player) continue;
            if (onlyPlayers && !(living instanceof PlayerEntity)) continue;
            if (mc.player.distanceTo(living) > range) continue;
            if (wallCheck && !mc.player.canSee(living)) continue;
            if (visibleOnly && living.isInvisible()) continue;

            float[] rot = rotations(mc.player, living);
            float yawDiff = MathHelper.wrapDegrees(rot[0] - mc.player.getYaw());
            float pitchDiff = rot[1] - mc.player.getPitch();
            if (onlyOnX) pitchDiff = 0;
            double ang = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
            if (ang > fov / 2.0) continue;

            if (ang < bestAng) {
                bestAng = ang;
                best = living;
            }
        }
        return best;
    }

    private void aimAt(MinecraftClient mc, LivingEntity target) {
        float[] rot = rotations(mc.player, target);
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        float dy = MathHelper.wrapDegrees(rot[0] - yaw);
        float dp = onlyOnX ? 0 : (rot[1] - pitch);

        float factor = MathHelper.clamp(speed / 50f, 0.015f, 1f);
        mc.player.setYaw(yaw + dy * factor);
        mc.player.setPitch(MathHelper.clamp(pitch + dp * factor, -90f, 90f));
    }

    private static float[] rotations(PlayerEntity from, Entity to) {
        Vec3d eye = from.getEyePos();
        Vec3d pos = to.getPos().add(0, to.getHeight() * 0.45, 0);
        double dx = pos.x - eye.x;
        double dy = pos.y - eye.y;
        double dz = pos.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{yaw, pitch};
    }
}