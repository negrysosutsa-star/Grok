package ft.triggerbot.util;

public final class TickCounter {
    private static int value = 0;
    private TickCounter() {}
    public static void update() { if (value > 0) value--; }
    public static boolean busy() { return value > 0; }
    public static void set(int n) { value = Math.max(value, n); }
}