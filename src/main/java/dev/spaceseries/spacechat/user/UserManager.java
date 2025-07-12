package dev.spaceseries.spacechat.user;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.saicone.ezlib.Dependency;
import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import dev.spaceseries.spacechat.model.User;
import dev.spaceseries.spacechat.model.manager.Manager;
import dev.spaceseries.spacechat.storage.StorageManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Dependency(value = "com.github.ben-manes.caffeine:caffeine:3.2.1", relocate = {"com.github.benmanes.caffeine.cache", "{package}.lib.caffeine"})
public class UserManager implements Manager {

    private final SpaceChatPlugin plugin;
    private final AsyncLoadingCache<UUID, User> userAsyncCache;
    private final StorageManager storageManager;
    private final Map<String, String> replyTarget = new HashMap<>();

    private final Map<String, List<String>> ignoredList = new HashMap<>();
    private final Set<String> pendingReload = new HashSet<>();
    private boolean onUpdate = false;
    private final Set<String> vanishedPlayers = new HashSet<>();
    private Map<String, String> onlinePlayers = new HashMap<>();
    private final Map<String, String> cachedOnlinePlayers = new ConcurrentHashMap<>();

    /**
     * Construct user manager
     */
    public UserManager(SpaceChatPlugin plugin) {
        this.plugin = plugin;
        this.storageManager = plugin.getStorageManager();
        userAsyncCache = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS)
                .buildAsync((u) -> storageManager.getCurrent().getUser(u));
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (onUpdate) {
                return;
            }
            onUpdate = true;
            try {
                if (!Bukkit.getOnlinePlayers().isEmpty()) {
                    final String id = SpaceChatConfigKeys.REDIS_SERVER_IDENTIFIER.get(plugin.getSpaceChatConfig().getAdapter());
                    final List<String> players = new ArrayList<>();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (isPlayerVanished(player.getName())
                                || player.hasPermission(SpaceChatConfigKeys.PERMISSIONS_UNLISTED.get(plugin.getSpaceChatConfig().getAdapter()))) {
                            continue;
                        }
                        players.add(player.getName());
                    }
                    setOnlinePlayers(id, players);
                    setOnlinePlayers(id, SpaceChatConfigKeys.FAKE_PLAYERS.get(plugin.getSpaceChatConfig().getAdapter()));
                    plugin.getServerSyncServiceManager().getStreamService().publishPlayerList(id, players);
                }
                onlinePlayers = new HashMap<>(cachedOnlinePlayers);
                cachedOnlinePlayers.clear();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            onUpdate = false;
        }, 200L, 80L);
    }

    /**
     * Uses user with a consumer
     *
     * @param uuid     uuid
     * @param consumer consumer
     */
    public void use(UUID uuid, Consumer<User> consumer) {
        userAsyncCache.get(uuid).thenAccept(consumer);
    }

    /**
     * Returns a user
     *
     * @param uuid uuid
     * @return user
     */
    public User get(UUID uuid) {
        return userAsyncCache.get(uuid).join();
    }

    /**
     * Invalidates user
     *
     * @param uuid uuid
     * @param name name
     */
    public void invalidate(UUID uuid, String name) {
        ignoredList.remove(name);
        replyTarget.remove(name);
        vanishedPlayers.remove(name);
        userAsyncCache.synchronous().invalidate(uuid);
    }

    /**
     * Change the current vanish status for player
     *
     * @param name Player name
     * @return     true if the player was vanished
     */
    public boolean vanish(String name) {
        return vanish(name, !vanishedPlayers.contains(name));
    }

    /**
     * Change the current vanish status for player
     *
     * @param name     Player name
     * @param activate true to vanish the player
     * @return         true if the player was vanished
     */
    public boolean vanish(String name, boolean activate) {
        if (activate) {
            vanishedPlayers.add(name);
            return true;
        } else {
            vanishedPlayers.remove(name);
            return false;
        }
    }

    /**
     * Update a user
     *
     * @param user user
     */
    public void update(User user) {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> storageManager.getCurrent().updateUser(user));
        } else {
            storageManager.getCurrent().updateUser(user);
        }
    }

    public void loadIgnoreList(String name) {
        List<String> list = plugin.getStorageManager().getCurrent().getIgnoreList(name);
        if (list == null) {
            pendingReload.add(name);
        } else {
            ignoredList.put(name, list);
        }
    }

    /**
     * Uses user with a consumer
     *
     * @param username username
     * @param consumer consumer
     */
    public void getByName(String username, Consumer<User> consumer) {
        CompletableFuture.supplyAsync(() -> storageManager.getCurrent().getUser(username)).thenAccept(consumer);
    }

    /**
     * Get reply target map
     *
     * @return Reply Target Map
     */
    public Map<String, String> getReplyTargetMap() {
        return replyTarget;
    }


    /**
     * Get ignore list map
     *
     * @return ignore list map
     */
    public Map<String, List<String>> getIgnoredList() {
        return ignoredList;
    }

    /**
     * Get ignore list for user
     *
     * @return ignore list for user
     */
    public List<String> getIgnoredList(String name) {
        final List<String> list = ignoredList.get(name);
        return list != null ? list : List.of();
    }


    /**
        Get reply target uuid
     */
    public String getReplyTarget(String sender){
        return replyTarget.get(sender);
    }

    /**
     * Get vanished players
     *
     * @return A set with vanished player names
     */
    public Set<String> getVanishedPlayers() {
        return vanishedPlayers;
    }

    /**
     * Get current online players separated by server identifiers
     *
     * @return A multimap with player names.
     */
    public Map<String, String> getOnlinePlayers() {
        return onlinePlayers;
    }

    public Map<String, List<String>> getOnlinePlayersByServer() {
        if (onlinePlayers.isEmpty()) {
            return new HashMap<>();
        }
        final Map<String, List<String>> map = new HashMap<>();
        for (var entry : onlinePlayers.entrySet()) {
            map.computeIfAbsent(entry.getValue(), s -> new ArrayList<>()).add(entry.getKey());
        }
        return map;
    }

    /**
     * Returns all
     *
     * @return all
     */
    public Map<UUID, User> getAll() {
        return userAsyncCache.synchronous().asMap();
    }

    public void setOnlinePlayers(String id, List<String> players) {
        for (String player : players) {
            cachedOnlinePlayers.put(player, id);
        }
    }

    public boolean isPlayerLoaded(String name) {
        boolean loaded = ignoredList.containsKey(name);
        if (loaded) {
            return true;
        }
        if (pendingReload.contains(name)) {
            pendingReload.remove(name);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadIgnoreList(name));
        }
        return false;
    }

    public boolean isPlayerOnline(String name) {
        return name.equalsIgnoreCase("@console") || onlinePlayers.containsKey(name);
    }

    public boolean isPlayerVanished(String name) {
        return vanishedPlayers.contains(name);
    }
}
