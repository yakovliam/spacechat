package dev.spaceseries.spacechat.chat;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import dev.spaceseries.spacechat.loader.ChatFormatLoader;
import dev.spaceseries.spacechat.loader.FormatLoader;
import dev.spaceseries.spacechat.loader.FormatManager;
import dev.spaceseries.spacechat.model.formatting.ChatFormat;
import dev.spaceseries.spacechat.model.formatting.FormatType;
import dev.spaceseries.spacechat.sync.ServerSyncServiceManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

import java.util.Comparator;
import java.util.Locale;

public class ChatFormatManager extends FormatManager<ChatFormat> {

    /**
     * The format loader
     */
    private final FormatLoader<ChatFormat> chatFormatFormatLoader;

    /**
     * Server sync service manager
     */
    private final ServerSyncServiceManager serverSyncServiceManager;

    /**
     * Initializes
     */
    public ChatFormatManager(SpaceChatPlugin plugin) {
        super(plugin);
        this.serverSyncServiceManager = plugin.getServerSyncServiceManager();

        // create format manager
        this.chatFormatFormatLoader = new ChatFormatLoader(plugin, FormatType.CHAT.getSectionKey().toLowerCase(Locale.ROOT));

        // load
        this.loadFormats();
    }

    /**
     * Loads formats
     */
    public void loadFormats() {
        chatFormatFormatLoader.load(this);
    }

    /**
     * Sends a chat message using the applicable format
     *
     * @param event   The event
     * @param message The message
     */
    public void send(PlayerEvent event, Component message) {
        ChatManager chatManager = plugin.getChatManager();

        // get player
        Player player = event.getPlayer();

        // get applicable format
        ChatFormat applicableFormat = getAll()
                .values()
                .stream()
                .filter(format -> player.hasPermission(format.getPermission()) || format.getHandle().equals("default")) // player has permission OR the format is default
                .max(Comparator.comparing(ChatFormat::getPriority))
                .orElse(getAll().values().stream()
                        .findFirst()
                        .orElse(null));

        // if relational
        if (SpaceChatConfigKeys.USE_RELATIONAL_PLACEHOLDERS.get(plugin.getSpaceChatConfig().getAdapter()) && !serverSyncServiceManager.isUsingNetwork()) {
            // send relational
            chatManager.sendRelationalChatMessage(player, message, applicableFormat == null ? null : applicableFormat.getFormat(), event);
        } else {
            chatManager.sendChatMessage(player, message, applicableFormat == null ? null : applicableFormat.getFormat(), event);
        }
    }
}
