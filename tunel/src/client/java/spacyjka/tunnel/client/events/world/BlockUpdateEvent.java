package spacyjka.tunnel.client.events.world;

import net.minecraft.core.BlockPos;

public class BlockUpdateEvent {
    private static final BlockUpdateEvent INSTANCE = new BlockUpdateEvent();

    public BlockPos pos;

    public static BlockUpdateEvent get(BlockPos pos) {
        INSTANCE.pos = pos;
        return INSTANCE;
    }
}
