package dev.spaceseries.spacechat.listener;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import dev.spaceseries.spacechat.model.Channel;
import dev.spaceseries.spacechat.replacer.SectionReplacer;
import dev.spaceseries.spacechat.util.color.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .hexColors()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .build();
    private final LegacyComponentSerializer LEGACY_URL = LegacyComponentSerializer.builder()
            .hexColors()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .extractUrls()
            .build();
    private final LegacyComponentSerializer URL = LegacyComponentSerializer.builder()
            .extractUrls()
            .build();

    private static final SectionReplacer SECTION_REPLACER = new SectionReplacer();

    private final SpaceChatPlugin plugin;

    public ChatListener(SpaceChatPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Listens for chat messages
     * At the MONITOR priority (runs near LAST) to accommodate for plugins that block chat (mutes, anti-bots, etc)
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAsyncChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;

        // clear recipients to "cancel"
        event.getRecipients().clear();

        final Player player = event.getPlayer();

        // get player's current channel
        Channel current = plugin.getServerSyncServiceManager().getDataService().getCurrentChannel(player.getUniqueId());

        // Avoid usage of MiniMessage or color section from player
        String msg = MiniMessage.miniMessage().escapeTags(SECTION_REPLACER.apply(event.getMessage(), player));

        final Component message;
        if (player.hasPermission(SpaceChatConfigKeys.PERMISSIONS_USE_CHAT_COLORS.get(plugin.getSpaceChatConfig().getAdapter()))) {
            msg = ColorUtil.color(msg, player, SpaceChatConfigKeys.PERMISSIONS_COLOR.get(plugin.getSpaceChatConfig().getAdapter()));
            // yes, the player has permission to use chat colors, so color message
            if (player.hasPermission(SpaceChatConfigKeys.PERMISSIONS_USE_CHAT_LINKS.get(plugin.getSpaceChatConfig().getAdapter()))) {
                message = LEGACY_URL.deserialize(msg);
            } else {
                message = LEGACY.deserialize(msg);
            }
        } else if (player.hasPermission(SpaceChatConfigKeys.PERMISSIONS_USE_CHAT_LINKS.get(plugin.getSpaceChatConfig().getAdapter()))) {
            message = URL.deserialize(msg);
        } else {
            // no, the player doesn't have permission to use chat colors, so just return the message (not colored)
            message = Component.text(msg);
        }

        // if not null, send through channel manager
        if (current != null && player.hasPermission(current.getPermission())) {
            plugin.getChannelManager().send(event, message, current);
            return;
        }

        // get chat format manager, send chat packet (this method also sets the format in console)
        plugin.getChatFormatManager().send(event, message);
    }
}
