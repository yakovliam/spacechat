package dev.spaceseries.spacechat.sync.redis.stream.packet.chat;

import dev.spaceseries.spacechat.model.formatting.ParsedFormat;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.model.Channel;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RedisChatPacketDeserializer implements JsonDeserializer<RedisChatPacket> {

    private final SpaceChatPlugin plugin;

    public RedisChatPacketDeserializer(SpaceChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public RedisChatPacket deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonObject object = json.getAsJsonObject();

        // get sender uuid
        String senderUUIDString = object.get("senderUUID").getAsString();
        // deserialize
        UUID sender = UUID.fromString(senderUUIDString);

        // get sender name
        String senderName = object.get("senderName").getAsString();

        // get channel string
        String channelStringHandle = object.get("channel") == null ? null : object.get("channel").getAsString();
        // deserialize / get (null = global)
        Channel channel = channelStringHandle == null ? null : plugin.getChannelManager().get(channelStringHandle, null);

        // get server identifier
        String serverIdentifier = object.get("serverIdentifier").getAsString();
        // get server display name
        String serverDisplayName = object.get("serverDisplayName").getAsString();

        // deserialize
        ParsedFormat parsedFormat = ParsedFormat.fromJson(object.get("component"));

        Set<String> mentionedPlayers;
        if (object.has("mentioned")) {
            mentionedPlayers = new HashSet<>();
            for (JsonElement element : object.get("mentioned").getAsJsonArray()) {
                mentionedPlayers.add(element.getAsString());
            }
        } else {
            mentionedPlayers = Set.of();
        }

        // return a new message
        return new RedisChatPacket(sender, senderName, channel, serverIdentifier, serverDisplayName, parsedFormat, mentionedPlayers);
    }
}
