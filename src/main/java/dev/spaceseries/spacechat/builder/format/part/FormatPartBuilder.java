package dev.spaceseries.spacechat.builder.format.part;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.builder.extra.ExtraBuilder;
import dev.spaceseries.spacechat.model.formatting.FormatPart;

import java.util.ArrayList;
import java.util.List;

public class FormatPartBuilder {

    /**
     * Builds an V (output) from a K (input)
     *
     * @param path
     * @param adapter
     * @return
     */
    public List<FormatPart> build(String path, ConfigurationAdapter adapter) {

        // create list
        List<FormatPart> formatPartList = new ArrayList<>();

        // loop through all keys in the root input
        for (String handle : adapter.getKeys(path, new ArrayList<>())) {
            // create format part
            FormatPart formatPart = new FormatPart();

            // get text
            String text = adapter.getString(path + "." + handle + ".text", null);

            // set text
            formatPart.setText(text);

            List<String> extraKeys = adapter.getKeys(path + "." + handle + ".extra", null);

            // if extra exists, parse
            if (extraKeys != null) {
                // set extra
                formatPart.setExtra(new ExtraBuilder().build(path + "." + handle + ".extra", adapter));
            }

            String line = adapter.getString(path + "." + handle + ".line", null);

            // if it contains "line" (singular minimessage compatibility)
            if (line != null) {
                // set line
                formatPart.setLine(line);
            }

            formatPart.setLineProtocol(adapter.getInteger(path + "." + handle + ".protocol", -1));

            // add format part
            formatPartList.add(formatPart);
        }

        // return
        return formatPartList;
    }
}
