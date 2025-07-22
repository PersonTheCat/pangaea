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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static personthecat.pangaea.test.TestUtils.assertSuccess;
import static personthecat.pangaea.test.TestUtils.encode;
import static personthecat.pangaea.test.TestUtils.parse;

public final class PatternCodecTest {
    private static final Codec<StringData> STRING_PATTERN =
        Codec.STRING.xmap(StringData::new, StringData::s);
    private static final Codec<IntData> INT_PATTERN =
        Codec.INT.xmap(IntData::new, IntData::i);

    private static final List<Pattern<TestData>> PATTERNS = List.of(
        Pattern.of(STRING_PATTERN, StringData.class),
        Pattern.of(INT_PATTERN, IntData.class)
    );

    private static final Codec<TestData> TEST_SUBJECT =
        new PatternCodec<>(PATTERNS).wrap(TestData.CODEC);

    @BeforeAll
    public static void setup() {
        TestData.register("string", StringData.CODEC);
        TestData.register("int", IntData.CODEC);
        TestData.register("no_pattern", NoPatternData.CODEC);
        TestData.freezeRegistry();
    }

    @Test
    public void decode_withoutPattern_parsesNormally() {
        final var result = parse(TEST_SUBJECT, "type: 'test:string', string: 'value'");
        assertSuccess("string: value", result.map(TestData::result));
    }

    @Test
    public void decode_withPattern_parsesPattern() {
        final var result = parse(TEST_SUBJECT, "'another value'");
        assertSuccess("string: another value", result.map(TestData::result));
    }

    @Test // running out of ideas on this one
    public void decode_withSomeOtherPattern_parsesPattern() {
        final var result = parse(TEST_SUBJECT, "123");
        assertSuccess("int: 123", result.map(TestData::result));
    }

    @Test
    public void encode_forDataWithPattern_writesPattern() {
        final var result = encode(TEST_SUBJECT, new IntData(456));
        assertSuccess(456, result);
    }

    @Test
    public void encode_forDataWithoutPattern_writesNormally() {
        final var result = encode(TEST_SUBJECT, NoPatternData.INSTANCE);
        assertSuccess(Map.of("type", "test:no_pattern"), result);
    }

    private interface TestData {
        Registry<MapCodec<? extends TestData>> REGISTRY = createRegistry();
        Codec<TestData> CODEC = createCodec();

        String result();
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

    private record StringData(String s) implements TestData {
        static final MapCodec<StringData> CODEC =
            Codec.STRING.fieldOf("string").xmap(StringData::new, StringData::s);

        @Override
        public String result() {
            return "string: " + this.s;
        }

        @Override
        public MapCodec<StringData> codec() {
            return CODEC;
        }
    }

    private record IntData(int i) implements TestData {
        static final MapCodec<IntData> CODEC =
            Codec.INT.fieldOf("int").xmap(IntData::new, IntData::i);

        @Override
        public String result() {
            return "int: " + this.i;
        }

        @Override
        public MapCodec<IntData> codec() {
            return CODEC;
        }
    }

    private enum NoPatternData implements TestData {
        INSTANCE;

        static final MapCodec<NoPatternData> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public String result() {
            return "You get what you get";
        }

        @Override
        public MapCodec<NoPatternData> codec() {
            return CODEC;
        }
    }
}