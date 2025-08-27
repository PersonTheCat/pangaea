package personthecat.pangaea.world.injector;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.FormattedException;

public class CleanupFailureException extends FormattedException {
    private static final String INJECTOR_CATEGORY = "pangaea.errorMenu.injectors";
    private static final String GENERIC_CLEANUP_ERROR = "pangaea.errorText.cleanupError";
    private final Component displayMessage;

    private CleanupFailureException(Component displayMessage, String message, Exception cause) {
        super(message, cause);
        this.displayMessage = displayMessage;
    }

    public static CleanupFailureException forInjector(ResourceKey<Injector> key, Exception cause) {
        final var displayMessage = Component.translatable(GENERIC_CLEANUP_ERROR, key.registry());
        return new CleanupFailureException(displayMessage, "Error running cleanup for injector: " + key, cause);
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
