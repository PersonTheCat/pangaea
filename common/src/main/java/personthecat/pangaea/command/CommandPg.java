package personthecat.pangaea.command;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import personthecat.pangaea.world.road.RoadMap;
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
