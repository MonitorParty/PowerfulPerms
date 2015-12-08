package com.github.cheesesoftware.PowerfulPerms.Bungee;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.cheesesoftware.PowerfulPerms.IPermissionsPlayer;
import com.github.cheesesoftware.PowerfulPerms.IPlugin;
import com.github.cheesesoftware.PowerfulPerms.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.SQL;
import com.google.common.io.ByteStreams;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class PowerfulPerms extends Plugin implements Listener, IPlugin {

    private SQL sql;
    private PermissionManager permissionManager;
    private Configuration config;

    public static String pluginPrefix = ChatColor.WHITE + "[" + ChatColor.BLUE + "PowerfulPerms" + ChatColor.WHITE + "] ";
    public static String pluginPrefixShort = ChatColor.WHITE + "[" + ChatColor.BLUE + "PP" + ChatColor.WHITE + "] ";
    public static String consolePrefix = "[PowerfulPerms] ";
    public static boolean debug = false;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));

        } catch (IOException e3) {
            e3.printStackTrace();
        }

        this.sql = new SQL(config.getString("host"), config.getString("database"), config.getInt("port"), config.getString("username"), config.getString("password"));
        PermissionManagerBase.redis_ip = config.getString("redis_ip");
        PermissionManagerBase.redis_port = config.getInt("redis_port");
        PermissionManagerBase.redis_password = config.getString("redis_password");
        debug = config.getBoolean("debug");

        try {
            if (sql.getConnection() == null || sql.getConnection().isClosed()) {
                getLogger().severe(pluginPrefix + "Could not access the database!");
            }
        } catch (SQLException e2) {
            getLogger().severe(pluginPrefix + "Could not access the database!");
            e2.printStackTrace();
        }

        permissionManager = new PermissionManager(sql, this);
        this.getProxy().getPluginManager().registerListener(this, this);
        this.getProxy().getPluginManager().registerListener(this, permissionManager);
    }

    @Override
    public void onDisable() {
        permissionManager.onDisable();
    }

    private void saveDefaultConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                InputStream is = getResourceAsStream("config.yml");
                OutputStream os = new FileOutputStream(configFile);
                ByteStreams.copy(is, os);
            } catch (IOException e) {
                throw new RuntimeException("Unable to create configuration file", e);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPermissionCheck(PermissionCheckEvent e) {
        if (e.getSender() instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) e.getSender();
            IPermissionsPlayer gp = permissionManager.getPermissionsPlayer(player.getUniqueId());
            boolean hasPermission = gp.hasPermission(e.getPermission());
            e.setHasPermission(hasPermission);
        }
    }

    public static PowerfulPerms getPlugin() {
        return (PowerfulPerms) ProxyServer.getInstance().getPluginManager().getPlugin("PowerfulPermsBungee");
    }

    public SQL getSQL() {
        return this.sql;
    }
    
    @Override
    public void runTaskAsynchronously(Runnable runnable) {
        this.getProxy().getScheduler().runAsync(this, runnable);
    }

    @Override
    public void runTaskLater(Runnable runnable, int delay) {
        this.getProxy().getScheduler().schedule(this, runnable, delay, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean isPlayerOnline(UUID uuid) {
       ProxiedPlayer player = this.getProxy().getPlayer(uuid);
       if(player != null)
           return true;
       return false;
    }

    @Override
    public UUID getPlayerUUID(String name) {
        ProxiedPlayer player = this.getProxy().getPlayer(name);
        if(player != null)
            return player.getUniqueId();
        return null;
    }
}