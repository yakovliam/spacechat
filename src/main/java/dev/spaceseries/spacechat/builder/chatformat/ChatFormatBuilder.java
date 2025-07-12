package dev.spaceseries.spacechat.builder.chatformat;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.builder.format.FormatBuilder;
import dev.spaceseries.spacechat.model.formatting.ChatFormat;
import dev.spaceseries.spacechat.model.formatting.Format;

public class ChatFormatBuilder {

    /**
     * Builds a format from a Configuration
     *
     * @param path
     * @param handle
     * @param adapter
     * @return The returned format
     */
    public ChatFormat build(String path, String handle, ConfigurationAdapter adapter) {

        // get priority
        Integer priority = adapter.getInteger(path + ".priority", -1);

        // get permission node
        String permission = adapter.getString(path + ".permission", null);

        // get parts
        Format parts = new FormatBuilder().build(path + ".format", adapter);

        // return
        return new ChatFormat(handle, priority, permission, parts);
    }
}
