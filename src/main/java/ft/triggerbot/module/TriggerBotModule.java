package ft.triggerbot.module;

import ft.triggerbot.util.CritChecks;
import ft.triggerbot.util.RandomUtil;
import ft.triggerbot.util.RotationUtil;
import ft.triggerbot.util.TickCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.MaceItem;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class TriggerBotModule extends Module {

    // ─── Настройки ───────────────────────────────────────────────
    /** "smart" или "only_crits" */
    public String  critMode       = "smart";
    /** Бить только игроков */
    public boolean onlyPlayers    = true;
    /** Только с оружием в руке */
    public boolean weaponOnly     = true;
    /** Пауза при использовании предмета (еда, зелье) */
    public boolean pauseOnUse     = false;
    /** Использовать свой процент кулдауна вместо ванильного */
    public boolean customCooldown = false;
    /** Минимальный % кулдауна для удара (0-100) */
    public int     cooldownPct    = 80;
    /** Мин. задержка между ударами, мс */
    public int     minDelay       = 0;
    /** Макс. задержка между ударами, мс */
    public int     maxDelay       = 0;
    /** Дальность удара */
    public double  range          = 3.0;

    // ─── Bind ─────────────────────────────────────────────────────
    public int keyCode = 45; // INSERT по умолчанию (GLFW_KEY_INSERT)

    // ─── Состояние ────────────────────────────────────────────────
    private long  nextAttackTime = 0;
    private final TickCounter tickCounter = new TickCounter();

    public interface ClientBridge {
        void onAttack(LivingEntity target);
    }

    public TriggerBotModule() {
        super("TriggerBot", 0);
    }

    @Override
    public void onDisable() {
        nextAttackTime = 0;
    }

    public void onTick(MinecraftClient mc) {
        if (!enabled) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        ClientPlayerEntity player = mc.player;

        // Пауза при использовании предмета
        if (pauseOnUse && player.isUsingItem()) return;

        // Только с оружием
        if (weaponOnly && !CritChecks.check6(mc)) return;

        // Арбалет — не атакуем
        if (mc.player.getMainHandStack().getItem() instanceof CrossbowItem) return;

        // Задержка
        long now = System.currentTimeMillis();
        if (now < nextAttackTime) return;

        // Поиск цели
        LivingEntity target = findTarget(mc, player);
        if (target == null) return;

        // Проверка кулдауна
        if (!checkCooldown(mc, player)) return;

        // Крит-режим
        if (!checkCrit(mc, player)) return;

        // Удар
        mc.interactionManager.attackEntity(player, target);
        player.swingHand(net.minecraft.util.Hand.MAIN_HAND);

        // Следующее время атаки
        int delay = minDelay;
        if (maxDelay > minDelay) {
            delay = minDelay + RandomUtil.nextInt(maxDelay - minDelay);
        }
        nextAttackTime = now + delay;
    }

    private LivingEntity findTarget(MinecraftClient mc, ClientPlayerEntity player) {
        double reach = range;
        Vec3d eye    = player.getCameraPosVec(1f);
        Vec3d look   = player.getRotationVec(1f);
        Vec3d end    = eye.add(look.multiply(reach));
        Box   box    = player.getBoundingBox().stretch(look.multiply(reach)).expand(1.0);

        EntityHitResult hit = ProjectileUtil.raycast(
            player, eye, end, box,
            e -> isValidTarget(player, e),
            reach * reach
        );

        if (hit == null) return null;
        Entity e = hit.getEntity();
        if (!(e instanceof LivingEntity living)) return null;
        if (!isValidTarget(player, living)) return null;
        return living;
    }

    private boolean isValidTarget(ClientPlayerEntity player, Entity entity) {
        if (entity == player) return false;
        if (!entity.isAlive()) return false;
        if (entity.isSpectator()) return false;
        if (!entity.canHit()) return false;
        if (onlyPlayers && !(entity instanceof PlayerEntity)) return false;
        if (!onlyPlayers && !(entity instanceof LivingEntity)) return false;
        return true;
    }

    private boolean checkCooldown(MinecraftClient mc, ClientPlayerEntity player) {
        if (customCooldown) {
            float progress = player.getAttackCooldownProgress(0f);
            return progress >= cooldownPct / 100f;
        }
        // Ванильный — ждём 100%
        return player.getAttackCooldownProgress(0f) >= 1.0f
            || mc.player.getMainHandStack().getItem() instanceof MaceItem;
    }

    private boolean checkCrit(MinecraftClient mc, ClientPlayerEntity player) {
        if ("only_crits".equals(critMode)) {
            return CritChecks.check(mc);
        }
        // smart — атакуем всегда, крит если можем
        return true;
    }
}
