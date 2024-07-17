package personthecat.pangaea.command;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import personthecat.catlib.command.CommandContextWrapper;
import personthecat.catlib.command.CommandSide;
import personthecat.catlib.command.annotations.ModCommand;
import personthecat.pangaea.data.MutableFunctionContext;
import personthecat.pangaea.util.Utils;
import personthecat.pangaea.world.level.LevelExtras;
import personthecat.pangaea.world.road.TmpRoadUtils;

public class CommandPg {

    @ModCommand(description = "Demo command for testing Pangaea")
    void welcome(final CommandContextWrapper ctx) {
        ctx.sendMessage("Welcome to Pangaea!");
    }
    
    @ModCommand(
        side = CommandSide.CLIENT,
        description = "Prints the road weight at this location")
    void sample(final CommandContextWrapper ctx) {
        final var sampler = ((ServerLevel) ctx.getLevel()).getChunkSource().randomState().sampler();
        final var graph = LevelExtras.getNoiseGraph(ctx.getLevel());
        final var pos = MutableFunctionContext.from(ctx.getPos());
        ctx.sendMessage("weight: {}", TmpRoadUtils.getWeight(graph, sampler, pos));
    }

    @ModCommand(description = "Testing possible ways to smooth terrain around roads")
    void sphere(final CommandContextWrapper ctx, int radius, boolean showTop) {
        var pos = ctx.assertPlayer().getOnPos().mutable().below((radius / 2) + 1);
        for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                for (int y = pos.getY() - radius; y <= pos.getY() + (radius / 2); y++) {
                    final int o = y > pos.getY() ? y - pos.getY() : 0; // 2x
                    if (Utils.distance(x, y + o, z, pos.getX(), pos.getY(), pos.getZ()) < radius) {
                        ctx.getLevel().setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
        }
        final var top = showTop ? Blocks.LIGHT_GRAY_STAINED_GLASS : Blocks.AIR;
        pos = pos.above(radius * 3 / 2);
        for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                for (int y = pos.getY() - radius; y <= pos.getY(); y++) {
                    if (Utils.distance(x, y, z, pos.getX(), pos.getY(), pos.getZ()) < radius) {
                        ctx.getLevel().setBlock(new BlockPos(x, y, z), top.defaultBlockState(), 2);
                    }
                }
            }
        }
    }
}
