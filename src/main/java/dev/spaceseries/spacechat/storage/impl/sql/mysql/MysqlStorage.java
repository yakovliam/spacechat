package dev.spaceseries.spacechat.storage.impl.sql.mysql;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import dev.spaceseries.spacechat.logging.wrap.LogChatWrapper;
import dev.spaceseries.spacechat.logging.wrap.LogType;
import dev.spaceseries.spacechat.logging.wrap.LogWrapper;
import dev.spaceseries.spacechat.model.Channel;
import dev.spaceseries.spacechat.model.User;
import dev.spaceseries.spacechat.storage.Storage;
import dev.spaceseries.spacechat.storage.StorageInitializationException;
import dev.spaceseries.spacechat.storage.impl.sql.mysql.factory.MySqlConnectionFactory;
import dev.spaceseries.spacechat.util.date.DateUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static dev.spaceseries.spacechat.config.SpaceChatConfigKeys.*;

public class MysqlStorage extends Storage {

    public static final String LOG_CHAT_CREATION_STATEMENT = "CREATE TABLE IF NOT EXISTS `%s` (\n" +
            "`uuid` TEXT NOT NULL,\n" +
            "`name` TEXT,\n" +
            "`message` TEXT,\n" +
            "`date` TEXT NOT NULL,\n" +
            "`id` INT NOT NULL AUTO_INCREMENT,\n" +
            "PRIMARY KEY (`id`)\n" +
            ");";
    private final String LOG_CHAT = "INSERT INTO " + STORAGE_MYSQL_TABLES_CHAT_LOGS.get(plugin.getSpaceChatConfig().getAdapter()) + " (uuid, name, message, date) VALUES(?, ?, ?, ?);";
    public static final String USERS_CREATION_STATEMENT = "CREATE TABLE IF NOT EXISTS `%s` (\n" +
            "`uuid` TEXT NOT NULL,\n" +
            "`username` TEXT NOT NULL,\n" +
            "`date` TEXT NOT NULL,\n" +
            "`id` INT NOT NULL AUTO_INCREMENT,\n" +
            "PRIMARY KEY (`id`)\n" +
            ");";
    private final String CREATE_USER = "INSERT INTO " + STORAGE_MYSQL_TABLES_USERS.get(plugin.getSpaceChatConfig().getAdapter()) + " (uuid, username, date) VALUES(?, ?, ?);";
    private final String SELECT_USER = "SELECT * FROM " + STORAGE_MYSQL_TABLES_USERS.get(plugin.getSpaceChatConfig().getAdapter()) + " WHERE uuid=?;";
    private final String SELECT_USER_USERNAME = "SELECT * FROM " + STORAGE_MYSQL_TABLES_USERS.get(plugin.getSpaceChatConfig().getAdapter()) + " WHERE username=?;";
    private final String UPDATE_USER = "UPDATE " + STORAGE_MYSQL_TABLES_USERS.get(plugin.getSpaceChatConfig().getAdapter()) + " SET username=? WHERE uuid=?;";
    public static final String USERS_SUBSCRIBED_CHANNELS_CREATION_STATEMENT = "CREATE TABLE IF NOT EXISTS `%s` (\n" +
            "`uuid` TEXT NOT NULL,\n" +
            "`channel` TEXT NOT NULL,\n" +
            "`id` INT NOT NULL AUTO_INCREMENT,\n" +
            "PRIMARY KEY (`id`)\n" +
            ");";
    private final String SELECT_SUBSCRIBED_CHANNELS = "SELECT channel FROM " + STORAGE_MYSQL_TABLES_SUBSCRIBED_CHANNELS.get(plugin.getSpaceChatConfig().getAdapter()) + " WHERE uuid=?;";
    private final String DELETE_SUBSCRIBED_CHANNEL = "DELETE FROM " + STORAGE_MYSQL_TABLES_SUBSCRIBED_CHANNELS.get(plugin.getSpaceChatConfig().getAdapter()) + " WHERE uuid=? AND channel=?;";
    private final String INSERT_SUBSCRIBED_CHANNEL = "INSERT INTO " + STORAGE_MYSQL_TABLES_SUBSCRIBED_CHANNELS.get(plugin.getSpaceChatConfig().getAdapter()) + " (uuid, channel) VALUES(?, ?);";


    public static final String IGNORE_CREATION_STATEMENT = "CREATE TABLE IF NOT EXISTS `%s` (\n" +
            "`username` TEXT NOT NULL,\n" +
            "`ignored_username` TEXT NOT NULL,\n" +
            "`id` INT NOT NULL AUTO_INCREMENT,\n" +
            "PRIMARY KEY (`id`)\n" +
            ");";

    private final String CREATE_IGNORE_USER = "INSERT INTO " + STORAGE_MYSQL_TABLES_IGNORE
            .get(plugin.getSpaceChatConfig().getAdapter()) + " (username, ignored_username) VALUES(?, ?);";

    private final String GET_IGNORE_USERS = "SELECT * FROM " + STORAGE_MYSQL_TABLES_IGNORE
            .get(plugin.getSpaceChatConfig().getAdapter()) + " WHERE username=?;";

    private final String IS_IGNORED = "SELECT * FROM " + STORAGE_MYSQL_TABLES_IGNORE
            .get(plugin.getSpaceChatConfig().getAdapter()) + " WHERE username=? AND ignored_username=?;";

    private final String DELETE_IGNORE_USER = "DELETE FROM " + STORAGE_MYSQL_TABLES_IGNORE
            .get(plugin.getSpaceChatConfig().getAdapter()) + " WHERE username=? AND ignored_username=?;";

    /**
     * The connection manager
     */
    private final MySqlConnectionFactory mysqlConnectionFactory;

    /**
     * Initializes new mysql storage
     */
    public MysqlStorage(SpaceChatPlugin plugin) throws StorageInitializationException {
        super(plugin);
        // initialize new connection manager
        mysqlConnectionFactory = new MySqlConnectionFactory(plugin.getSpaceChatConfig().get(SpaceChatConfigKeys.DATABASE_VALUES));
        this.mysqlConnectionFactory.init();

        this.init();
    }

    /**
     * Initializes the storage medium
     */
    @Override
    public void init() throws StorageInitializationException {
        try {
            SqlHelper.execute(mysqlConnectionFactory.getConnection(), String.format(MysqlStorage.LOG_CHAT_CREATION_STATEMENT, STORAGE_MYSQL_TABLES_CHAT_LOGS.get(plugin.getSpaceChatConfig().getAdapter())));
            SqlHelper.execute(mysqlConnectionFactory.getConnection(), String.format(MysqlStorage.USERS_CREATION_STATEMENT, STORAGE_MYSQL_TABLES_USERS.get(plugin.getSpaceChatConfig().getAdapter())));
            SqlHelper.execute(mysqlConnectionFactory.getConnection(), String.format(MysqlStorage.USERS_SUBSCRIBED_CHANNELS_CREATION_STATEMENT, STORAGE_MYSQL_TABLES_SUBSCRIBED_CHANNELS.get(plugin.getSpaceChatConfig().getAdapter())));
            SqlHelper.execute(mysqlConnectionFactory.getConnection(), String.format(MysqlStorage.IGNORE_CREATION_STATEMENT, STORAGE_MYSQL_TABLES_IGNORE.get(plugin.getSpaceChatConfig().getAdapter())));
        } catch (SQLException e) {
            e.printStackTrace();
            throw new StorageInitializationException();
        }
    }

    @Override
    public void log(LogWrapper data, boolean async) {
        // if chat
        if (data.getLogType() == LogType.CHAT) {
            logChat((LogChatWrapper) data, async);
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
        // create prepared statement
        try (Connection connection = mysqlConnectionFactory.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(SELECT_USER)) {
            // replace
            preparedStatement.setString(1, uuid.toString());

            // execute
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                String username = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName())
                        .orElse("");

                // create new user
                User user = new User(plugin, uuid, username, new Date(), new ArrayList<>());
                createUser(user);
                return user;
            }

            // build user and return
            String username = resultSet.getString("username");
            Date date = DateUtil.fromString(resultSet.getString("date"));

            // get channels that are subscribed
            List<Channel> subscribedChannels = getSubscribedChannels(uuid);

            return new User(plugin, uuid, username, date, subscribedChannels);
        } catch (SQLException throwables) {
            throwables.printStackTrace();

            return null;
        }
    }

    /**
     * Returns subscribed channels
     *
     * @return channels
     */
    private List<Channel> getSubscribedChannels(UUID uuid) {
        try (Connection connection = mysqlConnectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SUBSCRIBED_CHANNELS)) {
            // replace
            preparedStatement.setString(1, uuid.toString());

            // execute
            ResultSet resultSet = preparedStatement.executeQuery();

            List<Channel> channels = new ArrayList<>();

            while (resultSet.next()) {
                String channelHandle = resultSet.getString("channel");

                Channel channel = plugin.getChannelManager().get(channelHandle, null);
                if (channel != null) {
                    channels.add(channel);
                }
            }

            return channels;
        } catch (SQLException throwables) {
            throwables.printStackTrace();

            return new ArrayList<>();
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
        // create prepared statement
        try (Connection connection = mysqlConnectionFactory.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(SELECT_USER_USERNAME)) {
            // replace
            preparedStatement.setString(1, username);

            // execute
            ResultSet resultSet = preparedStatement.executeQuery();

            if (!resultSet.next()) {
                return null;
            }

            // build user and return
            UUID uuid = UUID.fromString(resultSet.getString("uuid"));
            Date date = DateUtil.fromString(resultSet.getString("date"));

            // get channels that are subscribed
            List<Channel> subscribedChannels = getSubscribedChannels(uuid);

            return new User(plugin, uuid, username, date, subscribedChannels);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a new user in the database
     *
     * @param user user
     */
    private void createUser(User user) {
        // create prepared statement
        try (Connection connection = mysqlConnectionFactory.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(CREATE_USER)) {
            // replace
            preparedStatement.setString(1, user.getUuid().toString());
            preparedStatement.setString(2, user.getUsername());
            preparedStatement.setString(3, DateUtil.toString(user.getDate()));

            // execute
            preparedStatement.execute();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Updates a user
     *
     * @param user user
     */
    @Override
    public void updateUser(User user) {
        // create prepared statement
        try (Connection connection = mysqlConnectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USER)) {
            // replace
            preparedStatement.setString(1, user.getUsername());
            preparedStatement.setString(2, user.getUuid().toString());

            // execute
            preparedStatement.execute();

            // delete remaining channels that shouldn't be there
            List<Channel> serverSideSubscribedList = getSubscribedChannels(user.getUuid());
            serverSideSubscribedList.forEach(serverSideSubscribedChannel -> {
                if (user.getSubscribedChannels().stream()
                        .noneMatch(c -> c.getHandle().equals(serverSideSubscribedChannel.getHandle()))) {
                    deleteChannelRow(user.getUuid(), serverSideSubscribedChannel);
                }
            });

            user.getSubscribedChannels().forEach(channel -> {
                if (serverSideSubscribedList.stream()
                        .anyMatch(c -> c.getHandle().equals(channel.getHandle()))) {
                    return;
                }
                insertChannelRow(user.getUuid(), channel);
            });

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Creates a new ignored user in the database
     *
     * @param username username
     * @param ignoredUsername ignored username
     */
    @Override
    public void createIgnoredUser(String username, String ignoredUsername) {
        // create prepared statement
        try (Connection connection = mysqlConnectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(CREATE_IGNORE_USER)) {
            // replace
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, ignoredUsername);
            // execute
            preparedStatement.execute();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Delete an ignored user in the database
     *
     * @param username username
     * @param ignoredUsername ignored username
     */
    @Override
    public void deleteIgnoredUser(String username, String ignoredUsername) {
        // create prepared statement
        try (Connection connection = mysqlConnectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_IGNORE_USER)) {
            // replace
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, ignoredUsername);
            // execute
            preparedStatement.execute();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


    /**
     * Get the list of ignored users of a user
     *
     * @param username username
     * @return list of ignored users
     */
    @Override
    public List<String> getIgnoreList(String username) {
        // create prepared statement
        try (Connection connection = mysqlConnectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(GET_IGNORE_USERS)) {
            preparedStatement.setString(1, username);
            ResultSet rs = preparedStatement.executeQuery();
            final List<String> ignoredUserList = new ArrayList<>();
            while (rs.next()){
                String ignoredUsername = rs.getString("ignored_username");
                ignoredUserList.add(ignoredUsername);
            }
            return ignoredUserList;
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    /**
     * Check if a player has ignored another
     *
     * @param username username
     * @param ignoredUsername ignoredUsername
     * @return true or false
     */
    @Override
    public boolean isIgnored(String username, String ignoredUsername) {
        try (Connection connection = mysqlConnectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(IS_IGNORED)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, ignoredUsername);
            ResultSet rs = preparedStatement.executeQuery();
            return rs.next();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts a channel row
     *
     * @param uuid    uuid
     * @param channel channel
     */
    private void insertChannelRow(UUID uuid, Channel channel) {
        // create prepared statement
        try (Connection connection = mysqlConnectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SUBSCRIBED_CHANNEL)) {
            // replace
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setString(2, channel.getHandle());

            // execute
            preparedStatement.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Deletes a channel row
     *
     * @param uuid    uuid
     * @param channel channel
     */
    private void deleteChannelRow(UUID uuid, Channel channel) {
        // create prepared statement
        try (Connection connection = mysqlConnectionFactory.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SUBSCRIBED_CHANNEL)) {
            // replace
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setString(2, channel.getHandle());

            // execute
            preparedStatement.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public void close() {
        this.mysqlConnectionFactory.shutdown();
    }

    /**
     * Logs chat to the MySQL database
     *
     * @param data  The data
     * @param async async
     */
    private void logChat(LogChatWrapper data, boolean async) {
        Runnable task = () -> {
            // create prepared statement
            try (Connection connection = mysqlConnectionFactory.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(LOG_CHAT)) {
                // replace
                preparedStatement.setString(1, data.getSenderUUID().toString());
                preparedStatement.setString(2, data.getSenderName());
                preparedStatement.setString(3, MiniMessage.miniMessage().serialize(data.getMessage()));
                preparedStatement.setString(4, DateUtil.toString(data.getAt()));

                // execute
                preparedStatement.execute();

            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        };

        if (async)
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        else
            Bukkit.getScheduler().runTask(plugin, task);
    }
}
