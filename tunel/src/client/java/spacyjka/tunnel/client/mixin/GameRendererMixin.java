/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.util.profiling.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import spacyjka.tunnel.client.TunelClient;
import spacyjka.tunnel.client.events.render.Render3DEvent;
import spacyjka.tunnel.client.gui.WidgetScreen;
import spacyjka.tunnel.client.renderer.Renderer3D;
import spacyjka.tunnel.client.renderer.TunelRenderPipelines;
import spacyjka.tunnel.client.utils.Utils;
import spacyjka.tunnel.client.utils.render.RenderUtils;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private Camera mainCamera;

    @Shadow
    protected abstract void bobView(PoseStack matrices, float tickDelta);

    @Shadow
    protected abstract void bobHurt(PoseStack matrices, float tickDelta);

    @Shadow
    @Final
    private GuiRenderer guiRenderer;

    @Shadow
    @Final
    private FogRenderer fogRenderer;

    @Shadow
    @Final
    GuiRenderState guiRenderState;

    @Unique
    private Renderer3D renderer;

    @Unique
    private Renderer3D depthRenderer;

    @Unique
    private final PoseStack matrices = new PoseStack();

    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void onRenderWorld(DeltaTracker tickCounter, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection, @Local(ordinal = 1) Matrix4f position, @Local(ordinal = 0) float tickDelta, @Local PoseStack matrixStack) {
        if (!Utils.canUpdate()) return;

        Profiler.get().push(TunelClient.MOD_ID + "_render");

        // Create renderer and event

        if (renderer == null) renderer = new Renderer3D(TunelRenderPipelines.WORLD_COLORED_LINES, TunelRenderPipelines.WORLD_COLORED);
        if (depthRenderer == null) depthRenderer = new Renderer3D(TunelRenderPipelines.WORLD_COLORED_LINES_DEPTH, TunelRenderPipelines.WORLD_COLORED_DEPTH);
        Render3DEvent event = Render3DEvent.get(matrixStack, renderer, depthRenderer, tickDelta, mainCamera.position().x, mainCamera.position().y, mainCamera.position().z);

        // Update model view matrix

        RenderSystem.getModelViewStack().pushMatrix().mul(position);

        matrices.pushPose();
        bobHurt(matrices, mainCamera.getPartialTickTime());
        if (minecraft.options.bobView().get())
            bobView(matrices, mainCamera.getPartialTickTime());

        Matrix4f inverseBob = new Matrix4f(matrices.last().pose()).invert();
        RenderSystem.getModelViewStack().mul(inverseBob);
        matrices.popPose();

        RenderUtils.updateScreenCenter(projection, position);

        // Render

        renderer.begin();
        depthRenderer.begin();
        TunelClient.EVENT_BUS.post(event);
        renderer.render(matrixStack);
        depthRenderer.render(matrixStack);

        // Revert model view matrix

        RenderSystem.getModelViewStack().popMatrix();

        Profiler.get().pop();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.AFTER))
    private void onRenderGui(DeltaTracker tickCounter, boolean tick, CallbackInfo info) {
        if (minecraft.screen instanceof WidgetScreen widgetScreen) {
            guiRenderState.reset();
            var mouseX = (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
            var mouseY = (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

            var context = new GuiGraphics(minecraft, guiRenderState, mouseX, mouseY);

            widgetScreen.renderCustom(context, mouseX, mouseY, tickCounter.getGameTimeDeltaTicks());

            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(minecraft.getMainRenderTarget().getDepthTexture(), 1.0);

            guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE));
            guiRenderer.incrementFrameNumber();
        }
    }
}
