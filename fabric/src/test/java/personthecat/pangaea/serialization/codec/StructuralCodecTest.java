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
import personthecat.pangaea.serialization.codec.StructuralCodec.Structure;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.pangaea.test.TestUtils.assertError;
import static personthecat.pangaea.test.TestUtils.assertSuccess;
import static personthecat.pangaea.test.TestUtils.encode;
import static personthecat.pangaea.test.TestUtils.parse;

public final class StructuralCodecTest {
    private static final MapCodec<SayHello> SAY_HELLO_STRUCTURE =
        Codec.STRING.fieldOf("say_hello").xmap(SayHello::new, SayHello::name);
    private static final MapCodec<SayName> SAY_NAME_STRUCTURE =
        Codec.STRING.fieldOf("say_name").xmap(SayName::new, SayName::name);
    private static final MapCodec<SayAnything> SAY_ANYTHING_PREFIXED_STRUCTURE =
        SayAnything.createCodec("say_anything_prefixed", "show_prefix", true);
    private static final MapCodec<SayAnything> SAY_ANYTHING_STRUCTURE =
        SayAnything.createCodec("say_anything", "show_prefix");

    private static final List<Structure<TestData>> STRUCTURES = List.of(
        Structure.of(SAY_HELLO_STRUCTURE, SayHello.class),
        Structure.of(SAY_NAME_STRUCTURE, SayName.class),
        Structure.of(SAY_ANYTHING_STRUCTURE, d -> d instanceof SayAnything a && !a.showPrefix),
        Structure.<TestData>of(SAY_ANYTHING_PREFIXED_STRUCTURE, d -> d instanceof SayAnything a && a.showPrefix)
            .withRequiredFields("say_anything_prefixed")
    );

    private static final Codec<TestData> TEST_SUBJECT =
        new StructuralCodec<>(STRUCTURES).wrap(TestData.CODEC);

    @BeforeAll
    public static void setup() {
        TestData.register("say_hello", SayHello.CODEC);
        TestData.register("say_name", SayName.CODEC);
        TestData.register("say_anything", SayAnything.CODEC);
        TestData.register("no_structure", NoStructure.CODEC);
        TestData.freezeRegistry();
    }

    @Test
    public void decode_withoutStructure_parsesNormally() {
        final var result = parse(TEST_SUBJECT, "type: 'test:say_hello', name: 'Carl'");
        assertSuccess("Hello, Carl!", result.map(TestData::result));
    }

    @Test
    public void decode_withStructure_parsesStructure() {
        final var result = parse(TEST_SUBJECT, "say_hello: 'John'");
        assertSuccess("Hello, John!", result.map(TestData::result));
    }

    @Test
    public void decode_withStructure_andType_doesNotParseStructure() {
        final var result = parse(TEST_SUBJECT, "type: 'test:say_name', say_hello: 'Billy'");
        assertError(result, "No key name");
    }

    @Test
    public void decode_whenStructureHasOptionalFields_requiresAllFields() {
        final var result = parse(TEST_SUBJECT, "say_anything: 'Help!'");
        assertError(result, "No structural fields or type");
    }

    @Test
    public void decode_whenStructureHasOptionalFields_andExplicitRequiredFields_onlyRequiresRequiredFields() {
        final var result = parse(TEST_SUBJECT, "say_anything_prefixed: 'It worked!'");
        assertSuccess("text: It worked!", result.map(TestData::result));
    }

    @Test
    public void decode_whenStructureHasOptionalFields_andExplicitRequiredFields_canUseOptionalFields() {
        final var result = parse(TEST_SUBJECT, "say_anything_prefixed: 'It still works!', show_prefix: false");
        assertSuccess("It still works!", result.map(TestData::result));
    }

    @Test
    public void encode_forDataWithSimpleStructure_writesStructure() {
        final var result = encode(TEST_SUBJECT, new SayHello("Bob"));
        assertSuccess(Map.of("say_hello", "Bob"), result);
    }

    @Test
    public void encode_forDataWithoutStructure_writesNormally() {
        final var result = encode(TEST_SUBJECT, NoStructure.INSTANCE);
        assertSuccess(Map.of("type", "test:no_structure"), result);
    }

    @Test
    public void encode_forDataWithMultipleStructures_chooseFirstMatch() {
        // note: in this scenario, the data cannot be parsed again.
        // this is considered a fault of the structure itself.
        // ideally, the encode check should fail or the structure should be fixed.
        // it is NOT a goal of this API to handle this edge case.
        final var result = encode(TEST_SUBJECT, new SayAnything("No prefix", false));
        assertSuccess(Map.of("say_anything", "No prefix"), result);
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

    private record SayHello(String name) implements TestData {
        static final MapCodec<SayHello> CODEC =
            Codec.STRING.fieldOf("name").xmap(SayHello::new, SayHello::name);

        @Override
        public String result() {
            return "Hello, " + this.name + "!";
        }

        @Override
        public MapCodec<SayHello> codec() {
            return CODEC;
        }
    }

    private record SayName(String name) implements TestData {
        static final MapCodec<SayName> CODEC =
            Codec.STRING.fieldOf("name").xmap(SayName::new, SayName::name);

        @Override
        public String result() {
            return "Your name is " + this.name;
        }

        @Override
        public MapCodec<SayName> codec() {
            return CODEC;
        }
    }

    private record SayAnything(String text, boolean showPrefix) implements TestData {
        static final MapCodec<SayAnything> CODEC = createCodec("text", "prefix");

        @Override
        public String result() {
            return (this.showPrefix ? "text: " : "") + this.text;
        }

        @Override
        public MapCodec<SayAnything> codec() {
            return CODEC;
        }

        static MapCodec<SayAnything> createCodec(String textField, String prefixField) {
            return createCodec(textField, prefixField, false);
        }

        static MapCodec<SayAnything> createCodec(String textField, String prefixField, boolean defaultPrefix) {
            return codecOf(
                field(Codec.STRING, textField, SayAnything::text),
                defaulted(Codec.BOOL, prefixField, defaultPrefix, SayAnything::showPrefix),
                SayAnything::new
            );
        }
    }

    private enum NoStructure implements TestData {
        INSTANCE;

        static final MapCodec<NoStructure> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public String result() {
            return "You get what you get";
        }

        @Override
        public MapCodec<NoStructure> codec() {
            return CODEC;
        }
    }
}