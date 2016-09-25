package com.github.cheesesoftware.PowerfulPerms.Vault;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.player.PlayerQuitEvent;

import com.github.cheesesoftware.PowerfulPerms.PowerfulPermissionManager;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionManagerBase;
import com.github.cheesesoftware.PowerfulPerms.common.PermissionPlayerBase;
import com.github.cheesesoftware.PowerfulPermsAPI.CachedGroup;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.google.common.util.concurrent.ListenableFuture;

import eu.taigacraft.importer.permissions.PermissionsImporter;

public class ImporterHook implements eu.taigacraft.importer.permissions.PermissionsImporter {

    PermissionManager permissionManager;

    public void hook(PowerfulPermsPlugin plugin) {
        permissionManager = plugin.getPermissionManager();
        PermissionsImporter.register("PowerfulPerms", this);
    }

    // Prefix

    @Override
    public String getPrefix(OfflinePlayer player) {
        return getPrefix(player, null, null);
    }

    @Override
    public String getPrefix(OfflinePlayer player, String worldname) {
        return getPrefix(player);
    }

    @Override
    public String getPrefix(OfflinePlayer player, String worldname, String ladder) {
        ListenableFuture<String> second = permissionManager.getPlayerPrefix(player.getUniqueId(), ladder);
        try {
            return second.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Suffix

    @Override
    public String getSuffix(OfflinePlayer player) {
        return getSuffix(player, null, null);
    }

    @Override
    public String getSuffix(OfflinePlayer player, String worldname) {
        return getSuffix(player);
    }

    @Override
    public String getSuffix(OfflinePlayer player, String worldname, String ladder) {
        ListenableFuture<String> second = permissionManager.getPlayerSuffix(player.getUniqueId(), ladder);
        try {
            return second.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getRank(OfflinePlayer player) {
        ListenableFuture<Group> second = permissionManager.getPlayerPrimaryGroup(player.getUniqueId());
        try {
            Group group = second.get();
            if (group != null)
                return group.getName();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Boolean hasPermission(OfflinePlayer player, String permission) {
        return hasPermission(player, permission, "");
    }

    @Override
    public Boolean hasPermission(OfflinePlayer player, String permission, String world) {
        ListenableFuture<Boolean> second = permissionManager.playerHasPermission(player.getUniqueId(), permission, world, "");
        try {
            return second.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<String> getRanks(OfflinePlayer player) {
        try {
            ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> second = permissionManager.getPlayerCurrentGroups(player.getUniqueId());
            LinkedHashMap<String, List<CachedGroup>> currentGroups = second.get();
            List<CachedGroup> cachedGroups = PermissionPlayerBase.getCachedGroups(PermissionManagerBase.serverName, currentGroups);
            List<Group> groups = PermissionPlayerBase.getGroups(cachedGroups);
            List<String> groupNames = new ArrayList<String>();
            for (Group group : groups)
                groupNames.add(group.getName());
            return groupNames;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return new ArrayList<String>();
    }

    @Override
    public void load(OfflinePlayer player) {
        PowerfulPermissionManager base = (PowerfulPermissionManager) permissionManager;
        base.loadPlayer(player.getUniqueId(), player.getName(), true, true);
        base.loadCachedPlayer(player);
    }

    @Override
    public void unload(OfflinePlayer player) {
        PowerfulPermissionManager base = (PowerfulPermissionManager) permissionManager;
        if (!player.isOnline())
            base.onPlayerQuit(new PlayerQuitEvent(null, null));
    }

    @Override
    public boolean isLoaded(OfflinePlayer arg0) {
        base.
        return true;
    }
}