package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import com.github.cheesesoftware.PowerfulPerms.common.ChatColor;
import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.Group;
import com.github.cheesesoftware.PowerfulPermsAPI.Permission;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;

public class GroupCommand extends SubCommand {

    public GroupCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp group <group>");
    }

    @Override
    public CommandResult execute(ICommand invoker, String sender, String[] args) {
        if (hasBasicPerms(invoker, sender, "powerfulperms.group")) {
            if (args != null && args.length >= 1) {
                int page = -1;
                if (args.length == 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        return CommandResult.noMatch;
                    }
                } else if (args.length > 2)
                    return CommandResult.noMatch;

                if (page <= 0)
                    page = 1;
                page--;

                Group group = permissionManager.getGroup(args[0]);
                if (group != null) {
                    // List group permissions
                    Queue<String> rows = new java.util.ArrayDeque<String>();

                    rows.add(ChatColor.BLUE + "Listing permissions for group " + group.getName() + ".");
                    rows.add(ChatColor.GREEN + "Ladder" + ChatColor.WHITE + ": " + group.getLadder());
                    rows.add(ChatColor.GREEN + "Rank" + ChatColor.WHITE + ": " + group.getRank());
                    List<Permission> permissions = group.getOwnPermissions();
                    if (permissions.size() > 0) {
                        for (Permission e : permissions)
                            rows.add(ChatColor.DARK_GREEN + e.getPermissionString() + ChatColor.WHITE + " (Server:"
                                    + (e.getServer() == null || e.getServer().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getServer()) + " World:"
                                    + (e.getServer() == null || e.getWorld().isEmpty() ? ChatColor.RED + "ALL" + ChatColor.WHITE : e.getWorld()) + ")");
                    } else
                        rows.add("Group has no permissions.");

                    List<List<String>> list = createList(rows, 19);
                    if (list.size() > 0) {
                        sendSender(invoker, sender, ChatColor.BLUE + "Page " + (page + 1) + " of " + list.size());
                        if (page < list.size()) {
                            for (String s : list.get(page))
                                sendSender(invoker, sender, s);
                        } else
                            sendSender(invoker, sender, "Invalid page. Page too high. ");
                    }
                } else
                    sendSender(invoker, sender, "Group does not exist.");
                return CommandResult.success;

            }
            return CommandResult.noMatch;
        }
        return CommandResult.noPermission;
    }

    private List<List<String>> createList(Queue<String> input, int rowsPerPage) {
        int rowWidth = 55;
        List<List<String>> list = new ArrayList<List<String>>();
        while (input.size() > 0) {
            List<String> page = new ArrayList<String>();
            for (int j = 0; j < rowsPerPage; j++) {
                if (input.size() > 0) {
                    String row = input.remove();
                    page.add(row);
                    if (row.length() > rowWidth)
                        j++;

                }
            }
            list.add(page);
        }
        return list;
    }

}