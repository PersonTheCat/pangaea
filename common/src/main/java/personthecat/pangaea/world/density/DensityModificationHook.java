package personthecat.pangaea.world.density;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import lombok.extern.log4j.Log4j2;
import net.minecraft.world.level.levelgen.DensityFunction;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.catlib.exception.GenericFormattedException;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.config.Cfg;
import personthecat.pangaea.mixin.DensityFunctionsAccessor;
import personthecat.pangaea.mixin.KeyDispatchCodecAccessor;
import personthecat.pangaea.serialization.codec.DensityFunctionBuilder;
import personthecat.pangaea.serialization.codec.StructuralDensityCodec;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Log4j2
public final class DensityModificationHook {
    private static final AtomicReference<Backup<MapCodec<DensityFunction>, DensityFunction>> BACKUP = new AtomicReference<>();

    private DensityModificationHook() {}

    public static void onConfigUpdated() {
        if (Cfg.enableDensityBuilders() || Cfg.enableStructuralDensity()) {
            injectFeatures();
        } else {
            restoreCodec();
        }
    }

    private static void injectFeatures() {
        try { // it would be ideal to replace the codec altogether, but that is not possible
            final var injector = resolveInjector();
            resetValues(injector);
            if (Cfg.enableStructuralDensity()) {
                StructuralDensityCodec.install(injector);
            }
            if (Cfg.enableDensityBuilders()) {
                DensityFunctionBuilder.install(injector);
            }
            log.info("Successfully injected modifications into density codec");
        } catch (final RuntimeException e) {
            LibErrorContext.warn(Pangaea.MOD, new GenericFormattedException(e));
        }
    }

    private static void restoreCodec() {
        if (BACKUP.get() != null) {
            resetValues(resolveInjector());
            BACKUP.set(null);
            log.info("Density codec has been restored");
        }
    }

    private static void resetValues(Injector injector) {
        final var backup = BACKUP.get();
        final var codec = injector.codec();
        if (backup == null) {
            BACKUP.set(new Backup<>(codec.getType(), codec.getDecoder(), codec.getEncoder()));
        } else {
            codec.setType(backup.type);
            codec.setDecoder(backup.decoder);
            codec.setEncoder(backup.encoder);
        }
    }

    @SuppressWarnings("unchecked")
    private static Injector resolveInjector() {
        final var codec = DensityFunctionsAccessor.getCodec();
        if (codec instanceof MapCodec.MapCodecCodec<DensityFunction> mapCodecCodec) {
            if (mapCodecCodec.codec() instanceof KeyDispatchCodec<?, ?> dispatch) {
                return new Injector((KeyDispatchCodecAccessor<MapCodec<DensityFunction>, DensityFunction>) dispatch);
            }
        }
        throw new IllegalStateException("Density codec hot-swapped by another mod. Cannot install builders: " + codec);
    }

    private record Backup<K, V>(
            Function<V, DataResult<K>> type,
            Function<K, DataResult<MapDecoder< V>>> decoder,
            Function<V, DataResult<MapEncoder<V>>> encoder) {}

    public record Injector(
            KeyDispatchCodecAccessor<MapCodec<DensityFunction>, DensityFunction> codec) {}
}
