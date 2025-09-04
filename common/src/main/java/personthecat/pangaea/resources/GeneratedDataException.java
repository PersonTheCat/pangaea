package personthecat.pangaea.resources;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.linting.StackTraceLinter;
import personthecat.catlib.linting.SyntaxLinter;
import xjs.data.JsonFormat;
import xjs.data.JsonValue;

public class GeneratedDataException extends FormattedException {
    private static final String WORLD_PACK_CATEGORY = "pangaea.errorMenu.worldPacks";
    private static final String GENERATED_DATA_INVALID = "pangaea.errorText.invalidGeneratedData";
    private final ResourceKey<?> key;
    private final Object data;
    private final String error;

    public GeneratedDataException(ResourceKey<?> key, Object data, String error, Throwable cause) {
        super(cause);
        this.key = key;
        this.data = data;
        this.error = error;
    }

    @Override
    public void onErrorReceived(Logger log) {
        log.error("Error loading generated resource {}: {}", this.key, this.error);
    }

    @Override
    public @NotNull String getCategory() {
        return WORLD_PACK_CATEGORY;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.literal(this.getFormattedKey());
    }

    @Override
    public @NotNull Component getDetailMessage() {
        final MutableComponent message = Component.empty();
        message.append(Component.translatable(GENERATED_DATA_INVALID, this.getFormattedKey()));
        message.append("\n\nData:\n\n");
        message.append(this.getFormattedData());
        message.append("\n\n");
        message.append(StackTraceLinter.format(this.readStacktrace()));
        message.append("\n\n");
        return message;
    }

    private Component getFormattedData() {
        if (this.data instanceof JsonValue json) {
            return SyntaxLinter.DEFAULT_LINTER.lint(json.toString(JsonFormat.DJS_FORMATTED));
        }
        return Component.literal(this.data.toString());
    }

    private String getFormattedKey() {
        return "[" + this.key.registry() + "/" + this.key.location() + "]";
    }
}
