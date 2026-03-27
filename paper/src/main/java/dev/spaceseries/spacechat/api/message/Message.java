package dev.spaceseries.spacechat.api.message;

import com.saicone.ezlib.Dependencies;
import com.saicone.ezlib.Dependency;
import com.saicone.ezlib.Repository;
import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Dependencies(value = {
        // Non-paper servers
        @Dependency(
                value = "net.kyori:adventure-api:4.26.1",
                repository = @Repository(url = "MavenCentral"),
                condition = {"paper=false", "adventure=true"},
                relocate = {
                        "net.kyori.adventure", "{package}.lib.adventure",
                        "net.kyori.examination", "{package}.lib.examination",
                        "net.kyori.option", "{package}.lib.option"
                }
        ),
        @Dependency(
                value = "net.kyori:adventure-platform-bukkit:4.4.1",
                repository = @Repository(url = "MavenCentral"),
                condition = {"paper=false", "adventure=true"},
                relocate = {
                        "net.kyori.adventure", "{package}.lib.adventure",
                        "net.kyori.examination", "{package}.lib.examination",
                        "net.kyori.option", "{package}.lib.option"
                }
        ),
        @Dependency(
                value = "net.kyori:adventure-text-minimessage:4.26.1",
                repository = @Repository(url = "MavenCentral"),
                condition = {"paper=false", "adventure=true"},
                relocate = {
                        "net.kyori.adventure", "{package}.lib.adventure",
                        "net.kyori.examination", "{package}.lib.examination",
                        "net.kyori.option", "{package}.lib.option"
                }
        )
})
public class Message {

    /**
     * Gets a message from a configuration section
     *
     * @param identifier identifier, also the path in the configuration at which the message is
     * @param adapter    adapter
     * @return message
     */
    public static Message fromConfigurationSection(String identifier, ConfigurationAdapter adapter) {
        // get lines list from adapter
        List<String> lines = adapter.getStringList(identifier, Collections.emptyList());
        return new Message(identifier, lines);
    }

    /**
     * Returns a builder using the identifier provided
     *
     * @param identifier identifier
     * @return builder
     */
    public static Builder builder(String identifier) {
        return new Builder(identifier);
    }

    /**
     * Returns a builder with no identifier
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The message identifier
     */
    private final String identifier;

    /**
     * The lines of the message
     * Will be combined into one component, split by the newline separator
     */
    private final List<String> lines;

    /**
     * Message
     *
     * @param identifier identifier
     * @param lines      lines
     */
    public Message(String identifier, List<String> lines) {
        this.identifier = identifier;
        this.lines = lines;
    }

    /**
     * Returns the message identifier
     *
     * @return identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Returns the lines
     *
     * @return lines
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * Parses the message into a component
     *
     * @return component
     */
    private Component parse() {
        // parse into component
        Component component = null;
        for (String line : lines) {
            if (component == null) {
                component = LegacyComponentSerializer.legacyAmpersand().deserialize(line);
            } else {
                component = component.appendNewline().append(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
        }

        return component;
    }

    /**
     * Replaces replacers inside the component
     *
     * @param component component
     * @param replacers replacers
     * @return component
     */
    private Component replace(Component component, String... replacers) {
        // create list of replacement pairs from array
        for (int i = 0; i < replacers.length; i += 2) {
            String replacer = replacers[i];
            String replacement = replacers[i + 1];

            component = component.replaceText((b) -> b.matchLiteral(replacer).replacement(replacement));
        }

        return component;
    }

    /**
     * Used as a proxy method to compile the entire message (parsing, replacing, etc) into a component
     *
     * @param replacers replacers
     * @return component
     */
    public Component compile(String... replacers) {
        return replace(parse(), replacers);
    }

    /**
     * Sends a message to a command sender
     *
     * @param commandSender command sender
     * @param replacers     replacers
     */
    public void message(CommandSender commandSender, String... replacers) {
        message(Collections.singletonList(commandSender), replacers);
    }

    /**
     * Sends a message to a 'list' of senders
     *
     * @param commandSenders command senders 'list'
     * @param replacers      replacers
     */
    public void message(Iterable<CommandSender> commandSenders, String... replacers) {
        Component output = compile(replacers);

        commandSenders.forEach(sender -> sender.sendMessage(output));
    }

    /**
     * Broadcasts a message to the entire server
     *
     * @param replacers replacers
     */
    public void broadcast(String... replacers) {
        Component output = compile(replacers);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(output);
        }
    }

    public static class Builder {

        /**
         * The message identifier
         */
        private final String identifier;

        /**
         * The lines of the message
         * Will be combined into one component, split by the newline separator
         */
        private final List<String> lines;

        /**
         * Builder
         *
         * @param identifier identifier
         */
        public Builder(String identifier) {
            this.identifier = identifier;
            this.lines = new ArrayList<>();
        }

        /**
         * Builder
         **/
        public Builder() {
            this(null);
        }

        /**
         * Returns the identifier
         *
         * @return identifier
         */
        public String getIdentifier() {
            return identifier;
        }

        /**
         * Adds a line
         *
         * @param line line
         * @return this
         */
        public Builder addLine(String line) {
            lines.add(line);
            return this;
        }

        /**
         * Returns the lines
         *
         * @return lines
         */
        public List<String> getLines() {
            return lines;
        }

        /**
         * Builds a builder into a message
         *
         * @return message
         */
        public Message build() {
            return new Message(this.identifier, this.lines);
        }
    }
}
