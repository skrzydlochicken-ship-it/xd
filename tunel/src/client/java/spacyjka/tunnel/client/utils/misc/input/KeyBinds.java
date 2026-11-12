/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.utils.misc.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import spacyjka.tunnel.client.TunelClient;

public class KeyBinds {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(TunelClient.identifier("tunel"));

    public static final KeyMapping OPEN_GUI = new KeyMapping("key.tunel.open-gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY);

    private KeyBinds() {
    }

    public static void register() {
        KeyBindingHelper.registerKeyBinding(OPEN_GUI);
    }
}
