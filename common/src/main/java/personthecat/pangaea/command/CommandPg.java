package personthecat.pangaea.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import personthecat.catlib.client.gui.SimpleTextPage;
import personthecat.catlib.command.CommandContextWrapper;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.exception.CommandExecutionException;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.serialization.codec.XjsOps;
import personthecat.catlib.serialization.json.JsonTransformer;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.world.road.RoadMap;
import personthecat.pangaea.world.road.TmpRoadUtils;
import xjs.data.JsonFormat;
import xjs.data.JsonValue;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CommandPg {

    @ModCommand(description = "Demo command for testing Pangaea")
    void welcome(final CommandContextWrapper ctx) {
        ctx.sendMessage("Welcome to Pangaea!");
    }

    @ModCommand(description = "Displays the initial_density_without_jaggedness value at the current position (and y=0)")
    void debugDensity(final CommandContextWrapper ctx) {
        final var router = ctx.getLevel().getChunkSource().randomState().router();
        final var pos = MutableFunctionContext.from(ctx.getPos());
        final var density = router.initialDensityWithoutJaggedness().compute(pos);
        final var density0 = router.initialDensityWithoutJaggedness().compute(pos.at(0));
        ctx.sendMessage("density: {}, density (at 0): {}", density, density0);
    }

    @ModCommand(description = "Displays the initial_density_without_jaggedness value at the given coordinates")
    void debugDensity(final CommandContextWrapper ctx, final int x, final int y, final int z) {
        final var router = ctx.getLevel().getChunkSource().randomState().router();
        ctx.sendMessage(String.valueOf(router.initialDensityWithoutJaggedness().compute(new MutableFunctionContext(x, y, z))));
    }

    @ModCommand(description = "Displays the terrain slope and erosion values at the current position")
    void debugSlope(final CommandContextWrapper ctx, final Optional<Boolean> initial) {
        final var router = ctx.getLevel().getChunkSource().randomState().router();
        final var f = initial.orElse(false) ? router.initialDensityWithoutJaggedness() : TmpRoadUtils.SURFACE.get();
        final var p = MutableFunctionContext.from(ctx.getPos());
        final var surface = f.compute(p);
        final var surface0 = f.compute(p.at(0));
        final var slope = Math.max(
            Math.abs(f.compute(p.south(1)) - f.compute(p.north(2))),
            Math.abs(f.compute(p.south(1).west(1)) - f.compute(p.east(2)))
        );
        ctx.sendMessage("surface: {}, surface (at 0): {}, slope: {}", surface, surface0, slope);
    }

    @ModCommand(description = "Displays the overworld/sloped_cheese value at the current position")
    void debugTerrain(final CommandContextWrapper ctx) {
        final var r = ctx.getLevel().registryAccess().registryOrThrow(Registries.DENSITY_FUNCTION);
        final var f = r.getOrThrow(ResourceKey.create(Registries.DENSITY_FUNCTION, new ResourceLocation("overworld/sloped_cheese")));
        final var p = MutableFunctionContext.from(ctx.getPos());
        ctx.sendMessage("overworld/sloped_cheese: {}", f.compute(p));
    }

    @Environment(EnvType.CLIENT)
    @ModCommand(description = "Prints the road weight at this location")
    void sample(final CommandContextWrapper ctx) {
        final var graph = LevelExtras.getNoiseGraph(ctx.getLevel());
        final var pos = MutableFunctionContext.from(ctx.getPos());
        ctx.sendMessage("weight: {}", TmpRoadUtils.getWeight(graph, pos));
    }

    @Environment(EnvType.CLIENT)
    @ModCommand(description = "Testing possible ways to smooth terrain around roads")
    void sphere(final CommandContextWrapper ctx, int radius, boolean showTop) {
        final var rand = new WorldgenRandom(RandomSource.create(ctx.getLevel().getSeed()));
        final var origin = ctx.assertPlayer().getOnPos();
        var pos = origin.below((radius / 2));
        rand.setDecorationSeed(ctx.getLevel().getSeed(), pos.getX(), pos.getZ());
        for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                for (int y = pos.getY() - radius; y <= pos.getY() + (radius / 2); y++) {
                    if (Utils.distance(x, y, z, pos.getX(), pos.getY(), pos.getZ()) >= radius) {
                        continue;
                    }
                    final Block block;
                    if (y >= pos.getY() + (radius / 2)) { // top
                        if (Utils.distance(x, y, z, origin.getX(), origin.getY(), origin.getZ()) > radius / 2.0 + 0.5) {
                            continue;
                        }
                        block = rand.nextDouble() <= 0.75 ? Blocks.GRAVEL : Blocks.GRASS_BLOCK;
                    } else {
                        block = Blocks.DIRT;
                    }
                    ctx.getLevel().setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
                }
            }
        }
        final var top = showTop ? Blocks.LIGHT_GRAY_STAINED_GLASS : Blocks.AIR;
        pos = pos.above(radius * 3 / 2);
        for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                for (int y = pos.getY() - radius; y <= pos.getY(); y++) {
                    if (Utils.distance(x, y, z, pos.getX(), pos.getY(), pos.getZ()) < radius) {
                        ctx.getLevel().setBlock(new BlockPos(x, y, z), top.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    @ModCommand(description = "Clears road region data from memory")
    void cacheClear(final CommandContextWrapper ctx) {
        RoadMap.clearAll(ctx.getServer());
        ctx.sendMessage("Successfully cleared road cache.");
    }

    @Environment(EnvType.CLIENT)
    @ModCommand(description = "Outputs the simplified final density function ")
    void printDensity(final CommandContextWrapper ctx, final Optional<DensityFunction> f) {
        final var ops = RegistryOps.create(XjsOps.INSTANCE, ctx.getServer().registryAccess());
        DensityFunction.HOLDER_HELPER_CODEC.encodeStart(ops, f.orElseGet(CommandPg::getFinalDensity))
            .ifSuccess(json -> renderJson(ctx, formatDensity(json)))
            .ifError(error -> ctx.sendError(error.message()));
    }

    private static DensityFunction getFinalDensity() {
        final var settings =
            DynamicRegistries.get(Registries.NOISE_SETTINGS).lookup(new ResourceLocation("overworld"));
        if (settings == null) {
            throw new CommandExecutionException("Couldn't find noise settings for current dim");
        }
        return settings.noiseRouter().finalDensity();
    }

    @Environment(EnvType.CLIENT)
    private static void renderJson(final CommandContextWrapper ctx, final JsonValue json) {
        final var text = json.toString(JsonFormat.DJS_FORMATTED).replace("\r", "");
        if (text.length() > 30_000) {
            ctx.sendError("I ain't reading all that");
            return;
        }
        final var component = text.length() < 15_000 ? ctx.lintMessage(text) : Component.literal(text);
        ctx.setScreen(new SimpleTextPage(null, Component.literal("Final Density"), component));
    }

    private static JsonValue formatDensity(final JsonValue json) {
        if (!json.isObject()) return json;
        return JsonTransformer.all()
            .reorder(List.of("type", "input", "min_inclusive", "max_exclusive"))
            .collapseArrays("clamp", a -> true)
            .collapseArrays("cache", a -> true)
            .collapseArrays("amplitudes", a -> a.size() <= 5)
            .getUpdated(json.asObject());
    }
}
