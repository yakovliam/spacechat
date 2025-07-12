package dev.spaceseries.spacechat.builder.format;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.builder.format.part.FormatPartBuilder;
import dev.spaceseries.spacechat.model.formatting.Format;

public class FormatBuilder {

    public Format build(String path, ConfigurationAdapter adapter) {
        return new Format(new FormatPartBuilder().build(path, adapter));
    }
}
