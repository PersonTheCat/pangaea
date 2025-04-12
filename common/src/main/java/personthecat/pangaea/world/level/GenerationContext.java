package personthecat.pangaea.world.level;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Aquifer.FluidStatus;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions.Marker;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import personthecat.pangaea.data.NoiseGraph;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.mixin.accessor.ChunkAccessAccessor;
import personthecat.pangaea.mixin.accessor.NoiseChunkAccessor;
import personthecat.pangaea.util.CommonBlocks;

import java.util.Map;

public final class GenerationContext extends WorldGenerationContext {

    private static final FluidStatus NO_FLUID = new FluidStatus(0, CommonBlocks.AIR);
    private static final Aquifer NO_AQUIFER = Aquifer.createDisabled((_, _, _) -> NO_FLUID);
    public final BiomeManager biomes;
    public final NoiseGraph noise;
    public final WorldgenRandom rand;
    public final ServerLevel level;
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
    public final ProtoChunk chunk;
    public final Map<Heightmap.Types, Heightmap> heightmaps;
    public final Heightmap oceanFloor;
    public final CarvingMask carvingMask;
    public final Aquifer aquifer;

    public GenerationContext(WorldGenLevel level, WorldgenRandom rand, ProtoChunk chunk, ChunkGenerator gen) {
        super(gen, level);
        final var source = gen.getBiomeSource();
        final var pos = chunk.getPos();
        this.noise = LevelExtras.getNoiseGraph(level.getLevel());
        this.rand = rand;
        this.level = level.getLevel();
        this.sampler = ((ServerChunkCache) level.getChunkSource()).randomState().sampler();
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
        this.chunk = chunk;
        this.heightmaps = ((ChunkAccessAccessor) chunk).getHeightmaps();
        this.oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        this.carvingMask = chunk.getOrCreateCarvingMask(GenerationStep.Carving.AIR);
        final var nc = chunk.getOrCreateNoiseChunk(_ -> null);
        this.aquifer = nc instanceof NoiseChunkAccessor ? nc.aquifer() : NO_AQUIFER;
    }

    public DensityFunction wrap(DensityFunction f) {
        final NoiseChunk c = this.chunk.getOrCreateNoiseChunk(_ -> null);
        if (c instanceof NoiseChunkAccessor a) {
            return a.invokeWrap(f);
        }
        return f.mapAll(f2 -> f2 instanceof Marker m ? m.wrapped() : f2);
    }

    public BlockState get(int x, int y, int z) {
        if (this.level.isOutsideBuildHeight(y)) {
            return CommonBlocks.VOID_AIR;
        }
        return this.getUnchecked(x, y, z);
    }

    public BlockState getUnchecked(int x, int y, int z) {
        final var section = this.chunk.getSections()[(y - this.minY) >> 4];
        if (section.hasOnlyAir()) {
            return CommonBlocks.AIR;
        }
        return section.getBlockState(x & 15, y & 15, z & 15);
    }

    public BlockState set(int x, int y, int z, BlockState state) {
        if (this.level.isOutsideBuildHeight(y)) {
            return state;
        }
        return this.setUnchecked(x, y, z, state);
    }

    public BlockState setUnchecked(int x, int y, int z, BlockState state) {
        final var section = this.chunk.getSections()[(y - this.minY) >> 4];
        if (section.hasOnlyAir() && state.is(Blocks.AIR)) {
            return state;
        }
        return section.setBlockState(x & 15, y & 15, z & 15, state, false);
    }

    public int getHeight(int x, int z) {
        return this.oceanFloor.getFirstAvailable(x & 15, z & 15) - 1;
    }
}
