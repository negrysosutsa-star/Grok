package ft.triggerbot;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import ft.triggerbot.gui.ClickGui;
import ft.triggerbot.module.AimAssistModule;
import ft.triggerbot.module.Module;
import ft.triggerbot.module.TriggerBotModule;
import ft.triggerbot.util.ChatUtil;
import ft.triggerbot.util.TickCounter;

public class ClientMain implements ClientModInitializer {
    public static ClientMain INSTANCE;
    public final TriggerBotModule triggerBot = new TriggerBotModule();
    public final AimAssistModule aimAssist = new AimAssistModule();
    public final Module[] modules = { triggerBot, aimAssist };

    public int guiKey = 260;
    private boolean unhooked, wasGui, wasHome, loaded;
    private final boolean[] wasKey = new boolean[2];

    @Override
    public void onInitializeClient() { INSTANCE = this; }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (unhooked || mc.player == null) return;
        TickCounter.update();

        boolean gui = key(mc, guiKey);
        if (gui && !wasGui && mc.currentScreen == null) mc.setScreen(new ClickGui());
        wasGui = gui;
        if (mc.currentScreen != null) return;

        boolean home = key(mc, 268);
        if (home && !wasHome) {
            unhooked = true;
            for (Module m : modules) m.enabled = false;
            ChatUtil.send("unhooked");
        }
        wasHome = home;

        if (!loaded) {
            ChatUtil.send("loaded | Insert menu | Home unhook");
            loaded = true;
        }

        for (int i = 0; i < modules.length; i++) {
            Module m = modules[i];
            if (m.key == 0) continue;
            boolean d = key(mc, m.key);
            if (d && !wasKey[i]) {
                m.toggle();
                ChatUtil.send(m.name + " " + (m.enabled ? "on" : "off"));
            }
            wasKey[i] = d;
        }

        for (Module m : modules) if (m.enabled) m.onTick(mc);
    }

    private static boolean key(MinecraftClient mc, int k) {
        if (k == 0) return false;
        return InputUtil.isKeyPressed(mc.getWindow().getHandle(), k);
    }

    public static String keyName(int key) {
        if (key == 0) return "NONE";
        try { return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase(); }
        catch (Exception e) { return String.valueOf(key); }
    }
}