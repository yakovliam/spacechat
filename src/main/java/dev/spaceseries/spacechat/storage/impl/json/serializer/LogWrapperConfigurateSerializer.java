package dev.spaceseries.spacechat.storage.impl.json.serializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.logging.wrap.LogChatWrapper;
import dev.spaceseries.spacechat.logging.wrap.LogType;
import dev.spaceseries.spacechat.logging.wrap.LogWrapper;
import dev.spaceseries.spacechat.replacer.SectionReplacer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.UUID;

public class LogWrapperConfigurateSerializer implements TypeSerializer<LogWrapper> {

    private static final SectionReplacer SECTION_REPLACER = new SectionReplacer();


    /**
     * SpaceChat plugin
     */
    private final SpaceChatPlugin plugin;

    /**
     * Gson
     */
    private final Gson gson;

    /**
     * Log wrapper configurate serializer
     *
     * @param plugin plugin
     */
    public LogWrapperConfigurateSerializer(SpaceChatPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                .create();
    }

    /**
     * Deserialize an object (of the correct type) from the given configuration
     * node.
     *
     * @param type the type of return value required
     * @param node the node containing serialized data
     * @return an object
     * @throws SerializationException if the presented data is invalid
     * @since 4.0.0
     */
    @Override
    public LogWrapper deserialize(Type type, ConfigurationNode node) throws SerializationException {

        LogType logType = LogType.valueOf(node.node("type").getString());

        if (logType == LogType.CHAT) {
            String senderName = node.node("senderName").getString();
            UUID senderUUID = UUID.fromString(node.node("senderUUID").getString());
            String message = node.node("message").getString("");
            Date date = gson.fromJson(node.node("date").getString(), Date.class);

            return new LogChatWrapper(LogType.CHAT, senderName, senderUUID, MiniMessage.miniMessage().deserialize(SECTION_REPLACER.apply(message, null)), date);
        }

        return null;
    }

    /**
     * Serialize an object to the given configuration node.
     *
     * @param type the type of the input object
     * @param obj  the object to be serialized
     * @param node the node to write to
     * @throws SerializationException if the object cannot be serialized
     * @since 4.0.0
     */
    @Override
    public void serialize(Type type, @Nullable LogWrapper obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            return;
        }

        if (obj.getLogType().equals(LogType.CHAT) && obj instanceof LogChatWrapper) {
            serializeLogChatWrapper((LogChatWrapper) obj, node);
        }
    }

    /**
     * Serializes log chat wrapper
     *
     * @param wrapper wrapper
     * @param node    node
     * @throws SerializationException
     */
    private void serializeLogChatWrapper(LogChatWrapper wrapper, ConfigurationNode node) throws SerializationException {
        node.node("senderName").set(wrapper.getSenderName());
        node.node("senderUUID").set(wrapper.getSenderUUID().toString());
        node.node("message").set(MiniMessage.miniMessage().serialize(wrapper.getMessage()));
        node.node("date").set(gson.toJson(wrapper.getAt()));
        node.node("type").set(wrapper.getLogType().name());
    }
}
