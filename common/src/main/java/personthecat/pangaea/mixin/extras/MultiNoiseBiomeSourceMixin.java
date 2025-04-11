package personthecat.pangaea.mixin.extras;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.ParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import personthecat.catlib.data.collections.ObserverSet;
import personthecat.catlib.data.collections.SimpleObserverSet;
import personthecat.pangaea.extras.MultiNoiseBiomeSourceExtras;
import personthecat.pangaea.world.biome.ParameterListModifierListener;

import java.util.ArrayList;
import java.util.function.Function;

@Mixin(MultiNoiseBiomeSource.class)
public class MultiNoiseBiomeSourceMixin implements MultiNoiseBiomeSourceExtras {

    @Unique // values are modified lazily due to the high cost of building the RTree
    private final ObserverSet<ParameterListModifierListener> pangaea$modifiers = new SimpleObserverSet<>();

    @Shadow
    @Mutable
    private @Final Either<ParameterList<Holder<Biome>>, Holder<MultiNoiseBiomeSourceParameterList>> parameters;

    @Inject(method = "collectPossibleBiomes", at = @At("HEAD"))
    private void modifyParameters(CallbackInfoReturnable<ParameterList<Holder<Biome>>> ci) {
        if (!this.pangaea$modifiers.isEmpty()) {
            final var original = this.parameters.map(Function.identity(), h -> h.value().parameters());
            final var modified = new ArrayList<>(original.values());

            this.pangaea$modifiers.forEach(listener -> listener.modify(modified));
            this.pangaea$modifiers.clear();
            this.parameters = Either.left(new ParameterList<>(modified));
        }
    }

    @Override
    public void pangaea$modifyBiomeParameters(ParameterListModifierListener listener) {
        this.pangaea$modifiers.add(listener);
    }
}
