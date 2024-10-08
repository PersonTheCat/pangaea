package personthecat.pangaea.world.injector;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.FormattedException;

public class InjectionWarningException extends FormattedException {
    private static final String INJECTOR_CATEGORY = "pangaea.errorMenu.injectors";
    private static final String NOT_A_NOISE_SOURCE = "pangaea.errorText.notANoiseSource";
    private final Component displayMessage;

    private InjectionWarningException(Component displayMessage, String message) {
        super(message);
        this.displayMessage = displayMessage;
    }

    public static InjectionWarningException incompatibleBiomeSource(ResourceKey<LevelStem> key) {
        final var displayMessage = Component.translatable(NOT_A_NOISE_SOURCE, key.location());
        return new InjectionWarningException(displayMessage, "Not a noise biome source: " + key.location());
    }

    @Override
    public @NotNull String getCategory() {
        return INJECTOR_CATEGORY;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return this.displayMessage;
    }
}
