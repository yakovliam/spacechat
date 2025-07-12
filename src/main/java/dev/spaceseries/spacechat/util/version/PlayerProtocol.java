package dev.spaceseries.spacechat.util.version;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Suppliers;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.viaversion.viaversion.api.Via;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class PlayerProtocol {

    private static final Supplier<Boolean> VIAVERSION = Suppliers.memoize(() -> Bukkit.getPluginManager().isPluginEnabled("ViaVersion"));
    private static final Supplier<Boolean> PROTOCOLLIB = Suppliers.memoize(() -> Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"));

    private static final Cache<UUID, Integer> CACHE = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

    PlayerProtocol() {
    }

    public static int getProtocol(Player player) {
        Integer protocol = CACHE.getIfPresent(player.getUniqueId());
        if (protocol == null) {
            if (VIAVERSION.get()) {
                protocol = Via.getAPI().getPlayerVersion(player);
            } else if (PROTOCOLLIB.get()) {
                protocol = ProtocolLibrary.getProtocolManager().getProtocolVersion(player);
            } else {
                throw new IllegalStateException("Cannot get player protocol on current server");
            }
            CACHE.put(player.getUniqueId(), protocol);
        }
        return protocol;
    }
}
