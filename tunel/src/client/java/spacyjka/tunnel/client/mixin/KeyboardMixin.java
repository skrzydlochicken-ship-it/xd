/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.mixin;

import spacyjka.tunnel.client.TunelClient;
import spacyjka.tunnel.client.events.tunel.CharTypedEvent;
import spacyjka.tunnel.client.events.tunel.KeyEvent;
import spacyjka.tunnel.client.gui.GuiKeyEvents;
import spacyjka.tunnel.client.gui.WidgetScreen;
import spacyjka.tunnel.client.utils.Utils;
import spacyjka.tunnel.client.utils.misc.input.Input;
import spacyjka.tunnel.client.utils.misc.input.KeyAction;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void onKey(long window, int action, net.minecraft.client.input.KeyEvent input, CallbackInfo ci) {
        int modifiers = input.modifiers();
        if (input.key() != GLFW.GLFW_KEY_UNKNOWN) {
            // on Linux/X11 the modifier is not active when the key is pressed and still active when the key is released
            // https://github.com/glfw/glfw/issues/1630
            if (action == GLFW.GLFW_PRESS) {
                modifiers |= Input.getModifier(input.key());
            } else if (action == GLFW.GLFW_RELEASE) {
                modifiers &= ~Input.getModifier(input.key());
            }

            if (minecraft.screen instanceof WidgetScreen && action == GLFW.GLFW_REPEAT) {
                ((WidgetScreen) minecraft.screen).keyRepeated(new net.minecraft.client.input.KeyEvent(input.key(), input.scancode(), modifiers));
            }

            if (GuiKeyEvents.canUseKeys) {
                Input.setKeyState(input.key(), action != GLFW.GLFW_RELEASE);
                if (TunelClient.EVENT_BUS.post(KeyEvent.get(new net.minecraft.client.input.KeyEvent(input.key(), input.scancode(), modifiers), KeyAction.get(action))).isCancelled()) ci.cancel();
            }
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onChar(long window, CharacterEvent input, CallbackInfo ci) {
        if (Utils.canUpdate() && !minecraft.isPaused() && (minecraft.screen == null || minecraft.screen instanceof WidgetScreen)) {
            if (TunelClient.EVENT_BUS.post(CharTypedEvent.get((char) input.codepoint())).isCancelled()) ci.cancel();
        }
    }
}
