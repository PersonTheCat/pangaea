package personthecat.pangaea.test;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.loader.impl.game.minecraft.MinecraftGameProvider;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.apache.commons.lang3.time.StopWatch;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

@Log4j2
public class DummyGameProvider extends MinecraftGameProvider {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void launch(ClassLoader loader) {
        // only apply catlib mixins, so we don't have to worry about remapping the others
        Mixins.getConfigs().removeIf(config -> !config.getName().contains("catlib"));
        MixinEnvironment.getCurrentEnvironment()
            .setOption(MixinEnvironment.Option.DISABLE_REFMAP, true);

        log.info("Bootstrapping default registries");
        final StopWatch sw = StopWatch.createStarted();
        try {
            TestUtils.runFromMixinEnabledClassLoader(Delegate.class, "launch");
        } catch (final Throwable e) {
            throw new IllegalStateException("Error running bootstrap", e);
        }
        log.info("Finished bootstrapping registries in {}", sw);
    }

    public static class Delegate {

        @SuppressWarnings("unused")
        public void launch() {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
        }
    }
}
