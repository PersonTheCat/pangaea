package personthecat.pangaea.world.injector;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import personthecat.catlib.exception.FormattedException;

import java.util.List;
import java.util.Map;

public class UnmetDependenciesException extends FormattedException {
    private static final String INJECTOR_CATEGORY = "pangaea.errorMenu.injectors";
    private static final String UNMET_DEPENDENCIES = "pangaea.errorText.unmetDependencies";
    private static final String UNMET_DEPENDENCIES_DETAILS = "pangaea.errorText.unmetDependencies.details";
    private final Map<ResourceLocation, List<ResourceLocation>> unmetDependencies;

    public UnmetDependenciesException(Map<ResourceLocation, List<ResourceLocation>> unmetDependencies) {
        super("The following dependencies are unmet: " + unmetDependencies);
        this.unmetDependencies = unmetDependencies;
    }

    @Override
    public @NotNull String getCategory() {
        return INJECTOR_CATEGORY;
    }

    @Override
    public @NotNull Component getDisplayMessage() {
        return Component.translatable(UNMET_DEPENDENCIES, this.unmetDependencies.size());
    }

    @Override
    public @NotNull Component getDetailMessage() {
        final MutableComponent message = Component.empty();
        message.append(Component.translatable(UNMET_DEPENDENCIES_DETAILS));

        this.unmetDependencies.forEach((id, list) -> {
            message.append("\n");
            message.append(Component.literal(id.toString()).setStyle(Style.EMPTY.withBold(true)));
            message.append(": ");
            list.forEach(dependency -> {
                message.append("\n * ");
                message.append(dependency.toString());
            });
        });
        return message;
    }
}
