package personthecat.pangaea.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockMatchTest.class)
public interface BlockMatchTestAccessor {

    @Accessor
    Block getBlock();
}
