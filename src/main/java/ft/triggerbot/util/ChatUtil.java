package ft.triggerbot.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatUtil {
    private static final Text PREFIX = Text.literal("ft").formatted(Formatting.GRAY)
            .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY));
    private ChatUtil() {}
    public static void send(String msg) {
        var p = MinecraftClient.getInstance().player;
        if (p == null) return;
        MutableText full = PREFIX.copy().append(Text.literal(msg).formatted(Formatting.WHITE));
        p.sendMessage(full, false);
    }
}