package dev.spaceseries.spacechat.storage.impl.sql.mysql.factory;

import com.saicone.ezlib.Dependency;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.spaceseries.spacechat.storage.impl.ConnectionFactory;
import dev.spaceseries.spacechat.storage.impl.sql.mysql.StorageCredentials;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Abstract {@link ConnectionFactory} using a {@link HikariDataSource}.
 */
@Dependency(value = "com.zaxxer:HikariCP:6.3.0",
        relocate = {
                "com.zaxxer.hikari", "{package}.lib.hikari"
        }
)
public abstract class HikariConnectionFactory implements ConnectionFactory {
    private final StorageCredentials configuration;
    private HikariDataSource hikari;

    public HikariConnectionFactory(StorageCredentials configuration) {
        this.configuration = configuration;
    }

    /**
     * Gets the default port used by the database
     *
     * @return the default port
     */
    protected abstract String defaultPort();

    /**
     * Configures the {@link HikariConfig} with the relevant database properties.
     *
     * <p>Each driver does this slightly differently...</p>
     *
     * @param config       the hikari config
     * @param address      the database address
     * @param port         the database port
     * @param databaseName the database name
     * @param username     the database username
     * @param password     the database password
     */
    protected abstract void configureDatabase(HikariConfig config, String address, String port, String databaseName, String username, String password);

    /**
     * Allows the connection factory instance to override certain properties before they are set.
     *
     * @param properties the current properties
     */
    protected void overrideProperties(Map<String, String> properties) {
        // https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery
        properties.putIfAbsent("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));
    }

    /**
     * Sets the given connection properties onto the config.
     *
     * @param config     the hikari config
     * @param properties the properties
     */
    protected void setProperties(HikariConfig config, Map<String, String> properties) {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            config.addDataSourceProperty(property.getKey(), property.getValue());
        }
    }

    /**
     * Called after the Hikari pool has been initialised
     */
    protected void postInitialize() {

    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();

        // set pool name so the logging output can be linked back to us
        config.setPoolName("spacechat-hikari");

        // get the database info/credentials from the config file
        String[] addressSplit = this.configuration.getAddress().split(":");
        String address = addressSplit[0];
        String port = addressSplit.length > 1 ? addressSplit[1] : defaultPort();

        // allow the implementation to configure the HikariConfig appropriately with these values
        configureDatabase(config, address, port, this.configuration.getDatabase(), this.configuration.getUsername(), this.configuration.getPassword());

        // get the extra connection properties from the config
        Map<String, String> properties = new HashMap<>(this.configuration.getProperties());

        // allow the implementation to override/make changes to these properties
        overrideProperties(properties);

        // set the properties
        setProperties(config, properties);

        // configure the connection pool
        config.setMaximumPoolSize(this.configuration.getMaxPoolSize());
        config.setMinimumIdle(this.configuration.getMinIdleConnections());
        config.setMaxLifetime(this.configuration.getMaxLifetime());
        config.setKeepaliveTime(this.configuration.getKeepAliveTime());
        config.setConnectionTimeout(this.configuration.getConnectionTimeout());

        // don't perform any initial connection validation - we subsequently call #getConnection
        // to setup the schema anyways
        config.setInitializationFailTimeout(-1);

        this.hikari = new HikariDataSource(config);

        postInitialize();
    }

    @Override
    public void shutdown() {
        if (this.hikari != null) {
            this.hikari.close();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (this.hikari == null) {
            throw new SQLException("Unable to get a connection from the pool. (hikari is null)");
        }

        Connection connection = this.hikari.getConnection();
        if (connection == null) {
            throw new SQLException("Unable to get a connection from the pool. (getConnection returned null)");
        }

        return connection;
    }
}