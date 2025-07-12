package dev.spaceseries.spacechat.loader;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.api.config.adapter.InheritConfigAdapter;
import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.builder.chatformat.ChatFormatBuilder;
import dev.spaceseries.spacechat.model.formatting.ChatFormat;

import java.util.ArrayList;

public class ChatFormatLoader extends FormatLoader<ChatFormat> {

    /**
     * Initializes
     */
    public ChatFormatLoader(SpaceChatPlugin plugin, String formatsSection) {
        super(plugin, formatsSection);
    }

    /**
     * Loads chat formats
     */
    @Override
    public void load(FormatManager<ChatFormat> formatManager) {
        ConfigurationAdapter adapter = getPlugin().getFormatsConfig().getAdapter();

        // loop through section keys
        for (String handle : adapter.getKeys(formatsSection, new ArrayList<>())) {

            final String path = formatsSection + "." + handle;
            final String inherit = adapter.getString(path + ".inherit", null);
            final ConfigurationAdapter usedAdapter;
            if (inherit == null) {
                usedAdapter = adapter;
            } else {
                usedAdapter = new InheritConfigAdapter(adapter, s -> {
                    if (s.startsWith(path)) {
                        return formatsSection + "." + inherit + s.substring(path.length());
                    }
                    return s;
                });
            }

            // add to manager
            formatManager.add(handle, new ChatFormatBuilder().build(path, handle, usedAdapter));
        }
    }
}
