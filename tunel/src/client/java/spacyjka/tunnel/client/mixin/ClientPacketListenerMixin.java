/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import spacyjka.tunnel.client.TunelClient;
import spacyjka.tunnel.client.events.game.GameJoinedEvent;
import spacyjka.tunnel.client.events.game.GameLeftEvent;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void onGameJoinHead(ClientboundLoginPacket packet, CallbackInfo info, @Share("worldNotNull") LocalBooleanRef worldNotNull) {
        worldNotNull.set(level != null);
    }

    @Inject(method = "handleLogin", at = @At("TAIL"))
    private void onGameJoinTail(ClientboundLoginPacket packet, CallbackInfo info, @Share("worldNotNull") LocalBooleanRef worldNotNull) {
        if (worldNotNull.get()) {
            TunelClient.EVENT_BUS.post(GameLeftEvent.get());
        }

        TunelClient.EVENT_BUS.post(GameJoinedEvent.get());
    }

    // the server sends a GameJoin packet after the reconfiguration phase
    @Inject(method = "handleConfigurationStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER))
    private void onEnterReconfiguration(ClientboundStartConfigurationPacket packet, CallbackInfo info) {
        TunelClient.EVENT_BUS.post(GameLeftEvent.get());
    }
}
