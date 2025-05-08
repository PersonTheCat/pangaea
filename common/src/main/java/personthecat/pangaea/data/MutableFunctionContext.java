package personthecat.pangaea.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;
import net.minecraft.world.phys.Vec3;

public class MutableFunctionContext implements FunctionContext {
    private int x;
    private int y;
    private int z;

    public MutableFunctionContext() {
        this(0, 0, 0);
    }

    public MutableFunctionContext(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static MutableFunctionContext from(FunctionContext ctx) {
        if (ctx instanceof MutableFunctionContext mutable) {
            return mutable;
        }
        return new MutableFunctionContext(ctx.blockX(), ctx.blockY(), ctx.blockZ());
    }

    public static MutableFunctionContext from(BlockPos pos) {
        return new MutableFunctionContext(pos.getX(), pos.getY(), pos.getZ());
    }

    public static MutableFunctionContext from(ChunkPos pos) {
        return new MutableFunctionContext(pos.getMiddleBlockX(), 0, pos.getMiddleBlockZ());
    }

    public static MutableFunctionContext from(Vec3 vec) {
        return new MutableFunctionContext((int) vec.x, (int) vec.y, (int) vec.z);
    }

    public MutableFunctionContext set(BlockPos pos) {
        return this.at(pos.getX(), pos.getY(), pos.getZ());
    }

    public MutableFunctionContext set(ChunkPos pos) {
        return this.at(pos.getMiddleBlockX(), pos.getMiddleBlockZ());
    }

    public MutableFunctionContext set(FunctionContext ctx) {
        return this.at(ctx.blockX(), ctx.blockY(), ctx.blockZ());
    }

    public MutableFunctionContext at(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public MutableFunctionContext at(int x, int z) {
        this.x = x;
        this.z = z;
        return this;
    }

    public MutableFunctionContext at(int y) {
        this.y = y;
        return this;
    }

    public MutableFunctionContext up(int amount) {
        this.y += amount;
        return this;
    }

    public MutableFunctionContext down(int amount) {
        this.y -= amount;
        return this;
    }

    public MutableFunctionContext north(int amount) {
        this.z -= amount;
        return this;
    }

    public MutableFunctionContext south(int amount) {
        this.z += amount;
        return this;
    }

    public MutableFunctionContext east(int amount) {
        this.x += amount;
        return this;
    }

    public MutableFunctionContext west(int amount) {
        this.x -= amount;
        return this;
    }

    @Override
    public int blockX() {
        return this.x;
    }

    @Override
    public int blockY() {
        return this.y;
    }

    @Override
    public int blockZ() {
        return this.z;
    }
}
