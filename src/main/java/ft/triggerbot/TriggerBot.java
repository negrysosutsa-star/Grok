package ft.triggerbot;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.MaceItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ft.triggerbot.util.ChatUtil;
import ft.triggerbot.util.CritChecks;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot implements ClientModInitializer {
    public static TriggerBot INSTANCE;

    // trigger
    public boolean tbEnabled = false;
    public int tbKey = 82; // R
    public String critMode = "smart"; // smart | only_crits
    public boolean onlyPlayers = true;
    public boolean weaponOnly = true;
    public boolean pauseOnUse = true;
    public boolean throughEntities = false;
    public float range = 3.0f;
    public int minDelay = 2;
    public int maxDelay = 5;
    public boolean customCooldown = false;
    public float cooldownPct = 80f;

    // aim
    public final AimAssist aim = new AimAssist();

    // meta
    public int guiKey = 260; // INSERT
    private boolean unhooked = false;
    private int delayTicks = 0;
    private boolean wasTb, wasAa, wasGui, wasHome;
    private boolean loadedMsg = false;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
    }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (unhooked || mc.player == null || mc.world == null) return;

        boolean guiDown = key(mc, guiKey);
        if (guiDown && !wasGui && mc.currentScreen == null) {
            mc.setScreen(new ClickGui());
        }
        wasGui = guiDown;

        if (mc.currentScreen != null) return;

        boolean home = key(mc, 268);
        if (home && !wasHome) {
            unhooked = true;
            tbEnabled = false;
            aim.enabled = false;
            ChatUtil.send("unhooked");
        }
        wasHome = home;

        if (!loadedMsg) {
            ChatUtil.send("loaded | Insert menu | Home unhook");
            loadedMsg = true;
        }

        if (tbKey != 0) {
            boolean d = key(mc, tbKey);
            if (d && !wasTb) {
                tbEnabled = !tbEnabled;
                ChatUtil.send("tb " + (tbEnabled ? "on" : "off"));
            }
            wasTb = d;
        }
        if (aim.key != 0) {
            boolean d = key(mc, aim.key);
            if (d && !wasAa) {
                aim.enabled = !aim.enabled;
                ChatUtil.send("aa " + (aim.enabled ? "on" : "off"));
            }
            wasAa = d;
        }

        aim.tick(mc);
        if (tbEnabled) doTrigger(mc);
    }

    private void doTrigger(MinecraftClient mc) {
        if (pauseOnUse && mc.player.isUsingItem()) return;
        if (delayTicks > 0) { delayTicks--; return; }
        if (weaponOnly && !CritChecks.check6()) return;

        Entity target = resolveTarget(mc);
        if (target == null) return;
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return;
        if (onlyPlayers && !(target instanceof PlayerEntity)) return;
        if (target == mc.player) return;
        if (mc.player.distanceTo(target) > range) return;

        float cd = mc.player.getAttackCooldownProgress(0.0f);

        if (CritChecks.isClass(MaceItem.class)) {
            if (cd < CritChecks.getFloatBySafe(0.4f)) return;
            if (!CritChecks.check3() && !CritChecks.check()) return;
        } else if (customCooldown) {
            if (cd < cooldownPct / 100f) return;
            if ("only_crits".equals(critMode) && !CritChecks.check3() && !CritChecks.check()) return;
        } else {
            if (!CritChecks.canHit(critMode, cd)) return;
        }

        // smart: if not critting and not impossible → only when not holding jump and on ground-ish
        if ("smart".equals(critMode) && !CritChecks.check3() && !CritChecks.check()) {
            if (mc.options.jumpKey.isPressed()) return;
        }

        CritChecks.attack(target);
        aim.onAttack();
        delayTicks = ThreadLocalRandom.current().nextInt(minDelay, Math.max(minDelay, maxDelay) + 1);
    }

    private Entity resolveTarget(MinecraftClient mc) {
        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            return ((EntityHitResult) hit).getEntity();
        }
        return null;
    }

    private static boolean key(MinecraftClient mc, int k) {
        if (k == 0) return false;
        return InputUtil.isKeyPressed(mc.getWindow().getHandle(), k);
    }

    public static String keyName(int key) {
        if (key == 0) return "NONE";
        try {
            return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase();
        } catch (Exception e) {
            return String.valueOf(key);
        }
    }
}