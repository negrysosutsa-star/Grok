package ft.triggerbot.mixin;

import ft.triggerbot.ClientMain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
// Scene (class_9779) не экспортирован — используем Object
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    // Точная сигнатура 1.21.4 из intermediary лога:
    // render(RenderTickCounter, Scene, boolean, Camera, GameRenderer, Matrix4f, Matrix4f)
    // class_9922 = RenderTickCounter
    // class_9779 = Scene (net.minecraft.client.render.Scene)
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(RenderTickCounter tickCounter,
                          Object scene,
                          boolean bl,
                          Camera camera,
                          GameRenderer gameRenderer,
                          Matrix4f viewMatrix,
                          Matrix4f projMatrix,
                          CallbackInfo ci) {
        if (ClientMain.INSTANCE == null) return;
        var esp = ClientMain.INSTANCE.esp;
        if (!esp.enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Vec3d cam = camera.getPos();
        float tickDelta = tickCounter.getTickDelta(true);

        var immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        MatrixStack ms = new MatrixStack();
        ms.multiplyPositionMatrix(viewMatrix);

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        esp.onRender(ms, immediate, tickDelta, cam);

        immediate.draw(net.minecraft.client.render.RenderLayer.LINES);
    }
}
