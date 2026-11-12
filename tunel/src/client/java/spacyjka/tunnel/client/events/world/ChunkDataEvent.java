package spacyjka.tunnel.client.events.world;

public class ChunkDataEvent {
    private static final ChunkDataEvent INSTANCE = new ChunkDataEvent();

    public int x, z;

    public static ChunkDataEvent get(int x, int z) {
        INSTANCE.x = x;
        INSTANCE.z = z;
        return INSTANCE;
    }
}
