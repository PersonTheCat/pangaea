package personthecat.pangaea.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.serialization.codec.BiomeCodecs;

import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;

// priority: very high since we are completely replacing the codec, allowing others to modify it
@Mixin(value = BiomeGenerationSettings.class, priority = -1000)
public class BiomeGenerationSettingsMixin {

    @Shadow
    private static final Logger LOGGER = LogUtils.getLogger();

    @WrapOperation(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;mapCodec(Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;"),
        remap = false)
    private static MapCodec<BiomeGenerationSettings> replaceCodec(Function<?, ?> builder, Operation<MapCodec<BiomeGenerationSettings>> original) {
        final var carverListCodec =
            ConfiguredWorldCarver.LIST_CODEC.promotePartial(Util.prefix("Carver: ", LOGGER::error));
        final var carvingKeys = StringRepresentable.keys(Carving.values());
        final var carversCodec = Codec.simpleMap(Carving.CODEC, carverListCodec, carvingKeys);
        final var featuresCodec =
            BiomeCodecs.SIMPLE_FEATURE_LIST.promotePartial(Util.prefix("Features: ", LOGGER::error));
        return codecOf(
            carversCodec.fieldOf("carvers").forGetter(s -> ((BiomeGenerationSettingsAccessor) s).getCarvers()),
            featuresCodec.fieldOf("features").forGetter(BiomeGenerationSettings::features),
            BiomeGenerationSettings::new
        );
    }
}
