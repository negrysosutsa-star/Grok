package ft.triggerbot.util;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public final class CritChecks {
    private CritChecks() {}

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public static boolean check() {
        var p = mc().player;
        if (p == null) return false;
        return p.isTouchingWater() || p.isSwimming() || p.isGliding()
                || p.isClimbing() || p.isInsideWall() || p.isInLava() || p.hasVehicle();
    }

    public static boolean check2() {
        var p = mc().player;
        if (p == null) return false;
        Item item = p.getMainHandStack().getItem();
        return item instanceof AxeItem || item instanceof TridentItem || item instanceof MaceItem;
    }

    public static boolean check3() {
        return isFloat(-0.01f);
    }

    public static boolean check4() {
        var p = mc().player;
        var opt = mc().options;
        if (p == null || opt == null) return false;
        if (!p.isTouchingWater() || p.isSubmergedInWater() || p.isSwimming()) return false;
        if (!opt.jumpKey.isPressed()) return false;
        return p.isOnGround() || p.getVelocity().y > 0.0;
    }

    public static boolean check5() {
        var p = mc().player;
        var w = mc().world;
        if (p == null || w == null) return false;
        BlockPos pos = p.getBlockPos().up(2);
        BlockState state = w.getBlockState(pos);
        return !state.isAir() && state.isFullCube(w, pos);
    }

    public static boolean isFloat(float value) {
        var p = mc().player;
        if (p == null) return false;
        if (p.isOnGround() || p.isInLava() || p.isTouchingWater() || p.hasVehicle()) return false;
        double vy = p.getVelocity().y;
        if (check5() && vy < -0.01) {
            double d1 = p.getY() - (p.getBlockPos().down().getY() + 1.0);
            if (d1 > 0.4) return true;
        }
        return vy < value;
    }

    public static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem
                || item instanceof TridentItem || item instanceof MaceItem;
    }

    public static boolean isClass(Class<?> cls) {
        var p = mc().player;
        if (p == null) return false;
        return cls.isInstance(p.getMainHandStack().getItem())
                || cls.isInstance(p.getOffHandStack().getItem());
    }

    public static boolean check6() {
        var p = mc().player;
        if (p == null) return false;
        return isWeapon(p.getMainHandStack()) || isWeapon(p.getOffHandStack());
    }

    public static void attack(Entity target) {
        var p = mc().player;
        var im = mc().interactionManager;
        if (p == null || im == null) return;
        im.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
        p.resetLastAttackedTicks();
    }

    public static float getFloat() {
        var p = mc().player;
        if (p == null) return 0.8f;
        boolean heavy = check2();
        if (!p.isOnGround() && !check()) return heavy ? 0.85f : 0.75f;
        return heavy ? 0.95f : 0.8f;
    }

    public static boolean canHit(String mode, float cdProgress) {
        var p = mc().player;
        if (p == null) return false;
        float need = getFloat();
        if (cdProgress < need) return false;
        boolean falling = isFloat(-0.01f);
        boolean impossible = check();
        if ("only_crits".equals(mode)) return impossible || falling;
        if (!impossible && p.getVelocity().y > 0.0) return false;
        if (p.isOnGround()) return true;
        return impossible || falling;
    }
}