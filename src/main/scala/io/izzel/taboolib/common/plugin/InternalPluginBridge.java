package io.izzel.taboolib.common.plugin;

import io.izzel.taboolib.util.asm.AsmVersionControl;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.instrument.ClassFileTransformer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author 坏黑
 * @since 2019-07-09 17:10
 */
public abstract class InternalPluginBridge {

    private static InternalPluginBridge handle;

    public static InternalPluginBridge handle() {
        return handle;
    }

    static {
        try {
            handle = (InternalPluginBridge) AsmVersionControl.createNMS("io.izzel.taboolib.common.plugin.bridge.BridgeImpl").translateBridge().newInstance();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Nullable
    abstract public <T> T getRegisteredService(Class<? extends T> clazz);

    @NotNull
    abstract public String setPlaceholders(Player player, String args);

    @NotNull
    abstract public List<String> setPlaceholders(Player player, List<String> args);

    abstract public void economyCreate(OfflinePlayer p);

    abstract public void economyTake(OfflinePlayer p, double d);

    abstract public void economyGive(OfflinePlayer p, double d);

    abstract public double economyLook(OfflinePlayer p);

    abstract public void permissionAdd(Player player, String perm);

    abstract public void permissionRemove(Player player, String perm);

    abstract public boolean permissionHas(Player player, String perm);

    @NotNull
    abstract public Collection<String> worldguardGetRegions(World world);

    @NotNull
    abstract public List<String> worldguardGetRegion(World world, Location location);

    abstract public boolean economyHooked();

    abstract public boolean permissionHooked();

    abstract public boolean placeholderHooked();

    abstract public boolean worldguardHooked();

    abstract public boolean isPlaceholderExpansion(Class<?> pluginClass);

    abstract public void registerExpansion(Class<?> pluginClass);

    abstract public void registerExpansionProxy(Class<?> expansionClass);

    @NotNull
    abstract public Map<String, Object> taboolibTLocaleSerialize(Object in);

    @Nullable
    abstract public FileConfiguration taboolibGetPlayerData(String username);

    abstract public int protocolSupportPlayerVersion(Player player);

    abstract public int viaVersionPlayerVersion(Player player);

    @NotNull
    abstract public Class<?> getClass(String name) throws ClassNotFoundException;

    @NotNull
    abstract public ClassLoader getClassLoader();
}
