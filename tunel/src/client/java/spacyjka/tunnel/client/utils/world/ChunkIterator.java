/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package spacyjka.tunnel.client.utils.world;

import spacyjka.tunnel.client.mixin.ClientChunkCacheAccessor;
import spacyjka.tunnel.client.mixin.ClientChunkCacheStorageAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import java.util.Iterator;

import static spacyjka.tunnel.client.TunelClient.mc;

public class ChunkIterator implements Iterator<ChunkAccess> {
    private final ClientChunkCacheStorageAccessor map = (ClientChunkCacheStorageAccessor) (Object) ((ClientChunkCacheAccessor) mc.level.getChunkSource()).tunel$getChunks();
    private final boolean onlyWithLoadedNeighbours;

    private int i = 0;
    private ChunkAccess chunk;

    public ChunkIterator(boolean onlyWithLoadedNeighbours) {
        this.onlyWithLoadedNeighbours = onlyWithLoadedNeighbours;

        getNext();
    }

    private ChunkAccess getNext() {
        ChunkAccess prev = chunk;
        chunk = null;

        while (i < map.tunel$getChunks().length()) {
            chunk = map.tunel$getChunks().get(i++);
            if (chunk != null && (!onlyWithLoadedNeighbours || isInRadius(chunk))) break;
        }

        return prev;
    }

    private boolean isInRadius(ChunkAccess chunk) {
        int x = chunk.getPos().x;
        int z = chunk.getPos().z;

        return mc.level.getChunkSource().hasChunk(x + 1, z) && mc.level.getChunkSource().hasChunk(x - 1, z) && mc.level.getChunkSource().hasChunk(x, z + 1) && mc.level.getChunkSource().hasChunk(x, z - 1);
    }

    @Override
    public boolean hasNext() {
        return chunk != null;
    }

    @Override
    public ChunkAccess next() {
        return getNext();
    }
}
