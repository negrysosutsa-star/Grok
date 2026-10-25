package ft.triggerbot.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.MaceItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ft.triggerbot.util.CritChecks;
import ft.triggerbot.util.TickCounter;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBotModule extends Module {
    public String critMode = "smart";
    public boolean onlyPlayers = true;
    public boolean weaponOnly = true;
    public boolean pauseOnUse = true;
    public float range = 3.0f;
    public int minDelay = 2;
    public int maxDelay = 5;
    public boolean customCooldown = false;
    public float cooldownPct = 80f;

    private int delayTicks = 0;

    public TriggerBotModule() { super("TriggerBot", 82); }

    @Override public void onDisable() { delayTicks = 0; }

    @Override
    public void onTick(MinecraftClient mc) {
        if (TickCounter.busy()) return;
        if (pauseOnUse && mc.player.isUsingItem()) return;
        if (delayTicks > 0) { delayTicks--; return; }
        if (weaponOnly && !CritChecks.check6()) return;
        if (CritChecks.check4()) return;

        Entity target = resolve(mc);
        if (target == null) return;
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return;
        if (onlyPlayers && !(target instanceof PlayerEntity)) return;
        if (target == mc.player) return;
        if (mc.player.distanceTo(target) > range) return;

        float cd = mc.player.getAttackCooldownProgress(0.0f);

        if (CritChecks.isClass(MaceItem.class)) {
            if (cd < 0.4f) return;
            if (!CritChecks.check3() && !CritChecks.check()) return;
        } else if (customCooldown) {
            if (cd < cooldownPct / 100f) return;
            if ("only_crits".equals(critMode) && !CritChecks.check3() && !CritChecks.check()) return;
        } else {
            if (!CritChecks.canHit(critMode, cd)) return;
        }

        if ("smart".equals(critMode) && !CritChecks.check3() && !CritChecks.check()) {
            if (mc.options.jumpKey.isPressed()) return;
        }

        CritChecks.attack(target);
        if (ClientBridge.aim != null) ClientBridge.aim.onAttack();
        delayTicks = ThreadLocalRandom.current().nextInt(minDelay, Math.max(minDelay, maxDelay) + 1);
    }

    private Entity resolve(MinecraftClient mc) {
        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY)
            return ((EntityHitResult) hit).getEntity();
        return null;
    }

    public static class ClientBridge {
        public static AimAssistModule aim;
    }
}