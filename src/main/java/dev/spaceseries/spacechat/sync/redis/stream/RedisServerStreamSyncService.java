package dev.spaceseries.spacechat.sync.redis.stream;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.saicone.ezlib.Dependencies;
import com.saicone.ezlib.Dependency;
import dev.spaceseries.spacechat.Messages;
import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.api.message.Message;
import dev.spaceseries.spacechat.chat.ChatManager;
import dev.spaceseries.spacechat.model.formatting.parsed.SimpleParsedFormat;
import dev.spaceseries.spacechat.sync.ServerStreamSyncService;
import dev.spaceseries.spacechat.sync.ServerSyncServiceManager;
import dev.spaceseries.spacechat.sync.packet.ReceiveStreamDataPacket;
import dev.spaceseries.spacechat.sync.packet.SendStreamDataPacket;
import dev.spaceseries.spacechat.sync.packet.StreamDataPacket;
import dev.spaceseries.spacechat.sync.provider.redis.RedisProvider;
import dev.spaceseries.spacechat.sync.redis.stream.packet.RedisPublishDataPacket;
import dev.spaceseries.spacechat.sync.redis.stream.packet.RedisStringReceiveDataPacket;
import dev.spaceseries.spacechat.sync.redis.stream.packet.broadcast.RedisBroadcastPacket;
import dev.spaceseries.spacechat.sync.redis.stream.packet.broadcast.RedisBroadcastPacketDeserializer;
import dev.spaceseries.spacechat.sync.redis.stream.packet.broadcast.RedisBroadcastPacketSerializer;
import dev.spaceseries.spacechat.sync.redis.stream.packet.chat.RedisChatPacket;
import dev.spaceseries.spacechat.sync.redis.stream.packet.chat.RedisChatPacketDeserializer;
import dev.spaceseries.spacechat.sync.redis.stream.packet.chat.RedisChatPacketSerializer;
import dev.spaceseries.spacechat.sync.redis.stream.packet.message.RedisMessageDeserializer;
import dev.spaceseries.spacechat.sync.redis.stream.packet.message.RedisMessagePacket;
import dev.spaceseries.spacechat.sync.redis.stream.packet.message.RedisMessageSerializer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static dev.spaceseries.spacechat.config.SpaceChatConfigKeys.*;

@Dependencies(
        value = {
                @Dependency(value = "redis.clients:jedis:5.2.0", relocate = {"org.json", "{package}.lib.json"}),
                @Dependency("org.slf4j:slf4j-nop:1.7.36")
        },
        relocations = {
                "redis.clients.jedis", "{package}.lib.jedis",
                "com.google.gson", "{package}.lib.gson",
                "org.apache.commons.pool2", "{package}.lib.commons.pool2",
                "org.slf4j", "{package}.lib.slf4j"
        }
)
public class RedisServerStreamSyncService extends ServerStreamSyncService {

    /**
     * Redis provider
     */
    private RedisProvider provider;

    /**
     * Redis Messenger
     * <p>
     * This class does all of the actual work and connection management
     */
    private Messenger messenger;

    private boolean messengerEnabled = true;

    /**
     * Gson
     * <p>
     * Responsible for serializing and deserializing messages
     */
    private Gson gson;

    private final Type mapType = new TypeToken<Map<String, Object>>(){}.getType();

    /**
     * Chat manager
     */
    private final ChatManager chatManager;

    /**
     * Construct server sync service
     *
     * @param plugin         plugin
     * @param serviceManager service manager
     */
    public RedisServerStreamSyncService(SpaceChatPlugin plugin, ServerSyncServiceManager serviceManager) {
        super(plugin, serviceManager);
        this.chatManager = plugin.getChatManager();
    }

    /**
     * Publishes a chat message across the server
     *
     * @param packet packet
     */
    @Override
    public void publishChat(SendStreamDataPacket<?> packet) {
        RedisChatPacket redisChatPacket = (RedisChatPacket) packet;

        // gson-ify the redis message
        String json = gson.toJson(redisChatPacket, RedisChatPacket.class);

        // publish to redis
        messenger.publish(new RedisPublishDataPacket(REDIS_CHAT_CHANNEL.get(plugin.getSpaceChatConfig().getAdapter()), json));
    }

    /**
     * Publishes a player message across the server
     *
     * @param packet packet
     */
    @Override
    public void publishMessage(SendStreamDataPacket<?> packet) {
        RedisMessagePacket redisMessagePacket = (RedisMessagePacket) packet;

        // gson-ify the redis message
        String json = gson.toJson(redisMessagePacket, RedisMessagePacket.class);

        //publish to redis
        messenger.publish(new RedisPublishDataPacket(REDIS_MESSAGE_CHANNEL.get(plugin.getSpaceChatConfig().getAdapter()), json));
    }

    /**
     * Publishes a broadcast message across the server
     *
     * @param packet packet
     */
    @Override
    public void publishBroadcast(SendStreamDataPacket<?> packet) {
        RedisBroadcastPacket redisBroadcastPacket = (RedisBroadcastPacket) packet;

        // gson-ify the redis message
        String json = gson.toJson(redisBroadcastPacket, RedisBroadcastPacket.class);

        // publish to redis
        messenger.publish(new RedisPublishDataPacket(
                REDIS_BROADCAST_CHANNEL.get(plugin.getSpaceChatConfig().getAdapter()), json));
    }

    @Override
    public void publishPlayerList(List<String> players) {
        publishPlayerList(REDIS_SERVER_IDENTIFIER.get(plugin.getSpaceChatConfig().getAdapter()), players);
    }

    /**
     * Published a list of players across the server
     *
     * @param id      Server id
     * @param players players
     */
    @Override
    public void publishPlayerList(String id, List<String> players) {

        // gson-ify the list
        String json = gson.toJson(Map.of("id", id, "players", players));

        // publish to redis
        messenger.publish(new RedisPublishDataPacket(
                REDIS_ONLINE_PLAYERS_CHANNEL.get(plugin.getSpaceChatConfig().getAdapter()), json));
    }


    /**
     * Receives an incoming chat message
     *
     * @param packet packet
     */
    @Override
    public void receiveChat(ReceiveStreamDataPacket<?> packet) {
        RedisStringReceiveDataPacket redisStringReceiveDataPacket = (RedisStringReceiveDataPacket) packet;
        // deserialize
        RedisChatPacket chatPacket = gson.fromJson(redisStringReceiveDataPacket.getData(), RedisChatPacket.class);

        // if the message is from ourselves, then return
        if (chatPacket.getServerIdentifier().equalsIgnoreCase(REDIS_SERVER_IDENTIFIER.get(plugin.getSpaceChatConfig().getAdapter()))) {
            return;
        }

        // if channel exists, send through that instead
        if (chatPacket.getChannel() != null && plugin.getChannelManager().get(chatPacket.getChannel().getHandle()) != null) {
            chatManager.sendComponentChannelMessage(null, chatPacket.getParsedFormat(), chatPacket.getChannel());
            return;
        }

        // send to all players filtering ignored players from sender
        chatManager.sendComponentChatMessage(chatPacket.getSenderName(), chatPacket.getParsedFormat());

        // send to all players
        //chatManager.sendComponentChatMessage(chatPacket.getComponent());
    }

    /**
     * Receives an incoming player message
     *
     * @param packet packet
     */
    @Override
    public void receiveMessage(ReceiveStreamDataPacket<?> packet) {
        RedisStringReceiveDataPacket redisStringReceiveDataPacket = (RedisStringReceiveDataPacket) packet;
        // deserialize
        RedisMessagePacket messagePacket = gson.fromJson(redisStringReceiveDataPacket.getData(), RedisMessagePacket.class);

        final String currentID = REDIS_SERVER_IDENTIFIER.get(plugin.getSpaceChatConfig().getAdapter());
        // if the message is from ourselves, then return
        if (messagePacket.getServerIdentifier().equalsIgnoreCase(currentID)) {
            return;
        }

        // put replier in map
        plugin.getUserManager().getReplyTargetMap().put(messagePacket.getReceiverName(), messagePacket.getSenderName());

        // Display message in console
        if (messagePacket.getSenderName().equalsIgnoreCase("@console")) {
            Messages.getInstance(plugin).messageFormatSend.message(Bukkit.getConsoleSender(),
                    "%receiver%", messagePacket.getReceiverName(),
                    "%message%", '[' + currentID + "] " + messagePacket.getMessage()
            );
        }

        final Message formatReceive;
        final boolean consoleReceiver = messagePacket.getReceiverName().equalsIgnoreCase("@console");
        final Player receiver;

        if (!consoleReceiver) {
            receiver = Bukkit.getPlayer(messagePacket.getReceiverName());
            // return if the receiver is not online
            if (receiver == null) {
                return;
            }

            formatReceive = Messages.getInstance(plugin).messageFormatReceive;
            Component componentReceive = formatReceive.compile(
                    "%sender%", messagePacket.getSenderName(),
                    "%message%", messagePacket.getMessage()
            );

            // if channel exists, send through that instead
            if (messagePacket.getChannel() != null && plugin.getChannelManager().get(messagePacket.getChannel().getHandle()) != null) {
                chatManager.sendComponentChannelMessage(null, new SimpleParsedFormat(componentReceive), messagePacket.getChannel());
                return;
            }
        } else {
            formatReceive = Messages.getInstance(plugin).messageFormatReceive;
            receiver = null;
        }

        // send to receiver
        formatReceive.message(consoleReceiver ? Bukkit.getConsoleSender() : receiver,
                "%sender%", messagePacket.getSenderName(),
                "%message%", messagePacket.getMessage()
        );
        if (!consoleReceiver) {
            PRIVATE_NOTIFICATION_SOUND.get(plugin.getSpaceChatConfig().getAdapter()).play(receiver);
        }
    }

    /**
     * Receives an incoming list of players
     *
     * @param packet packet
     */
    @Override
    public void receivePlayerList(ReceiveStreamDataPacket<?> packet) {
        RedisStringReceiveDataPacket redisStringReceiveDataPacket = (RedisStringReceiveDataPacket) packet;

        Map<String, Object> map = gson.fromJson(redisStringReceiveDataPacket.getData(), mapType);
        if (!map.containsKey("id")) {
            return;
        }
        String id = String.valueOf(map.get("id"));
        List<String> players = new ArrayList<>();
        Object object = map.get("players");
        if (object instanceof List) {
            for (Object player : (List<?>) object) {
                players.add(String.valueOf(player));
            }
        }

        //Update the online players
        if (!players.isEmpty()) {
            plugin.getUserManager().setOnlinePlayers(id, players);
        }
    }

    /**
     * Receives an incoming chat message
     *
     * @param packet packet
     */
    @Override
    public void receiveBroadcast(ReceiveStreamDataPacket<?> packet) {
        RedisStringReceiveDataPacket stringReceiveDataPacket = (RedisStringReceiveDataPacket) packet;

        // deserialize
        RedisBroadcastPacket broadcastPacket = gson.fromJson(stringReceiveDataPacket.getData(), RedisBroadcastPacket.class);

        // if the message is from ourselves, then return
        if (broadcastPacket.getServerIdentifier().equalsIgnoreCase(REDIS_SERVER_IDENTIFIER.get(plugin.getSpaceChatConfig().getAdapter()))) {
            return;
        }

        // send to all players
        chatManager.sendComponentMessage(broadcastPacket.getComponent());
    }

    /**
     * Starts the service in question
     */
    @Override
    public void start() {
        // initialize provider
        this.provider = getServiceManager().getRedisProvider();
        // initialize messenger
        this.messenger = new Messenger();
        // It's alive!
        alive();
        // initialize my super-duper gson adapter
        gson = new GsonBuilder()
                .registerTypeAdapter(RedisChatPacket.class, new RedisChatPacketSerializer())
                .registerTypeAdapter(RedisChatPacket.class, new RedisChatPacketDeserializer(plugin))
                .registerTypeAdapter(RedisBroadcastPacket.class, new RedisBroadcastPacketSerializer())
                .registerTypeAdapter(RedisBroadcastPacket.class, new RedisBroadcastPacketDeserializer())
                .registerTypeAdapter(RedisMessagePacket.class, new RedisMessageDeserializer(plugin))
                .registerTypeAdapter(RedisMessagePacket.class, new RedisMessageSerializer())
                .create();
    }

    private void alive() {
        // subscribing to redis pub/sub is a blocking operation.
        // we need to make a new thread in order to not block the main thread....
        new Thread(() -> {
            boolean reconnected = false;
            while (messengerEnabled && !Thread.interrupted() && provider.provide() != null && !provider.provide().isClosed()) {
                try (Jedis jedis = provider.provide().getResource()) {
                    if (reconnected) {
                        plugin.getLogger().log(Level.INFO, "Redis connection is alive again");
                    }
                    // Lock the thread
                    jedis.subscribe(messenger,
                            REDIS_CHAT_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter()),
                            REDIS_BROADCAST_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter()),
                            REDIS_MESSAGE_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter()),
                            REDIS_ONLINE_PLAYERS_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter())
                    );
                } catch (Throwable t) {
                    // Thread was unlocked
                    if (messengerEnabled) {
                        plugin.getLogger().log(Level.WARNING, "Redis connection dropped, automatic reconnection in 8 seconds...");
                        t.printStackTrace();
                        try {
                            messenger.unsubscribe(
                                    REDIS_CHAT_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter()),
                                    REDIS_BROADCAST_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter()),
                                    REDIS_MESSAGE_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter()),
                                    REDIS_ONLINE_PLAYERS_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter())
                            );
                        } catch (Throwable ignored) { }

                        // Make an instant subscribe if occurs any error on initialization
                        if (!reconnected) {
                            reconnected = true;
                        } else {
                            try {
                                Thread.sleep(8000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    } else {
                        return;
                    }
                }
            }
        }).start();
    }

    /**
     * Ends the service in question
     */
    @Override
    public void end() {
        messengerEnabled = false;
        try {
            if (this.provider.provide() != null && this.provider.provide().getResource().getClient() != null) {
                // unsubscribe from chat channel
                messenger.unsubscribe(
                        REDIS_CHAT_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter()),
                        REDIS_BROADCAST_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter()),
                        REDIS_MESSAGE_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter()),
                        REDIS_ONLINE_PLAYERS_CHANNEL.get(this.plugin.getSpaceChatConfig().getAdapter())
                );

                if (!provider.provide().isClosed()) {
                    provider.provide().close();
                }
            }
        } catch (Throwable ignored) { }
    }

    private class Messenger extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {
            // receiving
            // [channel] sent [message]

            // if it's the correct channel
            if (channel.equalsIgnoreCase(REDIS_CHAT_CHANNEL.get(plugin.getSpaceChatConfig().getAdapter()))) {
                receiveChat(new RedisStringReceiveDataPacket(message));
            } else if (channel.equalsIgnoreCase(REDIS_BROADCAST_CHANNEL.get(plugin.getSpaceChatConfig().getAdapter()))) {
                receiveBroadcast(new RedisStringReceiveDataPacket(message));
            } else if(channel.equalsIgnoreCase(REDIS_MESSAGE_CHANNEL.get(plugin.getSpaceChatConfig().getAdapter()))){
                receiveMessage(new RedisStringReceiveDataPacket(message));
            } else if (channel.equalsIgnoreCase(REDIS_ONLINE_PLAYERS_CHANNEL.get(plugin.getSpaceChatConfig().getAdapter()))){
                receivePlayerList(new RedisStringReceiveDataPacket(message));
            }
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            // we have subscribed to [channel]. We are currently subscribed to [subscribedChannels] channels.
            plugin.getLogger().log(Level.INFO, "SpaceChat subscribed to the redis channel '" + channel + "'");
        }

        @Override
        public void onUnsubscribe(String channel, int subscribedChannels) {
            // we have unsubscribed from [channel]. We are currently subscribed to another [subscribedChannels] channels.
            plugin.getLogger().log(Level.INFO, "SpaceChat unsubscribed from the redis channel '" + channel + "'");
        }

        /**
         * Publish a message
         *
         * @param dataPacket packet
         */
        public void publish(StreamDataPacket dataPacket) {
            RedisPublishDataPacket redisPublishDataPacket = (RedisPublishDataPacket) dataPacket;

            String channel = redisPublishDataPacket.getChannel();
            String message = redisPublishDataPacket.getMessage();
            // run async
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                provider.consumer(jedis -> jedis.publish(channel, message)).run();
            });
        }
    }
}
