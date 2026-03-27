package dev.spaceseries.spacechat.chat;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.api.message.Message;
import dev.spaceseries.spacechat.builder.live.NormalLiveChatFormatBuilder;
import dev.spaceseries.spacechat.builder.live.RelationalLiveChatFormatBuilder;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import dev.spaceseries.spacechat.logging.wrap.LogChatWrapper;
import dev.spaceseries.spacechat.logging.wrap.LogToType;
import dev.spaceseries.spacechat.logging.wrap.LogType;
import dev.spaceseries.spacechat.model.Channel;
import dev.spaceseries.spacechat.model.formatting.Format;
import dev.spaceseries.spacechat.model.formatting.ParsedFormat;
import dev.spaceseries.spacechat.model.formatting.parsed.SimpleParsedFormat;
import dev.spaceseries.spacechat.model.manager.Manager;
import dev.spaceseries.spacechat.sync.ServerDataSyncService;
import dev.spaceseries.spacechat.sync.ServerStreamSyncService;
import dev.spaceseries.spacechat.sync.redis.stream.packet.chat.RedisChatPacket;
import dev.spaceseries.spacechat.sync.redis.stream.packet.message.RedisMessagePacket;
import dev.spaceseries.spacechat.util.color.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ChatManager implements Manager {

    private final SpaceChatPlugin plugin;
    private ServerStreamSyncService serverStreamSyncService;
    private ServerDataSyncService serverDataSyncService;
    private final ConfigurationAdapter config;

    private final NormalLiveChatFormatBuilder normalBuilder;
    private final RelationalLiveChatFormatBuilder relationalBuilder;

    /**
     * Construct chat event manager
     *
     * @param plugin plugin
     */
    public ChatManager(SpaceChatPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getSpaceChatConfig().getAdapter();
        this.normalBuilder = new NormalLiveChatFormatBuilder(plugin);
        this.relationalBuilder = new RelationalLiveChatFormatBuilder(plugin);
    }

    /**
     * Initializes server sync services
     */
    public void initSyncServices() {
        this.serverStreamSyncService = plugin.getServerSyncServiceManager().getStreamService();
        this.serverDataSyncService = plugin.getServerSyncServiceManager().getDataService();
    }

    /**
     * Send a chat message
     * <p>
     * This does the same thing as {@link ChatManager#sendComponentMessage(Component)} but I just made it different for the sake
     * of understanding
     *
     * @param component component
     */
    public void sendComponentChatMessage(Component component) {
        sendComponentMessage(component);
    }

    /**
     * Send a chat message to a specific player
     * <p>
     * This does the same thing as {@link ChatManager#sendComponentMessage(ParsedFormat, Player)} but I just made it different for the sake
     * of understanding
     *
     * @param component component
     * @param to        to
     */
    public void sendComponentChatMessage(ParsedFormat component, Player to) {
        sendComponentMessage(component, to);
    }

    /**
     * Send a raw component to all players filter ignored players from sender
     *
     * @param parsedFormat component
     * @param senderName sender name
     */
    public void sendComponentChatMessage(String senderName, ParsedFormat parsedFormat) {

        // send chat message to all online players filtering ignored players from sender
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->{
            for (Player player : Bukkit.getOnlinePlayers()) {

                List<String> ignoredList = plugin.getUserManager().getIgnoredList(player.getName());
                if(!ignoredList.contains(senderName)) {
                    player.sendMessage(parsedFormat.asComponent(player));
                }
                /*plugin.getUserManager().getByName(player.getName(), user ->{
                    if(!user.isIgnored(senderName)){
                        Message.getAudienceProvider().player(player.getUniqueId()).sendMessage(component);
                    }
                });*/
            }
        });
    }

    /**
     * Send a raw component to all players
     *
     * @param component component
     */
    public void sendComponentMessage(Component component) {
        // send chat message to all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    /**
     * Send a raw component to a player
     *
     * @param parsedFormat component
     * @param to           to
     */
    public void sendComponentMessage(ParsedFormat parsedFormat, Player to) {
        // send chat message to all online players
        to.sendMessage(parsedFormat.asComponent(to));
    }

    /**
     * Send a raw component to a channel
     *
     * @param parsedFormat component
     * @param channel   channel
     */
    public void sendComponentChannelMessage(Player from, ParsedFormat parsedFormat, Channel channel) {
        // get all subscribed players to that channel
        List<Player> subscribedPlayers = serverDataSyncService.getSubscribedUUIDs(channel)
                .stream().map(Bukkit::getPlayer)
                .collect(Collectors.toList());

        // even if not listening, add the sender to the list of players listening so that they can view the message
        // that they sent themselves
        if (from != null && !subscribedPlayers.contains(from)) {
            subscribedPlayers.add(from);
        }

        List<Player> subscribedPlayersWithPermission = subscribedPlayers.stream()
                .filter(p -> p.hasPermission(channel.getPermission()))
                .collect(Collectors.toList());

        // if a player in the list doesn't have permission to view it, then unsubscribe them
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> subscribedPlayers.forEach(p -> {
            if (!p.hasPermission(channel.getPermission())) {
                serverDataSyncService.unsubscribeFromChannel(p.getUniqueId(), channel);
            }
        }));


        subscribedPlayersWithPermission.forEach(p -> sendComponentMessage(parsedFormat, p));
    }

    /**
     * Send a chat message
     *
     * @param message message
     */
    public void sendPrivateMessage(CommandSender sender, String receiver, String message, Message formatSend) {
        final String senderName = sender instanceof Player ? sender.getName() : "@console";

        // get player's current channel, and send through that (if null, that means 'global')
        Channel applicableChannel = sender instanceof Player ? serverDataSyncService.getCurrentChannel(((Player) sender).getUniqueId()) : null;

        formatSend.message(sender, "%receiver%", receiver, "%message%", message);

        // send via redis
        serverStreamSyncService.publishMessage(new RedisMessagePacket(sender instanceof Player ? ((Player) sender).getUniqueId() : null, senderName,
                receiver, applicableChannel, SpaceChatConfigKeys.REDIS_SERVER_IDENTIFIER.get(config),
                SpaceChatConfigKeys.REDIS_SERVER_DISPLAYNAME.get(config), message));
    }

    /**
     * Send a chat message
     *
     * @param from    player that the message is from
     * @param message message
     * @param format  format
     * @param event   event
     */
    public void sendChatMessage(Player from, Component message, Format format, PlayerEvent event) {
        // get player's current channel, and send through that (if null, that means 'global')
        Channel applicableChannel = serverDataSyncService.getCurrentChannel(from.getUniqueId());

        ParsedFormat parsed;

        // if null, return
        if (format == null) {
            // build components default message
            // this only happens if it's not possible to find a chat format
            parsed = new SimpleParsedFormat(Component.text()
                    .append(Component.text(from.getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text("> ", NamedTextColor.GRAY))
                    .append(message)
                    .build());
        } else { // if not null
            // get baseComponents from live builder
            parsed = normalBuilder.build(from, message, format, true);
        }

        // if channel exists, then send through it
        if (applicableChannel != null) {
            sendComponentChannelMessage(from, parsed, applicableChannel);
        } else {
            // send component message to entire server
            sendComponentChatMessage(from.getName(), parsed);
            //sendComponentChatMessage(parsed); OLD
        }

        // log to storage
        plugin.getLogManagerImpl()
                .log(new LogChatWrapper(LogType.CHAT, from.getName(), from.getUniqueId(), message, new Date()),
                        LogType.CHAT,
                        LogToType.STORAGE
                );

        // send via redis (it won't do anything if redis isn't enabled, so we can be sure that we aren't using dead methods that will throw an exception)
        serverStreamSyncService.publishChat(
                new RedisChatPacket(
                        from.getUniqueId(),
                        from.getName(),
                        applicableChannel,
                        SpaceChatConfigKeys.REDIS_SERVER_IDENTIFIER.get(config),
                        SpaceChatConfigKeys.REDIS_SERVER_DISPLAYNAME.get(config),
                        parsed,
                        plugin.getUserManager().mention(from, message)
                )
        );

        // log to console
        if (event != null) { // if there's an event, log w/ the event
            plugin.getLogManagerImpl()
                    .log(parsed.asComponent().children()
                            .stream()
                            .map(c -> LegacyComponentSerializer.legacySection().serialize(c))
                            .map(ColorUtil::translateFromAmpersand)
                            .map(ColorUtil::stripColor)
                            .collect(Collectors.joining()), LogType.CHAT, LogToType.CONSOLE, event);
        } else {
            plugin.getLogManagerImpl() // if there's no event, just log to console without using the event
                    .log(parsed.asComponent().children()
                            .stream()
                            .map(c -> LegacyComponentSerializer.legacySection().serialize(c))
                            .map(ColorUtil::translateFromAmpersand)
                            .map(ColorUtil::stripColor)
                            .collect(Collectors.joining()), LogType.CHAT, LogToType.CONSOLE);
        }

        // note: storage logging is handled in the actual chat format manager because there's no need to log
        // if a message come from redis. This is really a generified version of my initial idea
        // but it's pretty good and it works
    }


    /**
     * Send a chat message with relational placeholders
     *
     * @param from    player that the message is from
     * @param message message
     * @param format  format format
     * @param event   event
     */
    public void sendRelationalChatMessage(Player from, Component message, Format format, PlayerEvent event) {
        // do relational parsing
        Bukkit.getOnlinePlayers().forEach(to -> {
            Component component;

            if (format == null) {
                // build components default message
                // this only happens if it's not possible to find a chat format
                component = Component.text()
                        .append(Component.text(from.getDisplayName(), NamedTextColor.AQUA))
                        .append(Component.text("> ", NamedTextColor.GRAY))
                        .append(message)
                        .build();
            } else { // if not null
                // get baseComponents from live builder
                component = relationalBuilder.build(from, to, message, format);
            }

            // send to 'to-player'
            sendComponentChatMessage(new SimpleParsedFormat(component), to);
        });

        // log to storage
        plugin.getLogManagerImpl()
                .log(new LogChatWrapper(LogType.CHAT, from.getName(), from.getUniqueId(), message, new Date()),
                        LogType.CHAT,
                        LogToType.STORAGE
                );

        // component to use with storage and logging
        Component sampledComponent;

        if (format == null) {
            // build components default message
            // this only happens if it's not possible to find a chat format
            sampledComponent = Component.text()
                    .append(Component.text(from.getDisplayName(), NamedTextColor.AQUA))
                    .append(Component.text("> ", NamedTextColor.GRAY))
                    .append(message)
                    .build();
        } else { // if not null
            // get baseComponents from live builder
            sampledComponent = normalBuilder.build(from, message, format, false).asComponent();
        }

        // log to console
        if (event != null) {// if there's an event, log w/ the event
            plugin.getLogManagerImpl()
                    .log(sampledComponent.children()
                            .stream()
                            .map(c -> LegacyComponentSerializer.legacySection().serialize(c))
                            .map(ColorUtil::translateFromAmpersand)
                            .map(ColorUtil::stripColor)
                            .collect(Collectors.joining()), LogType.CHAT, LogToType.CONSOLE, event);

        } else {
            plugin.getLogManagerImpl() // if there's no event, just log to console without using the event
                    .log(sampledComponent.children()
                            .stream()
                            .map(c -> LegacyComponentSerializer.legacySection().serialize(c))
                            .map(ColorUtil::translateFromAmpersand)
                            .map(ColorUtil::stripColor)
                            .collect(Collectors.joining()), LogType.CHAT, LogToType.CONSOLE);
        }

        // note: storage logging is handled in the actual chat format manager because there's no need to log
        // if a message come from redis. This is really a generified version of my initial idea
        // but it's pretty good and it works
    }
}
