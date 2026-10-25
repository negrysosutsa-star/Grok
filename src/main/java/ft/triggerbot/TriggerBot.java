package ft.triggerbot;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ft.triggerbot.util.ChatUtil;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot implements ClientModInitializer {
    public static TriggerBot INSTANCE;

    // ===== TriggerBot =====
    public boolean tbEnabled = false;
    public int tbKey = 82; // R
    public boolean onlyPlayers = true;
    public boolean onlyCrits = true;
    public boolean weaponOnly = true;
    public boolean pauseOnUse = true;
    public float range = 3.0f;
    public int minDelay = 2;
    public int maxDelay = 5;

    // ===== AimAssist =====
    public boolean aaEnabled = false;
    public int aaKey = 0; // none by default
    public float aaRange = 4.5f;
    public float aaFov = 90f;
    public float aaSpeed = 8f; // lower = slower
    public boolean aaPlayersOnly = true;
    public boolean aaWeaponOnly = true;
    public boolean aaVisibleOnly = true;

    // ===== internal =====
    public int guiKey = 260; // INSERT
    private boolean unhooked = false;
    private int delayTicks = 0;
    private boolean wasTbKey, wasAaKey, wasGuiKey, wasHome;
    private boolean loadedMsg = false;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
    }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (unhooked || mc.player == null || mc.world == null) return;

        // Insert → gui
        boolean guiDown = isKeyDown(mc, guiKey);
        if (guiDown && !wasGuiKey && mc.currentScreen == null) {
            mc.setScreen(new ClickGui());
        }
        wasGuiKey = guiDown;

        if (mc.currentScreen != null) return;

        // Home unhook
        boolean home = isKeyDown(mc, 268);
        if (home && !wasHome) {
            unhooked = true;
            tbEnabled = false;
            aaEnabled = false;
            ChatUtil.send("unhooked");
        }
        wasHome = home;

        if (!loadedMsg) {
            ChatUtil.send("loaded | Insert menu | Home unhook");
            loadedMsg = true;
        }

        // keybinds toggle
        if (tbKey != 0) {
            boolean d = isKeyDown(mc, tbKey);
            if (d && !wasTbKey) {
                tbEnabled = !tbEnabled;
                ChatUtil.send("trigger " + (tbEnabled ? "on" : "off"));
            }
            wasTbKey = d;
        }
        if (aaKey != 0) {
            boolean d = isKeyDown(mc, aaKey);
            if (d && !wasAaKey) {
                aaEnabled = !aaEnabled;
                ChatUtil.send("aimassist " + (aaEnabled ? "on" : "off"));
            }
            wasAaKey = d;
        }

        // aim assist first (smooth look)
        if (aaEnabled) doAimAssist(mc);

        // trigger
        if (tbEnabled) doTrigger(mc);
    }

    private void doTrigger(MinecraftClient mc) {
        if (pauseOnUse && mc.player.isUsingItem()) return;
        if (delayTicks > 0) { delayTicks--; return; }

        if (weaponOnly) {
            Item item = mc.player.getMainHandStack().getItem();
            if (!(item instanceof SwordItem || item instanceof AxeItem || item instanceof MaceItem)) return;
        }

        Entity target = findCrosshairTarget(mc, range);
        if (target == null) return;
        if (!(target instanceof LivingEntity living) || !living.isAlive()) return;
        if (onlyPlayers && !(target instanceof PlayerEntity)) return;
        if (target == mc.player) return;

        if (!isCooldownReady(mc)) return;
        if (onlyCrits && !isCrit(mc)) return;

        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
        delayTicks = ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1);
    }

    private void doAimAssist(MinecraftClient mc) {
        if (aaWeaponOnly) {
            Item item = mc.player.getMainHandStack().getItem();
            if (!(item instanceof SwordItem || item instanceof AxeItem || item instanceof MaceItem)) return;
        }

        LivingEntity best = null;
        double bestAng = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living) || !living.isAlive()) continue;
            if (living == mc.player) continue;
            if (aaPlayersOnly && !(living instanceof PlayerEntity)) continue;
            if (mc.player.distanceTo(living) > aaRange) continue;
            if (aaVisibleOnly && !mc.player.canSee(living)) continue;

            float[] rot = getRotations(mc.player, living);
            float yawDiff = MathHelper.wrapDegrees(rot[0] - mc.player.getYaw());
            float pitchDiff = rot[1] - mc.player.getPitch();
            double ang = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
            if (ang > aaFov / 2f) continue;

            if (ang < bestAng) {
                bestAng = ang;
                best = living;
            }
        }

        if (best == null) return;

        float[] rot = getRotations(mc.player, best);
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();

        float dy = MathHelper.wrapDegrees(rot[0] - yaw);
        float dp = rot[1] - pitch;

        // smooth
        float factor = MathHelper.clamp(aaSpeed / 50f, 0.02f, 1f);
        mc.player.setYaw(yaw + dy * factor);
        mc.player.setPitch(MathHelper.clamp(pitch + dp * factor, -90f, 90f));
    }

    private Entity findCrosshairTarget(MinecraftClient mc, float reach) {
        HitResult hit = mc.crosshairTarget;
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            Entity e = ((EntityHitResult) hit).getEntity();
            if (mc.player.distanceTo(e) <= reach) return e;
        }
        return null;
    }

    private boolean isCooldownReady(MinecraftClient mc) {
        float need = mc.player.isOnGround() ? 1.0f : 0.9f;
        return mc.player.getAttackCooldownProgress(0.5f) >= need;
    }

    private boolean isCrit(MinecraftClient mc) {
        if (mc.player.getAbilities().flying) return true;
        if (mc.player.hasStatusEffect(StatusEffects.BLINDNESS)) return true;
        if (mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) return true;
        if (mc.player.isTouchingWater() || mc.player.isInLava()) return true;
        if (mc.player.isClimbing() || mc.player.hasVehicle()) return true;
        // system-style: velocity.y < 0 is more reliable than fallDistance alone
        return !mc.player.isOnGround() && mc.player.getVelocity().y < -0.01;
    }

    private static float[] getRotations(PlayerEntity from, Entity to) {
        Vec3d eye = from.getEyePos();
        Vec3d pos = to.getPos().add(0, to.getHeight() * 0.5, 0);
        double dx = pos.x - eye.x;
        double dy = pos.y - eye.y;
        double dz = pos.z - eye.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new float[]{yaw, pitch};
    }

    public static boolean isKeyDown(MinecraftClient mc, int key) {
        if (key == 0) return false;
        return InputUtil.isKeyPressed(mc.getWindow().getHandle(), key);
    }

    public static String keyName(int key) {
        if (key == 0) return "NONE";
        return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase();
    }
}