package com.github.cheesesoftware.PowerfulPerms.command;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.github.cheesesoftware.PowerfulPerms.common.ICommand;
import com.github.cheesesoftware.PowerfulPermsAPI.PermissionManager;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.github.cheesesoftware.PowerfulPermsAPI.Response;

public class UserRemovePermissionCommand extends SubCommand {

    public UserRemovePermissionCommand(PowerfulPermsPlugin plugin, PermissionManager permissionManager) {
        super(plugin, permissionManager);
        usage.add("/pp user <user> remove <permission> (server) (world) (expires)");
    }

    @Override
    public CommandResult execute(final ICommand invoker, final String sender, final String[] args) throws InterruptedException, ExecutionException {
        if (hasBasicPerms(invoker, sender, "powerfulperms.user.remove")) {
            if (args != null && args.length >= 2 && args[1].equalsIgnoreCase("remove")) {
                if (args.length < 3) {
                    sendSender(invoker, sender, getUsage());
                    return CommandResult.success;
                }
                final String playerName = args[0];
                final String permission = args[2];

                UUID uuid = permissionManager.getConvertUUIDBase(playerName);
                if (uuid == null) {
                    sendSender(invoker, sender, "Could not find player UUID.");
                } else {
                    String world = "";
                    String server = "";
                    Date expires = null;
                    if (args.length >= 4)
                        server = args[3];
                    if (args.length >= 5)
                        world = args[4];
                    if (args.length >= 6 && !args[5].equalsIgnoreCase("NONE")) {
                        if (args[5].equalsIgnoreCase("ANY"))
                            expires = Utils.getAnyDate();
                        else
                            expires = Utils.getDate(args[5]);
                        if (expires == null) {
                            sendSender(invoker, sender, "Invalid expiration format.");
                            return CommandResult.success;
                        }
                    }
                    if (server.equalsIgnoreCase("all"))
                        server = "";
                    if (world.equalsIgnoreCase("all"))
                        world = "";

                    Response response = permissionManager.removePlayerPermissionBase(uuid, permission, world, server, expires);
                    sendSender(invoker, sender, response.getResponse());
                }
                return CommandResult.success;
            } else
                return CommandResult.noMatch;
        } else
            return CommandResult.noPermission;
    }

    @Override
    public List<String> tabComplete(ICommand invoker, String sender, String[] args) {
        if ("remove".startsWith(args[0].toLowerCase())) {
            List<String> output = new ArrayList<String>();
            output.add("remove");
            return output;
        }
        // TODO: suggest permission + world + server that the player has
        return null;
    }
}
