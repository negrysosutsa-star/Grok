package ft.triggerbot.aim;

/**
 * Базовый класс режима прицеливания.
 */
public abstract class AimMode {
    public final String name;

    protected AimMode(String name) { this.name = name; }

    /** Вызывается когда взяли новую цель */
    public abstract void onNewTarget();

    /** Вызывается когда потеряли цель */
    public abstract void onReset();

    /** Вызывается каждый тик пока цель активна */
    public void tick(long nowMs) {}

    /**
     * Вычислить сдвиг углов за этот тик.
     * @param dYaw        угловая дельта до цели по yaw
     * @param dPitch      угловая дельта до цели по pitch
     * @param totalAngle  суммарный угол (градусы)
     * @param frameFactor frame-factor из RotationUtil
     * @param nowMs       текущий System.currentTimeMillis()
     * @param targetStartMs когда взяли цель
     * @return float[]{moveYaw, movePitch}
     */
    public abstract float[] compute(double dYaw, double dPitch,
                                    double totalAngle, double frameFactor,
                                    long nowMs, long targetStartMs);
}
