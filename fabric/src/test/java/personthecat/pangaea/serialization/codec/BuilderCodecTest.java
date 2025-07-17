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
import personthecat.catlib.serialization.codec.FieldDescriptor;
import personthecat.pangaea.serialization.codec.BuilderCodec.BuilderField;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.pangaea.test.TestUtils.assertSuccess;
import static personthecat.pangaea.test.TestUtils.encode;
import static personthecat.pangaea.test.TestUtils.parse;

public final class BuilderCodecTest {
    private static final List<BuilderField<TestAppender, ?, ?>> FIELDS = List.of(
        BuilderField.of(TestAppender.class, PrefixAppender.class)
            .parsingRequired(Codec.STRING, "prefix")
            .wrap((p, a) -> new PrefixAppender(p))
            .unwrap(a -> null, PrefixAppender::prefix),
        BuilderField.of(TestAppender.class, StringAppender.class)
            .parsing(Codec.STRING, "add_s")
            .wrap((s, a) -> new StringAppender(a, s))
            .unwrap(StringAppender::next, StringAppender::s),
        BuilderField.of(TestAppender.class, IntAppender.class)
            .parsing(Codec.INT, "add_i")
            .wrap((i, a) -> new IntAppender(a, i))
            .unwrap(IntAppender::next, IntAppender::i));
    private static final Codec<TestAppender> BUILDER_AS_DEFAULT =
        new BuilderCodec<>(FIELDS).wrap(TestAppender.CODEC);
    private static final Codec<TestAppender> BUILDER_AS_UNION =
        new BuilderCodec<>(FIELDS).asUnionOf(TestAppender.CODEC);

    @BeforeAll
    public static void setup() {
        Registry.register(TestAppender.REGISTRY, TestAppender.id("string"), StringAppender.CODEC);
        Registry.register(TestAppender.REGISTRY, TestAppender.id("int"), IntAppender.CODEC);
        Registry.register(TestAppender.REGISTRY, TestAppender.id("prefix"), PrefixAppender.CODEC);
        Registry.register(TestAppender.REGISTRY, TestAppender.id("non_buildable"), NonBuildableAppender.CODEC);
        TestAppender.REGISTRY.freeze();
    }

    @Test
    public void decode_withoutUnion_parsesSingleField() {
        final var result = parse(BUILDER_AS_DEFAULT, "prefix: 'abc'");
        assertSuccess(new PrefixAppender("abc"), result);
    }

    @Test
    public void decode_withoutUnion_parsesMultipleFields() {
        final var result = parse(BUILDER_AS_DEFAULT, "prefix: 'abc', add_s: 'xyz', add_i: 123");
        assertSuccess(new IntAppender(new StringAppender(new PrefixAppender("abc"), "xyz"), 123), result);
    }

    @Test
    public void decode_withUnion_parsesUnionOnly() {
        final var result = parse(BUILDER_AS_UNION, "type: 'test:prefix', prefix: 'abc'");
        assertSuccess(new PrefixAppender("abc"), result);
    }

    @Test
    public void decode_withUnion_parsesUnion_andSingleField() {
        final var result = parse(BUILDER_AS_UNION, "type: 'test:prefix', prefix: 'abc', add_s: 'xyz'");
        assertSuccess(new StringAppender(new PrefixAppender("abc"), "xyz"), result);
    }

    @Test
    public void decode_withUnion_parsesUnion_andMultipleFields() {
        final var result = parse(BUILDER_AS_UNION, "type: 'test:prefix', prefix: 'abc', add_s: 'xyz', add_i: 123");
        assertSuccess(new IntAppender(new StringAppender(new PrefixAppender("abc"), "xyz"), 123), result);
    }

    @Test
    public void encode_withoutUnion_writesSingleField() {
        final var result = encode(BUILDER_AS_DEFAULT, new PrefixAppender("abc"));
        assertSuccess(Map.of("prefix", "abc"), result);
    }

    @Test
    public void encode_withoutUnion_writesMultipleFields() {
        final var result = encode(BUILDER_AS_DEFAULT, new IntAppender(new StringAppender(new PrefixAppender("abc"), "xyz"), 123));
        assertSuccess(Map.of("add_i", 123, "add_s", "xyz", "prefix", "abc"), result);
    }

    @Test
    public void encode_withUnion_encodesSingleField() {
        final var result = encode(BUILDER_AS_UNION, new PrefixAppender("abc"));
        assertSuccess(Map.of("prefix", "abc"), result);
    }

    @Test
    public void encode_withUnion_encodesMultipleFields() {
        final var result = encode(BUILDER_AS_UNION, new IntAppender(new StringAppender(new PrefixAppender("abc"), "xyz"), 123));
        assertSuccess(Map.of("add_i", 123, "add_s", "xyz", "prefix", "abc"), result);
    }

    @Test
    public void encode_withUnion_andSomeNonBuildableFields_encodesMixture() {
        final var result = encode(BUILDER_AS_UNION, new StringAppender(new NonBuildableAppender(true), "xyz"));
        assertSuccess(Map.of("type", "test:non_buildable", "b", true, "add_s", "xyz"), result);
    }

    private interface TestAppender {
        Registry<MapCodec<? extends TestAppender>> REGISTRY = createRegistry();
        Codec<TestAppender> CODEC = createCodec();

        String result();
        MapCodec<? extends TestAppender> codec();

        private static Registry<MapCodec<? extends TestAppender>> createRegistry() {
            return new MappedRegistry<>(key(), Lifecycle.experimental());
        }

        private static Codec<TestAppender> createCodec() {
            return REGISTRY.byNameCodec().dispatch(TestAppender::codec, Function.identity());
        }

        private static ResourceKey<Registry<MapCodec<? extends TestAppender>>> key() {
            return ResourceKey.createRegistryKey(new ResourceLocation("test", "test"));
        }

        private static ResourceKey<MapCodec<? extends TestAppender>> id(String path) {
            return ResourceKey.create(key(), new ResourceLocation("test", path));
        }
    }

    private record StringAppender(TestAppender next, String s) implements TestAppender {
        static final MapCodec<StringAppender> CODEC = codecOf(
            FieldDescriptor.field(TestAppender.CODEC, "next", StringAppender::next),
            FieldDescriptor.field(Codec.STRING, "s", StringAppender::s),
            StringAppender::new
        );

        @Override
        public String result() {
            return this.next.result() + this.s;
        }

        @Override
        public MapCodec<StringAppender> codec() {
            return CODEC;
        }
    }

    private record IntAppender(TestAppender next, int i) implements TestAppender {
        static final MapCodec<IntAppender> CODEC = codecOf(
            FieldDescriptor.field(TestAppender.CODEC, "next", IntAppender::next),
            FieldDescriptor.field(Codec.INT, "i", IntAppender::i),
            IntAppender::new
        );

        @Override
        public String result() {
            return this.next.result() + this.i;
        }

        @Override
        public MapCodec<IntAppender> codec() {
            return CODEC;
        }
    }

    private record PrefixAppender(String prefix) implements TestAppender {
        static final MapCodec<PrefixAppender> CODEC =
            Codec.STRING.fieldOf("prefix").xmap(PrefixAppender::new, PrefixAppender::prefix);

        @Override
        public String result() {
            return this.prefix;
        }

        @Override
        public MapCodec<PrefixAppender> codec() {
            return CODEC;
        }
    }

    private record NonBuildableAppender(boolean b) implements TestAppender {
        static final MapCodec<NonBuildableAppender> CODEC =
            Codec.BOOL.fieldOf("b").xmap(NonBuildableAppender::new, NonBuildableAppender::b);

        @Override
        public String result() {
            return String.valueOf(this.b);
        }

        @Override
        public MapCodec<? extends TestAppender> codec() {
            return CODEC;
        }
    }
}
