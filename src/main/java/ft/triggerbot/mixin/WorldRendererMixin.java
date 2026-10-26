package ft.triggerbot.mixin;

import ft.triggerbot.ClientMain;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    // В 1.21.4 сигнатура render:
    // render(Camera, boolean, GameRenderer, Matrix4f, Matrix4f, CallbackInfo)
    // class_9922 = Camera, class_9779 = GameRenderer
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(Camera camera,
                          boolean bl,
                          GameRenderer gameRenderer,
                          Matrix4f matrix4f,
                          Matrix4f matrix4f2,
                          CallbackInfo ci) {
        if (ClientMain.INSTANCE == null) return;
        var esp = ClientMain.INSTANCE.esp;
        if (!esp.enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        Vec3d cam = camera.getPos();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        var immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        MatrixStack ms = new MatrixStack();
        ms.multiplyPositionMatrix(matrix4f);

        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        esp.onRender(ms, immediate, tickDelta, cam);

        immediate.draw(net.minecraft.client.render.RenderLayer.LINES);
    }
}
