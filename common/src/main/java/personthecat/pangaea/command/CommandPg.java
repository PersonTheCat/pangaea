package personthecat.pangaea.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import personthecat.catlib.client.gui.SimpleTextPage;
import personthecat.catlib.command.CommandContextWrapper;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.command.annotations.Nullable;
import personthecat.catlib.exception.CommandExecutionException;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.serialization.codec.XjsOps;
import personthecat.catlib.serialization.json.JsonTransformer;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.level.LevelExtras;
import personthecat.pangaea.world.road.RoadMap;
import personthecat.pangaea.world.road.TmpRoadUtils;
import xjs.data.JsonFormat;
import xjs.data.JsonValue;

import java.util.List;

public class CommandPg {

    @ModCommand(description = "Demo command for testing Pangaea")
    void welcome(final CommandContextWrapper ctx) {
        ctx.sendMessage("Welcome to Pangaea!");
    }

    @Environment(EnvType.CLIENT)
    @ModCommand(description = "Prints the road weight at this location")
    void sample(final CommandContextWrapper ctx) {
        final var sampler = ((ServerLevel) ctx.getLevel()).getChunkSource().randomState().sampler();
        final var graph = LevelExtras.getNoiseGraph(ctx.getLevel());
        final var pos = MutableFunctionContext.from(ctx.getPos());
        ctx.sendMessage("weight: {}", TmpRoadUtils.getWeight(graph, sampler, pos));
    }

    @Environment(EnvType.CLIENT)
    @ModCommand(description = "Testing possible ways to smooth terrain around roads")
    void sphere(final CommandContextWrapper ctx, int radius, boolean showTop) {
        var pos = ctx.assertPlayer().getOnPos().mutable().below((radius / 2) + 1);
        for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                for (int y = pos.getY() - radius; y <= pos.getY() + (radius / 2); y++) {
                    final int o = y > pos.getY() ? y - pos.getY() : 0; // 2x
                    if (Utils.distance(x, y + o, z, pos.getX(), pos.getY(), pos.getZ()) < radius) {
                        ctx.getLevel().setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                    }
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
    void printDensity(final CommandContextWrapper ctx, final @Nullable DensityFunction f) {
        final var ops = RegistryOps.create(XjsOps.INSTANCE, ctx.getServer().registryAccess());
        DensityFunction.HOLDER_HELPER_CODEC.encodeStart(ops, f != null ? f : getFinalDensity())
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
            .reorder(List.of("type"))
            .collapseArrays("clamp", a -> true)
            .collapseArrays("cache", a -> true)
            .collapseArrays("amplitudes", a -> a.size() <= 5)
            .getUpdated(json.asObject());
    }
}
