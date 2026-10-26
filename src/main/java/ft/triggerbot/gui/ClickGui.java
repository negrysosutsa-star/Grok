package ft.triggerbot.gui;

import ft.triggerbot.ClientMain;
import ft.triggerbot.module.AimAssistModule;
import ft.triggerbot.module.TriggerBotModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClickGui extends Screen {

    private static final int COL_BG        = 0xE5121212;
    private static final int COL_HEADER    = 0xFF1A1A1A;
    private static final int COL_ACCENT    = 0xFF5865F2;
    private static final int COL_ACCENT2   = 0xFF4752C4;
    private static final int COL_ENABLED   = 0xFF43B581;
    private static final int COL_DISABLED  = 0xFF36393F;
    private static final int COL_HOVER     = 0xFF2C2F33;
    private static final int COL_TEXT      = 0xFFDCDDDE;
    private static final int COL_SUBTEXT   = 0xFF72767D;
    private static final int COL_SLIDER_BG = 0xFF2F3136;
    private static final int COL_DIVIDER   = 0xFF2C2F33;

    private static final int WIN_W = 310;
    private static final int WIN_H = 460;
    private static final int TAB_H = 32;
    private static final int ROW_H = 26;
    private static final int PAD   = 12;

    private final TriggerBotModule triggerBot;
    private final AimAssistModule  aimAssist;

    private int activeTab = 0;

    private int winX, winY;
    private boolean dragging;
    private int dragOffX, dragOffY;

    private float scrollY = 0;
    private static final float SCROLL_SPEED = 18f;
    private int contentHeight = 0;

    private int capturingKey = -1;

    public ClickGui() {
        super(Text.literal("FT Client"));
        this.triggerBot = ClientMain.INSTANCE.triggerBot;
        this.aimAssist  = ClientMain.INSTANCE.aimAssist;
    }

    @Override
    protected void init() {
        MinecraftClient mc = MinecraftClient.getInstance();
        winX = (mc.getWindow().getScaledWidth()  - WIN_W) / 2;
        winY = (mc.getWindow().getScaledHeight() - WIN_H) / 2;
        scrollY = 0;
    }

    // ════════════════════════════════════════════════════════════════
    // RENDER
    // ════════════════════════════════════════════════════════════════

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);

        int wx = winX, wy = winY;

        fillRect(ctx, wx - 4, wy - 4, WIN_W + 8, WIN_H + 8, 0x55000000);
        fillRect(ctx, wx, wy, WIN_W, WIN_H, COL_BG);
        fillRect(ctx, wx, wy, WIN_W, TAB_H, COL_HEADER);
        drawString(ctx, "§bFT Client §7v1.2", wx + PAD, wy + TAB_H / 2 - 4, COL_TEXT);

        int tabW = WIN_W / 2;
        for (int i = 0; i < 2; i++) {
            String label   = i == 0 ? "TriggerBot" : "AimAssist";
            boolean active  = activeTab == i;
            boolean enabled = i == 0 ? triggerBot.enabled : aimAssist.enabled;
            int tx = wx + i * tabW, ty = wy + TAB_H;
            boolean hover = mouseX >= tx && mouseX < tx + tabW && mouseY >= ty && mouseY < ty + 22;
            fillRect(ctx, tx, ty, tabW, 22, active ? COL_ACCENT : (hover ? COL_HOVER : COL_HEADER));
            fillRect(ctx, tx + 8, ty + 8, 6, 6, enabled ? COL_ENABLED : COL_DISABLED);
            drawString(ctx, label, tx + 20, ty + 7, active ? 0xFFFFFFFF : COL_SUBTEXT);
            if (active) fillRect(ctx, tx, ty + 20, tabW, 2, COL_ACCENT);
        }

        int contentTop = wy + TAB_H + 22 + 2;
        int contentH   = WIN_H - TAB_H - 22 - 2;

        ctx.enableScissor(wx, contentTop, wx + WIN_W, contentTop + contentH);
        int y = contentTop - (int) scrollY;
        int rowW = WIN_W - PAD * 2;
        int totalH = activeTab == 0
            ? renderTBTab(ctx, wx + PAD, y, rowW, mouseX, mouseY)
            : renderAATab(ctx, wx + PAD, y, rowW, mouseX, mouseY);
        contentHeight = totalH;
        ctx.disableScissor();

        if (contentHeight > contentH) {
            float ratio = (float) contentH / contentHeight;
            int sbH = Math.max(20, (int)(contentH * ratio));
            int sbX = wx + WIN_W - 4;
            int sbY = contentTop + (int)(scrollY / contentHeight * contentH);
            fillRect(ctx, sbX, contentTop, 3, contentH, 0x22FFFFFF);
            fillRect(ctx, sbX, sbY, 3, sbH, COL_ACCENT);
        }

        fillRect(ctx, wx, contentTop, WIN_W, 1, COL_DIVIDER);
        super.render(ctx, mouseX, mouseY, delta);
    }

    // ════════════════════════════════════════════════════════════════
    // TABS RENDER
    // ════════════════════════════════════════════════════════════════

    private int renderTBTab(DrawContext ctx, int x, int y, int w, int mx, int my) {
        int startY = y;

        y = renderToggleRow(ctx, x, y, w, mx, my, "TriggerBot", triggerBot.enabled);
        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "РЕЖИМ");
        y = renderCycleRow(ctx, x, y, w, mx, my, "Крит-режим",
            new String[]{"smart","only_crits"}, new String[]{"Смарт","Только криты"},
            triggerBot.critMode);
        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "ФИЛЬТРЫ");
        y = renderToggleRow(ctx, x, y, w, mx, my, "Только игроки",    triggerBot.onlyPlayers);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Только с оружием", triggerBot.weaponOnly);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Пауза при исп.",   triggerBot.pauseOnUse);
        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "КУЛДАУН");
        y = renderToggleRow(ctx, x, y, w, mx, my, "Свой кулдаун %",   triggerBot.customCooldown);
        if (triggerBot.customCooldown)
            y = renderSliderRow(ctx, x, y, w, "Кулдаун %", triggerBot.cooldownPct, 0f, 100f, 0);
        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "ЗАДЕРЖКА");
        y = renderSliderRow(ctx, x, y, w, "Мин. задержка (мс)", triggerBot.minDelay, 0f, 300f, 0);
        y = renderSliderRow(ctx, x, y, w, "Макс. задержка (мс)", triggerBot.maxDelay, 0f, 300f, 0);
        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "ДОПОЛНИТЕЛЬНО");
        y = renderToggleRow(ctx, x, y, w, mx, my, "Сброс спринта", triggerBot.sprintReset);
        y = renderToggleRow(ctx, x, y, w, mx, my, "TPS-поправка",  triggerBot.tpsSink);
        y = renderSliderRow(ctx, x, y, w, "Дальность", (float) triggerBot.range, 1f, 6f, 1);
        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "БИНДИНГ");
        y = renderKeybindRow(ctx, x, y, w, mx, my, "Кнопка TB", keyName(triggerBot.keyCode), 0);

        return y - startY + 8;
    }

    private int renderAATab(DrawContext ctx, int x, int y, int w, int mx, int my) {
        int startY = y;

        y = renderToggleRow(ctx, x, y, w, mx, my, "AimAssist", aimAssist.enabled);
        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "РЕЖИМ ПРИЦЕЛИВАНИЯ");
        y = renderCycleRow(ctx, x, y, w, mx, my, "Режим",
            new String[]{"legit","free"}, new String[]{"Легит","Свободный"},
            aimAssist.aimModeStr);

        if ("legit".equals(aimAssist.aimModeStr)) {
            y = renderSectionLabel(ctx, x, y, "  ЛЕГИТ");
            y = renderSliderRow(ctx, x, y, w, "Скорость",    (float) aimAssist.legitAim.speed,     0.1f, 3f,   2);
            y = renderSliderRow(ctx, x, y, w, "Сглаживание", (float) aimAssist.legitAim.smoothing,  0.2f, 3f,   2);
            y = renderSliderRow(ctx, x, y, w, "Макс. лерп",  (float) aimAssist.legitAim.maxLerp,    0.2f, 2.5f, 2);
            y = renderSliderRow(ctx, x, y, w, "Мин. лерп",   (float) aimAssist.legitAim.minLerp,    0.01f,0.5f, 2);
            y = renderToggleRow(ctx, x, y, w, mx, my, "Рандомизация", aimAssist.legitAim.randomize);
        } else {
            y = renderSectionLabel(ctx, x, y, "  СВОБОДНЫЙ");
            y = renderSliderRow(ctx, x, y, w, "Скорость",       (float) aimAssist.freeAim.speed,          5f, 20f, 1);
            y = renderSliderRow(ctx, x, y, w, "Сила коррекции", (float) aimAssist.freeAim.corrStrength,   0.1f,1f, 2);
            y = renderSliderRow(ctx, x, y, w, "Свобода вниз",   (float) aimAssist.freeAim.freedomDown,   -1f, 1f, 2);
            y = renderSliderRow(ctx, x, y, w, "Порог замедл.",  (float) aimAssist.freeAim.slowThreshold,  1f, 20f, 1);
            y = renderToggleRow(ctx, x, y, w, mx, my, "Плавный вход",  aimAssist.freeAim.smoothEntry);
            y = renderToggleRow(ctx, x, y, w, mx, my, "Учитыв. сенсу", aimAssist.freeAim.useSens);
            y = renderToggleRow(ctx, x, y, w, mx, my, "Замедл. вблизи",aimAssist.freeAim.slowNear);
            y = renderToggleRow(ctx, x, y, w, mx, my, "Шум наводки",   aimAssist.freeAim.noise);
        }

        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "ОБЩИЕ");
        y = renderToggleRow(ctx, x, y, w, mx, my, "Только игроки",    aimAssist.onlyPlayers);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Только с оружием", aimAssist.weaponOnly);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Проверка стен",    aimAssist.wallCheck);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Только по X",      aimAssist.onlyOnX);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Видимые только",   aimAssist.visibleOnly);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Не при еде",       aimAssist.notWhileEating);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Не в инвентаре",   aimAssist.notInInventory);
        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "ПАРАМЕТРЫ");
        y = renderSliderRow(ctx, x, y, w, "Дальность",       (float) aimAssist.range,        1f, 6f,   1);
        y = renderSliderRow(ctx, x, y, w, "FOV",             (float) aimAssist.fov,           10f,360f, 0);
        y = renderSliderRow(ctx, x, y, w, "Удержание (сек)", (float) aimAssist.holdAfterHit,  0f, 5f,  1);
        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, "БИНДИНГ");
        y = renderKeybindRow(ctx, x, y, w, mx, my, "Кнопка AA", keyName(aimAssist.keyCode), 1);

        return y - startY + 8;
    }

    // ════════════════════════════════════════════════════════════════
    // КОМПОНЕНТЫ
    // ════════════════════════════════════════════════════════════════

    private int renderToggleRow(DrawContext ctx, int x, int y, int w,
                                int mx, int my, String label, boolean value) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        if (hover) fillRect(ctx, x - 1, y, w + 2, ROW_H, COL_HOVER);

        int tW = 32, tH = 14;
        int tx = x + w - tW - 2, ty = y + (ROW_H - tH) / 2;
        fillRoundRect(ctx, tx, ty, tW, tH, value ? COL_ENABLED : COL_DISABLED);
        int cx = value ? tx + tW - tH + 1 : tx + 1;
        fillRoundRect(ctx, cx, ty + 1, tH - 2, tH - 2, 0xFFFFFFFF);
        drawString(ctx, label, x + 6, y + (ROW_H - 8) / 2, value ? COL_TEXT : COL_SUBTEXT);
        return y + ROW_H;
    }

    private int renderCycleRow(DrawContext ctx, int x, int y, int w,
                               int mx, int my, String label,
                               String[] vals, String[] display, String current) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        if (hover) fillRect(ctx, x - 1, y, w + 2, ROW_H, COL_HOVER);

        String disp = current;
        for (int i = 0; i < vals.length; i++)
            if (vals[i].equals(current)) { disp = display[i]; break; }

        int bW = 90, bX = x + w - bW - 2, bY = y + (ROW_H - 16) / 2;
        fillRoundRect(ctx, bX, bY, bW, 16, COL_ACCENT2);
        drawString(ctx, disp, bX + (bW - textWidth(disp)) / 2, bY + 4, 0xFFFFFFFF);
        drawString(ctx, label, x + 6, y + (ROW_H - 8) / 2, COL_TEXT);
        return y + ROW_H;
    }

    private int renderSliderRow(DrawContext ctx, int x, int y, int w,
                                String label, float value, float min, float max, int dec) {
        int h = ROW_H + 10;
        String valStr = dec == 0 ? String.valueOf((int) value)
                                 : String.format("%." + dec + "f", value);
        drawString(ctx, label, x + 6, y + 5, COL_SUBTEXT);
        drawString(ctx, valStr, x + w - textWidth(valStr) - 4, y + 5, COL_ACCENT);

        int sX = x + 6, sW = w - 12, sY = y + 19;
        fillRoundRect(ctx, sX, sY, sW, 4, COL_SLIDER_BG);
        int filled = (int)(sW * Math.max(0f, Math.min(1f, (value - min) / (max - min))));
        fillRoundRect(ctx, sX, sY, filled, 4, COL_ACCENT);
        fillRoundRect(ctx, sX + filled - 4, sY - 3, 8, 10, 0xFFFFFFFF);
        return y + h;
    }

    private int renderKeybindRow(DrawContext ctx, int x, int y, int w,
                                 int mx, int my, String label, String keyName, int bindIdx) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        if (hover) fillRect(ctx, x - 1, y, w + 2, ROW_H, COL_HOVER);

        boolean cap = capturingKey == bindIdx;
        String disp = cap ? "§e[нажми клавишу]" : keyName;
        int bW = 100, bX = x + w - bW - 2, bY = y + (ROW_H - 16) / 2;
        fillRoundRect(ctx, bX, bY, bW, 16, cap ? COL_ACCENT : COL_SLIDER_BG);
        String raw = cap ? "[нажми клавишу]" : keyName;
        drawString(ctx, disp, bX + (bW - textWidth(raw)) / 2, bY + 4, 0xFFFFFFFF);
        drawString(ctx, label, x + 6, y + (ROW_H - 8) / 2, COL_TEXT);
        return y + ROW_H;
    }

    private int renderDivider(DrawContext ctx, int x, int y, int w) {
        fillRect(ctx, x, y + 5, w, 1, COL_DIVIDER);
        return y + 11;
    }

    private int renderSectionLabel(DrawContext ctx, int x, int y, String label) {
        drawString(ctx, "§8" + label, x + 6, y + 2, COL_SUBTEXT);
        return y + 14;
    }

    // ════════════════════════════════════════════════════════════════
    // MOUSE / KEY
    // ════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int imx = (int) mouseX, imy = (int) mouseY;

        if (capturingKey >= 0) { capturingKey = -1; return true; }

        if (imx >= winX && imx < winX + WIN_W && imy >= winY && imy < winY + TAB_H) {
            dragging = true; dragOffX = imx - winX; dragOffY = imy - winY; return true;
        }

        int tabW = WIN_W / 2, tabY = winY + TAB_H;
        for (int i = 0; i < 2; i++) {
            int tx = winX + i * tabW;
            if (imx >= tx && imx < tx + tabW && imy >= tabY && imy < tabY + 22) {
                activeTab = i; scrollY = 0; return true;
            }
        }

        int contentTop = winY + TAB_H + 22 + 2;
        int contentH   = WIN_H - TAB_H - 22 - 2;
        if (imx >= winX && imx < winX + WIN_W && imy >= contentTop && imy < contentTop + contentH) {
            int x = winX + PAD, w = WIN_W - PAD * 2;
            int y = contentTop - (int) scrollY;
            if (activeTab == 0) clickTBTab(x, y, w, imx, imy);
            else                clickAATab(x, y, w, imx, imy);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickTBTab(int x, int y, int w, int imx, int imy) {
        if (hit(x, y, w, imx, imy, ROW_H)) { triggerBot.toggle(); return; } y += ROW_H;
        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H)) { triggerBot.critMode = triggerBot.critMode.equals("smart") ? "only_crits" : "smart"; return; } y += ROW_H;
        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H)) { triggerBot.onlyPlayers   = !triggerBot.onlyPlayers;   return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H)) { triggerBot.weaponOnly    = !triggerBot.weaponOnly;    return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H)) { triggerBot.pauseOnUse   = !triggerBot.pauseOnUse;    return; } y += ROW_H;
        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H)) { triggerBot.customCooldown = !triggerBot.customCooldown; return; } y += ROW_H;
        if (triggerBot.customCooldown) {
            if (hit(x, y, w, imx, imy, ROW_H + 10)) { triggerBot.cooldownPct = (int) sv(x, y, w, imx, 0f, 100f); return; } y += ROW_H + 10;
        }
        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H + 10)) { triggerBot.minDelay = (int) sv(x, y, w, imx, 0f, 300f); return; } y += ROW_H + 10;
        if (hit(x, y, w, imx, imy, ROW_H + 10)) { triggerBot.maxDelay = (int) sv(x, y, w, imx, 0f, 300f); return; } y += ROW_H + 10;
        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H)) { triggerBot.sprintReset = !triggerBot.sprintReset; return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H)) { triggerBot.tpsSink     = !triggerBot.tpsSink;     return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H + 10)) { triggerBot.range  = sv(x, y, w, imx, 1f, 6f); return; } y += ROW_H + 10;
        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H)) { capturingKey = 0; }
    }

    private void clickAATab(int x, int y, int w, int imx, int imy) {
        if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.toggle(); return; } y += ROW_H;
        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.aimModeStr = aimAssist.aimModeStr.equals("legit") ? "free" : "legit"; return; } y += ROW_H;

        if ("legit".equals(aimAssist.aimModeStr)) {
            y += 14;
            if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.legitAim.speed     = sv(x, y, w, imx, 0.1f, 3f);    return; } y += ROW_H + 10;
            if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.legitAim.smoothing = sv(x, y, w, imx, 0.2f, 3f);    return; } y += ROW_H + 10;
            if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.legitAim.maxLerp   = sv(x, y, w, imx, 0.2f, 2.5f);  return; } y += ROW_H + 10;
            if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.legitAim.minLerp   = sv(x, y, w, imx, 0.01f, 0.5f); return; } y += ROW_H + 10;
            if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.legitAim.randomize = !aimAssist.legitAim.randomize; return; } y += ROW_H;
        } else {
            y += 14;
            if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.freeAim.speed         = sv(x, y, w, imx, 5f, 20f);  return; } y += ROW_H + 10;
            if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.freeAim.corrStrength  = sv(x, y, w, imx, 0.1f, 1f); return; } y += ROW_H + 10;
            if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.freeAim.freedomDown   = sv(x, y, w, imx, -1f, 1f);  return; } y += ROW_H + 10;
            if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.freeAim.slowThreshold = sv(x, y, w, imx, 1f, 20f);  return; } y += ROW_H + 10;
            if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.freeAim.smoothEntry = !aimAssist.freeAim.smoothEntry; return; } y += ROW_H;
            if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.freeAim.useSens    = !aimAssist.freeAim.useSens;    return; } y += ROW_H;
            if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.freeAim.slowNear   = !aimAssist.freeAim.slowNear;   return; } y += ROW_H;
            if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.freeAim.noise      = !aimAssist.freeAim.noise;      return; } y += ROW_H;
        }

        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.onlyPlayers    = !aimAssist.onlyPlayers;    return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.weaponOnly      = !aimAssist.weaponOnly;     return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.wallCheck       = !aimAssist.wallCheck;      return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.onlyOnX         = !aimAssist.onlyOnX;        return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.visibleOnly     = !aimAssist.visibleOnly;    return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.notWhileEating  = !aimAssist.notWhileEating; return; } y += ROW_H;
        if (hit(x, y, w, imx, imy, ROW_H)) { aimAssist.notInInventory  = !aimAssist.notInInventory; return; } y += ROW_H;
        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.range       = sv(x, y, w, imx, 1f, 6f);    return; } y += ROW_H + 10;
        if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.fov         = sv(x, y, w, imx, 10f, 360f); return; } y += ROW_H + 10;
        if (hit(x, y, w, imx, imy, ROW_H + 10)) { aimAssist.holdAfterHit= sv(x, y, w, imx, 0f, 5f);    return; } y += ROW_H + 10;
        y += 11; y += 14;
        if (hit(x, y, w, imx, imy, ROW_H)) { capturingKey = 1; }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging) { winX = (int) mouseX - dragOffX; winY = (int) mouseY - dragOffY; return true; }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        int contentH   = WIN_H - TAB_H - 22 - 2;
        int maxScroll  = Math.max(0, contentHeight - contentH);
        scrollY = (float) Math.max(0, Math.min(scrollY - vAmt * SCROLL_SPEED, maxScroll));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingKey == 0) { triggerBot.keyCode = keyCode; capturingKey = -1; return true; }
        if (capturingKey == 1) { aimAssist.keyCode  = keyCode; capturingKey = -1; return true; }
        if (keyCode == 256) { MinecraftClient.getInstance().setScreen(null); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    // ════════════════════════════════════════════════════════════════
    // УТИЛИТЫ
    // ════════════════════════════════════════════════════════════════

    private boolean hit(int x, int y, int w, int mx, int my, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** slider value из позиции мыши */
    private float sv(int x, int y, int w, int mx, float min, float max) {
        int sX = x + 6, sW = w - 12;
        float pct = Math.max(0f, Math.min(1f, (float)(mx - sX) / sW));
        return min + (max - min) * pct;
    }

    private void fillRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    private void fillRoundRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + 1, 0);
        ctx.fill(x + w - 1, y, x + w, y + 1, 0);
        ctx.fill(x, y + h - 1, x + 1, y + h, 0);
        ctx.fill(x + w - 1, y + h - 1, x + w, y + h, 0);
    }

    private void drawString(DrawContext ctx, String text, int x, int y, int color) {
        ctx.drawText(MinecraftClient.getInstance().textRenderer, text, x, y, color, false);
    }

    private int textWidth(String text) {
        return MinecraftClient.getInstance().textRenderer.getWidth(text);
    }

    private static String keyName(int keyCode) {
        if (keyCode <= 0) return "NONE";
        try {
            String n = org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0);
            if (n != null) return n.toUpperCase();
        } catch (Exception ignored) {}
        return "KEY_" + keyCode;
    }
}
