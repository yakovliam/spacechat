package dev.spaceseries.spacechat.listener;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import dev.spaceseries.spacechat.model.Channel;
import dev.spaceseries.spacechat.replacer.SectionReplacer;
import io.papermc.paper.event.player.AsyncChatDecorateEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.StringJoiner;

public class ChatListener implements Listener {

    private static final SectionReplacer SECTION_REPLACER = new SectionReplacer();

    private final SpaceChatPlugin plugin;

    public ChatListener(SpaceChatPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAsyncChat(AsyncChatDecorateEvent event) {
        final Player player = event.player();
        if (player == null) {
            return;
        }
        Component result = escapeMiniMessage(event.result());
        String message = SECTION_REPLACER.apply(MiniMessage.miniMessage().serialize(result), player);
        if (player.hasPermission(SpaceChatConfigKeys.PERMISSIONS_USE_CHAT_COLORS.get(plugin.getSpaceChatConfig().getAdapter()))) {
            // yes, the player has permission to use chat colors, so color message
            if (message.contains("&")) {
                message = color(message);
            }
        }
        if (player.hasPermission(SpaceChatConfigKeys.PERMISSIONS_USE_CHAT_LINKS.get(plugin.getSpaceChatConfig().getAdapter()))) {
            if (message.contains("http")) {
                message = urls(message);
            }
        }

        event.result(MiniMessage.miniMessage().deserialize(message));
    }

    private static Component escapeMiniMessage(Component component) {
        final String json = GsonComponentSerializer.gson().serialize(component);
        return GsonComponentSerializer.gson().deserialize(MiniMessage.miniMessage().escapeTags(json));
    }

    private static String color(String s) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c != '&' || i + 1 >= s.length()) {
                builder.append(c);
                continue;
            }
            switch (s.charAt(i + 1)) {
                case '0':
                    builder.append("<black>");
                    break;
                case '1':
                    builder.append("<dark_blue>");
                    break;
                case '2':
                    builder.append("<dark_green>");
                    break;
                case '3':
                    builder.append("<dark_aqua>");
                    break;
                case '4':
                    builder.append("<dark_red>");
                    break;
                case '5':
                    builder.append("<dark_purple>");
                    break;
                case '6':
                    builder.append("<gold>");
                    break;
                case '7':
                    builder.append("<gray>");
                    break;
                case '8':
                    builder.append("<dark_gray>");
                    break;
                case '9':
                    builder.append("<blue>");
                    break;
                case 'a':
                    builder.append("<green>");
                    break;
                case 'b':
                    builder.append("<aqua>");
                    break;
                case 'c':
                    builder.append("<red>");
                    break;
                case 'd':
                    builder.append("<light_purple>");
                    break;
                case 'e':
                    builder.append("<yellow>");
                    break;
                case 'f':
                    builder.append("<white>");
                    break;
                default:
                    builder.append(c);
                    continue;
            }
            i++;
        }
        return builder.toString();
    }

    private static String urls(String message) {
        final StringJoiner joiner = new StringJoiner(" ");
        for (String s : message.split(" ")) {
            int index = s.indexOf("https://");
            if (index < 0) {
                index = s.indexOf("http://");
            }
            if (index < 0) {
                joiner.add(s);
            } else {
                int end = s.indexOf('<', index);
                if (end < 0) {
                    end = s.length();
                }
                final StringBuilder builder = new StringBuilder();
                builder.append(s, 0, index);
                final String url = s.substring(index + 1, end);
                builder.append("<click:open_url:").append(url).append(">").append(url).append("</click>");
                if (end < s.length()) {
                    builder.append(s.substring(end));
                }
                joiner.add(builder.toString());
            }
        }
        return joiner.toString();
    }

    /**
     * Listens for chat messages
     * At the MONITOR priority (runs near LAST) to accommodate for plugins that block chat (mutes, anti-bots, etc)
     *
     * @param event The event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAsyncChat(AsyncChatEvent event) {
        if (event.isCancelled()) return;

        // clear viewers to "cancel", but also maintain console viewer
        event.viewers().removeIf(viewer -> viewer instanceof Player);

        final Player player = event.getPlayer();

        // get player's current channel
        Channel current = plugin.getServerSyncServiceManager().getDataService().getCurrentChannel(player.getUniqueId());

        // if not null, send through channel manager
        if (current != null && player.hasPermission(current.getPermission())) {
            plugin.getChannelManager().send(event, event.message(), current);
            return;
        }

        // get chat format manager, send chat packet (this method also sets the format in console)
        plugin.getChatFormatManager().send(event, event.message());
    }
}
