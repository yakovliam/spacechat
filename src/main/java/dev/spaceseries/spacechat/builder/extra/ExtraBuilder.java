package dev.spaceseries.spacechat.builder.extra;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.model.formatting.Extra;

import java.util.List;

public class ExtraBuilder {

    /**
     * Builds an V (output) from a K (input)
     *
     * @param path
     * @param adapter
     * @return The extra
     */
    public Extra build(String path, ConfigurationAdapter adapter) {

        // create object
        Extra extra = new Extra();

        List<String> clickKeys = adapter.getKeys(path + ".click", null);

        // check if "click" exists
        if (clickKeys != null) {
            // use click builder to set extra
            extra.setClickAction(new ClickActionBuilder().build(path + ".click", adapter));
        }

        List<String> hoverKeys = adapter.getKeys(path + ".hover", null);

        // check if "hover" exists
        if (hoverKeys != null) {
            // use hover builder to set extra
            extra.setHoverAction(new HoverActionBuilder().build(path + ".hover", adapter));
        }

        // return
        return extra;
    }
}
