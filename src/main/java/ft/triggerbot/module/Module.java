package ft.triggerbot.module;

import net.minecraft.client.MinecraftClient;

public abstract class Module {
    public final String name;
    public boolean enabled;
    public int key;

    protected Module(String name, int defaultKey) {
        this.name = name;
        this.key = defaultKey;
        this.enabled = false;
    }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public abstract void onTick(MinecraftClient mc);
}