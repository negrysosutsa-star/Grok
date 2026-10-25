package ft.triggerbot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ClickGui extends Screen {
    private final TriggerBot m = TriggerBot.INSTANCE;
    private boolean listeningTbKey = false;
    private boolean listeningAaKey = false;

    public ClickGui() {
        super(Text.literal("FT Client"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = 40;
        int w = 200;
        int h = 18;
        int g = 22;

        // --- TRIGGER ---
        addDrawableChild(btn(cx, y, w, h, "TriggerBot: " + onOff(m.tbEnabled), b -> {
            m.tbEnabled = !m.tbEnabled;
            b.setMessage(Text.literal("TriggerBot: " + onOff(m.tbEnabled)));
        }));
        y += g;

        addDrawableChild(btn(cx, y, w, h, "TB Bind: " + TriggerBot.keyName(m.tbKey), b -> {
            listeningTbKey = true;
            listeningAaKey = false;
            b.setMessage(Text.literal("TB Bind: ..."));
        }));
        y += g;

        addDrawableChild(btn(cx, y, w, h, "Only Players: " + onOff(m.onlyPlayers), b -> {
            m.onlyPlayers = !m.onlyPlayers;
            b.setMessage(Text.literal("Only Players: " + onOff(m.onlyPlayers)));
        }));
        y += g;

        addDrawableChild(btn(cx, y, w, h, "Only Crits: " + onOff(m.onlyCrits), b -> {
            m.onlyCrits = !m.onlyCrits;
            b.setMessage(Text.literal("Only Crits: " + onOff(m.onlyCrits)));
        }));
        y += g;

        addDrawableChild(btn(cx, y, w, h, "Weapon Only: " + onOff(m.weaponOnly), b -> {
            m.weaponOnly = !m.weaponOnly;
            b.setMessage(Text.literal("Weapon Only: " + onOff(m.weaponOnly)));
        }));
        y += g;

        addDrawableChild(btn(cx, y, w, h, "Pause On Use: " + onOff(m.pauseOnUse), b -> {
            m.pauseOnUse = !m.pauseOnUse;
            b.setMessage(Text.literal("Pause On Use: " + onOff(m.pauseOnUse)));
        }));
        y += g;

        addDrawableChild(new IntSlider(cx - w / 2, y, w, h, "Min Delay", m.minDelay, 0, 15, v -> m.minDelay = v));
        y += g;
        addDrawableChild(new IntSlider(cx - w / 2, y, w, h, "Max Delay", m.maxDelay, 0, 15, v -> {
            m.maxDelay = v;
            if (m.maxDelay < m.minDelay) m.minDelay = m.maxDelay;
        }));
        y += g;
        addDrawableChild(new FloatSlider(cx - w / 2, y, w, h, "TB Range", m.range, 1f, 6f, v -> m.range = v));
        y += g + 6;

        // --- AIM ASSIST ---
        addDrawableChild(btn(cx, y, w, h, "AimAssist: " + onOff(m.aaEnabled), b -> {
            m.aaEnabled = !m.aaEnabled;
            b.setMessage(Text.literal("AimAssist: " + onOff(m.aaEnabled)));
        }));
        y += g;

        addDrawableChild(btn(cx, y, w, h, "AA Bind: " + TriggerBot.keyName(m.aaKey), b -> {
            listeningAaKey = true;
            listeningTbKey = false;
            b.setMessage(Text.literal("AA Bind: ..."));
        }));
        y += g;

        addDrawableChild(btn(cx, y, w, h, "AA Players Only: " + onOff(m.aaPlayersOnly), b -> {
            m.aaPlayersOnly = !m.aaPlayersOnly;
            b.setMessage(Text.literal("AA Players Only: " + onOff(m.aaPlayersOnly)));
        }));
        y += g;

        addDrawableChild(btn(cx, y, w, h, "AA Weapon Only: " + onOff(m.aaWeaponOnly), b -> {
            m.aaWeaponOnly = !m.aaWeaponOnly;
            b.setMessage(Text.literal("AA Weapon Only: " + onOff(m.aaWeaponOnly)));
        }));
        y += g;

        addDrawableChild(btn(cx, y, w, h, "AA Visible Only: " + onOff(m.aaVisibleOnly), b -> {
            m.aaVisibleOnly = !m.aaVisibleOnly;
            b.setMessage(Text.literal("AA Visible Only: " + onOff(m.aaVisibleOnly)));
        }));
        y += g;

        addDrawableChild(new FloatSlider(cx - w / 2, y, w, h, "AA Range", m.aaRange, 1f, 6f, v -> m.aaRange = v));
        y += g;
        addDrawableChild(new FloatSlider(cx - w / 2, y, w, h, "AA FOV", m.aaFov, 20f, 360f, v -> m.aaFov = v));
        y += g;
        addDrawableChild(new FloatSlider(cx - w / 2, y, w, h, "AA Speed", m.aaSpeed, 1f, 30f, v -> m.aaSpeed = v));
        y += g + 8;

        addDrawableChild(btn(cx, y, w, h, "Close", b -> close()));
    }

    private ButtonWidget btn(int cx, int y, int w, int h, String text, ButtonWidget.PressAction a) {
        return ButtonWidget.builder(Text.literal(text), a).dimensions(cx - w / 2, y, w, h).build();
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listeningTbKey) {
            m.tbKey = keyCode == GLFW.GLFW_KEY_ESCAPE ? 0 : keyCode;
            listeningTbKey = false;
            clearAndInit();
            return true;
        }
        if (listeningAaKey) {
            m.aaKey = keyCode == GLFW.GLFW_KEY_ESCAPE ? 0 : keyCode;
            listeningAaKey = false;
            clearAndInit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_INSERT || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFF);
        if (listeningTbKey || listeningAaKey) {
            ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("press key (ESC = none)"), width / 2, height - 20, 0xFFAA00);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private class IntSlider extends SliderWidget {
        private final String label;
        private final int min, max;
        private final java.util.function.IntConsumer setter;

        IntSlider(int x, int y, int w, int h, String label, int value, int min, int max, java.util.function.IntConsumer setter) {
            super(x, y, w, h, Text.empty(), (value - min) / (double) (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int v = (int) Math.round(min + value * (max - min));
            setMessage(Text.literal(label + ": " + v));
        }

        @Override
        protected void applyValue() {
            int v = (int) Math.round(min + value * (max - min));
            setter.accept(v);
        }
    }

    private class FloatSlider extends SliderWidget {
        private final String label;
        private final float min, max;
        private final java.util.function.FloatConsumer setter;

        FloatSlider(int x, int y, int w, int h, String label, float value, float min, float max, java.util.function.FloatConsumer setter) {
            super(x, y, w, h, Text.empty(), (value - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            float v = (float) (min + value * (max - min));
            setMessage(Text.literal(label + ": " + String.format("%.1f", v)));
        }

        @Override
        protected void applyValue() {
            float v = (float) (min + value * (max - min));
            setter.accept(v);
        }
    }
}