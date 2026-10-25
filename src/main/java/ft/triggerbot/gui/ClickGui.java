package ft.triggerbot.gui;

import ft.triggerbot.ClientMain;
import ft.triggerbot.module.AimAssistModule;
import ft.triggerbot.module.TriggerBotModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ClickGui extends Screen {

    // Цвета (ARGB)
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

    // 0 = TriggerBot, 1 = AimAssist
    private int activeTab = 0;

    // Drag
    private int winX, winY;
    private boolean dragging;
    private int dragOffX, dragOffY;

    // Slider drag state
    private int draggingSlider = -1; // индекс слайдера
    private int mx, my;

    // Scroll
    private float scrollY = 0;
    private static final float SCROLL_SPEED = 18f;
    private int contentHeight = 0;

    // Keybind capture
    private int capturingKey = -1; // -1 = нет, 0 = TB bind, 1 = AA bind

    // ==================================================================
    // Описание всех строк UI — собирается при рендере
    // ==================================================================
    private interface Row {
        void render(DrawContext ctx, int wx, int wy, int rowW, int y, int mouseX, int mouseY);
        boolean onClick(int wx, int wy, int rowW, int y, int mouseX, int mouseY, int btn);
        default boolean onScrollOver(int wx, int wy, int rowW, int y, int mouseX, int mouseY) { return false; }
        int getHeight();
    }

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

    // ────────────────────────────────────────────────────────────────
    // RENDER
    // ────────────────────────────────────────────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Затемнение фона
        this.renderBackground(ctx, mouseX, mouseY, delta);

        int wx = winX;
        int wy = winY;

        // Тень окна
        fillRect(ctx, wx - 4, wy - 4, WIN_W + 8, WIN_H + 8, 0x55000000);

        // Основное окно
        fillRect(ctx, wx, wy, WIN_W, WIN_H, COL_BG);

        // Заголовок
        fillRect(ctx, wx, wy, WIN_W, TAB_H, COL_HEADER);
        drawString(ctx, "§bFT Client §7v1.2", wx + PAD, wy + TAB_H / 2 - 4, COL_TEXT);

        // Вкладки
        int tabW = WIN_W / 2;
        for (int i = 0; i < 2; i++) {
            String label = i == 0 ? "TriggerBot" : "AimAssist";
            boolean active = activeTab == i;
            boolean enabled = i == 0 ? triggerBot.enabled : aimAssist.enabled;
            int tx = wx + i * tabW;
            int ty = wy + TAB_H;
            boolean hover = mouseX >= tx && mouseX < tx + tabW && mouseY >= ty && mouseY < ty + 22;

            int bgCol = active ? COL_ACCENT : (hover ? COL_HOVER : COL_HEADER);
            fillRect(ctx, tx, ty, tabW, 22, bgCol);

            // Индикатор включён
            int indicatorCol = enabled ? COL_ENABLED : COL_DISABLED;
            fillRect(ctx, tx + 8, ty + 8, 6, 6, indicatorCol);

            drawString(ctx, label, tx + 20, ty + 7, active ? 0xFFFFFFFF : COL_SUBTEXT);

            // Нижняя линия активной вкладки
            if (active) {
                fillRect(ctx, tx, ty + 20, tabW, 2, COL_ACCENT);
            }
        }

        int contentTop = wy + TAB_H + 22 + 2;
        int contentH   = WIN_H - TAB_H - 22 - 2;

        // Клип контента
        ctx.enableScissor(wx, contentTop, wx + WIN_W, contentTop + contentH);

        int y = contentTop - (int) scrollY;
        int rowW = WIN_W - PAD * 2;
        int totalH = 0;

        // Рендер строк текущей вкладки
        if (activeTab == 0) {
            totalH = renderTBTab(ctx, wx + PAD, y, rowW, mouseX, mouseY);
        } else {
            totalH = renderAATab(ctx, wx + PAD, y, rowW, mouseX, mouseY);
        }
        contentHeight = totalH;

        ctx.disableScissor();

        // Скроллбар
        if (contentHeight > contentH) {
            float ratio    = (float) contentH / contentHeight;
            int sbH        = Math.max(20, (int) (contentH * ratio));
            int sbX        = wx + WIN_W - 4;
            int sbY        = contentTop + (int) (scrollY / contentHeight * contentH);
            fillRect(ctx, sbX, contentTop, 3, contentH, 0x22FFFFFF);
            fillRect(ctx, sbX, sbY, 3, sbH, COL_ACCENT);
        }

        // Линия-разделитель после вкладок
        fillRect(ctx, wx, contentTop, WIN_W, 1, COL_DIVIDER);

        super.render(ctx, mouseX, mouseY, delta);
    }
// ────────────────────────────────────────────────────────────────
    // TriggerBot tab
    // ────────────────────────────────────────────────────────────────
    private int renderTBTab(DrawContext ctx, int x, int y, int w, int mx, int my) {
        int startY = y;

        // Включить/выключить
        y = renderToggleRow(ctx, x, y, w, mx, my, "TriggerBot", triggerBot.enabled, 100);

        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, w, "НАСТРОЙКИ");

        // Режим крита
        y = renderCycleRow(ctx, x, y, w, mx, my, "Режим",
            new String[]{"smart", "only_crits"},
            new String[]{"Смарт", "Только криты"},
            triggerBot.critMode, 101);

        // Цели
        y = renderToggleRow(ctx, x, y, w, mx, my, "Только игроки", triggerBot.onlyPlayers, 102);

        // Только с оружием
        y = renderToggleRow(ctx, x, y, w, mx, my, "Только с оружием", triggerBot.weaponOnly, 103);

        // Пауза при использовании
        y = renderToggleRow(ctx, x, y, w, mx, my, "Пауза при использ.", triggerBot.pauseOnUse, 104);

        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, w, "КУЛДАУН");

        // Свой кулдаун
        y = renderToggleRow(ctx, x, y, w, mx, my, "Свой кулдаун %", triggerBot.customCooldown, 105);

        if (triggerBot.customCooldown) {
            y = renderSliderRow(ctx, x, y, w, mx, my,
                "Кулдаун", triggerBot.cooldownPct, 0f, 100f, 0, 200);
        }

        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, w, "ЗАДЕРЖКА");

        y = renderSliderRow(ctx, x, y, w, mx, my,
            "Мин. задержка (мс)", triggerBot.minDelay, 0f, 300f, 0, 201);
        y = renderSliderRow(ctx, x, y, w, mx, my,
            "Макс. задержка (мс)", triggerBot.maxDelay, 0f, 300f, 0, 202);

        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, w, "ДИСТАНЦИЯ");

        y = renderSliderRow(ctx, x, y, w, mx, my,
            "Дальность", (float) triggerBot.range, 1f, 6f, 1, 203);

        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, w, "БИНДИНГ");

        y = renderKeybindRow(ctx, x, y, w, mx, my, "Кнопка TB",
            keyName(triggerBot.keyCode), 0);

        return y - startY + 8;
    }

    // ────────────────────────────────────────────────────────────────
    // AimAssist tab
    // ────────────────────────────────────────────────────────────────
    private int renderAATab(DrawContext ctx, int x, int y, int w, int mx, int my) {
        int startY = y;

        y = renderToggleRow(ctx, x, y, w, mx, my, "AimAssist", aimAssist.enabled, 110);

        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, w, "НАСТРОЙКИ");

        y = renderToggleRow(ctx, x, y, w, mx, my, "Только игроки", aimAssist.onlyPlayers, 111);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Только с оружием", aimAssist.weaponOnly, 112);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Проверка стен",   aimAssist.wallCheck, 113);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Только по X",     aimAssist.onlyOnX, 114);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Видимые только",  aimAssist.visibleOnly, 115);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Не при еде",      aimAssist.notWhileEating, 116);
        y = renderToggleRow(ctx, x, y, w, mx, my, "Не в инвентаре",  aimAssist.notInInventory, 117);

        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, w, "ПАРАМЕТРЫ");

        y = renderSliderRow(ctx, x, y, w, mx, my,
            "Дальность", (float) aimAssist.range, 1f, 6f, 1, 210);
        y = renderSliderRow(ctx, x, y, w, mx, my,
            "FOV", (float) aimAssist.fov, 10f, 360f, 0, 211);
        y = renderSliderRow(ctx, x, y, w, mx, my,
            "Скорость", (float) aimAssist.speed, 0.5f, 10f, 1, 212);
        y = renderSliderRow(ctx, x, y, w, mx, my,
            "Удержание после удара", (float) aimAssist.holdAfterHit, 0f, 5f, 1, 213);

        y = renderDivider(ctx, x, y, w);
        y = renderSectionLabel(ctx, x, y, w, "БИНДИНГ");

        y = renderKeybindRow(ctx, x, y, w, mx, my, "Кнопка AA",
            keyName(aimAssist.keyCode), 1);

        return y - startY + 8;
    }

    // ────────────────────────────────────────────────────────────────
    // Рендер компонентов
    // ────────────────────────────────────────────────────────────────

    private int renderToggleRow(DrawContext ctx, int x, int y, int w, int mx, int my,
                                String label, boolean value, int id) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        if (hover) fillRect(ctx, x - 1, y, w + 2, ROW_H, COL_HOVER);

        // Переключатель справа
        int toggleW = 32, toggleH = 14;
        int tx = x + w - toggleW - 2;
        int ty = y + (ROW_H - toggleH) / 2;

        fillRoundRect(ctx, tx, ty, toggleW, toggleH, value ? COL_ENABLED : COL_DISABLED);
        int circleX = value ? tx + toggleW - toggleH + 1 : tx + 1;
        int circleY = ty + 1;
        fillRoundRect(ctx, circleX, circleY, toggleH - 2, toggleH - 2, 0xFFFFFFFF);

        drawString(ctx, label, x + 6, y + (ROW_H - 8) / 2, value ? COL_TEXT : COL_SUBTEXT);

        return y + ROW_H;
    }

    private int renderCycleRow(DrawContext ctx, int x, int y, int w, int mx, int my,
                               String label, String[] vals, String[] display,
                               String current, int id) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        if (hover) fillRect(ctx, x - 1, y, w + 2, ROW_H, COL_HOVER);

        // Найти текущий индекс
        String disp = current;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i].equals(current)) { disp = display[i]; break; }
        }

        int btnW = 90;
        int btnX = x + w - btnW - 2;
        int btnY = y + (ROW_H - 16) / 2;
        fillRoundRect(ctx, btnX, btnY, btnW, 16, COL_ACCENT2);
        int textX = btnX + (btnW - textWidth(disp)) / 2;
        drawString(ctx, disp, textX, btnY + 4, 0xFFFFFFFF);

        drawString(ctx, label, x + 6, y + (ROW_H - 8) / 2, COL_TEXT);
        return y + ROW_H;
    }

    private int renderSliderRow(DrawContext ctx, int x, int y, int w, int mx, int my,
                                String label, float value, float min, float max,
                                int decimals, int id) {
        int h = ROW_H + 10;
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        if (hover) fillRect(ctx, x - 1, y, w + 2, h, COL_HOVER);

        // Значение
        String valStr = decimals == 0
            ? String.valueOf((int) value)
            : String.format("%." + decimals + "f", value);
        int valW = textWidth(valStr);

        drawString(ctx, label, x + 6, y + 5, COL_SUBTEXT);
        drawString(ctx, valStr, x + w - valW - 4, y + 5, COL_ACCENT);

        // Трек
        int sliderX = x + 6;
        int sliderW = w - 12;
        int sliderY = y + 19;
        int sliderH = 4;

        fillRoundRect(ctx, sliderX, sliderY, sliderW, sliderH, COL_SLIDER_BG);

        float pct = (value - min) / (max - min);
        int filled = (int) (sliderW * pct);
        fillRoundRect(ctx, sliderX, sliderY, filled, sliderH, COL_ACCENT);

        // Ручка
        int handleX = sliderX + filled - 4;
        int handleY = sliderY - 3;
        fillRoundRect(ctx, handleX, handleY, 8, 10, 0xFFFFFFFF);

        return y + h;
    }
private int renderKeybindRow(DrawContext ctx, int x, int y, int w, int mx, int my,
                                 String label, String keyName, int bindIndex) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
        if (hover) fillRect(ctx, x - 1, y, w + 2, ROW_H, COL_HOVER);

        boolean capturing = capturingKey == bindIndex;
        String display = capturing ? "§e[нажми клавишу]" : "§7" + keyName;

        int btnW = 100;
        int btnX = x + w - btnW - 2;
        int btnY = y + (ROW_H - 16) / 2;
        fillRoundRect(ctx, btnX, btnY, btnW, 16, capturing ? COL_ACCENT : COL_SLIDER_BG);
        int textX = btnX + (btnW - textWidth(capturing ? "[нажми клавишу]" : keyName)) / 2;
        drawString(ctx, display, textX, btnY + 4, 0xFFFFFFFF);

        drawString(ctx, label, x + 6, y + (ROW_H - 8) / 2, COL_TEXT);
        return y + ROW_H;
    }

    private int renderDivider(DrawContext ctx, int x, int y, int w) {
        fillRect(ctx, x, y + 5, w, 1, COL_DIVIDER);
        return y + 11;
    }

    private int renderSectionLabel(DrawContext ctx, int x, int y, int w, String label) {
        drawString(ctx, "§8" + label, x + 6, y + 2, COL_SUBTEXT);
        return y + 14;
    }

    // ────────────────────────────────────────────────────────────────
    // MOUSE / KEY
    // ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int imx = (int) mouseX;
        int imy = (int) mouseY;

        // Закрыть захват бинда по любому клику вне кнопки
        if (capturingKey >= 0) {
            capturingKey = -1;
            return true;
        }

        // Drag header
        if (imx >= winX && imx < winX + WIN_W && imy >= winY && imy < winY + TAB_H) {
            dragging = true;
            dragOffX = imx - winX;
            dragOffY = imy - winY;
            return true;
        }

        // Tab switching
        int tabW = WIN_W / 2;
        int tabY = winY + TAB_H;
        for (int i = 0; i < 2; i++) {
            int tx = winX + i * tabW;
            if (imx >= tx && imx < tx + tabW && imy >= tabY && imy < tabY + 22) {
                activeTab = i;
                scrollY = 0;
                return true;
            }
        }

        // Content clicks
        int contentTop = winY + TAB_H + 22 + 2;
        int contentH   = WIN_H - TAB_H - 22 - 2;
        if (imx >= winX && imx < winX + WIN_W && imy >= contentTop && imy < contentTop + contentH) {
            handleContentClick(imx, imy, button, contentTop);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleContentClick(int imx, int imy, int button, int contentTop) {
        int x  = winX + PAD;
        int w  = WIN_W - PAD * 2;
        int y  = contentTop - (int) scrollY;

        if (activeTab == 0) {
            y = clickTBTab(x, y, w, imx, imy, button);
        } else {
            y = clickAATab(x, y, w, imx, imy, button);
        }
    }

    private int clickTBTab(int x, int y, int w, int imx, int imy, int btn) {
        // toggle TriggerBot
        if (hitRow(x, y, w, imx, imy)) { triggerBot.toggle(); return y; }
        y += ROW_H; y += 11; y += 14; // divider + section

        // critMode
        if (hitRow(x, y, w, imx, imy)) {
            triggerBot.critMode = triggerBot.critMode.equals("smart") ? "only_crits" : "smart";
        }
        y += ROW_H;

        // onlyPlayers
        if (hitRow(x, y, w, imx, imy)) triggerBot.onlyPlayers = !triggerBot.onlyPlayers;
        y += ROW_H;

        // weaponOnly
        if (hitRow(x, y, w, imx, imy)) triggerBot.weaponOnly = !triggerBot.weaponOnly;
        y += ROW_H;

        // pauseOnUse
        if (hitRow(x, y, w, imx, imy)) triggerBot.pauseOnUse = !triggerBot.pauseOnUse;
        y += ROW_H;

        y += 11; y += 14; // divider + section

        // customCooldown
        if (hitRow(x, y, w, imx, imy)) triggerBot.customCooldown = !triggerBot.customCooldown;
        y += ROW_H;

        if (triggerBot.customCooldown) {
            if (hitSlider(x, y, w, imx, imy)) {
                triggerBot.cooldownPct = (int) sliderValue(x, y, w, imx, 0f, 100f);
            }
            y += ROW_H + 10;
        }

        y += 11; y += 14;

        // minDelay
        if (hitSlider(x, y, w, imx, imy)) triggerBot.minDelay = (int) sliderValue(x, y, w, imx, 0f, 300f);
        y += ROW_H + 10;

        // maxDelay
        if (hitSlider(x, y, w, imx, imy)) triggerBot.maxDelay = (int) sliderValue(x, y, w, imx, 0f, 300f);
        y += ROW_H + 10;

        y += 11; y += 14;

        // range
        if (hitSlider(x, y, w, imx, imy)) triggerBot.range = sliderValue(x, y, w, imx, 1f, 6f);
        y += ROW_H + 10;

        y += 11; y += 14;

        // keybind
        if (hitRow(x, y, w, imx, imy)) capturingKey = 0;
        y += ROW_H;

        return y;
    }

    private int clickAATab(int x, int y, int w, int imx, int imy, int btn) {
        if (hitRow(x, y, w, imx, imy)) { aimAssist.toggle(); return y; }
        y += ROW_H; y += 11; y += 14;

        if (hitRow(x, y, w, imx, imy)) aimAssist.onlyPlayers  = !aimAssist.onlyPlayers;  y += ROW_H;
        if (hitRow(x, y, w, imx, imy)) aimAssist.weaponOnly   = !aimAssist.weaponOnly;   y += ROW_H;
        if (hitRow(x, y, w, imx, imy)) aimAssist.wallCheck    = !aimAssist.wallCheck;    y += ROW_H;
        if (hitRow(x, y, w, imx, imy)) aimAssist.onlyOnX      = !aimAssist.onlyOnX;      y += ROW_H;
        if (hitRow(x, y, w, imx, imy)) aimAssist.visibleOnly  = !aimAssist.visibleOnly;  y += ROW_H;
        if (hitRow(x, y, w, imx, imy)) aimAssist.notWhileEating = !aimAssist.notWhileEating; y += ROW_H;
        if (hitRow(x, y, w, imx, imy)) aimAssist.notInInventory = !aimAssist.notInInventory; y += ROW_H;

        y += 11; y += 14;

        if (hitSlider(x, y, w, imx, imy)) aimAssist.range = sliderValue(x, y, w, imx, 1f, 6f);
        y += ROW_H + 10;

        if (hitSlider(x, y, w, imx, imy)) aimAssist.fov = sliderValue(x, y, w, imx, 10f, 360f);
        y += ROW_H + 10;

        if (hitSlider(x, y, w, imx, imy)) aimAssist.speed = sliderValue(x, y, w, imx, 0.5f, 10f);
        y += ROW_H + 10;

        if (hitSlider(x, y, w, imx, imy)) aimAssist.holdAfterHit = sliderValue(x, y, w, imx, 0f, 5f);
        y += ROW_H + 10;

        y += 11; y += 14;

        if (hitRow(x, y, w, imx, imy)) capturingKey = 1;
        y += ROW_H;

        return y;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging) {
            winX = (int) mouseX - dragOffX;
            winY = (int) mouseY - dragOffY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        int contentH = WIN_H - TAB_H - 22 - 2;
        int maxScroll = Math.max(0, contentHeight - contentH);
        scrollY = (float) Math.max(0, Math.min(scrollY - vAmt * SCROLL_SPEED, maxScroll));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturingKey == 0) {
            triggerBot.keyCode = keyCode;
            capturingKey = -1;
            return true;
        }
        if (capturingKey == 1) {
            aimAssist.keyCode = keyCode;
            capturingKey = -1;
            return true;
        }
        if (keyCode == 256) { // ESC
            MinecraftClient.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
// ────────────────────────────────────────────────────────────────
    // Утилиты
    // ────────────────────────────────────────────────────────────────

    private boolean hitRow(int x, int y, int w, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + ROW_H;
    }

    private boolean hitSlider(int x, int y, int w, int mx, int my) {
        int h = ROW_H + 10;
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private float sliderValue(int x, int y, int w, int mx, float min, float max) {
        int sliderX = x + 6;
        int sliderW = w - 12;
        float pct = Math.max(0f, Math.min(1f, (float)(mx - sliderX) / sliderW));
        return min + (max - min) * pct;
    }

    private void fillRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    private void fillRoundRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        // Простой прямоугольник — скруглить нельзя без матрикса, просто заливка
        ctx.fill(x, y, x + w, y + h, color);
        // Лёгкие "срезы" углов на 1px
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
            String name = org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0);
            if (name != null) return name.toUpperCase();
        } catch (Exception ignored) {}
        return "KEY_" + keyCode;
    }
}
