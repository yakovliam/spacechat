package dev.spaceseries.spacechat.loader;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.builder.channel.ChannelBuilder;
import dev.spaceseries.spacechat.model.manager.MapManager;
import dev.spaceseries.spacechat.model.Channel;

import java.util.ArrayList;

public class ChannelLoader implements Loader<MapManager<String, Channel>> {

    /**
     * Section for channels
     */
    private final String channelsSection;

    /**
     * Plugin
     */
    private final SpaceChatPlugin plugin;


    /**
     * Initializes
     */
    public ChannelLoader(SpaceChatPlugin plugin, String channelsSection) {
        this.plugin = plugin;
        this.channelsSection = channelsSection;
    }


    /**
     * Load channels
     *
     * @param stringChannelMapManager channel manager
     */
    @Override
    public void load(MapManager<String, Channel> stringChannelMapManager) {
        ConfigurationAdapter adapter = plugin.getChannelsConfig().getAdapter();

        // loop through section keys
        for (String handle : adapter.getKeys(channelsSection, new ArrayList<>())) {
            // add to manager
            stringChannelMapManager.add(handle, new ChannelBuilder().build(channelsSection + "." + handle, handle, adapter));
        }
    }
}

