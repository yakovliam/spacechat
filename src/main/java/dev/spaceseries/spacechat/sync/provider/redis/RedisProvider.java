package dev.spaceseries.spacechat.sync.provider.redis;

import dev.spaceseries.spacechat.SpaceChatPlugin;
import dev.spaceseries.spacechat.config.SpaceChatConfigKeys;
import dev.spaceseries.spacechat.sync.provider.Provider;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisDataException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;
import java.util.function.Function;

public class RedisProvider implements Provider<JedisPool> {

    /**
     * Pool
     */
    private JedisPool pool;
    private String password;

    /**
     * Construct redis provider
     */
    public RedisProvider(SpaceChatPlugin plugin) {
        String url = SpaceChatConfigKeys.REDIS_URL.get(plugin.getSpaceChatConfig().getAdapter());
        if (url.contains("@")) {
            String s = url.substring(0, url.lastIndexOf("@"));
            if (s.contains(":")) {
                this.password = s.substring(s.lastIndexOf(":") + 1);
            }
        }
        try {
            // initialize pool
            pool = new JedisPool(new URI(url));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            pool = null;
        }
    }

    @Override
    public JedisPool provide() {
        return pool;
    }

    public PoolConsumer consumer(Consumer<Jedis> consumer) {
        return new PoolConsumer(consumer);
    }

    public <T> PoolFunction<T> function(Function<Jedis, T> function) {
        return new PoolFunction<>(function);
    }

    /**
     * Ends the provided pool
     */
    public void end() {
        if (this.pool != null && !this.pool.isClosed()) {
            this.pool.close();
        }
    }

    public class PoolConsumer {

        private final Consumer<Jedis> delegate;

        public PoolConsumer(Consumer<Jedis> delegate) {
            this.delegate = delegate;
        }

        public void accept(Jedis jedis) {
            delegate.accept(jedis);
        }

        public void run() {
            try (Jedis jedis = pool.getResource()) {
                try {
                    accept(jedis);
                } catch (JedisDataException e) {
                    // Fix Java +16 error
                    if (e.getMessage().contains("NOAUTH") || e.getMessage().contains("unauthenticated")) {
                        jedis.auth(password);
                        accept(jedis);
                    } else {
                        e.printStackTrace();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public class PoolFunction<T> {

        private final Function<Jedis, T> delegate;
        private T optional = null;

        public PoolFunction(Function<Jedis, T> delegate) {
            this.delegate = delegate;
        }

        public T apply(Jedis jedis) {
            return delegate.apply(jedis);
        }

        public PoolFunction<T> or(T optional) {
            this.optional = optional;
            return this;
        }

        public T get() {
            try (Jedis jedis = pool.getResource()) {
                try {
                    return apply(jedis);
                } catch (JedisDataException e) {
                    // Fix Java +16 error
                    if (e.getMessage().contains("NOAUTH") || e.getMessage().contains("unauthenticated")) {
                        jedis.auth(password);
                        return apply(jedis);
                    } else {
                        e.printStackTrace();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return getOptional();
        }

        public T getOptional() {
            return optional;
        }
    }
}
