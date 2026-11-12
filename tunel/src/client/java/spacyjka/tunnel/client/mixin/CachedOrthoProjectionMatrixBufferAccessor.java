/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.mixin;

import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CachedOrthoProjectionMatrixBuffer.class)
public interface CachedOrthoProjectionMatrixBufferAccessor {
    @Invoker("createProjectionMatrix")
    Matrix4f tunel$callGetMatrix(float width, float height);
}
