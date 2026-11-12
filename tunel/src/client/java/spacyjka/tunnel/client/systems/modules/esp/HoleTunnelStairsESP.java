package spacyjka.tunnel.client.systems.modules.esp;

import spacyjka.tunnel.client.systems.modules.Categories;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import spacyjka.tunnel.client.events.render.Render3DEvent;
import spacyjka.tunnel.client.events.world.TickEvent;
import spacyjka.tunnel.client.renderer.Renderer3D;
import spacyjka.tunnel.client.renderer.ShapeMode;
import spacyjka.tunnel.client.settings.*;
import spacyjka.tunnel.client.systems.modules.Module;
import spacyjka.tunnel.client.utils.Utils;
import spacyjka.tunnel.client.utils.network.TunelExecutor;
import spacyjka.tunnel.client.utils.render.color.SettingColor;
import spacyjka.tunnel.client.eventbus.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HoleTunnelStairsESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHParams = settings.createGroup("Hole Parameters");
    private final SettingGroup sgTParams = settings.createGroup("Tunnel Parameters");
    private final SettingGroup sgSParams = settings.createGroup("Stairs Parameters");
    private final SettingGroup sgRender = settings.createGroup("Rendering");
    private final Setting<DetectionMode> detectionMode = sgGeneral.add(new EnumSetting.Builder<DetectionMode>()
        .name("Detection Mode")
        .description("Choose what to detect: holes, tunnels, stairs, or all.")
        .defaultValue(DetectionMode.ALL)
        .build()
    );
    private final Setting<Integer> maxChunks = sgGeneral.add(new IntSetting.Builder()
        .name("Chunks to process/tick")
        .description("Amount of Chunks to process per tick")
        .defaultValue(10)
        .min(1)
        .sliderRange(1, 100)
        .build()
    );
    private final Setting<Boolean> airBlocks = sgGeneral.add(new BoolSetting.Builder()
        .name("Detect only Air blocks as passable.")
        .description("Only marks tunnels or holes if their blocks are air as oppose to if the blocks are passable.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> minY = sgGeneral.add(new IntSetting.Builder()
        .name("Detection Y Minimum OffSet")
        .description("Scans blocks above or at this this many blocks from minimum build limit.")
        .min(0)
        .sliderRange(0,319)
        .defaultValue(0)
        .build()
    );
    private final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder()
        .name("Detection Y Maximum OffSet")
        .description("Scans blocks below or at this this many blocks from maximum build limit.")
        .min(0)
        .sliderRange(0,319)
        .defaultValue(0)
        .build()
    );
    private final Setting<Integer> minHoleDepth = sgHParams.add(new IntSetting.Builder()
        .name("Min Hole Depth")
        .description("Minimum height of a hole in blocks; shorter holes are not shown at all (e.g. 40)")
        .defaultValue(4)
        .min(1)
        .sliderMax(128)
        .onChanged(v -> rescanAll())
        .build()
    );
    private final Setting<Integer> maxHoleWidth = sgHParams.add(new IntSetting.Builder()
        .name("Max Hole Width")
        .description("Widest cross-section (in blocks, per axis) that still counts as part of a hole. 1 = only 1x1 shafts.")
        .defaultValue(2)
        .min(1)
        .sliderMax(5)
        .onChanged(v -> rescanAll())
        .build()
    );
    private final Setting<Integer> holeGapTolerance = sgHParams.add(new IntSetting.Builder()
        .name("Hole Side Gap Tolerance")
        .description("How many levels in a row a hole may be wider than Max Hole Width (crossing tunnels, mined out rooms) and still count as one hole. 0 = strict.")
        .defaultValue(3)
        .min(0)
        .sliderMax(10)
        .onChanged(v -> rescanAll())
        .build()
    );
    private final Setting<Boolean> playerToBedrock = sgHParams.add(new BoolSetting.Builder()
        .name("From player to bedrock (close)")
        .description("Only show holes that start around your Y level and end around bedrock.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> playerToBedrockTolerance = sgHParams.add(new IntSetting.Builder()
        .name("Player/Bedrock Y Tolerance")
        .description("How many blocks the top of the hole may be from your Y level, and its bottom from the bottom of the world.")
        .defaultValue(8)
        .min(0)
        .sliderMax(32)
        .visible(playerToBedrock::get)
        .build()
    );
    private final Setting<Integer> minTunnelLength = sgTParams.add(new IntSetting.Builder()
        .name("Min Tunnel Length")
        .description("Minimum length for a tunnel to be detected")
        .defaultValue(3)
        .min(1)
        .sliderMax(20)
        .build()
    );
    private final Setting<Integer> minTunnelHeight = sgTParams.add(new IntSetting.Builder()
        .name("Min Tunnel Height")
        .description("Minimum height of the tunnels to be detected")
        .defaultValue(2)
        .min(1)
        .sliderMax(10)
        .build()
    );
    private final Setting<Integer> maxTunnelHeight = sgTParams.add(new IntSetting.Builder()
        .name("Max Tunnel Height")
        .description("Maximum height of the tunnels to be detected")
        .defaultValue(3)
        .min(2)
        .sliderMax(10)
        .build()
    );
    private final Setting<Boolean> diagonals = sgTParams.add(new BoolSetting.Builder()
        .name("Detect Diagonal Tunnels.")
        .description("Detects diagonal tunnels when tunnels are selected to be detected.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> minDiagonalLength = sgTParams.add(new IntSetting.Builder()
        .name("Min Diagonal Tunnel Length")
        .description("Minimum length for diagonal tunnels to be detected")
        .defaultValue(3)
        .min(1)
        .sliderMax(20)
        .visible(diagonals::get)
        .build()
    );
    private final Setting<Integer> minDiagonalWidth = sgTParams.add(new IntSetting.Builder()
        .name("Min Diagonal Tunnel Width")
        .description("Minimum width for diagonal tunnels to be detected")
        .defaultValue(2)
        .min(2)
        .sliderMax(10)
        .visible(diagonals::get)
        .build()
    );
    private final Setting<Integer> maxDiagonalWidth = sgTParams.add(new IntSetting.Builder()
        .name("Max Diagonal Tunnel Width")
        .description("Maximum width for diagonal tunnels to be detected")
        .defaultValue(4)
        .min(2)
        .sliderMax(10)
        .visible(diagonals::get)
        .build()
    );
    private final Setting<Integer> minStaircaseLength = sgSParams.add(new IntSetting.Builder()
        .name("Min Staircase Length")
        .description("Minimum length for a staircase to be detected")
        .defaultValue(3)
        .min(1)
        .sliderMax(20)
        .build()
    );
    private final Setting<Integer> minStaircaseHeight = sgSParams.add(new IntSetting.Builder()
        .name("Min Staircase Height")
        .description("Minimum height of the staircase to be detected")
        .defaultValue(3)
        .min(2)
        .sliderMax(10)
        .build()
    );
    private final Setting<Integer> maxStaircaseHeight = sgSParams.add(new IntSetting.Builder()
        .name("Max Staircase Height")
        .description("Maximum height of the staircase to be detected")
        .defaultValue(5)
        .min(2)
        .sliderMax(10)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered.")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> holeLineColor = sgRender.add(new ColorSetting.Builder()
        .name("1x1-hole-line-color")
        .description("The color of the lines for 1x1 holes being rendered.")
        .defaultValue(new SettingColor(255, 0, 0, 95))
        .build()
    );
    private final Setting<SettingColor> holeSideColor = sgRender.add(new ColorSetting.Builder()
        .name("1x1-hole-side-color")
        .description("The color of the sides for 1x1 holes being rendered.")
        .defaultValue(new SettingColor(255, 0, 0, 30))
        .build()
    );
    private final Setting<SettingColor> hole3x1LineColor = sgRender.add(new ColorSetting.Builder()
        .name("3x1-hole-line-color")
        .description("The color of the lines for 3x1 holes being rendered.")
        .defaultValue(new SettingColor(255, 165, 0, 95))
        .build()
    );
    private final Setting<SettingColor> hole3x1SideColor = sgRender.add(new ColorSetting.Builder()
        .name("3x1-hole-side-color")
        .description("The color of the sides for 3x1 holes being rendered.")
        .defaultValue(new SettingColor(255, 165, 0, 30))
        .build()
    );
    private final Setting<SettingColor> tunnelLineColor = sgRender.add(new ColorSetting.Builder()
        .name("tunnel-line-color")
        .description("The color of the lines for the tunnels being rendered.")
        .defaultValue(new SettingColor(0, 0, 255, 95))
        .build()
    );
    private final Setting<SettingColor> tunnelSideColor = sgRender.add(new ColorSetting.Builder()
        .name("tunnel-side-color")
        .description("The color of the sides for the tunnels being rendered.")
        .defaultValue(new SettingColor(0, 0, 255, 30))
        .build()
    );
    private final Setting<SettingColor> staircaseLineColor = sgRender.add(new ColorSetting.Builder()
        .name("staircase-line-color")
        .description("The color of the lines for the staircases being rendered.")
        .defaultValue(new SettingColor(255, 0, 255, 95))
        .build()
    );
    private final Setting<SettingColor> staircaseSideColor = sgRender.add(new ColorSetting.Builder()
        .name("staircase-side-color")
        .description("The color of the sides for the staircases being rendered.")
        .defaultValue(new SettingColor(255, 0, 255, 30))
        .build()
    );

    private static final Direction[] DIRECTIONS = { Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH };
    private final Long2ObjectMap<TChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final Queue<ChunkAccess> chunkQueue = new LinkedList<>();
    // chunk key -> ticks left before rescan; servers that reveal blocks late would otherwise leave stale boxes
    private final it.unimi.dsi.fastutil.longs.Long2IntMap dirtyChunks = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    private static final int RESCAN_DELAY_TICKS = 10;
    private final Set<AABB> holes = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // every block already assigned to a detected hole, packed with BlockPos.asLong
    private final Set<Long> holeCells = ConcurrentHashMap.newKeySet();
    // box -> {bottom Y, top Y} of the whole hole it belongs to, for filters that depend on the player
    private final Map<AABB, int[]> holeExtents = new ConcurrentHashMap<>();
    private final Set<AABB> tunnels = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<AABB> staircases = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<AABB> holes3x1 = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public HoleTunnelStairsESP() {
        super(Categories.Esp, "hole-tunnel-stair-esp", "Finds and highlights holes and tunnels and stairs.");
    }

    public Set<AABB> getHoles() {
        return new HashSet<>(holes);
    }

    @Override
    public void onDeactivate() {
        chunks.clear();
        chunkQueue.clear();
        dirtyChunks.clear();
        holes.clear();
        holeCells.clear(); holeExtents.clear();
        tunnels.clear();
        staircases.clear();
        holes3x1.clear();
    }

    // once a second, not every tick. per tick this did thousands of chunk lookups and tanked fps
    private static final int CLEANUP_INTERVAL_TICKS = 20;
    private int cleanupTicks = 0;
    private final Set<Long> loadedChunkKeys = new HashSet<>();

    @EventHandler
    private void onBlockUpdate(spacyjka.tunnel.client.events.world.BlockUpdateEvent event) {
        int x = event.pos.getX(), z = event.pos.getZ();
        // sections are validated against horizontal neighbours, so an edge block also affects the next chunk
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx != 0 && dz != 0) continue;
                dirtyChunks.put(ChunkPos.asLong((x + dx) >> 4, (z + dz) >> 4), RESCAN_DELAY_TICKS);
            }
        }
    }

    @EventHandler
    private void onChunkData(spacyjka.tunnel.client.events.world.ChunkDataEvent event) {
        // a scan that ran before a neighbour arrived saw air there and cut border holes short
        dirtyChunks.put(ChunkPos.asLong(event.x, event.z), RESCAN_DELAY_TICKS);
        dirtyChunks.put(ChunkPos.asLong(event.x + 1, event.z), RESCAN_DELAY_TICKS);
        dirtyChunks.put(ChunkPos.asLong(event.x - 1, event.z), RESCAN_DELAY_TICKS);
        dirtyChunks.put(ChunkPos.asLong(event.x, event.z + 1), RESCAN_DELAY_TICKS);
        dirtyChunks.put(ChunkPos.asLong(event.x, event.z - 1), RESCAN_DELAY_TICKS);
    }

    private void rescanDirtyChunks() {
        if (dirtyChunks.isEmpty()) return;

        var it = dirtyChunks.long2IntEntrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.getIntValue() > 1) {
                entry.setValue(entry.getIntValue() - 1);
                continue;
            }

            long key = entry.getLongKey();
            it.remove();

            // dropping the record makes onTick queue the chunk again
            TChunk old = chunks.remove(key);
            if (old == null) continue;
            old.stale = true;
            removeBoxesInChunk(holes, key);
            removeHoleCellsInChunk(key);
            removeBoxesInChunk(tunnels, key);
            removeBoxesInChunk(staircases, key);
            removeBoxesInChunk(holes3x1, key);
        }
    }

    private void removeBoxesInChunk(Set<AABB> boxSet, long key) {
        boxSet.removeIf(box -> ChunkPos.asLong((int) Math.floor(box.minX) >> 4, (int) Math.floor(box.minZ) >> 4) == key);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        synchronized (chunks) {
            rescanDirtyChunks();

            for (TChunk tChunk : chunks.values()) tChunk.marked = false;

            for (ChunkAccess chunk : Utils.chunks(true)) {
                long key = ChunkPos.asLong(chunk.getPos().x, chunk.getPos().z);

                if (chunks.containsKey(key)) chunks.get(key).marked = true;
                else if (!chunkQueue.contains(chunk)) {
                    chunkQueue.add(chunk);
                }
            }

            processChunkQueue();
            chunks.values().removeIf(tChunk -> !tChunk.marked);
        }

        if (++cleanupTicks >= CLEANUP_INTERVAL_TICKS) {
            cleanupTicks = 0;
            removeBoxesOutsideRenderDistance();
        }
    }

    private void removeBoxesOutsideRenderDistance() {
        loadedChunkKeys.clear();
        for (ChunkAccess chunk : Utils.chunks(true)) {
            loadedChunkKeys.add(ChunkPos.asLong(chunk.getPos().x, chunk.getPos().z));
        }

        if (loadedChunkKeys.isEmpty()) return;

        removeBoxesOutsideRenderDistance(holes);
        holeExtents.keySet().retainAll(holes);
        holeCells.removeIf(cell -> !loadedChunkKeys.contains(ChunkPos.asLong(BlockPos.getX(cell) >> 4, BlockPos.getZ(cell) >> 4)));
        removeBoxesOutsideRenderDistance(tunnels);
        removeBoxesOutsideRenderDistance(staircases);
        removeBoxesOutsideRenderDistance(holes3x1);
    }

    private void removeBoxesOutsideRenderDistance(Set<AABB> boxSet) {
        boxSet.removeIf(box -> !loadedChunkKeys.contains(
            ChunkPos.asLong((int) Math.floor(box.minX) >> 4, (int) Math.floor(box.minZ) >> 4)));
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        switch (detectionMode.get()) {
            case ALL:
                renderHoles(event.renderer);
                renderTunnels(event.renderer);
                renderStaircases(event.renderer);
                render3x1Holes(event.renderer);
                break;
            case HOLES_AND_TUNNELS:
                renderHoles(event.renderer);
                renderTunnels(event.renderer);
                render3x1Holes(event.renderer);
                break;
            case HOLES_AND_STAIRCASES:
                renderHoles(event.renderer);
                renderStaircases(event.renderer);
                render3x1Holes(event.renderer);
                break;
            case TUNNELS_AND_STAIRCASES:
                renderTunnels(event.renderer);
                renderStaircases(event.renderer);
                break;
            case HOLES:
                renderHoles(event.renderer);
                render3x1Holes(event.renderer);
                break;
            case TUNNELS:
                renderTunnels(event.renderer);
                break;
            case STAIRCASES:
                renderStaircases(event.renderer);
                break;
            case HOLES_3X1_AND_TUNNELS:
                renderHoles(event.renderer);
                render3x1Holes(event.renderer);
                renderTunnels(event.renderer);
                break;
        }
    }

    private void renderHoles(Renderer3D renderer) {
        boolean filter = playerToBedrock.get() && mc.player != null && mc.level != null;
        int tolerance = playerToBedrockTolerance.get();
        for (AABB box : holes) {
            if (filter) {
                int[] extent = holeExtents.get(box);
                if (extent == null
                    || extent[0] - mc.level.getMinY() > tolerance
                    || Math.abs(extent[1] + 1 - mc.player.getY()) > tolerance) continue;
            }
            renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, holeSideColor.get(), holeLineColor.get(), shapeMode.get(), 0);
        }
    }

    private void render3x1Holes(Renderer3D renderer) {
        for (AABB box : holes3x1) {
            renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, hole3x1SideColor.get(), hole3x1LineColor.get(), shapeMode.get(), 0);
        }
    }

    private void renderTunnels(Renderer3D renderer) {
        for (AABB box : tunnels) {
            renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, tunnelSideColor.get(), tunnelLineColor.get(), shapeMode.get(), 0);
        }
    }

    private void renderStaircases(Renderer3D renderer) {
        for (AABB box : staircases) {
            renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, staircaseSideColor.get(), staircaseLineColor.get(), shapeMode.get(), 0);
        }
    }

    private void processChunkQueue() {
        int maxChunksPerTick = maxChunks.get();
        int processed = 0;

        while (!chunkQueue.isEmpty() && processed < maxChunksPerTick) {
            ChunkAccess chunk = chunkQueue.poll();
            if (chunk != null) {
                TChunk tChunk = new TChunk(chunk.getPos().x, chunk.getPos().z);
                chunks.put(tChunk.getKey(), tChunk);

                TunelExecutor.execute(() -> searchChunk(chunk, tChunk));
                processed++;
            }
        }
    }

    private void searchChunk(ChunkAccess chunk, TChunk tChunk) {
        var sections = chunk.getSections();
        int Ymin = mc.level.getMinY() + minY.get();
        int Ymax = mc.level.getMaxY() - maxY.get();
        int Y = mc.level.getMinY();
        for (LevelChunkSection section : sections) {
            if (tChunk.stale) return;
            if (section != null && !section.hasOnlyAir()) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            int currentY = Y + y;
                            if (currentY <= Ymin || currentY >= Ymax) continue;
                            BlockPos pos = chunk.getPos().getBlockAt(x, currentY, z);
                            if (isPassableBlock(pos)) {
                                switch (detectionMode.get()) {
                                    case ALL:
                                        checkHole(pos, holes);
                                        check3x1Hole(pos, holes3x1);
                                        checkTunnel(pos);
                                        if (diagonals.get()) checkDiagonalTunnel(pos);
                                        checkStaircase(pos);
                                        break;
                                    case HOLES_AND_TUNNELS:
                                        checkHole(pos, holes);
                                        check3x1Hole(pos, holes3x1);
                                        checkTunnel(pos);
                                        if (diagonals.get()) checkDiagonalTunnel(pos);
                                        break;
                                    case HOLES_AND_STAIRCASES:
                                        checkHole(pos, holes);
                                        check3x1Hole(pos, holes3x1);
                                        checkStaircase(pos);
                                        break;
                                    case TUNNELS_AND_STAIRCASES:
                                        checkTunnel(pos);
                                        if (diagonals.get()) checkDiagonalTunnel(pos);
                                        checkStaircase(pos);
                                        break;
                                    case HOLES:
                                        checkHole(pos, holes);
                                        check3x1Hole(pos, holes3x1);
                                        break;
                                    case TUNNELS:
                                        checkTunnel(pos);
                                        if (diagonals.get()) checkDiagonalTunnel(pos);
                                        break;
                                    case STAIRCASES:
                                        checkStaircase(pos);
                                        break;
                                    case HOLES_3X1_AND_TUNNELS:
                                        checkHole(pos, holes);
                                        check3x1Hole(pos, holes3x1);
                                        checkTunnel(pos);
                                        if (diagonals.get()) checkDiagonalTunnel(pos);
                                        break;
                                }
                            }
                        }
                    }
                }
            }
            Y += 16;
        }
    }

    private void checkHole(BlockPos pos, Set<AABB> holes) {
        if (holeCells.contains(pos.asLong())) return;

        int maxWidth = maxHoleWidth.get();
        Set<BlockPos> start = crossSection(List.of(pos), maxWidth);
        if (start == null) return;

        List<BlockPos> cells = new ArrayList<>(start);
        // walking both ways makes the result independent of which block of the hole was scanned first
        int top = followHole(start, 1, maxWidth, cells);
        int bottom = followHole(start, -1, maxWidth, cells);

        if (top - bottom + 1 < minHoleDepth.get()) return;

        // one box per vertical run of air in each column, so only blocks that are really open get highlighted
        Map<Long, List<Integer>> columns = new HashMap<>();
        for (BlockPos cell : cells) {
            if (holeCells.add(cell.asLong())) {
                columns.computeIfAbsent(ChunkPos.asLong(cell.getX(), cell.getZ()), k -> new ArrayList<>()).add(cell.getY());
            }
        }

        for (Map.Entry<Long, List<Integer>> column : columns.entrySet()) {
            int x = ChunkPos.getX(column.getKey()), z = ChunkPos.getZ(column.getKey());
            List<Integer> ys = column.getValue();
            Collections.sort(ys);

            int runStart = ys.get(0), previous = runStart;
            for (int i = 1; i <= ys.size(); i++) {
                if (i < ys.size() && ys.get(i) == previous + 1) {
                    previous = ys.get(i);
                    continue;
                }
                AABB box = new AABB(x, runStart, z, x + 1, previous + 1, z + 1);
                holeExtents.put(box, new int[] { bottom, top });
                holes.add(box);
                if (i < ys.size()) {
                    runStart = ys.get(i);
                    previous = runStart;
                }
            }
        }
    }

    // Passable blocks on one level reachable from the seeds, or null when that area is wider than maxWidth
    // (open cave, tunnel, surface). This is what lets a hole change width and direction on the way down.
    private Set<BlockPos> crossSection(Collection<BlockPos> seeds, int maxWidth) {
        Set<BlockPos> cells = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos seed : seeds) {
            if (cells.add(seed)) queue.add(seed);
        }

        while (!queue.isEmpty()) {
            BlockPos cell = queue.poll();
            minX = Math.min(minX, cell.getX());
            maxX = Math.max(maxX, cell.getX());
            minZ = Math.min(minZ, cell.getZ());
            maxZ = Math.max(maxZ, cell.getZ());
            if (maxX - minX >= maxWidth || maxZ - minZ >= maxWidth) return null;

            for (Direction dir : DIRECTIONS) {
                BlockPos next = cell.relative(dir);
                if (!cells.contains(next) && isPassableBlock(next)) {
                    cells.add(next);
                    queue.add(next);
                }
            }
        }

        return cells;
    }

    // Follows the hole up (step 1) or down (step -1), collects its blocks and returns the last Y that belongs to it.
    private int followHole(Set<BlockPos> start, int step, int maxWidth, List<BlockPos> out) {
        Set<BlockPos> cells = start;
        int y = start.iterator().next().getY();
        int last = y;
        int gap = 0;
        int tolerance = holeGapTolerance.get();
        int minY = mc.level.getMinY(), maxY = mc.level.getMaxY();
        // blocks of too-wide levels, kept only if the hole narrows again after them
        List<BlockPos> bridged = new ArrayList<>();

        while (true) {
            y += step;
            if (y < minY || y > maxY) break;

            List<BlockPos> carried = new ArrayList<>();
            for (BlockPos cell : cells) {
                BlockPos next = cell.atY(y);
                if (isPassableBlock(next)) carried.add(next);
            }
            if (carried.isEmpty()) break;

            Set<BlockPos> level = crossSection(carried, maxWidth);
            if (level == null) {
                if (++gap > tolerance) break;
                cells = new HashSet<>(carried);
                bridged.addAll(carried);
            } else {
                gap = 0;
                cells = level;
                last = y;
                out.addAll(bridged);
                bridged.clear();
                out.addAll(level);
            }
        }

        return last;
    }

    private void removeHoleCellsInChunk(long key) {
        holeCells.removeIf(cell -> ChunkPos.asLong(BlockPos.getX(cell) >> 4, BlockPos.getZ(cell) >> 4) == key);
    }

    // settings only apply while scanning, so changing one has to throw away the old results
    private void rescanAll() {
        if (chunks == null) return;
        synchronized (chunks) {
            for (TChunk tChunk : chunks.values()) tChunk.stale = true;
            chunks.clear();
            chunkQueue.clear();
            dirtyChunks.clear();
            holes.clear();
            holeCells.clear(); holeExtents.clear();
            tunnels.clear();
            staircases.clear();
            holes3x1.clear();
        }
    }

    private void check3x1Hole(BlockPos pos, Set<AABB> holes3x1) {
        if (isValid3x1HoleSectionX(pos)) {
            int top = holeTop(pos, this::isValid3x1HoleSectionX);
            if (top - pos.getY() >= minHoleDepth.get()) {
                AABB holeBox = new AABB(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 3, top, pos.getZ() + 1
                );
                if (!holes3x1.contains(holeBox) && holes3x1.stream().noneMatch(existingHole -> existingHole.intersects(holeBox))) {
                    holes3x1.add(holeBox);
                }
            }
        }

        if (isValid3x1HoleSectionZ(pos)) {
            int top = holeTop(pos, this::isValid3x1HoleSectionZ);
            if (top - pos.getY() >= minHoleDepth.get()) {
                AABB holeBox = new AABB(
                    pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, top, pos.getZ() + 3
                );
                if (!holes3x1.contains(holeBox) && holes3x1.stream().noneMatch(existingHole -> existingHole.intersects(holeBox))) {
                    holes3x1.add(holeBox);
                }
            }
        }
    }

    // walks up from a valid section; sections with an opening to the side are bridged up to the tolerance,
    // so a shaft with blocks mined out of its walls stays one hole instead of short pieces below min depth
    private int holeTop(BlockPos pos, java.util.function.Predicate<BlockPos> validSection) {
        BlockPos.MutableBlockPos currentPos = pos.mutable();
        int top = pos.getY();
        int gap = 0;
        int tolerance = holeGapTolerance.get();
        while (true) {
            if (validSection.test(currentPos)) {
                top = currentPos.getY() + 1;
                gap = 0;
            } else if (gap < tolerance && isPassableBlock(currentPos)) {
                gap++;
            } else {
                break;
            }
            currentPos.move(Direction.UP);
        }
        return top;
    }

    private boolean isValidHoleSection(BlockPos pos) {
        return isPassableBlock(pos) && !isPassableBlock(pos.north()) && !isPassableBlock(pos.south()) && !isPassableBlock(pos.east()) && !isPassableBlock(pos.west());
    }

    private boolean isValid3x1HoleSectionX(BlockPos pos) {
        return isPassableBlock(pos) &&
            isPassableBlock(pos.east()) &&
            isPassableBlock(pos.east(2)) &&
            !isPassableBlock(pos.north()) &&
            !isPassableBlock(pos.south()) &&
            !isPassableBlock(pos.east(3)) &&
            !isPassableBlock(pos.west()) &&
            !isPassableBlock(pos.east().north()) &&
            !isPassableBlock(pos.east().south()) &&
            !isPassableBlock(pos.east(2).north()) &&
            !isPassableBlock(pos.east(2).south());
    }

    private boolean isValid3x1HoleSectionZ(BlockPos pos) {
        return isPassableBlock(pos) &&
            isPassableBlock(pos.south()) &&
            isPassableBlock(pos.south(2)) &&
            !isPassableBlock(pos.east()) &&
            !isPassableBlock(pos.west()) &&
            !isPassableBlock(pos.south(3)) &&
            !isPassableBlock(pos.north()) &&
            !isPassableBlock(pos.south().east()) &&
            !isPassableBlock(pos.south().west()) &&
            !isPassableBlock(pos.south(2).east()) &&
            !isPassableBlock(pos.south(2).west());
    }

    private void checkTunnel(BlockPos pos) {
        for (Direction dir : DIRECTIONS) {
            BlockPos.MutableBlockPos currentPos = pos.mutable();
            int stepCount = 0;
            BlockPos startPos = null;
            BlockPos endPos = null;
            int maxHeight = 0;
            if (startPos == null && isTunnelSection(currentPos, dir)) {
                startPos = currentPos.immutable();
            }
            while (isTunnelSection(currentPos, dir)) {
                maxHeight = Math.max(maxHeight, getTunnelHeight(currentPos));
                endPos = currentPos.immutable();
                currentPos.move(dir);
                stepCount++;
            }

            if (stepCount >= minTunnelLength.get() && maxHeight >= minTunnelHeight.get() && maxHeight <= maxTunnelHeight.get()) {
                AABB tunnelBox = new AABB(
                    Math.min(startPos.getX(), endPos.getX()),
                    startPos.getY(),
                    Math.min(startPos.getZ(), endPos.getZ()),
                    Math.max(startPos.getX(), endPos.getX()) + 1,
                    startPos.getY() + maxHeight,
                    Math.max(startPos.getZ(), endPos.getZ()) + 1
                );

                if (!tunnels.contains(tunnelBox) && tunnels.stream().noneMatch(existingTunnel -> existingTunnel.intersects(tunnelBox))) {
                    tunnels.add(tunnelBox);
                }
            }
        }
    }

    private boolean isTunnelSection(BlockPos pos, Direction dir) {
        int height = getTunnelHeight(pos);
        if (height < minTunnelHeight.get() || height > maxTunnelHeight.get()) return false;
        if (isPassableBlock(pos.below()) || isPassableBlock(pos.above(height))) return false;
        Direction[] perpDirs = dir.getAxis() == Direction.Axis.X ? new Direction[]{Direction.NORTH, Direction.SOUTH} : new Direction[]{Direction.EAST, Direction.WEST};
        for (Direction perpDir : perpDirs) {
            for (int i = 0; i < height; i++) {
                if (isPassableBlock(pos.above(i).relative(perpDir))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkDiagonalTunnel(BlockPos pos) {
        for (Direction dir : DIRECTIONS) {
            for (int i = minDiagonalWidth.get() - 1; i < maxDiagonalWidth.get(); i++) {
                BlockPos.MutableBlockPos currentPos = pos.mutable();
                int stepCount = 0;
                List<AABB> potentialBoxes = new ArrayList<>();

                Direction checkingDir = dir;
                boolean turnRight = true;

                while (isDiagonalTunnelSection(currentPos, checkingDir)) {
                    int height = getTunnelHeight(currentPos);
                    AABB tunnelBox = new AABB(
                        currentPos.getX(),
                        currentPos.getY(),
                        currentPos.getZ(),
                        currentPos.getX() + 1,
                        currentPos.getY() + height,
                        currentPos.getZ() + 1
                    );
                    if (!potentialBoxes.contains(tunnelBox) && !potentialBoxes.stream().anyMatch(existingDiagonal -> existingDiagonal.intersects(tunnelBox))) {
                        potentialBoxes.add(tunnelBox);
                    }

                    if (turnRight) {
                        checkingDir = checkingDir.getClockWise();
                        currentPos.move(checkingDir.getClockWise(), i);
                        turnRight = false;
                    } else {
                        checkingDir = checkingDir.getCounterClockWise();
                        currentPos.move(checkingDir.getCounterClockWise(), i);
                        turnRight = true;
                    }
                    stepCount++;
                }

                if (stepCount / minDiagonalWidth.get() >= minDiagonalLength.get()) {
                    potentialBoxes.forEach(potentialBox -> {
                        if (!tunnels.contains(potentialBox) && tunnels.stream().noneMatch(existingDiagonal -> existingDiagonal.intersects(potentialBox))) {
                            tunnels.add(potentialBox);
                        }
                    });
                }
            }
        }
    }

    private boolean isDiagonalTunnelSection(BlockPos pos, Direction dir) {
        int height = getTunnelHeight(pos);
        if (height < minTunnelHeight.get() || height > maxTunnelHeight.get()) return false;
        if (isPassableBlock(pos.below()) || isPassableBlock(pos.above(height))) return false;

        boolean wasPassableBlockFound = false;
        for (int i = 0; i < height; i++) {
            if (isPassableBlock(pos.above(i).relative(dir))) wasPassableBlockFound = true;
        }
        if (wasPassableBlockFound) return false;

        return true;
    }

    private int getTunnelHeight(BlockPos pos) {
        int height = 0;
        while (isPassableBlock(pos.above(height)) && height < maxTunnelHeight.get()) {
            height++;
        }
        return height;
    }

    private void checkStaircase(BlockPos pos) {
        for (Direction dir : DIRECTIONS) {
            BlockPos.MutableBlockPos currentPos = pos.mutable();
            int stepCount = 0;
            List<AABB> potentialStaircaseBoxes = new ArrayList<>();

            while (isStaircaseSection(currentPos, dir)) {
                int height = getStaircaseHeight(currentPos);
                AABB stairsBox = new AABB(
                    currentPos.getX(),
                    currentPos.getY(),
                    currentPos.getZ(),
                    currentPos.getX() + 1,
                    currentPos.getY() + height,
                    currentPos.getZ() + 1
                );
                if (!potentialStaircaseBoxes.contains(stairsBox) && !potentialStaircaseBoxes.stream().anyMatch(existingStaircase -> existingStaircase.intersects(stairsBox))) {
                    potentialStaircaseBoxes.add(stairsBox);
                }
                currentPos.move(dir);
                currentPos.move(Direction.UP);
                stepCount++;
            }

            for (AABB stairsBox : potentialStaircaseBoxes) {
                if (stepCount >= minStaircaseLength.get() && !staircases.contains(stairsBox) && !staircases.stream().anyMatch(existingStaircase -> existingStaircase.intersects(stairsBox))) {
                    staircases.add(stairsBox);
                }
            }
        }
    }

    private int getStaircaseHeight(BlockPos pos) {
        int height = 0;
        while (isPassableBlock(pos.above(height)) && height < maxStaircaseHeight.get()) {
            height++;
        }
        return height;
    }

    private boolean isStaircaseSection(BlockPos pos, Direction dir) {
        int height = getStaircaseHeight(pos);
        if (height < minStaircaseHeight.get() || height > maxStaircaseHeight.get()) return false;
        if (isPassableBlock(pos.below()) || isPassableBlock(pos.above(height))) return false;
        Direction[] perpDirs = dir.getAxis() == Direction.Axis.X ? new Direction[]{Direction.NORTH, Direction.SOUTH} : new Direction[]{Direction.EAST, Direction.WEST};
        for (Direction perpDir : perpDirs) {
            for (int i = 0; i < height; i++) {
                if (isPassableBlock(pos.above(i).relative(perpDir))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPassableBlock(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (airBlocks.get()) {
            return state.isAir();
        } else {
            VoxelShape shape = state.getCollisionShape(mc.level, pos);
            return shape.isEmpty() || !Shapes.block().equals(shape);
        }
    }

    public enum DetectionMode {
        ALL,
        HOLES_AND_TUNNELS,
        HOLES_AND_STAIRCASES,
        TUNNELS_AND_STAIRCASES,
        HOLES,
        TUNNELS,
        STAIRCASES,
        HOLES_3X1_AND_TUNNELS
    }

    private class TChunk {
        private final int x, z;
        public boolean marked;
        // set when a rescan supersedes this scan, so the old pass stops adding boxes
        public volatile boolean stale;

        public TChunk(int x, int z) {
            this.x = x;
            this.z = z;
            this.marked = true;
        }

        public long getKey() {
            return ChunkPos.asLong(x, z);
        }
    }
}
