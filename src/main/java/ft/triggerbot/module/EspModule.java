package ft.triggerbot.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class EspModule extends Module {

    // ─── Настройки ────────────────────────────────────────────────
    /** Только игроков или всех мобов */
    public boolean onlyPlayers  = true;
    /** Показывать линию здоровья */
    public boolean healthBar    = true;
    /** Показывать имя */
    public boolean showName     = true;
    /** Показывать дистанцию */
    public boolean showDist     = true;
    /** Цвет боксов: 0=team, 1=health, 2=fixed */
    public int     colorMode    = 1;
    /** Фиксированный цвет (colorMode=2): ARGB компоненты */
    public float   fixedR = 0.35f, fixedG = 0.75f, fixedB = 1.0f;

    public int keyCode = 0;

    // ─── Состояние — заполняется из mixin ─────────────────────────
    // Рендер вызывается из WorldRendererMixin
    private MatrixStack           pendingStack;
    private VertexConsumerProvider pendingVcp;
    private float                  pendingTick;
    private Vec3d                  pendingCam;

    public EspModule() { super("ESP", 2); }

    // Вызывается из WorldRendererMixin после рендера сущностей
    public void onRender(MatrixStack ms, VertexConsumerProvider vcp,
                         float tickDelta, Vec3d cam) {
        if (!enabled) return;
        var mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity living)) continue;
            if (e == mc.player) continue;
            if (onlyPlayers && !(e instanceof PlayerEntity)) continue;
            if (!living.isAlive()) continue;

            drawBox(ms, vcp, living, tickDelta, cam);
        }
    }

    private void drawBox(MatrixStack ms, VertexConsumerProvider vcp,
                         LivingEntity e, float tick, Vec3d cam) {
        double lerpX = MathHelper.lerp(tick, e.prevX, e.getX()) - cam.x;
        double lerpY = MathHelper.lerp(tick, e.prevY, e.getY()) - cam.y;
        double lerpZ = MathHelper.lerp(tick, e.prevZ, e.getZ()) - cam.z;

        Box box = e.getBoundingBox().offset(-e.getX(), -e.getY(), -e.getZ());

        ms.push();
        ms.translate(lerpX, lerpY, lerpZ);

        // Цвет
        float r, g, b;
        if (colorMode == 1) {
            // По здоровью: зелёный → красный
            float hp = e.getHealth() / e.getMaxHealth();
            r = 1.0f - hp;
            g = hp;
            b = 0.1f;
        } else if (colorMode == 0 && e instanceof PlayerEntity) {
            // Team — просто голубой для чужих
            r = 0.2f; g = 0.6f; b = 1.0f;
        } else {
            r = fixedR; g = fixedG; b = fixedB;
        }

        VertexConsumer vc = vcp.getBuffer(RenderLayer.LINES);
        Matrix4f m = ms.peek().getPositionMatrix();
        drawWireBox(vc, m, box, r, g, b, 1.0f);

        ms.pop();
    }

    /** Рисует wireframe box (12 рёбер) */
    private static void drawWireBox(VertexConsumer vc, Matrix4f m,
                                    Box b, float r, float g, float bl, float a) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;

        // Нижние 4 ребра
        line(vc, m, x0,y0,z0, x1,y0,z0, r,g,bl,a);
        line(vc, m, x1,y0,z0, x1,y0,z1, r,g,bl,a);
        line(vc, m, x1,y0,z1, x0,y0,z1, r,g,bl,a);
        line(vc, m, x0,y0,z1, x0,y0,z0, r,g,bl,a);
        // Верхние 4 ребра
        line(vc, m, x0,y1,z0, x1,y1,z0, r,g,bl,a);
        line(vc, m, x1,y1,z0, x1,y1,z1, r,g,bl,a);
        line(vc, m, x1,y1,z1, x0,y1,z1, r,g,bl,a);
        line(vc, m, x0,y1,z1, x0,y1,z0, r,g,bl,a);
        // Вертикальные 4 ребра
        line(vc, m, x0,y0,z0, x0,y1,z0, r,g,bl,a);
        line(vc, m, x1,y0,z0, x1,y1,z0, r,g,bl,a);
        line(vc, m, x1,y0,z1, x1,y1,z1, r,g,bl,a);
        line(vc, m, x0,y0,z1, x0,y1,z1, r,g,bl,a);
    }

    private static void line(VertexConsumer vc, Matrix4f m,
                              float x0, float y0, float z0,
                              float x1, float y1, float z1,
                              float r, float g, float b, float a) {
        float nx = x1 - x0, ny = y1 - y0, nz = z1 - z0;
        float len = (float) Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len == 0) return;
        nx /= len; ny /= len; nz /= len;
        vc.vertex(m, x0, y0, z0).color(r, g, b, a).normal(nx, ny, nz);
        vc.vertex(m, x1, y1, z1).color(r, g, b, a).normal(nx, ny, nz);
    }
}
