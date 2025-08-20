package personthecat.pangaea.mixin.extras;

import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import personthecat.pangaea.extras.ChunkAccessExtras;
import personthecat.pangaea.util.Utils;

@Mixin(ChunkAccess.class)
public class ChunkAccessMixin implements ChunkAccessExtras {

    @Unique
    private final byte[] pangaea$approximateHeights = new byte[25];

    @Override
    public void pangaea$setApproximateHeight(int x, int z, int h) {
        this.pangaea$approximateHeights[pangaea$heightIndex(x, z)] = (byte) h;
    }

    @Override
    public int pangaea$getApproximateHeight(int x, int z) {
        return this.pangaea$approximateHeights[pangaea$heightIndex(x, z)] & 0xFF;
    }

    @Override
    public int pangaea$getInterpolatedHeight(int rx, int rz) {
        final int lx = pangaea$lowerQuarter(rx);
        final int lz = pangaea$lowerQuarter(rz);

        final int lXlZ = this.pangaea$getApproximateHeight(lx, lz);
        if (rx == lx && rz == lz) {
            return lXlZ;
        }
        final int uXuZ = this.pangaea$getApproximateHeight(lx + 4, lz + 4);
        final int lXuZ = this.pangaea$getApproximateHeight(lx, lz + 4);
        final int uXlZ = this.pangaea$getApproximateHeight(lx + 4, lz);

        final float tX = (rx - lx) / (lx == 12 ? 3F : 4F); // distance is only 3 at upper edge
        final float tZ = (rz - lz) / (lx == 12 ? 3F : 4F);

        final float l = Utils.lerp(lXlZ, lXuZ, tX);
        final float r = Utils.lerp(uXlZ, uXuZ, tX);

        return (int) Utils.lerp(l, r, tZ);
    }

    @Unique
    private static int pangaea$heightIndex(int x, int z) {
        if (x == 15) x = 16; // adjust edges for alignment
        if (z == 15) z = 16;
        return x / 4 * 5 + z / 4;
    }

    @Unique
    private static int pangaea$lowerQuarter(int i) {
        return i >> 2 << 2;
    }
}
