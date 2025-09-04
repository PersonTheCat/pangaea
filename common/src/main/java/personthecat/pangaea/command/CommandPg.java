package personthecat.pangaea.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.DensityFunction;
import personthecat.catlib.client.gui.SimpleTextPage;
import personthecat.catlib.command.CommandContextWrapper;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.catlib.exception.CommandExecutionException;
import personthecat.catlib.registry.DynamicRegistries;
import personthecat.catlib.serialization.codec.XjsOps;
import personthecat.catlib.serialization.json.JsonTransformer;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.extras.LevelExtras;
import personthecat.pangaea.resources.builtin.BuiltInWorldPack;
import personthecat.pangaea.world.road.RoadMap;
import personthecat.pangaea.world.road.RoadRegion;
import xjs.data.JsonFormat;
import xjs.data.JsonValue;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static personthecat.catlib.command.CommandUtils.clickToRun;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CommandPg {

    @ModCommand(description = "Demo command for testing Pangaea")
    void welcome(final CommandContextWrapper ctx) {
        ctx.sendMessage("Welcome to Pangaea!");
    }

    @ModCommand(description = "Export the built-in data pack generated from settings")
    void exportPack(final CommandContextWrapper ctx, final String path, final Optional<Boolean> replace) throws IOException {
        final var out = Pangaea.MOD.configFolder().toPath().resolve("exports").resolve(path);
        BuiltInWorldPack.buildExtension().export(out, replace.orElse(false));
        ctx.sendMessage("Exported built-in pack to {}", out);
    }

    @ModCommand(description = "Locates the nearest road vertex in a 3,072 block radius")
    void locateRoads(final CommandContextWrapper ctx, final Optional<Boolean> generate) {
        final var map = LevelExtras.getRoadMap(ctx.getLevel());
        final int ax = (int) ctx.getPos().x;
        final int az = (int) ctx.getPos().z;
        final short x = RoadRegion.absToRegion(ax);
        final short z = RoadRegion.absToRegion(az);
        var nearest = map.getRegion(x, z).getNearest(ax, az);
        boolean loadingPrinted = false;
        boolean regionsMissing = false;
        if (nearest == null) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    short x2 = (short) (x + dz);
                    short z2 = (short) (z + dz);

                    if (!map.hasRegion(x2, z2)) {
                        if (generate.orElse(false)) {
                            if (!loadingPrinted) {
                                ctx.sendMessage("Loading...");
                                loadingPrinted = true;
                            }
                        } else {
                            regionsMissing = true;
                            continue;
                        }
                    }
                    map.loadOrGenerateRegion(x2, z2);
                    final var n = map.getRegion(x2, z2).getNearest(ax, az);
                    if (n == null) {
                        continue;
                    }
                    if (nearest == null || n.distance() < nearest.distance()) {
                        nearest = n;
                    }
                }
            }
        }
        if (nearest == null) {
            if (regionsMissing) {
                ctx.sendMessage("No roads were found. Try running with the generate option for better results");
            } else {
                ctx.sendMessage("There are no roads nearby");
            }
            return;
        }
        final var tpStyle = Style.EMPTY
            .withUnderlined(true)
            .withClickEvent(clickToRun("/tp %s ~ %s".formatted(nearest.x(), nearest.z())));
        final var message = Component.literal("found road at ")
            .append(Component.literal("[%s, %s]".formatted(nearest.x(), nearest.z())).withStyle(tpStyle));
        ctx.sendMessage(message);
    }

    @ModCommand(description = "Clears road region data from memory")
    void cacheClear(final CommandContextWrapper ctx) {
        RoadMap.clearAll(ctx.getServer());
        ctx.sendMessage("Successfully cleared road cache.");
    }

    @ModCommand(description = "Testing distance cache")
    void debugDistance(final CommandContextWrapper ctx) {
        final var graph = LevelExtras.getNoiseGraph(ctx.getLevel());
        final var pos = ctx.getPos();
        ctx.sendMessage("Nearest vertex: {} blocks", graph.getApproximateRoadDistance((int) pos.x, (int) pos.z));
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
