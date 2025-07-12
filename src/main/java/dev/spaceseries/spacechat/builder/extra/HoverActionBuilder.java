package dev.spaceseries.spacechat.builder.extra;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.model.formatting.action.HoverAction;

import java.util.Collections;
import java.util.List;

public class HoverActionBuilder {

    /**
     * Builds an V (output) from a K (input)
     *
     * @param path
     * @param adapter
     * @return
     */
    public HoverAction build(String path, ConfigurationAdapter adapter) {

        // create object
        HoverAction hoverAction = new HoverAction();

        // get lines
        List<String> lines = adapter.getStringList(path + ".lines", Collections.emptyList());

        // set lines
        hoverAction.setLines(lines);

        // return
        return hoverAction;
    }
}
