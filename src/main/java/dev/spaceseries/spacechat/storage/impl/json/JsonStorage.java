package dev.spaceseries.spacechat.storage.impl.json;

import com.saicone.ezlib.Dependency;
import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.logging.wrap.LogWrapper;
import dev.spaceseries.spacechat.model.User;
import dev.spaceseries.spacechat.storage.Storage;
import dev.spaceseries.spacechat.storage.StorageInitializationException;
import org.bukkit.Bukkit;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.time.Instant;
import java.util.*;

@Dependency(value = "org.spongepowered:configurate-gson:4.2.0",
        relocate = {
                "org.spongepowered.configurate", "{package}.lib.configurate",
                "io.leangen.geantyref", "{package}.lib.geantyref",
                "org.checkerframework", "{package}.lib.checkerframework",
                "com.google.errorprone.annotations", "{package}.lib.annotations.errorprone",
                "net.kyori.option", "{package}.lib.option"
        }
)
public class JsonStorage extends Storage {

    /**
     * Configurate provider
     */
    private final JsonConfigurateProvider jsonConfigurateProvider;

    /**
     * Json storage
     *
     * @param plugin plugin
     */
    public JsonStorage(SpaceChatPlugin plugin) throws StorageInitializationException {
        super(plugin);
        this.jsonConfigurateProvider = new JsonConfigurateProvider(plugin);

        this.init();
    }

    /**
     * Initializes the storage medium
     */
    @Override
    public void init() throws StorageInitializationException {
        jsonConfigurateProvider.init();
    }

    /**
     * Logs to the storage medium
     *
     * @param data  The data to log
     * @param async async
     */
    @Override
    public void log(LogWrapper data, boolean async) {
        BasicConfigurationNode logs = jsonConfigurateProvider.provideRoot().node("logs");
        Runnable task = () -> {
            try {
                List<LogWrapper> list = logs.getList(LogWrapper.class);
                list.add(data);

                logs.setList(LogWrapper.class, list);

                save();
            } catch (SerializationException e) {
                e.printStackTrace();
            }
        };

        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * Gets a user
     *
     * @param uuid uuid
     * @return user
     */
    @Override
    public User getUser(UUID uuid) {
        BasicConfigurationNode users = jsonConfigurateProvider.provideRoot().node("users");

        // get users
        try {
            List<User> usersList = users.getList(User.class);
            // get user
            User user = usersList.stream()
                    .filter(u -> u.getUuid().equals(uuid))
                    .findFirst()
                    .orElse(null);

            if (user == null) {
                // since it's null, create it
                user = new User(plugin, uuid, null, Date.from(Instant.now()), new ArrayList<>());

                // add to configuration file
                usersList.add(user);
                users.setList(User.class, usersList);

                // save
                save();

                // return
                return user;
            } else {
                // return
                return user;
            }

        } catch (SerializationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets a user by their username
     *
     * @param username username
     * @return user
     */
    @Override
    public User getUser(String username) {
        BasicConfigurationNode users = jsonConfigurateProvider.provideRoot().node("users");

        // get users
        try {
            List<User> usersList = users.getList(User.class);

            // get and return
            return usersList.stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst()
                    .orElse(null);

        } catch (SerializationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Updates a user
     *
     * @param user user
     */
    @Override
    public void updateUser(User user) {
        BasicConfigurationNode users = jsonConfigurateProvider.provideRoot().node("users");

        // get users
        try {
            List<User> usersList = users.getList(User.class);

            // remove if exists
            usersList.removeIf(u -> u.getUuid().equals(user.getUuid()));

            // add to list
            usersList.add(user);

            users.setList(User.class, usersList);

            // save
            save();

        } catch (SerializationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void createIgnoredUser(String username, String ignoredUsername) {

    }

    @Override
    public void deleteIgnoredUser(String username, String ignoredUsername) {

    }

    @Override
    public List<String> getIgnoreList(String username) {
        return null;
    }

    @Override
    public boolean isIgnored(String username, String ignoredUsername) {
        return false;
    }


    /**
     * Closes the storage medium
     */
    @Override
    public void close() {
        save();
    }

    /**
     * Saves the file
     */
    private void save() {
        jsonConfigurateProvider.save(jsonConfigurateProvider.provideRoot());
    }
}
