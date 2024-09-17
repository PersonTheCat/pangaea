package personthecat.pangaea.serialization.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;
import personthecat.pangaea.config.Cfg;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

import static personthecat.catlib.serialization.codec.CodecUtils.codecOf;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleAny;
import static personthecat.catlib.serialization.codec.CodecUtils.simpleEither;
import static personthecat.catlib.serialization.codec.FieldDescriptor.field;
import static personthecat.catlib.serialization.codec.FieldDescriptor.defaulted;

public final class ColorCodecs {
    private static final Codec<Integer> CHANNEL_CODEC = Codec.intRange(0, 255);
    private static final Codec<Color> COLOR_LIST = CHANNEL_CODEC.listOf(3, 4)
        .xmap(ColorCodecs::fromChannels, ColorCodecs::toChannels);
    private static final Codec<Color> COLOR_OBJECT = codecOf(
        field(CHANNEL_CODEC, "red", Color::getRed),
        field(CHANNEL_CODEC, "green", Color::getGreen),
        field(CHANNEL_CODEC, "blue", Color::getBlue),
        defaulted(CHANNEL_CODEC, "alpha", 255, Color::getAlpha),
        Color::new
    ).codec();
    private static final Codec<Color> COLOR_CODE = Codec.INT.xmap(Color::new, Color::getRGB);
    private static final Codec<Color> COLOR_NAME = ChatFormatting.CODEC
        .flatXmap(ColorCodecs::fromChat, ColorCodecs::toChat);
    public static final Codec<Color> COLOR = simpleAny(COLOR_LIST, COLOR_OBJECT, COLOR_CODE, COLOR_NAME)
        .withEncoder(color -> toChat(color).isSuccess() ? COLOR_NAME : COLOR_OBJECT);
    public static final Codec<Integer> COLOR_INT = simpleEither(Codec.INT, COLOR.map(Color::getRGB))
        .withEncoder(i -> Cfg.encodeReadableColors() ? COLOR.comap(Color::new) : Codec.INT);

    private ColorCodecs() {}

    private static Color fromChannels(List<Integer> channels) {
        if (channels.size() == 3) {
            return new Color(channels.get(0), channels.get(1), channels.get(2));
        }
        return new Color(channels.get(0), channels.get(1), channels.get(2), channels.get(3));
    }

    private static List<Integer> toChannels(Color color) {
        if (color.getAlpha() == 255) {
            return List.of(color.getRed(), color.getGreen(), color.getBlue());
        }
        return List.of(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    private static DataResult<Color> fromChat(ChatFormatting chat) {
        if (chat.isColor()) {
            final var color = Objects.requireNonNull(chat.getColor(), "Invalid chat format");
            return DataResult.success(new Color(color, true));
        }
        return DataResult.error(() -> "Not a color: " + chat.getName());
    }

    private static DataResult<ChatFormatting> toChat(Color color) {
        final var rgb = color.getRGB();
        for (final var chat : ChatFormatting.values()) {
            if (chat.getColor() != null && chat.getColor() == rgb) {
                return DataResult.success(chat);
            }
        }
        return DataResult.error(() -> "No matching format: " + color);
    }
}
