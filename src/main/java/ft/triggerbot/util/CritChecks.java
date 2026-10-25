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
    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static boolean check() {
        var p = mc().player;
        if (p == null) return false;
        return p.isTouchingWater() || p.isSwimming() || p.isGliding()
            || p.isClimbing() || p.isInsideWall() || p.isInLava() || p.hasVehicle();
    }

    public static boolean check2() {
        var p = mc().player;
        if (p == null) return false;
        Item i = p.getMainHandStack().getItem();
        return i instanceof AxeItem || i instanceof TridentItem || i instanceof MaceItem;
    }

    public static boolean check3() { return isFloat(-0.01f); }

    public static boolean check4() {
        var p = mc().player; var o = mc().options;
        if (p == null || o == null) return false;
        if (!p.isTouchingWater() || p.isSubmergedInWater() || p.isSwimming()) return false;
        if (!o.jumpKey.isPressed()) return false;
        return p.isOnGround() || p.getVelocity().y > 0.0;
    }

    public static boolean check5() {
        var p = mc().player; var w = mc().world;
        if (p == null || w == null) return false;
        BlockPos pos = p.getBlockPos().up(2);
        BlockState st = w.getBlockState(pos);
        return !st.isAir() && st.isFullCube(w, pos);
    }

    public static boolean isFloat(float v) {
        var p = mc().player;
        if (p == null) return false;
        if (p.isOnGround() || p.isInLava() || p.isTouchingWater() || p.hasVehicle()) return false;
        double vy = p.getVelocity().y;
        if (check5() && vy < -0.01) {
            double d = p.getY() - (p.getBlockPos().down().getY() + 1.0);
            if (d > 0.4) return true;
        }
        return vy < v;
    }

    public static boolean isWeapon(ItemStack s) {
        if (s.isEmpty()) return false;
        Item i = s.getItem();
        return i instanceof SwordItem || i instanceof AxeItem || i instanceof TridentItem || i instanceof MaceItem;
    }

    public static boolean isClass(Class<?> c) {
        var p = mc().player;
        if (p == null) return false;
        return c.isInstance(p.getMainHandStack().getItem()) || c.isInstance(p.getOffHandStack().getItem());
    }

    public static boolean check6() {
        var p = mc().player;
        if (p == null) return false;
        return isWeapon(p.getMainHandStack()) || isWeapon(p.getOffHandStack());
    }

    public static void attack(Entity t) {
        var p = mc().player; var im = mc().interactionManager;
        if (p == null || im == null) return;
        im.attackEntity(p, t);
        p.swingHand(Hand.MAIN_HAND);
        p.resetLastAttackedTicks();
    }

    public static float getFloat() {
        var p = mc().player;
        if (p == null) return 0.8f;
        boolean h = check2();
        if (!p.isOnGround() && !check()) return h ? 0.85f : 0.75f;
        return h ? 0.95f : 0.8f;
    }

    public static boolean canHit(String mode, float cd) {
        var p = mc().player;
        if (p == null) return false;
        float need = getFloat();
        if (cd < need) return false;
        boolean falling = isFloat(-0.01f);
        boolean impossible = check();
        if ("only_crits".equals(mode)) return impossible || falling;
        if (!impossible && p.getVelocity().y > 0.0) return false;
        if (p.isOnGround()) return true;
        return impossible || falling;
    }
}