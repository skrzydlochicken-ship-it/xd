package spacyjka.tunnel.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import spacyjka.tunnel.client.TunelClient;
import spacyjka.tunnel.client.events.world.BlockUpdateEvent;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "setServerVerifiedBlockState", at = @At("TAIL"))
    private void onServerBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo info) {
        TunelClient.EVENT_BUS.post(BlockUpdateEvent.get(pos));
    }
}
