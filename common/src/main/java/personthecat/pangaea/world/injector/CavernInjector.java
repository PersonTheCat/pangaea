package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import lombok.extern.log4j.Log4j2;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import personthecat.catlib.serialization.codec.DefaultTypeCodec;
import personthecat.pangaea.world.density.DensityController;

import static personthecat.catlib.serialization.codec.CodecUtils.asParent;

import static personthecat.pangaea.world.injector.InjectorUtil.swapFinalDensity;
import static personthecat.pangaea.world.injector.InjectorUtil.swapNoiseRouter;

@Log4j2
public record CavernInjector(InjectionMap<DensityFunction> modifications) implements Injector {
    private static final Codec<DensityFunction> DENSITY_CODEC =
        new DefaultTypeCodec<>(DensityFunction.DIRECT_CODEC, asParent(DensityController.CODEC.codec()),
            (f, ops) -> f instanceof DensityController);
    public static final MapCodec<CavernInjector> CODEC =
        MapCodec.assumeMapUnsafe(InjectionMap.codecOfMap(DENSITY_CODEC))
            .xmap(CavernInjector::new, i -> i.modifications);

    @Override
    public void inject(InjectionContext ctx) {
        final var registry = ctx.registries().registryOrThrow(Registries.NOISE_SETTINGS);
        this.modifications.forEach((id, f) -> {
            var settings = registry.get(id);
            if (settings == null) {
                log.warn("Unknown noise settings. Cannot inject caverns: {}", id);
                return;
            }
            settings = swapNoiseRouter(settings, swapFinalDensity(settings.noiseRouter(), f));
            Registry.register(registry, id, settings);
        });
    }

    @Override
    public MapCodec<? extends Injector> codec() {
        return CODEC;
    }
}
