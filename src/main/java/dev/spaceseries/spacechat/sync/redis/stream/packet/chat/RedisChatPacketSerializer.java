package dev.spaceseries.spacechat.sync.redis.stream.packet.chat;

import com.google.gson.JsonArray;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class RedisChatPacketSerializer implements JsonSerializer<RedisChatPacket> {

    @Override
    public JsonElement serialize(RedisChatPacket src, Type typeOfSrc, JsonSerializationContext context) {
        // create json element
        JsonObject element = new JsonObject();

        // add properties
        element.addProperty("senderUUID", src.getSender().toString());
        element.addProperty("senderName", src.getSenderName());
        element.addProperty("channel", src.getChannel() == null ? null : src.getChannel().getHandle());
        element.addProperty("serverIdentifier", src.getServerIdentifier());
        element.addProperty("serverDisplayName", src.getServerDisplayName());
        element.add("component", src.getParsedFormat().asJson());
        if (!src.getMentionedPlayers().isEmpty()) {
            final JsonArray mentioned = new JsonArray();
            for (String player : src.getMentionedPlayers()) {
                mentioned.add(player);
            }
            element.add("mentioned", mentioned);
        }

        return element;
    }
}
