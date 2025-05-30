package personthecat.pangaea.mixin.codec;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.pangaea.serialization.codec.ColorCodecs;

import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.optional;

// priority: very high since we are completely replacing the codec, allowing others to modify it
@Mixin(value = BiomeSpecialEffects.class, priority = -1000)
public class BiomeSpecialEffectsMixin {

    @WrapOperation(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<BiomeSpecialEffects> replaceCodec(Function<?, ?> builder, Operation<Codec<BiomeSpecialEffects>> original) {
        return codecOf(
            field(ColorCodecs.COLOR_INT, "fog_color", BiomeSpecialEffects::getFogColor),
            field(ColorCodecs.COLOR_INT, "water_color", BiomeSpecialEffects::getWaterColor),
            field(ColorCodecs.COLOR_INT, "water_fog_color", BiomeSpecialEffects::getWaterFogColor),
            field(ColorCodecs.COLOR_INT, "sky_color", BiomeSpecialEffects::getSkyColor),
            optional(ColorCodecs.COLOR_INT, "foliage_color", BiomeSpecialEffects::getFoliageColorOverride),
            optional(ColorCodecs.COLOR_INT, "grass_color", BiomeSpecialEffects::getGrassColorOverride),
            defaulted(GrassColorModifier.CODEC, "grass_color_modifier", GrassColorModifier.NONE, BiomeSpecialEffects::getGrassColorModifier),
            optional(AmbientParticleSettings.CODEC, "particle", BiomeSpecialEffects::getAmbientParticleSettings),
            optional(SoundEvent.CODEC, "ambient_sound", BiomeSpecialEffects::getAmbientLoopSoundEvent),
            optional(AmbientMoodSettings.CODEC, "mood_sound", BiomeSpecialEffects::getAmbientMoodSettings),
            optional(AmbientAdditionsSettings.CODEC, "additions_sound", BiomeSpecialEffects::getAmbientAdditionsSettings),
            optional(Music.CODEC, "music", BiomeSpecialEffects::getBackgroundMusic),
            BiomeSpecialEffects::new
        ).codec();
    }
}
