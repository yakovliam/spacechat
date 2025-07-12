package dev.spaceseries.spacechat.builder.channel;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.builder.format.FormatBuilder;
import dev.spaceseries.spacechat.model.Channel;
import dev.spaceseries.spacechat.model.formatting.Format;

public class ChannelBuilder {

    public Channel build(String path, String handle, ConfigurationAdapter adapter) {

        // get permission
        String permission = adapter.getString(path + ".permission", null);

        // build format
        Format format = new FormatBuilder().build(path + ".format", adapter);

        return new Channel(handle, permission, format);
    }
}
