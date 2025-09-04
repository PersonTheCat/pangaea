package personthecat.pangaea.mixin.extras;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.extras.CarverExtras;

@Mixin(WorldCarver.class)
public class WorldCarverMixin<C extends CarverConfiguration> implements CarverExtras<C> {

    @Unique
    private Codec<C> pangaea$codec;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void postInit(Codec<C> codec, CallbackInfo ci) {
        this.pangaea$codec = codec;
    }

    @Override
    public Codec<C> pangaea$getCodec() {
        return this.pangaea$codec;
    }
}
