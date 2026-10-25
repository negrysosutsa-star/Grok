package ft.triggerbot;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ft.triggerbot.util.ChatUtil;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot implements ClientModInitializer {
    public static TriggerBot INSTANCE;

    public boolean onlyPlayers = true;
    public boolean onlyCrits = true;
    public boolean pauseOnUse = true;
    public boolean weaponOnly = true;
    public int minDelay = 2;
    public int maxDelay = 5;

    public boolean enabled = false;
    private boolean unhooked = false;
    private int delayTicks = 0;
    private boolean wasR = false;
    private boolean wasHome = false;
    private boolean wasRShift = false;
    private boolean loadedMsg = false;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
    }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (unhooked) return;

        boolean rshift = isKeyDown(mc, 344);
        if (rshift && !wasRShift && mc.currentScreen == null) {
            mc.setScreen(new ClickGui());
        }
        wasRShift = rshift;

        if (mc.currentScreen != null) return;

        boolean home = isKeyDown(mc, 268);
        if (home && !wasHome) {
            unhooked = true;
            enabled = false;
            ChatUtil.send("unhooked. F3+D clear chat");
        }
        wasHome = home;

        if (!loadedMsg && mc.world != null && mc.player != null) {
            ChatUtil.send("loaded | R toggle | RShift gui | Home unhook");
            loadedMsg = true;
        }

        boolean r = isKeyDown(mc, 82);
        if (r && !wasR) {
            enabled = !enabled;
            ChatUtil.send(enabled ? "on" : "off");
            if (!enabled) delayTicks = 0;
        }
        wasR = r;

        if (!enabled || mc.player == null || mc.world == null) return;
        if (pauseOnUse && mc.player.isUsingItem()) return;

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        if (weaponOnly) {
            Item item = mc.player.getMainHandStack().getItem();
            if (!(item instanceof SwordItem || item instanceof AxeItem || item instanceof MaceItem)) return;
        }

        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;

        Entity target = ((EntityHitResult) hit).getEntity();
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return;
        if (onlyPlayers && !(target instanceof PlayerEntity)) return;
        if (target == mc.player) return;

        if (!isReady(mc)) return;
        if (onlyCrits && !isCrit(mc)) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        delayTicks = ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
    }

    private boolean isReady(MinecraftClient mc) {
        // в воздухе можно чуть раньше
        float need = mc.player.isOnGround() ? 1.0f : 0.9f;
        return mc.player.getAttackCooldownProgress(0.5f) >= need;
    }

    private boolean isCrit(MinecraftClient mc) {
        // состояния в которых крит физически невозможен — бьём обычным
        if (mc.player.getAbilities().flying) return true;
        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) return true;
        if (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) return true;
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return true;
        if (mc.player.isClimbing()) return true;
        if (mc.player.hasVehicle()) return true;

        // настоящий крит: не на земле и уже падаем
        return !mc.player.isOnGround() && mc.player.fallDistance > 0.0f;
    }

    private static boolean isKeyDown(MinecraftClient mc, int key) {
        return net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow().getHandle(), key);
    }
}