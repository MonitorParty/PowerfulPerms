package com.github.cheesesoftware.PowerfulPerms.database;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.github.cheesesoftware.PowerfulPermsAPI.DBDocument;
import com.github.cheesesoftware.PowerfulPermsAPI.IScheduler;
import com.github.cheesesoftware.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.google.common.base.Charsets;

public class MySQLDatabase extends Database {

    private SQL sql;

    public MySQLDatabase(IScheduler scheduler, SQL sql) {
        super(scheduler);
        this.sql = sql;
    }

    private DBResult fromResultSet(ResultSet r) throws SQLException {
        ArrayList<DBDocument> rows = new ArrayList<DBDocument>();
        ResultSetMetaData md = r.getMetaData();
        int columns = md.getColumnCount();
        while (r.next()) {
            Map<String, Object> row = new HashMap<String, Object>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(md.getColumnName(i), r.getObject(i));
            }
            rows.add(new DBDocument(row));
        }
        return new DBResult(rows);
    }

    @Override
    public void applyPatches(PowerfulPermsPlugin plugin) {
        if (plugin.getOldVersion() < 233) {
            // Set [default] UUID
            final PowerfulPermsPlugin pl = plugin;
            setPlayerUUID("[default]", java.util.UUID.nameUUIDFromBytes(("[default]").getBytes(Charsets.UTF_8)), new DBRunnable(true) {

                @Override
                public void run() {
                    pl.getLogger().info("Applied database patch #1: Inserted UUID for player [default].");
                }
            });
        }

        if (plugin.getOldVersion() < 240) {
            // Add "ladder" and "rank" columns to groups table
            try {

                PreparedStatement s = sql.getConnection().prepareStatement("SHOW COLUMNS FROM `" + tblGroups + "` LIKE 'ladder';");
                ResultSet result = s.executeQuery();
                if (!result.next()) {
                    s.close();
                    s = sql.getConnection().prepareStatement("ALTER TABLE `" + tblGroups + "` ADD COLUMN `ladder` VARCHAR(64) NOT NULL AFTER `suffix`,ADD COLUMN `rank` INT NOT NULL AFTER `ladder`");
                    s.execute();
                    s.close();
                    plugin.getLogger().info("Applied database patch #2: Added columns 'ladder' and 'rank' to groups table.");
                } else {
                    plugin.getLogger().info("Skipping database patch #2.");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void tableExists(final String table, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean exists = false;
                try {

                    DatabaseMetaData dbm = sql.getConnection().getMetaData();
                    ResultSet tables = dbm.getTables(null, null, table, null);
                    if (tables.next())
                        exists = true;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                done.setResult(new DBResult(exists));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void createGroupsTable(final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                String groupsTable = "CREATE TABLE `"
                        + tblGroups
                        + "` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`name` varchar(255) NOT NULL,`parents` longtext NOT NULL,`prefix` text NOT NULL,`suffix` text NOT NULL,`ladder` varchar(64) NOT NULL,`rank` int(11) NOT NULL,PRIMARY KEY (`id`),UNIQUE KEY `id_UNIQUE` (`id`),UNIQUE KEY `name_UNIQUE` (`name`)) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8";
                try {
                    PreparedStatement s = sql.getConnection().prepareStatement(groupsTable);
                    s.execute();
                    s.close();

                    // Insert one group "Guest"
                    s = sql.getConnection().prepareStatement(
                            "INSERT INTO `" + tblGroups + "` (`id`, `name`, `parents`, `prefix`, `suffix`, `ladder`, `rank`) VALUES ('1', 'Guest', '', '[Guest]', ': ', 'default', '100');");
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void createPlayersTable(final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                String playersTable = "CREATE TABLE `"
                        + tblPlayers
                        + "` (`uuid` varchar(36) NOT NULL DEFAULT '',`name` varchar(32) NOT NULL,`groups` longtext NOT NULL,`prefix` text NOT NULL,`suffix` text NOT NULL,PRIMARY KEY (`name`,`uuid`),UNIQUE KEY `uuid_UNIQUE` (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
                try {
                    PreparedStatement s = sql.getConnection().prepareStatement(playersTable);
                    s.execute();
                    s.close();

                    // Insert player [default]
                    s = sql.getConnection().prepareStatement(
                            "INSERT INTO `" + tblPlayers + "` (`uuid`, `name`, `groups`, `prefix`, `suffix`) VALUES ('"
                                    + java.util.UUID.nameUUIDFromBytes(("[default]").getBytes(Charsets.UTF_8)).toString() + "', '[default]', ':1:;', '', '');");
                    s.execute();
                    s.close();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void createPermissionsTable(final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                String permissionsTable = "CREATE TABLE `"
                        + tblPermissions
                        + "` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`playeruuid` varchar(36) NOT NULL,`playername` varchar(45) NOT NULL,`groupname` varchar(255) NOT NULL,`permission` varchar(128) NOT NULL,`world` varchar(128) NOT NULL,`server` varchar(128) NOT NULL,PRIMARY KEY (`id`,`playeruuid`,`playername`,`groupname`),UNIQUE KEY `id_UNIQUE` (`id`)) ENGINE=InnoDB DEFAULT CHARSET=utf8";
                try {
                    PreparedStatement s = sql.getConnection().prepareStatement(permissionsTable);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void insertGroup(final String group, final String parents, final String prefix, final String suffix, final String ladder, final int rank, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblGroups + " SET `name`=?, `parents`=?, `prefix`=?, `suffix`=?, `ladder`=?, `rank`=?");
                    s.setString(1, group);
                    s.setString(2, parents);
                    s.setString(3, prefix);
                    s.setString(4, suffix);
                    s.setString(5, ladder);
                    s.setInt(6, rank);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void insertPlayer(final UUID uuid, final String name, final String groups, final String prefix, final String suffix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("INSERT INTO " + tblPlayers + " SET `uuid`=?, `name`=?, `groups`=?, `prefix`=?, `suffix`=?;");
                    s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.setString(3, groups);
                    s.setString(4, prefix);
                    s.setString(5, suffix);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void getPlayer(final UUID uuid, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `uuid`=?");
                    s.setString(1, uuid.toString());
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void getPlayers(final String name, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPlayers + " WHERE `name`=?");
                    s.setString(1, name);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void setPlayerName(final UUID uuid, final String name, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `name`=? WHERE `uuid`=?;");
                    s.setString(1, name);
                    s.setString(2, uuid.toString());
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void setPlayerUUID(final String name, final UUID uuid, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `uuid`=? WHERE `name`=?;");
                    s.setString(1, uuid.toString());
                    s.setString(2, name);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }
    
    @Override
    public void getGroupPlayers(String group, UUID uuid, DBRunnable done) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void getGroups(final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblGroups);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void getGroupPermissions(final String group, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `groupname`=?");
                    s.setString(1, group);
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void getPlayerPermissions(final UUID uuid, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                DBResult result;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `playeruuid`=?");
                    s.setString(1, uuid.toString());
                    s.execute();
                    ResultSet r = s.getResultSet();
                    result = fromResultSet(r);
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    result = new DBResult(false);
                }

                done.setResult(result);
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void playerHasPermission(final UUID uuid, final String permission, final String world, final String server, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = false;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("SELECT * FROM " + tblPermissions + " WHERE `playeruuid`=? AND `permission`=? AND `world`=? AND `server`=?");
                    s.setString(1, uuid.toString());
                    s.setString(2, permission);
                    s.setString(3, world);
                    s.setString(4, server);
                    s.execute();
                    ResultSet result = s.getResultSet();
                    if (result.next()) {
                        success = true;
                    }
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void insertPermission(final UUID uuid, final String name, final String group, final String permission, final String world, final String server, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement(
                            "INSERT INTO " + tblPermissions + " SET `playeruuid`=?, `playername`=?, `groupname`=?, `permission`=?, `world`=?, `server`=?");
                    if (uuid != null)
                        s.setString(1, uuid.toString());
                    else
                        s.setString(1, "");
                    s.setString(2, name);
                    s.setString(3, group);
                    s.setString(4, permission);
                    s.setString(5, world);
                    s.setString(6, server);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    // Run if name in permissions table doesn't match player name
    @Override
    public void updatePlayerPermissions(final UUID uuid, final String name, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPermissions + " SET `playername`=? WHERE `playeruuid`=?;");
                    s.setString(1, name);
                    s.setString(2, uuid.toString());
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void deletePlayerPermission(final UUID uuid, final String permission, final String world, final String server, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM `" + tblPermissions + "` WHERE `playeruuid`=? AND `permission`=? AND `server`=? AND `world`=?");

                    s.setString(1, uuid.toString());
                    s.setString(2, permission);
                    s.setString(3, server);
                    s.setString(4, world);

                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success, amount));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void deletePlayerPermissions(final UUID uuid, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    String statement = "DELETE FROM `" + tblPermissions + "` WHERE `playeruuid`=?";
                    PreparedStatement s = sql.getConnection().prepareStatement(statement);

                    s.setString(1, uuid.toString());
                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success, amount));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void deleteGroupPermission(final String group, final String permission, final String world, final String server, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblPermissions + " WHERE `groupName`=? AND `permission`=? AND `world`=? AND `server`=?");
                    s.setString(1, group);
                    s.setString(2, permission);
                    s.setString(3, world);
                    s.setString(4, server);
                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success, amount));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void deleteGroupPermissions(final String group, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblPermissions + " WHERE `groupName`=?");
                    s.setString(1, group);
                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success, amount));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void setPlayerPrefix(final UUID uuid, final String prefix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `prefix`=? WHERE `uuid`=?");
                    s.setString(1, prefix);
                    s.setString(2, uuid.toString());
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void setPlayerSuffix(final UUID uuid, final String suffix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `suffix`=? WHERE `uuid`=?");
                    s.setString(1, suffix);
                    s.setString(2, uuid.toString());
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void setPlayerGroups(final UUID uuid, final String groups, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblPlayers + " SET `groups`=? WHERE `uuid`=?");
                    s.setString(1, groups);
                    s.setString(2, uuid.toString());
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void deleteGroup(final String group, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;
                int amount = 0;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("DELETE FROM " + tblGroups + " WHERE `name`=?;");
                    s.setString(1, group);
                    amount = s.executeUpdate();
                    if (amount <= 0)
                        success = false;
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                final boolean success2 = success;
                final int amount2 = amount;

                deleteGroupPermissions(group, new DBRunnable(true) {

                    @Override
                    public void run() {
                        done.setResult(new DBResult(success2, amount2));
                        scheduler.runSync(done, done.sameThread());
                    }
                });
            }
        }, done.sameThread());
    }

    @Override
    public void setGroupParents(final String group, final String parents, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `parents`=? WHERE `name`=?");
                    s.setString(1, parents);
                    s.setString(2, group);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void setGroupPrefix(final String group, final String prefix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `prefix`=? WHERE `name`=?");
                    s.setString(1, prefix);
                    s.setString(2, group);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    @Override
    public void setGroupSuffix(final String group, final String suffix, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `suffix`=? WHERE `name`=?");
                    s.setString(1, suffix);
                    s.setString(2, group);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    public void setGroupLadder(final String group, final String ladder, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `ladder`=? WHERE `name`=?");
                    s.setString(1, ladder);
                    s.setString(2, group);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

    public void setGroupRank(final String group, final int rank, final DBRunnable done) {
        scheduler.runAsync(new Runnable() {

            @Override
            public void run() {
                boolean success = true;

                try {
                    PreparedStatement s = sql.getConnection().prepareStatement("UPDATE " + tblGroups + " SET `rank`=? WHERE `name`=?");
                    s.setInt(1, rank);
                    s.setString(2, group);
                    s.execute();
                    s.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    success = false;
                }

                done.setResult(new DBResult(success));
                scheduler.runSync(done, done.sameThread());
            }
        }, done.sameThread());
    }

}
