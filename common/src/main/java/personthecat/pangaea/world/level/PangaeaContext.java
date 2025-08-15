package personthecat.pangaea.world.level;

import net.minecraft.core.Holder;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Aquifer.FluidStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions.Marker;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import personthecat.catlib.data.ForkJoinThreadLocal;
import personthecat.pangaea.data.Counter;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.extras.WorldGenRegionExtras;
import personthecat.pangaea.mixin.accessor.NoiseChunkAccessor;
import personthecat.pangaea.util.CommonBlocks;

public final class PangaeaContext extends WorldGenerationContext {

    private static final ForkJoinThreadLocal<PangaeaContext> INSTANCE = ForkJoinThreadLocal.create();
    private static final FluidStatus NO_FLUID = new FluidStatus(0, CommonBlocks.AIR);
    private static final Aquifer NO_AQUIFER = Aquifer.createDisabled((x, y, z) -> NO_FLUID);
    public final BiomeManager biomes;
    public final NoiseGraph noise;
    public final WorldgenRandom rand;
    public final WorldGenLevel level;
    public final NoiseRouter router;
    public final Climate.Sampler sampler;
    public final int chunkX;
    public final int chunkZ;
    public final int actualX;
    public final int actualZ;
    public final int centerX;
    public final int centerZ;
    public final int seaLevel;
    public final int minY;
    public final long seed;
    public final ChunkGenerator gen;
    public final ProtoChunk chunk;
    public final Heightmap oceanFloor;
    public final Heightmap worldSurface;
    public final CarvingMask carvingMask;
    public final Aquifer aquifer;
    public final MutableFunctionContext targetPos;
    public final Counter featureIndex;
    private boolean enableDensityWrap;

    public PangaeaContext(WorldGenLevel level, WorldgenRandom rand, ProtoChunk chunk, ChunkGenerator gen) {
        super(gen, level);
        final var source = gen.getBiomeSource();
        final var pos = chunk.getPos();
        this.noise = LevelExtras.getNoiseGraph(level.getLevel());
        this.rand = rand;
        this.level = level;
        this.router = level.getLevel().getChunkSource().randomState().router();
        this.sampler = level.getLevel().getChunkSource().randomState().sampler();
        this.biomes = level.getBiomeManager().withDifferentSource((x, y, z) ->
            source.getNoiseBiome(x, y, z, this.sampler));
        this.chunkX = pos.x;
        this.chunkZ = pos.z;
        this.actualX = this.chunkX << 4;
        this.actualZ = this.chunkZ << 4;
        this.centerX = this.actualX + 8;
        this.centerZ = this.actualZ + 8;
        this.seaLevel = gen.getSeaLevel();
        this.minY = this.level.getMinBuildHeight();
        this.seed = level.getSeed();
        this.gen = gen;
        this.chunk = chunk;
        this.oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        this.worldSurface = chunk.getOrCreateHeightmapUnprimed(getSurfaceType(chunk));
        this.carvingMask = chunk.getOrCreateCarvingMask(GenerationStep.Carving.AIR);
        final var nc = chunk.getOrCreateNoiseChunk(c -> null);
        this.aquifer = nc instanceof NoiseChunkAccessor ? nc.aquifer() : NO_AQUIFER;
        this.targetPos = MutableFunctionContext.from(pos);
        this.featureIndex = new Counter();
        this.enableDensityWrap = true;
    }

    private static Heightmap.Types getSurfaceType(ProtoChunk chunk) {
        return chunk.getStatus().isOrAfter(ChunkStatus.CARVERS)
            ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG;
    }

    public static PangaeaContext init(WorldGenLevel level, ProtoChunk chunk, ChunkGenerator gen) {
        if (gen instanceof NoiseBasedChunkGenerator noise) {
            final var rand = new WorldgenRandom(
                noise.generatorSettings().value().getRandomSource().newInstance(level.getSeed()));
            return init(level, rand, chunk, gen);
        }
        final var rand = new WorldgenRandom(new XoroshiroRandomSource(level.getSeed()));
        return init(level, rand, chunk, gen);
    }

    public static PangaeaContext init(
            WorldGenLevel level, WorldgenRandom rand, ProtoChunk chunk, ChunkGenerator gen) {
        final var ctx = new PangaeaContext(level, rand, chunk, gen);
        INSTANCE.set(ctx);
        // ctx is doubly linked here to minimize cost of thread-local access
        WorldGenRegionExtras.setPangaeaContext(level, ctx);
        return ctx;
    }

    public static PangaeaContext get() {
        final var ctx = INSTANCE.get();
        if (ctx != null) {
            return ctx;
        }
        throw new IllegalStateException("Pangaea context not installed");
    }

    public static PangaeaContext get(WorldGenerationContext wgc) {
        if (wgc instanceof PlacementContext pc) {
            return WorldGenRegionExtras.getPangaeaContext(pc.getLevel());
        } else if (wgc instanceof PangaeaContext gc) {
            return gc;
        }
        return get();
    }

    public static PangaeaContext get(WorldGenLevel level) {
        // assumes context has been installed correctly
        return WorldGenRegionExtras.getPangaeaContext(level);
    }

    public void enableDensityWrap(boolean enable) {
        this.enableDensityWrap = enable;
    }

    public DensityFunction wrap(DensityFunction f) {
        final NoiseChunk c = this.chunk.getOrCreateNoiseChunk(a -> null);
        if (this.enableDensityWrap && c instanceof NoiseChunkAccessor a) {
            return a.invokeWrap(f);
        }
        return f.mapAll(f2 -> f2 instanceof Marker m ? m.wrapped() : f2);
    }

    public BlockState getBlock(int x, int y, int z) {
        final var section = this.chunk.getSections()[(y - this.minY) >> 4];
        if (section.hasOnlyAir()) {
            return CommonBlocks.AIR;
        }
        return section.getBlockState(x & 15, y & 15, z & 15);
    }

    public void setBlock(int x, int y, int z, BlockState state, int updates) {
        final var section = this.chunk.getSections()[(y - this.minY) >> 4];
        if (section.hasOnlyAir() && state.is(Blocks.AIR)) {
            return;
        }
        section.setBlockState(x & 15, y & 15, z & 15, state, false);
        if ((updates & BlockUpdates.HEIGHTMAP) == BlockUpdates.HEIGHTMAP) {
            this.worldSurface.update(x & 15, y, z & 15, state);
        }
    }

    public int getHeight(int x, int z) {
        return this.oceanFloor.getFirstAvailable(x & 15, z & 15) - 1;
    }

    public int getHeightChecked(int x, int z) {
        int cX = x >> 4;
        int cZ = z >> 4;
        if (cX == this.chunkX && cZ == this.chunkZ) {
            return this.oceanFloor.getFirstAvailable(x & 15, z & 15) - 1;
        }
        final var c = this.level.getChunk(cX, cZ);
        if (c.getStatus().isOrAfter(ChunkStatus.NOISE)) {
            return c.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG)
                .getFirstAvailable(x & 15, z & 15) - 1;
        }
        final var rand = this.level.getLevel().getChunkSource().randomState();
        return this.gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, this.level, rand) - 1;
    }

    public Holder<Biome> getApproximateBiome(int x, int z) {
        return this.noise.getApproximateBiome(this.biomes, x, z);
    }
}
