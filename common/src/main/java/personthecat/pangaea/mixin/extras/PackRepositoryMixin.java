package personthecat.pangaea.mixin.extras;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import personthecat.pangaea.resources.worldpack.WorldPackRepositorySource;

import java.util.Set;

// Must go after fabric mixin so data is mutable (true for all platforms)
@Mixin(value = PackRepository.class, priority = 1500)
public class PackRepositoryMixin {

    @Shadow @Final private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addWorldPackRepository(CallbackInfo ci) {
        this.sources.add(WorldPackRepositorySource.INSTANCE);
    }
}
