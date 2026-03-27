package dev.spaceseries.spacechat.builder.live;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import dev.spaceseries.spacechat.model.formatting.Extra;
import dev.spaceseries.spacechat.model.formatting.Format;
import dev.spaceseries.spacechat.model.formatting.ParsedFormat;
import dev.spaceseries.spacechat.model.formatting.parsed.ConditionalParsedFormat;
import dev.spaceseries.spacechat.model.formatting.parsed.ParsedFormatPart;
import dev.spaceseries.spacechat.model.formatting.parsed.SimpleParsedFormat;
import dev.spaceseries.spacechat.parser.MessageParser;
import dev.spaceseries.spacechat.replacer.AmpersandReplacer;
import dev.spaceseries.spacechat.replacer.SectionReplacer;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class NormalLiveChatFormatBuilder extends LiveChatFormatBuilder {

    /**
     * Ampersand replacer
     */
    private static final AmpersandReplacer AMPERSAND_REPLACER = new AmpersandReplacer();

    /**
     * Section replacer
     */
    private static final SectionReplacer SECTION_REPLACER = new SectionReplacer();

    public NormalLiveChatFormatBuilder(SpaceChatPlugin plugin) {
        super(plugin);
    }

    /**
     * Builds an array of baseComponents from a message, player, and format
     *
     * @param player      the player that send the message
     * @param message     the message as component
     * @param format      the format to parse the provided player and message
     * @param conditional true if the builder accept conditional formats
     * @return            a parsed format using provided arguments
     */
    public ParsedFormat build(Player player, Component message, Format format, boolean conditional) {
        if (conditional && format.isConditional()) {
            final List<ParsedFormatPart> parts = new ArrayList<>();
            build(player, message, format, false, (textComponent, lineComponent, lineProtocol) -> {
                parts.add(new ParsedFormatPart(
                        textComponent == null ? null : Component.text().append(textComponent).build(),
                        lineComponent == null ? null : Component.text().append(lineComponent).build(),
                        lineProtocol
                ));
            });
            return new ConditionalParsedFormat(parts);
        } else {
            final ComponentBuilder<TextComponent, TextComponent.Builder> partComponentBuilder = Component.text();
            build(player, message, format, true, (textComponent, lineComponent, lineProtocol) -> {
                if (lineComponent != null) {
                    partComponentBuilder.append(lineComponent);
                } else {
                    partComponentBuilder.append(textComponent);
                }
            });
            return new SimpleParsedFormat(partComponentBuilder.build());
        }
    }

    private void build(Player player, Component message, Format format, boolean line, FormatPartConsumer consumer) {
        // loop through format parts
        format.getFormatParts().forEach(formatPart -> {
            // create component builder
            Component parsedMiniMessage = null;
            // if the part has "line", it is a SINGLE MiniMessage...in that case, just parse & return (continues to next part if exists, which it shouldn't)
            if (formatPart.getLine() != null) {
                // replace placeholders
                String mmWithPlaceholdersReplaced = SECTION_REPLACER.apply(PlaceholderAPI.setPlaceholders(player, AMPERSAND_REPLACER.apply(formatPart.getLine(), player)), player);

                // parse message
                Component parsedMessage = new MessageParser(plugin).parse(player, message);

                parsedMessage = escapeColors(parsedMessage);

                // parse miniMessage
                parsedMiniMessage = MiniMessage.miniMessage().deserialize(mmWithPlaceholdersReplaced.replace("<chat_message>", MiniMessage.miniMessage().serialize(parsedMessage)));

                if (line || formatPart.getText() == null) {
                    consumer.accept(null, parsedMiniMessage, -1);
                    return;
                }
            }

            String text = formatPart.getText();

            // basically what I am doing here is converting & -> section, then replacing placeholders, then section -> &
            // this just bypasses PAPI's hacky way of coloring text which shouldn't even be implemented...
            text = SECTION_REPLACER.apply(PlaceholderAPI.setPlaceholders(player, AMPERSAND_REPLACER.apply(text, player)), player);

            // build text from legacy (and replace <chat_message> with the actual message)
            // and check permissions for chat colors
            Component parsedText;

            parsedText = LegacyComponentSerializer.legacyAmpersand().deserialize(text)
                    .replaceText((b) -> b.matchLiteral("<chat_message>").replacement(escapeColors(message)));

            // parse message
            parsedText = new MessageParser(plugin).parse(player, parsedText);

            /* Retaining events for MULTIPLE components */

            // parse extra (if applicable)
            if (formatPart.getExtra() != null) {
                Extra extra = formatPart.getExtra();

                // if contains click action
                if (extra.getClickAction() != null) {
                    // apply
                    parsedText = parsedText.clickEvent(extra.getClickAction().toClickEvent(player));
                }

                // if contains hover action
                if (extra.getHoverAction() != null) {
                    // apply
                    parsedText = parsedText.hoverEvent(extra.getHoverAction().toHoverEvent(player));
                }
            }

            consumer.accept(parsedText, parsedMiniMessage, formatPart.getLineProtocol());
        });
    }
    
    private Component escapeColors(Component message) {
        for (String regex : SpaceChatConfigKeys.CHAT_ESCAPE_COLOR.get(plugin.getSpaceChatConfig().getAdapter())) {
            message = message.replaceText(builder -> builder.match(Pattern.compile(regex)).replacement((result ->
                    result.colorIfAbsent(NamedTextColor.WHITE)
            )));
        }
        return message;
    }

    @FunctionalInterface
    public interface FormatPartConsumer {
        void accept(Component textComponent, Component lineComponent, int lineProtocol);
    }
}
