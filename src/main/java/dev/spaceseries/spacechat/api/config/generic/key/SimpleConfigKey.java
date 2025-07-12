package dev.spaceseries.spacechat.api.config.generic.key;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;

import java.util.function.Function;

/**
 * Basic {@link ConfigKey} implementation.
 *
 * @param <T> the value type
 */
public class SimpleConfigKey<T> implements ConfigKey<T> {
    private final Function<? super ConfigurationAdapter, ? extends T> function;

    private int ordinal = -1;
    private boolean reloadable = true;
    private boolean memoize = false;

    private T cachedValue;

    SimpleConfigKey(Function<? super ConfigurationAdapter, ? extends T> function) {
        this.function = function;
    }

    @Override
    public T get(ConfigurationAdapter adapter) {
        if (memoize) {
            if (cachedValue == null) {
                cachedValue = this.function.apply(adapter);
            }
            return cachedValue;
        }
        return this.function.apply(adapter);
    }

    @Override
    public int ordinal() {
        return this.ordinal;
    }

    @Override
    public boolean reloadable() {
        return this.reloadable;
    }

    @Override
    public boolean memoize() {
        return memoize;
    }

    @Override
    public void clear() {
        cachedValue = null;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public void setMemoize(boolean memoize) {
        this.memoize = memoize;
    }
}
