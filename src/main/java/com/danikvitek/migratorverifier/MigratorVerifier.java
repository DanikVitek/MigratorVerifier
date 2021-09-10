package com.danikvitek.migratorverifier;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public final class MigratorVerifier extends JavaPlugin implements Listener {

    private static boolean usePermission;
    private static File migratedListFile;
    private static YamlConfiguration modifyMigratedListFile;

    private static LuckPerms luckPermsAPI;

    public static File getMigratedListFile() {
        return migratedListFile;
    }
    public static YamlConfiguration getModifyMigratedListFile() {
        return modifyMigratedListFile;
    }

    public static boolean getUsePermission() {
        return usePermission;
    }

    public static Permission verifiedMigration = new Permission("migratorverifier.migrator.verified", PermissionDefault.FALSE);

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        migratedListFile = new File(getDataFolder(), "migrated.yml");
        if (!migratedListFile.exists()) {
            try {
                migratedListFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        modifyMigratedListFile = YamlConfiguration.loadConfiguration(migratedListFile);

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null)
            luckPermsAPI = provider.getProvider();

        usePermission = getConfig().getBoolean("use_permissions", false);

        Bukkit.getPluginManager().registerEvents(this, this);
        if (usePermission) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission(verifiedMigration)) {
                    boolean isMigrated = false;
                    try {
                        isMigrated = isMigrated(player);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    player.addAttachment(this, verifiedMigration.getName(), isMigrated);
                    if (!isMigrated && getConfig().getString("wear_cape_message", "&3If you have migrated then wear your cape and rejoin the server to be registered as migrated").length() > 0)
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                getConfig().getString("wear_cape_message", "&3If you have migrated then wear your cape and rejoin the server to be registered as migrated")));
                }
            }
        }
        else
            for (Player player: Bukkit.getOnlinePlayers()) {
                try {
                    if (!isMigrated(player))
                        if (getConfig().getString("wear_cape_message", "&3If you have migrated then wear your cape and rejoin the server to be registered as migrated").length() > 0)
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    getConfig().getString("wear_cape_message", "&3If you have migrated then wear your cape and rejoin the server to be registered as migrated")));
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (usePermission) {
            if (!player.hasPermission(verifiedMigration)) {
                boolean isMigrated = false;
                try {
                    isMigrated = isMigrated(player);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                player.addAttachment(this, verifiedMigration.getName(), isMigrated);
                if (!isMigrated && getConfig().getString("wear_cape_message", "&3If you have migrated then wear your cape and rejoin the server to be registered as migrated").length() > 0)
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            getConfig().getString("wear_cape_message", "&3If you have migrated then wear your cape and rejoin the server to be registered as migrated")));
            }
        }
        else {
            try {
                if (!isMigrated(player))
                    if (getConfig().getString("wear_cape_message", "&3If you have migrated then wear your cape and rejoin the server to be registered as migrated").length() > 0)
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                getConfig().getString("wear_cape_message", "&3If you have migrated then wear your cape and rejoin the server to be registered as migrated")));
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isMigrated(Player player) throws ExecutionException, InterruptedException {
        return new PlayerData(player).isMigrated();
    }

    public boolean isWearingMigratorsCape(Player player) throws ExecutionException, InterruptedException {
        return new PlayerData(player).isWearingMigratorsCape();
    }

    protected static LuckPerms getLuckPermsAPI() {
        return luckPermsAPI;
    }
}
