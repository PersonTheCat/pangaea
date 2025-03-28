package personthecat.pangaea.world.biome;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.FormattedException;

import java.util.List;

public class UnboundBiomesException extends FormattedException {
    private static final String BIOME_CATEGORY = "pangaea.errorMenu.biomes";
    private static final String UNBOUND_BIOMES = "pangaea.errorText.unboundBiomes";
    private static final String UNBOUND_BIOMES_DETAILS_1 = "pangaea.errorText.unboundBiomes.details1";
    private static final String UNBOUND_BIOMES_DETAILS_2 = "pangaea.errorText.unboundBiomes.details2";
    private final List<String> unboundBiomes;

    public UnboundBiomesException(List<String> unboundBiomes) {
        super("The following biomes were not bound in your layout: " + unboundBiomes);
        this.unboundBiomes = unboundBiomes;
    }

    @Override
    public @NotNull String getCategory() {
        return BIOME_CATEGORY;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.translatable(UNBOUND_BIOMES, this.unboundBiomes.size());
    }

    @Override
    public @NotNull Component getDetailMessage() {
        final MutableComponent message = Component.empty();
        message.append(Component.translatable(UNBOUND_BIOMES_DETAILS_1));
        this.unboundBiomes.forEach(id -> message.append("\n * ").append(id));
        message.append("\n");
        message.append(Component.translatable(UNBOUND_BIOMES_DETAILS_2));
        return message;
    }
}
