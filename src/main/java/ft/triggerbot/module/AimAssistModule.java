package ft.triggerbot.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ft.triggerbot.util.CritChecks;
import ft.triggerbot.util.RotationUtil;

public class AimAssistModule extends Module {
    public float range = 4.5f;
    public float fov = 90f;
    public float speed = 12f;
    public float holdAfterHit = 5f;
    public boolean onlyPlayers = true;
    public boolean weaponOnly = true;
    public boolean visibleOnly = true;
    public boolean notWhileEating = true;
    public boolean notInInventory = true;
    public boolean wallCheck = true;
    public boolean onlyOnX = false;

    private final RotationUtil rotation = new RotationUtil();
    private int holdTicks = 0;
    private LivingEntity lastTarget = null;

    public AimAssistModule() {
        super("AimAssist", 0);
        TriggerBotModule.ClientBridge.aim = this;
    }

    public void onAttack() { holdTicks = (int) holdAfterHit; }

    @Override
    public void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null && notInInventory) return;
        
        // Исправленная проверка на еду (без UseAction)
        if (notWhileEating && mc.player.isUsingItem()) {
            if (mc.player.getActiveItem().isFood()) return;
        }
        
        if (weaponOnly && !CritChecks.check6()) return;

        rotation.update();

        if (holdTicks > 0 && lastTarget != null && lastTarget.isAlive()) {
            holdTicks--;
            look(mc, lastTarget);
            return;
        }

        LivingEntity best = find(mc);
        if (best == null) { lastTarget = null; return; }
        lastTarget = best;
        look(mc, best);
    }

    private void look(MinecraftClient mc, LivingEntity t) {
        Vec3d point = t.getPos().add(0, t.getHeight() * 0.45, 0);
        if (onlyOnX) {
            float[] a = RotationUtil.angles(point);
            rotation.softLook(speed, mc.player.getPitch(), a[0]);
        } else {
            rotation.softLookAt(speed, point);
        }
    }

    private LivingEntity find(MinecraftClient mc) {
        LivingEntity best = null;
        double bestAng = Double.MAX_VALUE;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (living == mc.player) continue;
            if (onlyPlayers && !(living instanceof PlayerEntity)) continue;
            if (mc.player.distanceTo(living) > range) continue;
            if (wallCheck && !mc.player.canSee(living)) continue;
            if (visibleOnly && living.isInvisible()) continue;
            float[] a = RotationUtil.angles(living.getPos().add(0, living.getHeight() * 0.45, 0));
            float dy = MathHelper.wrapDegrees(a[0] - mc.player.getYaw());
            float dp = onlyOnX ? 0 : (a[1] - mc.player.getPitch());
            double ang = Math.sqrt(dy * dy + dp * dp);
            if (ang > fov / 2.0) continue;
            if (ang < bestAng) { bestAng = ang; best = living; }
        }
        return best;
    }
}