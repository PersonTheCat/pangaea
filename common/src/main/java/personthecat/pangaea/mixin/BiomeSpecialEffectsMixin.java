package personthecat.pangaea.mixin;

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

// priority: very high since we are completely replacing the codec, allowing others to modify it
@Mixin(value = BiomeSpecialEffects.class, priority = -1000)
public class BiomeSpecialEffectsMixin {

    @WrapOperation(
        method = "<clinit>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"),
        remap = false)
    private static Codec<BiomeSpecialEffects> replaceCodec(Function<?, ?> builder, Operation<Codec<BiomeSpecialEffects>> original) {
        return codecOf(
            ColorCodecs.COLOR_INT.fieldOf("fog_color").forGetter(BiomeSpecialEffects::getFogColor),
            ColorCodecs.COLOR_INT.fieldOf("water_color").forGetter(BiomeSpecialEffects::getWaterColor),
            ColorCodecs.COLOR_INT.fieldOf("water_fog_color").forGetter(BiomeSpecialEffects::getWaterFogColor),
            ColorCodecs.COLOR_INT.fieldOf("sky_color").forGetter(BiomeSpecialEffects::getSkyColor),
            ColorCodecs.COLOR_INT.optionalFieldOf("foliage_color").forGetter(BiomeSpecialEffects::getFoliageColorOverride),
            ColorCodecs.COLOR_INT.optionalFieldOf("grass_color").forGetter(BiomeSpecialEffects::getGrassColorOverride),
            GrassColorModifier.CODEC.optionalFieldOf("grass_color_modifier", GrassColorModifier.NONE).forGetter(BiomeSpecialEffects::getGrassColorModifier),
            AmbientParticleSettings.CODEC.optionalFieldOf("particle").forGetter(BiomeSpecialEffects::getAmbientParticleSettings),
            SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter(BiomeSpecialEffects::getAmbientLoopSoundEvent),
            AmbientMoodSettings.CODEC.optionalFieldOf("mood_sound").forGetter(BiomeSpecialEffects::getAmbientMoodSettings),
            AmbientAdditionsSettings.CODEC.optionalFieldOf("additions_sound").forGetter(BiomeSpecialEffects::getAmbientAdditionsSettings),
            Music.CODEC.optionalFieldOf("music").forGetter(BiomeSpecialEffects::getBackgroundMusic),
            BiomeSpecialEffects::new
        ).codec();
    }
}
