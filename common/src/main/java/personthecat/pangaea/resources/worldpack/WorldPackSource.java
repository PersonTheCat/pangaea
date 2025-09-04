package personthecat.pangaea.resources.worldpack;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackSource;

import java.util.function.UnaryOperator;

public final class WorldPackSource {
    public static final PackSource REQUIRED = PackSource.create(decorateWithSource("pangaea.pack.source.required"), true);
    public static final PackSource OPTIONAL = PackSource.create(decorateWithSource("pangaea.pack.source.optional"), false);

    private WorldPackSource() {}

    public static PackSource get(boolean required) {
        return required ? REQUIRED : OPTIONAL;
    }

    private static UnaryOperator<Component> decorateWithSource(String sourceKey) {
        final var source = Component.translatable(sourceKey);
        return name -> Component.translatable("pack.nameAndSource", name, source).withStyle(ChatFormatting.GRAY);
    }
}
