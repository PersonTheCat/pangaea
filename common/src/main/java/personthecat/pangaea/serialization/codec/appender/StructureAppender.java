package personthecat.pangaea.serialization.codec.appender;

import com.mojang.serialization.Codec;
import personthecat.pangaea.serialization.codec.StructuralCodec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class StructureAppender implements CodecAppender {
    public static final int PRIORITY = CaptureAppender.PRIORITY + 50;
    public static final Info<StructureAppender> INFO = StructureAppender::new;
    private final List<StructuralCodec.Structure<?>> structures = new ArrayList<>();
    private final Condition condition = new Condition();

    public <A> void addStructures(Collection<? extends StructuralCodec.Structure<? extends A>> structure) {
        this.structures.addAll(structure);
    }

    @Override
    public void addEncodeCondition(BooleanSupplier condition) {
        this.condition.addCondition(condition);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A> Codec<A> append(String typeKey, Codec<A> codec) {
        final var structures = (List<StructuralCodec.Structure<A>>) (Object) this.structures;
        if (!structures.isEmpty()) {
            return new StructuralCodec<>(structures, this.condition.get()).wrap(typeKey, codec);
        }
        return codec;
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public Info<StructureAppender> info() {
        return INFO;
    }
}
