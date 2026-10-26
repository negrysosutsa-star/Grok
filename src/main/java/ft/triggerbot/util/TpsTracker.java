package ft.triggerbot.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.network.packet.Packet;

public final class TpsTracker {
    private static final TpsTracker INSTANCE = new TpsTracker();
    private final ArrayDeque<Float> samples = new ArrayDeque<>(20);
    private long lastTime = 0L;
    private float tps = 20.0f;

    private TpsTracker() {}

    public static TpsTracker getInstance() { return INSTANCE; }

    /** Вызывать из mixin на получение WorldTimeUpdateS2CPacket */
    public void onPacket(Packet<?> packet) {
        if (!(packet instanceof WorldTimeUpdateS2CPacket)) return;
        long now = System.currentTimeMillis();
        if (lastTime != 0L) {
            long delta = Math.clamp(now - lastTime, 250L, 10000L);
            if (samples.size() >= 20) samples.poll();
            samples.add(20.0f * (1000.0f / (float) delta));

            float sum = 0f;
            for (float s : samples) sum += RandomUtil.getFloatByFloatFloatFloat(0f, 20f, s);
            if (!samples.isEmpty()) tps = sum / samples.size();
        }
        lastTime = now;
    }

    /** Текущий TPS сервера, округлённый до 2 знаков */
    public float getFloat() {
        float raw = RandomUtil.getFloatByFloatFloatFloat(0f, 20f, tps);
        return new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP).floatValue();
    }

    /** true если сервер лагает (TPS < 19) */
    public boolean isLagging() { return tps < 19.0f; }
}
