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
    public final AimAssistModule  aimAssist  = new AimAssistModule();
    public final Module[] modules = { triggerBot, aimAssist };

    public int guiKey = 260; // INSERT
    private boolean unhooked, wasGui, wasHome, loaded;
    private final boolean[] wasKey = new boolean[2];

    @Override
    public void onInitializeClient() { INSTANCE = this; }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (unhooked || mc.player == null) return;
        TickCounter.update();

        // Открыть GUI
        boolean gui = key(mc, guiKey);
        if (gui && !wasGui && mc.currentScreen == null) mc.setScreen(new ClickGui());
        wasGui = gui;
        if (mc.currentScreen != null) return;

        // Unhook
        boolean home = key(mc, 268);
        if (home && !wasHome) {
            unhooked = true;
            for (Module m : modules) m.enabled = false;
            ChatUtil.send("unhooked");
        }
        wasHome = home;

        // Приветствие
        if (!loaded) {
            ChatUtil.send("loaded | Insert menu | Home unhook");
            loaded = true;
        }

        // Биндинги из поля keyCode (используем .key только как алиас, см. ниже)
        for (int i = 0; i < modules.length; i++) {
            Module m = modules[i];
            int k = getModuleKey(m);
            if (k == 0) continue;
            boolean d = key(mc, k);
            if (d && !wasKey[i]) {
                m.toggle();
                ChatUtil.send(m.name + " " + (m.enabled ? "on" : "off"));
            }
            wasKey[i] = d;
        }

        // Тик модулей
        triggerBot.onTick(mc, aimAssist); // передаём aimAssist для onAttack
        if (aimAssist.enabled) aimAssist.onTick(mc);
    }

    /** Получить кнопку модуля — читаем keyCode из конкретных классов */
    private static int getModuleKey(Module m) {
        if (m instanceof TriggerBotModule tb) return tb.keyCode;
        if (m instanceof AimAssistModule  aa) return aa.keyCode;
        return m.key;
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
