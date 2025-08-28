package personthecat.pangaea.world.level;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.FormattedException;

public class ContextNotInstalledException extends FormattedException {
    private static final String CONTEXT_NOT_INSTALLED = "pangaea.errorText.contextNotInstalled";

    public ContextNotInstalledException(Exception cause) {
        super(cause);
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.translatable(CONTEXT_NOT_INSTALLED);
    }
}
