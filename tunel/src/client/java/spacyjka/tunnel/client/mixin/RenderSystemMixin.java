/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import spacyjka.tunnel.client.renderer.MeshUniforms;
import spacyjka.tunnel.client.systems.modules.Modules;
import spacyjka.tunnel.client.systems.modules.misc.InventoryTweaks;
import spacyjka.tunnel.client.utils.render.postprocess.ChamsShader;
import spacyjka.tunnel.client.utils.render.postprocess.OutlineUniforms;
import spacyjka.tunnel.client.utils.render.postprocess.PostProcessShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static spacyjka.tunnel.client.TunelClient.mc;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
    @Inject(method = "flipFrame", at = @At("TAIL"))
    private static void meteor$flipFrame(CallbackInfo info) {
        MeshUniforms.flipFrame();
        PostProcessShader.flipFrame();
        ChamsShader.flipFrame();
        OutlineUniforms.flipFrame();

        if (Modules.get() == null || mc.player == null) return;
        if (Modules.get().get(InventoryTweaks.class).frameInput()) ((MinecraftClientAccessor) mc).meteor$handleInputEvents();
    }
}
