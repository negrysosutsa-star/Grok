package ft.triggerbot;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class ClickGui extends Screen {
    private final TriggerBot tb = TriggerBot.INSTANCE;

    public ClickGui() {
        super(Text.literal("FT TriggerBot"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 80;
        int w = 160;
        int h = 20;
        int gap = 24;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Enabled: " + (tb.enabled ? "ON" : "OFF")),
                b -> {
                    tb.enabled = !tb.enabled;
                    b.setMessage(Text.literal("Enabled: " + (tb.enabled ? "ON" : "OFF")));
                }
        ).dimensions(cx - w / 2, y, w, h).build());
        y += gap;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Only Players: " + (tb.onlyPlayers ? "ON" : "OFF")),
                b -> {
                    tb.onlyPlayers = !tb.onlyPlayers;
                    b.setMessage(Text.literal("Only Players: " + (tb.onlyPlayers ? "ON" : "OFF")));
                }
        ).dimensions(cx - w / 2, y, w, h).build());
        y += gap;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Only Crits: " + (tb.onlyCrits ? "ON" : "OFF")),
                b -> {
                    tb.onlyCrits = !tb.onlyCrits;
                    b.setMessage(Text.literal("Only Crits: " + (tb.onlyCrits ? "ON" : "OFF")));
                }
        ).dimensions(cx - w / 2, y, w, h).build());
        y += gap;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Weapon Only: " + (tb.weaponOnly ? "ON" : "OFF")),
                b -> {
                    tb.weaponOnly = !tb.weaponOnly;
                    b.setMessage(Text.literal("Weapon Only: " + (tb.weaponOnly ? "ON" : "OFF")));
                }
        ).dimensions(cx - w / 2, y, w, h).build());
        y += gap;

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Pause On Use: " + (tb.pauseOnUse ? "ON" : "OFF")),
                b -> {
                    tb.pauseOnUse = !tb.pauseOnUse;
                    b.setMessage(Text.literal("Pause On Use: " + (tb.pauseOnUse ? "ON" : "OFF")));
                }
        ).dimensions(cx - w / 2, y, w, h).build());
        y += gap;

        addDrawableChild(new DelaySlider(cx - w / 2, y, w, h, tb.minDelay, true));
        y += gap;

        addDrawableChild(new DelaySlider(cx - w / 2, y, w, h, tb.maxDelay, false));
        y += gap + 8;

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(cx - w / 2, y, w, h).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFF);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private class DelaySlider extends SliderWidget {
        private final boolean isMin;

        public DelaySlider(int x, int y, int width, int height, int value, boolean isMin) {
            super(x, y, width, height, Text.empty(), (value - 1) / 19.0);
            this.isMin = isMin;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int val = (int) Math.round(1 + value * 19);
            setMessage(Text.literal((isMin ? "Min Delay: " : "Max Delay: ") + val));
        }

        @Override
        protected void applyValue() {
            int val = (int) Math.round(1 + value * 19);
            if (isMin) {
                tb.minDelay = val;
                if (tb.minDelay > tb.maxDelay) tb.maxDelay = tb.minDelay;
            } else {
                tb.maxDelay = val;
                if (tb.maxDelay < tb.minDelay) tb.minDelay = tb.maxDelay;
            }
        }
    }
}