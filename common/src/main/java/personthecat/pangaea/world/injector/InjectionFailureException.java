package personthecat.pangaea.world.injector;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.FormattedException;
import personthecat.catlib.linting.StackTraceLinter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class InjectionFailureException extends FormattedException {
    private static final String INJECTOR_CATEGORY = "pangaea.errorMenu.injectors";
    private static final String INJECTION_FAILURES = "pangaea.errorText.injectionFailures";
    private final Map<ResourceKey<Injector>, Throwable> failures;

    public InjectionFailureException(Map<ResourceKey<Injector>, Throwable> failures) {
        super("Could not apply injectors: " + failures);
        this.failures = failures;
    }

    @Override
    public void onErrorReceived(final Logger log) {
        this.failures.forEach((id, error) -> log.error(id.location(), error));
    }

    @Override
    public @NotNull String getCategory() {
        return INJECTOR_CATEGORY;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.translatable(INJECTION_FAILURES, this.failures.size());
    }

    @Override
    public @NotNull Component getDetailMessage() {
        final MutableComponent message = Component.empty();
        this.failures.forEach((id, error) -> {
            message.append(Component.literal(id.location().toString()).setStyle(Style.EMPTY.withBold(true)));
            message.append(":\n\n");
            message.append(StackTraceLinter.format(createStackTraceText(error)));
            message.append("\n\n");
        });
        return message;
    }

    private static String createStackTraceText(Throwable t) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        final Throwable cause = t.getCause();
        (cause != null ? cause : t).printStackTrace(pw);
        return sw.toString().replace("\t", "    ").replace("\r", "");
    }
}
