package personthecat.pangaea.world.injector;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.SurfaceRules.RuleSource;
import org.jetbrains.annotations.Nullable;
import personthecat.catlib.data.DimensionPredicate;
import personthecat.catlib.event.error.LibErrorContext;
import personthecat.pangaea.Pangaea;
import personthecat.pangaea.mixin.accessor.NoiseGeneratorSettingsAccessor;
import personthecat.pangaea.world.surface.InjectedRuleSource;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;

public record SurfaceRuleInjector(
        DimensionPredicate dimensions,
        boolean replaceOriginal,
        @Nullable RuleSource beforeAll,
        @Nullable RuleSource afterAll) implements Injector {

    public static final MapCodec<SurfaceRuleInjector> CODEC = codecOf(
        field(DimensionPredicate.CODEC, "dimensions", SurfaceRuleInjector::dimensions),
        defaulted(Codec.BOOL, "replace_original", false, SurfaceRuleInjector::replaceOriginal),
        nullable(RuleSource.CODEC, "before_all", SurfaceRuleInjector::beforeAll),
        nullable(RuleSource.CODEC, "after_all", SurfaceRuleInjector::afterAll),
        SurfaceRuleInjector::new
    );

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        ctx.registries().registryOrThrow(Registries.LEVEL_STEM).holders().forEach(stem -> {
            if (this.dimensions.test(stem.value())) {
                this.modifyDimension(stem);
                ctx.addCleanupTask(key, () -> this.restorePrefix(stem));
            }
        });
    }

    @SuppressWarnings("ConstantConditions")
    private void modifyDimension(Holder<LevelStem> stem) {
        if (!(stem.value().generator() instanceof NoiseBasedChunkGenerator g)) {
            final var key = stem.unwrapKey().orElseThrow();
            LibErrorContext.warn(Pangaea.MOD, InjectionWarningException.incompatibleGenerator(key));
            return;
        }
        if (!((Object) g.generatorSettings().value() instanceof NoiseGeneratorSettingsAccessor s)) {
            throw new IllegalStateException("NoiseGeneratorSettings mixin not applied successfully");
        }
        var rule = g.generatorSettings().value().surfaceRule();
        if (!(rule instanceof InjectedRuleSource)) {
            rule = InjectedRuleSource.wrap(rule);
            s.setSurfaceRule(rule);
        }
        this.applyInjections((InjectedRuleSource) rule);
    }

    private void applyInjections(InjectedRuleSource rule) {
        if (this.replaceOriginal) {
            rule.original().setValue(null);
        }
        if (this.beforeAll != null) {
            rule.beforeAll().addFirst(this.beforeAll);
        }
        if (this.afterAll != null) {
            rule.afterAll().addLast(this.afterAll);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void restorePrefix(Holder<LevelStem> stem) {
        if (!(stem.value().generator() instanceof NoiseBasedChunkGenerator g)) {
            return;
        }
        final var rule = g.generatorSettings().value().surfaceRule();
        if (rule instanceof InjectedRuleSource injected) {
            ((NoiseGeneratorSettingsAccessor) (Object) g.generatorSettings().value()).setSurfaceRule(injected.optimize());
        }
    }

    @Override
    public Phase phase() {
        return Phase.DIMENSION;
    }

    @Override
    public MapCodec<SurfaceRuleInjector> codec() {
        return CODEC;
    }
}
