package ft.triggerbot.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class KeyBindings {
    private KeyBindings() {}

    private static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    /** Сброс спринта + отпустить все движения перед ударом */
    public static void resetSprint() {
        var mc = mc();
        if (mc.options == null) return;
        if (mc.player != null) mc.player.setSprinting(false);
        for (KeyBinding kb : getMovementKeys()) kb.setPressed(false);
    }

    /** Отпустить клавишу атаки */
    public static void releaseAttack() {
        var mc = mc();
        if (mc != null && mc.options != null) mc.options.attackKey.setPressed(false);
    }

    /** Симулировать нажатие атаки (для крит-прыжка) */
    public static void pressAttack() {
        var mc = mc();
        if (mc == null || mc.options == null) return;
        var key = InputUtil.fromTranslationKey(mc.options.attackKey.getBoundKeyTranslationKey());
        KeyBinding.onKeyPressed(key);
    }

    /** Восстановить реальное состояние движения по хардверу */
    public static void restoreMovement() {
        var mc = mc();
        if (mc == null || mc.options == null || mc.getWindow() == null || mc.currentScreen != null) return;
        long handle = mc.getWindow().getHandle();
        for (KeyBinding kb : getMovementKeys()) {
            int code = kb.getDefaultKey().getCode();
            kb.setPressed(InputUtil.isKeyPressed(handle, code));
        }
    }

    /** Отпустить use-клавишу */
    public static void releaseUse() {
        var mc = mc();
        if (mc != null && mc.options != null) mc.options.useKey.setPressed(false);
    }

    private static KeyBinding[] getMovementKeys() {
        var opts = mc().options;
        return new KeyBinding[]{ opts.forwardKey };
    }
}
