package dev.spaceseries.spacechat.api.config.adapter;

import dev.spaceseries.spacechat.api.config.generic.adapter.ConfigurationAdapter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class InheritConfigAdapter implements ConfigurationAdapter {

    private final ConfigurationAdapter delegate;
    private final UnaryOperator<String> pathMapper;

    public InheritConfigAdapter(@NotNull ConfigurationAdapter delegate, @NotNull UnaryOperator<String> pathMapper) {
        this.delegate = delegate;
        this.pathMapper = pathMapper;
    }

    @Override
    public Plugin getPlugin() {
        return delegate.getPlugin();
    }

    @Override
    public void reload() {
        delegate.reload();
    }

    @Override
    public String getString(String path, String def) {
        return delegate.getString(path, delegate.getString(pathMapper.apply(path), def));
    }

    @Override
    public int getInteger(String path, int def) {
        return delegate.getInteger(path, delegate.getInteger(pathMapper.apply(path), def));
    }

    @Override
    public long getLong(String path, long def) {
        return delegate.getLong(path, delegate.getLong(pathMapper.apply(path), def));
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return delegate.getBoolean(path, delegate.getBoolean(pathMapper.apply(path), def));
    }

    @Override
    public List<String> getStringList(String path, List<String> def) {
        return delegate.getStringList(path, delegate.getStringList(pathMapper.apply(path), def));
    }

    @Override
    public List<String> getKeys(String path, List<String> def) {
        return delegate.getKeys(path, delegate.getKeys(pathMapper.apply(path), def));
    }

    @Override
    public Map<String, String> getStringMap(String path, Map<String, String> def) {
        return delegate.getStringMap(path, delegate.getStringMap(pathMapper.apply(path), def));
    }
}
