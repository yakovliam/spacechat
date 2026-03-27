package dev.spaceseries.spacechat.sync.provider.redis;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import dev.spaceseries.spacechat.sync.provider.Provider;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.RedisClient;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RedisProvider implements Provider<RedisClient> {

    /**
     * Client
     */
    private final RedisClient client;

    /**
     * Construct redis provider
     */
    public RedisProvider(SpaceChatPlugin plugin) {
        final String url = SpaceChatConfigKeys.REDIS_URL.get(plugin.getSpaceChatConfig().getAdapter());
        this.client = RedisClient.create(url);
    }

    @Override
    public RedisClient provide() {
        return client;
    }

    public void run(@NotNull Consumer<RedisClient> consumer) {
        try {
            consumer.accept(client);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public <T> T get(@NotNull Function<RedisClient, T> function) {
        try {
            return function.apply(client);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public <T> T get(@NotNull Function<RedisClient, T> function, @NotNull Supplier<T> optional) {
        try {
            return function.apply(client);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return optional.get();
    }

    /**
     * Ends the provided pool
     */
    public void end() {
        try {
            this.client.close();
        } catch (Throwable ignored) { }
        try {
            this.client.getPool().close();
        } catch (Throwable ignored) { }
    }
}
