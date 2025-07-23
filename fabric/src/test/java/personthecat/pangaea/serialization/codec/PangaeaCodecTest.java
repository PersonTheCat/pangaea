package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import personthecat.pangaea.serialization.codec.PatternCodec.Pattern;
import personthecat.pangaea.serialization.codec.StructuralCodec.Structure;

import java.util.function.Function;

import static personthecat.pangaea.test.TestUtils.assertSuccess;
import static personthecat.pangaea.test.TestUtils.parse;

// most responsibility is held by CodecAppenders and custom codec APIs
public final class PangaeaCodecTest {

    private static final Codec<Constant> CONSTANT_PATTERN =
        Codec.INT.xmap(Constant::new, Constant::value);
    private static final MapCodec<TimesTwo> TIMES_TWO_STRUCTURE =
        Codec.INT.fieldOf("times_two").xmap(TimesTwo::new, TimesTwo::value);

    private static final Codec<TestData> TEST_SUBJECT =
        PangaeaCodec.create(TestData.CODEC);

    @BeforeAll
    public static void setup() {
        TestData.register("constant", Constant.CODEC);
        TestData.register("time_two", TimesTwo.CODEC);
        TestData.register("regular_data", RegularData.CODEC);
        TestData.freezeRegistry();

        PangaeaCodec.get(TestData.class)
            .addPatterns(Pattern.of(CONSTANT_PATTERN, Constant.class))
            .addStructures(Structure.of(TIMES_TWO_STRUCTURE, TimesTwo.class));
    }

    @Test
    public void decode_withMatchingPattern_parsesPattern() {
        final var result = parse(TEST_SUBJECT, "123");
        assertSuccess(123, result.map(TestData::result));
    }

    @Test
    public void decode_withMatchingStructure_parsesStructure() {
        final var result = parse(TEST_SUBJECT, "times_two: 10");
        assertSuccess(20, result.map(TestData::result));
    }

    @Test
    public void decode_withRegularData_parsesData() {
        final var result = parse(TEST_SUBJECT, "type: 'test:regular_data'");
        assertSuccess(-1, result.map(TestData::result));
    }

    private interface TestData {
        Registry<MapCodec<? extends TestData>> REGISTRY = createRegistry();
        Codec<TestData> CODEC = createCodec();

        int result();
        MapCodec<? extends TestData> codec();

        static void register(String id, MapCodec<? extends TestData> codec) {
            Registry.register(REGISTRY, id(id), codec);
        }

        static void freezeRegistry() {
            REGISTRY.freeze();
        }

        private static Registry<MapCodec<? extends TestData>> createRegistry() {
            return new MappedRegistry<>(key(), Lifecycle.experimental());
        }

        private static Codec<TestData> createCodec() {
            return REGISTRY.byNameCodec().dispatch(TestData::codec, Function.identity());
        }

        private static ResourceKey<Registry<MapCodec<? extends TestData>>> key() {
            return ResourceKey.createRegistryKey(new ResourceLocation("test", "test"));
        }

        private static ResourceKey<MapCodec<? extends TestData>> id(String path) {
            return ResourceKey.create(key(), new ResourceLocation("test", path));
        }
    }

    private record Constant(int value) implements TestData {
        static final MapCodec<Constant> CODEC =
            Codec.INT.fieldOf("value").xmap(Constant::new, Constant::value);

        @Override
        public int result() {
            return this.value;
        }

        @Override
        public MapCodec<Constant> codec() {
            return CODEC;
        }
    }

    private record TimesTwo(int value) implements TestData {
        static final MapCodec<TimesTwo> CODEC =
            Codec.INT.fieldOf("value").xmap(TimesTwo::new, TimesTwo::value);

        @Override
        public int result() {
            return this.value * 2;
        }

        @Override
        public MapCodec<TimesTwo> codec() {
            return CODEC;
        }
    }

    private enum RegularData implements TestData {
        INSTANCE;

        static final MapCodec<RegularData> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public int result() {
            return -1;
        }

        @Override
        public MapCodec<RegularData> codec() {
            return CODEC;
        }
    }
}
