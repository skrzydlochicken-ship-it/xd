/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Map;
import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("ALL")
    static Map<String, KeyMapping> getKeysById() { return null; }

    @Accessor("key")
    InputConstants.Key tunel$getKey();

    @Accessor("clickCount")
    int tunel$getTimesPressed();

    @Accessor("clickCount")
    void tunel$setTimesPressed(int timesPressed);

    @Invoker("release")
    void tunel$invokeReset();
}
