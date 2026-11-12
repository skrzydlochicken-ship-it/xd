/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.profiling.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import spacyjka.tunnel.client.TunelClient;
import spacyjka.tunnel.client.events.game.GameLeftEvent;
import spacyjka.tunnel.client.events.game.OpenScreenEvent;
import spacyjka.tunnel.client.events.game.ResourcePacksReloadedEvent;
import spacyjka.tunnel.client.events.world.TickEvent;
import spacyjka.tunnel.client.gui.WidgetScreen;
import spacyjka.tunnel.client.utils.Utils;

import java.util.concurrent.CompletableFuture;

@Mixin(value = Minecraft.class, priority = 1001)
public abstract class MinecraftMixin {
    @Unique private long lastTime;
    @Unique private boolean firstFrame;

    @Shadow public ClientLevel level;
    @Shadow @Final public MouseHandler mouseHandler;
    @Shadow @Final private Window window;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        TunelClient.INSTANCE.onInitializeClient();
        firstFrame = true;
    }

    @Inject(at = @At("HEAD"), method = "tick")
    private void onPreTick(CallbackInfo info) {
        Profiler.get().push(TunelClient.MOD_ID + "_pre_update");
        TunelClient.EVENT_BUS.post(TickEvent.Pre.get());
        Profiler.get().pop();
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void onTick(CallbackInfo info) {
        Profiler.get().push(TunelClient.MOD_ID + "_post_update");
        TunelClient.EVENT_BUS.post(TickEvent.Post.get());
        Profiler.get().pop();
    }

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At("HEAD"))
    private void onDisconnect(Screen screen, boolean transferring, boolean stopSound, CallbackInfo info) {
        if (level != null) {
            TunelClient.EVENT_BUS.post(GameLeftEvent.get());
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof WidgetScreen) screen.mouseMoved(mouseHandler.xpos() * window.getGuiScale(), mouseHandler.ypos() * window.getGuiScale());

        OpenScreenEvent event = OpenScreenEvent.get(screen);
        TunelClient.EVENT_BUS.post(event);

        if (event.isCancelled()) info.cancel();
    }

    @ModifyReturnValue(method = "reloadResourcePacks(ZLnet/minecraft/client/Minecraft$GameLoadCookie;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private CompletableFuture<Void> onReloadResourcesNewCompletableFuture(CompletableFuture<Void> original) {
        return original.thenRun(() -> TunelClient.EVENT_BUS.post(ResourcePacksReloadedEvent.get()));
    }

    // Time delta

    @Inject(method = "runTick", at = @At("HEAD"))
    private void onRender(CallbackInfo info) {
        long time = System.currentTimeMillis();

        if (firstFrame) {
            lastTime = time;
            firstFrame = false;
        }

        Utils.frameTime = (time - lastTime) / 1000.0;
        lastTime = time;
    }
}
