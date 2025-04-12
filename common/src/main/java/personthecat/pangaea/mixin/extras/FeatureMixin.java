package personthecat.pangaea.mixin.extras;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.extras.FeatureExtras;

@Mixin(Feature.class)
public class FeatureMixin<FC extends FeatureConfiguration> implements FeatureExtras<FC> {

    @Unique
    private Codec<FC> pangaea$codec;

    @Inject(at = @At("RETURN"), method = "<init>")
    public void postInit(Codec<FC> codec, CallbackInfo ci) {
        this.pangaea$codec = codec;
    }

    @Override
    public Codec<FC> pangaea$getCodec() {
        return this.pangaea$codec;
    }
}
