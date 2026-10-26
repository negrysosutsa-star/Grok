package ft.triggerbot.mixin;

import ft.triggerbot.ClientMain;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(
        method = "render",
        at = @At("TAIL")
    )
    private void onRender(MatrixStack matrices, float tickDelta,
                          long limitTime, boolean renderBlockOutline,
                          Camera camera, GameRenderer gameRenderer,
                          LightmapTextureManager lightmapTextureManager,
                          Matrix4f projectionMatrix, CallbackInfo ci) {
        if (ClientMain.INSTANCE == null) return;
        var esp = ClientMain.INSTANCE.esp;
        if (!esp.enabled) return;

        Vec3d cam = camera.getPos();

        // Получаем immediate из WorldRenderer через accessor или создаём временный
        // В 1.21.4 render принимает VertexConsumerProvider.Immediate через поле
        net.minecraft.client.render.VertexConsumerProvider.Immediate immediate =
            net.minecraft.client.MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        // Включаем depth test чтобы ESP не светился сквозь стены (можно отключить)
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        esp.onRender(matrices, immediate, tickDelta, cam);

        immediate.draw(net.minecraft.client.render.RenderLayer.LINES);
    }
}
