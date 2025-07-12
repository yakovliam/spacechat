package dev.spaceseries.spacechat;

import com.saicone.ezlib.Dependencies;
import com.saicone.ezlib.Dependency;
import com.saicone.ezlib.EzlibLoader;
import dev.spaceseries.spacechat.api.config.adapter.BukkitConfigAdapter;
import dev.spaceseries.spacechat.api.message.Message;
import dev.spaceseries.spacechat.channel.ChannelManager;
import dev.spaceseries.spacechat.chat.ChatFormatManager;
import dev.spaceseries.spacechat.chat.ChatManager;
import dev.spaceseries.spacechat.command.CommandManager;
import dev.spaceseries.spacechat.config.SpaceChatConfig;
import dev.spaceseries.spacechat.external.papi.SpaceChatExpansion;
import dev.spaceseries.spacechat.listener.ChatListener;
import dev.spaceseries.spacechat.listener.JoinQuitListener;
import dev.spaceseries.spacechat.listener.VanishListener;
import dev.spaceseries.spacechat.logging.LogManagerImpl;
import dev.spaceseries.spacechat.storage.StorageManager;
import dev.spaceseries.spacechat.sync.ServerSyncServiceManager;
import dev.spaceseries.spacechat.user.UserManager;
import dev.spaceseries.spacechat.util.version.VersionUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Dependencies(value = {
        @Dependency(
                value = "org.bstats:bstats-bukkit:3.1.0",
                relocate = {"org.bstats", "{package}.lib.bstats"}),
        @Dependency(
                value = "com.github.cryptomorin:XSeries:13.2.0",
                relocate = {"com.cryptomorin.xseries", "{package}.lib.xseries"})
}, relocations = {
        "org.jetbrains.annotations", "{package}.lib.annotations",
        "org.intellij.lang.annotations", "{package}.lib.annotations.lang"
})
public final class SpaceChatPlugin extends JavaPlugin {

    @SuppressWarnings("all")
    private static final boolean PAPER_PLATFORM = "paper.platform".equalsIgnoreCase("true");
    @SuppressWarnings("all")
    private static final boolean LOAD_ADVENTURE = "load.adventure".equalsIgnoreCase("true");

    /**
     * The library loader
     */
    private EzlibLoader libraryLoader;

    /**
     * The formats config
     */
    private SpaceChatConfig formatsConfig;

    /**
     * The main plugin config
     */
    private SpaceChatConfig spaceChatConfig;

    /**
     * The plugin's language configuration
     */
    private SpaceChatConfig langConfig;

    /**
     * The chat format manager
     */
    private ChatFormatManager chatFormatManager;

    /**
     * The log manager
     */
    private LogManagerImpl logManagerImpl;

    /**
     * User manager
     */
    private UserManager userManager;

    /**
     * The storage manager
     */
    private StorageManager storageManager;

    /**
     * Server sync service manager
     */
    private ServerSyncServiceManager serverSyncServiceManager;

    /**
     * Channel manager
     */
    private ChannelManager channelManager;

    /**
     * Channels config
     */
    private SpaceChatConfig channelsConfig;

    /**
     * Chat manager
     */
    private ChatManager chatManager;

    @Override
    public void onLoad() {
        getLogger().info("We're currently downloading some data to make this plugin work correctly, so please wait. This may take a while.");
        libraryLoader = new EzlibLoader()
                .replace("{package}", "dev.spaceseries.spacechat")
                .logger((level, msg) -> {
                    switch (level) {
                        case 3:
                            getLogger().info(msg);
                            break;
                        case 2:
                            getLogger().warning(msg);
                            break;
                        case 1:
                            getLogger().severe(msg);
                            break;
                        default:
                            break;
                    }
                })
                .condition("paper", EzlibLoader.Condition.valueOf(PAPER_PLATFORM))
                .condition("adventure", EzlibLoader.Condition.valueOf(LOAD_ADVENTURE))
                .load();
    }

    /**
     * Runs on enable
     */
    @Override
    public void onEnable() {
        if (!PAPER_PLATFORM) {
            Message.initAudience(this);
        }

        // load configs
        loadConfigs();
        // load formats
        loadFormats();
        // load chat manager
        loadChatManager();
        // load connection managers
        loadSyncServices();

        // initialize sync services, since they have to be instantiated and used after the chat manager
        // is created
        this.chatManager.initSyncServices();

        // load channels
        loadChannels();
        // load storage
        loadStorage();
        // load users
        loadUsers();

        // initialize messages
        Messages.renew();
        Messages.getInstance(this);

        // initialize log manager
        logManagerImpl = new LogManagerImpl(this);

        // initialize commands
        new CommandManager(this);

        // register chat listener
        this.getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        // register join listener
        this.getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);
        new VanishListener(this).registerEvents();

        // register placeholder expansion
        new SpaceChatExpansion(this).register();

        // initialize metrics
        new Metrics(this, 7508);

        // log initialization method
        this.getLogger().info("Detected that SpaceChat is running under " + VersionUtil.getServerBukkitVersion().toString());

        // initialize file watcher
        // new FileWatcher(this);
    }

    @Override
    public void onDisable() {
        // stop redis supervisor
        if (serverSyncServiceManager != null)
            serverSyncServiceManager.end();

        // close the storage connection pool (if applicable)
        if (storageManager != null && storageManager.getCurrent() != null) {
            // save all users
            userManager.getAll().forEach((key, value) -> storageManager.getCurrent().updateUser(value));
            storageManager.getCurrent().close();
        }
    }

    /**
     * Reloads configurations
     */
    public void loadConfigs() {
        // initialize configs
        if (spaceChatConfig != null) {
            spaceChatConfig.reload();
        } else {
            spaceChatConfig = new SpaceChatConfig(provideConfigAdapter("config.yml"));
        }

        if (formatsConfig != null) {
            formatsConfig.reload();
        } else {
            formatsConfig = new SpaceChatConfig(provideConfigAdapter("formats.yml"));
        }

        if (channelsConfig != null) {
            channelsConfig.reload();
        } else {
            channelsConfig = new SpaceChatConfig(provideConfigAdapter("channels.yml"));
        }

        if (langConfig != null) {
            langConfig.reload();
        } else {
            langConfig = new SpaceChatConfig(provideConfigAdapter("lang.yml"));
        }
    }

    /**
     * Loads formats
     */
    public void loadFormats() {
        // initialize chat format manager (also loads all formats!)
        chatFormatManager = new ChatFormatManager(this);
    }

    /**
     * Loads channels
     */
    public void loadChannels() {
        this.channelManager = new ChannelManager(this);
    }

    /**
     * Loads storage manager
     */
    public void loadStorage() {
        // initialize storage
        storageManager = new StorageManager(this);
    }

    /**
     * Loads the chat manager
     */
    public void loadChatManager() {
        this.chatManager = new ChatManager(this);
    }

    /**
     * Loads users
     * <p>
     * More often than not, this will NOT be called on the reload command
     */
    public void loadUsers() {
        // initialize users
        userManager = new UserManager(this);
    }

    /**
     * Loads connection managers
     */
    public void loadSyncServices() {
        // stop sync service supervisor
        if (serverSyncServiceManager != null)
            serverSyncServiceManager.end();

        // initialize
        this.serverSyncServiceManager = new ServerSyncServiceManager(this);
    }

    /**
     * Resolves a configuration
     *
     * @param fileName file name
     * @return configuration path
     */
    public Path resolveConfig(String fileName) {
        Path configFile = getDataFolder().toPath().resolve(fileName);

        // if the config doesn't exist, create it based on the template in the resources dir
        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFile.getParent());
            } catch (IOException e) {
                // ignore
            }

            try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
                Files.copy(is, configFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return configFile;
    }

    /**
     * Provides a configuration adapter
     *
     * @param fileName file name
     * @return config adapter
     */
    private BukkitConfigAdapter provideConfigAdapter(String fileName) {
        return new BukkitConfigAdapter(this, resolveConfig(fileName).toFile());
    }

    /**
     * Loads messages
     */
    public void loadMessages() {
        Messages.renew();
    }

    /**
     * Returns the library loader
     *
     * @return library loader
     */
    public EzlibLoader getLibraryLoader() {
        return libraryLoader;
    }

    /**
     * Returns the formats config
     *
     * @return formats config
     */
    public SpaceChatConfig getFormatsConfig() {
        return formatsConfig;
    }

    /**
     * Returns the language config
     *
     * @return language config
     */
    public SpaceChatConfig getLangConfig() {
        return langConfig;
    }

    /**
     * Returns the channels config
     *
     * @return channels config
     */
    public SpaceChatConfig getChannelsConfig() {
        return channelsConfig;
    }

    /**
     * Returns the main space chat config
     *
     * @return config
     */
    public SpaceChatConfig getSpaceChatConfig() {
        return spaceChatConfig;
    }

    /**
     * Returns chat format manager
     *
     * @return chat format manager
     */
    public ChatFormatManager getChatFormatManager() {
        return chatFormatManager;
    }

    /**
     * Returns channel manager
     *
     * @return channel manager
     */
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
     * Returns log manager implementation
     *
     * @return log manager
     */
    public LogManagerImpl getLogManagerImpl() {
        return logManagerImpl;
    }

    /**
     * Returns storage manager
     *
     * @return storage manager
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }

    /**
     * Returns server sync service manager
     *
     * @return server sync service manager
     */
    public ServerSyncServiceManager getServerSyncServiceManager() {
        return serverSyncServiceManager;
    }

    /**
     * Returns user manager
     *
     * @return user manager
     */
    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * Returns chat manager
     *
     * @return chat manager
     */
    public ChatManager getChatManager() {
        return chatManager;
    }
}
