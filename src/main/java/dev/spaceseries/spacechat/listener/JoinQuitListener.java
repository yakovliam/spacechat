package dev.spaceseries.spacechat.listener;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.model.User;
import dev.spaceseries.spacechat.user.AsyncPermission;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class JoinQuitListener implements Listener {

    private final SpaceChatPlugin plugin;

    public JoinQuitListener(SpaceChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final String name = event.getPlayer().getName();
        final UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getUserManager().isPlayerLoaded(name)) {
                User user = plugin.getUserManager().get(uuid);

                // update
                plugin.getUserManager().update(user);
            }

            // invalidate
            plugin.getUserManager().invalidate(uuid, name);
            AsyncPermission.invalidate(event.getPlayer());
        });
    }

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        final String name = event.getName();
        final UUID uuid = event.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getUserManager().loadIgnoreList(name);

            // handle with user manager
            plugin.getUserManager().use(uuid, (user) -> {
                // if username not equal, update
                if (!name.equals(user.getUsername())) {
                    plugin.getUserManager().update(new User(plugin, user.getUuid(), name, user.getDate(), user.getSubscribedChannels()));
                }
            });
        });
    }
}
