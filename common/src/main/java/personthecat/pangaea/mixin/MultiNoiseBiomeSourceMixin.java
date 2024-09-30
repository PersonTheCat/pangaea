package personthecat.pangaea.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.ParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import personthecat.catlib.data.collections.ObserverSet;
import personthecat.catlib.data.collections.SimpleObserverSet;
import personthecat.pangaea.world.biome.MultiNoiseBiomeSourceExtras;
import personthecat.pangaea.world.biome.ParameterListModifierListener;

import java.util.ArrayList;

@Mixin(MultiNoiseBiomeSource.class)
public class MultiNoiseBiomeSourceMixin implements MultiNoiseBiomeSourceExtras {

    @Unique // values are modified lazily due to the high cost of building the RTree
    private final ObserverSet<ParameterListModifierListener> pangaea$modifiers = new SimpleObserverSet<>();

    @ModifyReturnValue(method = "parameters", at = @At("RETURN"))
    private ParameterList<Holder<Biome>> modifyParameters(ParameterList<Holder<Biome>> original) {
        if (this.pangaea$modifiers.isEmpty()) {
            return original;
        }
        final var modified = new ArrayList<>(original.values());
        this.pangaea$modifiers.forEach(listener -> listener.modify(modified));
        return new ParameterList<>(modified);
    }

    @Override
    public void pangaea$modifyBiomeParameters(ParameterListModifierListener listener) {
        this.pangaea$modifiers.add(listener);
    }
}
