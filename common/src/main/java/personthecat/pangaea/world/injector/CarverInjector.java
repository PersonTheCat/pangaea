package personthecat.pangaea.world.injector;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import personthecat.catlib.data.BiomePredicate;
import personthecat.catlib.data.IdList;
import personthecat.catlib.registry.CommonRegistries;
import personthecat.catlib.serialization.codec.capture.CaptureCategory;
import personthecat.pangaea.serialization.codec.CarverCodecs;
import personthecat.pangaea.serialization.codec.PangaeaCodec;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.nullable;
import static personthecat.catlib.serialization.codec.FieldDescriptor.union;

public record CarverInjector(
        BiomePredicate biomes,
        IdList<ConfiguredWorldCarver<?>> remove,
        IdList<WorldCarver<?>> removeCarvers,
        InjectionMap<AddedCarver> inject) implements Injector {

    public static final MapCodec<CarverInjector> CODEC =
        PangaeaCodec.build(CarverInjector::createCodec)
            .addCaptures(CaptureCategory.get(AddedCarver.class).captors())
            .mapCodec();

    @Override
    public void inject(ResourceKey<Injector> key, InjectionContext ctx) {
        if (this.remove != null) {
            ctx.addRemovals(this.biomes, mods -> mods.removeCarver(this.remove));
        }
        if (this.removeCarvers != null) {
            ctx.addRemovals(this.biomes, mods -> mods.removeCarver(holder -> {
                final var carver = CommonRegistries.CARVER.getHolder(holder.value().worldCarver());
                return this.removeCarvers.test(carver);
            }));
        }
        this.inject.forEach((id, mods) -> mods.apply(this.biomes, id, ctx));
    }

    @Override
    public MapCodec<CarverInjector> codec() {
        return CODEC;
    }

    private static MapCodec<CarverInjector> createCodec(CaptureCategory<CarverInjector> cat) {
        final var injectionListCodec = InjectionMap.codecOfEasyList(AddedCarver.CODEC.codec());
        return codecOf(
            defaulted(BiomePredicate.CODEC, "biomes", BiomePredicate.ALL_BIOMES, CarverInjector::biomes),
            nullable(IdList.codecOf(Registries.CONFIGURED_CARVER), "remove", CarverInjector::remove),
            nullable(IdList.codecOf(Registries.CARVER), "remove_carvers", CarverInjector::removeCarvers),
            defaulted(injectionListCodec, "inject", new InjectionMap<>(), CarverInjector::inject),
            CarverInjector::new
        );
    }

    public record AddedCarver(Carving step, ConfiguredWorldCarver<?> carver) {
        public static final MapCodec<AddedCarver> CODEC = PangaeaCodec.buildMap(cat -> codecOf(
            cat.defaulted(Carving.CODEC, "step", Carving.AIR, AddedCarver::step),
            union(CarverCodecs.FLAT_CONFIG, AddedCarver::carver),
            AddedCarver::new
        ));

        private void apply(BiomePredicate biomes, ResourceLocation id, InjectionContext ctx) {
            final var registry = ctx.registries().registryOrThrow(Registries.CONFIGURED_CARVER);
            final var step = this.step;
            final var holder = Registry.registerForHolder(registry, id, this.carver);

            ctx.addAdditions(biomes, mods -> mods.addCarver(step, holder));
        }
    }
}
