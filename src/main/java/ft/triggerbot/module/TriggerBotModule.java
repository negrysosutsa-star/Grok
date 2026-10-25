package ft.triggerbot.module;

import ft.triggerbot.util.CritChecks;
import ft.triggerbot.util.RandomUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.MaceItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class TriggerBotModule extends Module {

    public String  critMode       = "smart";
    public boolean onlyPlayers    = true;
    public boolean weaponOnly     = true;
    public boolean pauseOnUse     = false;
    public boolean customCooldown = false;
    public int     cooldownPct    = 80;
    public int     minDelay       = 0;
    public int     maxDelay       = 0;
    public double  range          = 3.0;
    public int     keyCode        = 0;

    private long nextAttackTime = 0;

    public TriggerBotModule() { super("TriggerBot", 0); }

    @Override
    public void onDisable() { nextAttackTime = 0; }

    public void onTick(MinecraftClient mc, AimAssistModule aimAssist) {
        if (!enabled) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        ClientPlayerEntity player = mc.player;

        if (pauseOnUse && player.isUsingItem()) return;
        if (weaponOnly && !CritChecks.check6()) return;
        if (player.getMainHandStack().getItem() instanceof CrossbowItem) return;

        long now = System.currentTimeMillis();
        if (now < nextAttackTime) return;

        LivingEntity target = findTarget(player);
        if (target == null) return;
        if (!checkCooldown(player)) return;
        if (!checkCrit(player)) return;

        CritChecks.attack(target);

        if (aimAssist != null && aimAssist.enabled) aimAssist.onAttack(target);

        int delay = minDelay;
        if (maxDelay > minDelay) delay = minDelay + RandomUtil.nextInt(maxDelay - minDelay);
        nextAttackTime = now + delay;
    }

    @Override
    public void onTick(MinecraftClient mc) { onTick(mc, null); }

    private LivingEntity findTarget(ClientPlayerEntity player) {
        Vec3d eye  = player.getCameraPosVec(1f);
        Vec3d look = player.getRotationVec(1f);
        Vec3d end  = eye.add(look.multiply(range));
        Box   box  = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0);

        EntityHitResult hit = ProjectileUtil.raycast(
            player, eye, end, box,
            e -> isValidTarget(player, e),
            range * range
        );
        if (hit == null) return null;
        Entity e = hit.getEntity();
        return e instanceof LivingEntity l ? l : null;
    }

    private boolean isValidTarget(ClientPlayerEntity player, Entity e) {
        if (e == player || !e.isAlive() || e.isSpectator() || !e.canHit()) return false;
        if (onlyPlayers && !(e instanceof PlayerEntity)) return false;
        return true;
    }

    private boolean checkCooldown(ClientPlayerEntity player) {
        float p = player.getAttackCooldownProgress(0f);
        if (player.getMainHandStack().getItem() instanceof MaceItem) return p >= 0.4f;
        if (customCooldown) return p >= cooldownPct / 100f;
        return CritChecks.canHit(critMode, p);
    }

    private boolean checkCrit(ClientPlayerEntity player) {
        if ("only_crits".equals(critMode)) return CritChecks.check();
        return true;
    }
}
