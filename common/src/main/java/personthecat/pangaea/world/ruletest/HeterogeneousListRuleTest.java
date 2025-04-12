package personthecat.pangaea.world.ruletest;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;
import personthecat.catlib.data.IdList;

import static personthecat.catlib.serialization.codec.CodecUtils.idList;

public class HeterogeneousListRuleTest extends RuleTest {
    public static final MapCodec<HeterogeneousListRuleTest> CODEC =
        idList(Registries.BLOCK).fieldOf("list").xmap(HeterogeneousListRuleTest::new, t -> t.list);
    public static final RuleTestType<HeterogeneousListRuleTest> TYPE = () -> CODEC;

    public final IdList<Block> list;

    public HeterogeneousListRuleTest(IdList<Block> list) {
        this.list = list;
    }

    @Override
    public boolean test(BlockState state, RandomSource rand) {
        return this.list.test(state.getBlockHolder());
    }

    @Override
    protected RuleTestType<HeterogeneousListRuleTest> getType() {
        return TYPE;
    }
}
