package dev.spaceseries.spacechat.builder.extra;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import dev.spaceseries.spacechat.model.formatting.action.ClickAction;
import dev.spaceseries.spacechat.model.formatting.action.ClickActionType;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class ClickActionBuilder {

    /**
     * Builds an V (output) from a K (input)
     *
     * @param path
     * @param adapter
     * @return
     */
    public ClickAction build(String path, ConfigurationAdapter adapter) {

        // create object
        ClickAction clickAction = new ClickAction();

        // get the action type
        String actionTypeString = adapter.getString(path + ".action", null);
        ClickActionType clickActionType = null;

        try {
            // get action type from string
            clickActionType = ClickActionType.valueOf(actionTypeString.toUpperCase());
        } catch (Exception ignored) {
            Bukkit.getLogger().log(Level.SEVERE, "Error while parsing Extra " + path + ": " + actionTypeString + " is not a valid extra action");
        }

        // set action type
        clickAction.setClickActionType(clickActionType);

        // get value
        String value = adapter.getString(path + ".value", null);

        // set value
        clickAction.setValue(value);

        // return
        return clickAction;
    }
}
