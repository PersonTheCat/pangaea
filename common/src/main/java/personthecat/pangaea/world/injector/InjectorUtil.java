package personthecat.pangaea.world.injector;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;

public final class InjectorUtil {
    private InjectorUtil() {}

    @SuppressWarnings("deprecation")
    public static NoiseGeneratorSettings swapNoiseRouter(NoiseGeneratorSettings settings, NoiseRouter router) {
        return new NoiseGeneratorSettings(
            settings.noiseSettings(),
            settings.defaultBlock(),
            settings.defaultFluid(),
            router,
            settings.surfaceRule(),
            settings.spawnTarget(),
            settings.seaLevel(),
            settings.disableMobGeneration(),
            settings.aquifersEnabled(),
            settings.oreVeinsEnabled(),
            settings.useLegacyRandomSource());
    }

    public static NoiseRouter swapFinalDensity(NoiseRouter router, DensityFunction f) {
        return new NoiseRouter(
            router.barrierNoise(),
            router.fluidLevelFloodednessNoise(),
            router.fluidLevelSpreadNoise(),
            router.lavaNoise(),
            router.temperature(),
            router.vegetation(),
            router.continents(),
            router.erosion(),
            router.depth(),
            router.ridges(),
            router.initialDensityWithoutJaggedness(),
            f,
            router.veinToggle(),
            router.veinRidged(),
            router.veinGap());
    }
}
