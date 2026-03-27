package dev.spaceseries.spacechat.user;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeMutateEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AsyncPermission {

    private static AsyncPermission INSTANCE;

    public static void init(@NotNull AsyncPermission instance) {
        INSTANCE = instance;
    }

    public static boolean check(@NotNull Player player, @NotNull String permission) {
        return INSTANCE.get(player, permission);
    }

    public static void invalidate(@NotNull Player player) {
        INSTANCE.remove(player);
    }

    private final Plugin plugin;
    private final Map<Integer, Map<String, Boolean>> permissions = new HashMap<>();

    public AsyncPermission(@NotNull Plugin plugin) {
        this.plugin = plugin;
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            listen();
        }
    }

    private void listen() {
        final EventBus eventBus = LuckPermsProvider.get().getEventBus();
        final Consumer<NodeMutateEvent> consumer = event -> {
            if (!event.isUser()) {
                return;
            }

            final Player player = Bukkit.getPlayer(((User) event.getTarget()).getUniqueId());
            if (player == null) {
                return;
            }

            Node node;
            if (event instanceof NodeAddEvent) {
                node = ((NodeAddEvent) event).getNode();
            } else if (event instanceof NodeRemoveEvent) {
                node = ((NodeRemoveEvent) event).getNode();
            } else {
                return;
            }

            if (node instanceof PermissionNode) {
                final String permission = ((PermissionNode) node).getPermission();
                update(player, permission);
            }
        };
        eventBus.subscribe(this.plugin, NodeAddEvent.class, consumer);
        eventBus.subscribe(this.plugin, NodeRemoveEvent.class, consumer);
    }

    public boolean get(@NotNull Player player, @NotNull String permission) {
        Map<String, Boolean> map = this.permissions.get(player.getEntityId());
        if (map == null) {
            map = new HashMap<>();
            this.permissions.put(player.getEntityId(), map);
        }
        Boolean result = map.get(permission);
        if (result == null) {
            result = player.hasPermission(permission);
            map.put(permission, result);
        }
        return result;
    }

    public void update(@NotNull Player player, @NotNull String permission) {
        final Map<String, Boolean> map = this.permissions.get(player.getEntityId());
        if (map == null) {
            return;
        }
        if (map.get(permission) == null) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> map.put(permission, player.hasPermission(permission)));
    }

    public void remove(@NotNull Player player) {
        this.permissions.remove(player.getEntityId());
    }
}
