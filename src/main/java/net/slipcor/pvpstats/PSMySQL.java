package net.slipcor.pvpstats;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * MySQL access class
 *
 * @author slipcor
 */

public final class PSMySQL {

    private PSMySQL() {

    }

    private static PVPStats plugin = null;

    private static void mysqlQuery(final String query) {
        if (plugin.mySQL) {
            try {
                plugin.sqlHandler.executeQuery(query, true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean mysqlExists(final String query) {
        ResultSet result = null;
        if (plugin.mySQL) {
            try {
                result = plugin.sqlHandler.executeQuery(query, false);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                while (result != null && result.next()) {
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static void incKill(final Player player, int elo) {
        if (player.hasPermission("pvpstats.count")) {
            boolean incStreak;
            if (PVPData.hasStreak(player.getName())) {
                incStreak = PVPData.addStreak(player.getName());
            } else {
                PVPData.setStreak(player.getName(), 1);
                PVPData.setMaxStreak(player.getName(), 1);
                incStreak = true;
            }
            checkAndDo(player.getName(), player.getUniqueId(), true, incStreak, elo);
        }
    }

    private static void incDeath(final Player player, int elo) {
        if (player.hasPermission("pvpstats.count")) {
            PVPData.setStreak(player.getName(), 0);
            checkAndDo(player.getName(), player.getUniqueId(), false, false, elo);
        }
    }

    private static void checkAndDo(final String sPlayer, final UUID pid, final boolean kill, final boolean addStreak, int elo) {
        if (PVPStats.useUUIDs) {
            if (!mysqlExists("SELECT * FROM `" + plugin.dbTable + "` WHERE `uid` = '" + pid
                    + "';")) {
                final int kills = kill ? 1 : 0;
                final int deaths = kill ? 0 : 1;
                mysqlQuery("INSERT INTO `" + plugin.dbTable + "` (`name`, `uid`, `kills`,`deaths`,`elo`) VALUES ('"
                        + sPlayer + "', '" + pid + "', " + kills + ", " + deaths + ", " + elo + ")");
                PVPData.setKills(sPlayer, kills);
                PVPData.setDeaths(sPlayer, deaths);
                return;
            } else {
                final String var = kill ? "kills" : "deaths";
                mysqlQuery("UPDATE `" + plugin.dbTable + "` SET `" + var + "` = `" + var
                        + "`+1, `elo` = '" + elo + "' WHERE `uid` = '" + pid + "'");
            }
            if (addStreak && kill) {
                mysqlQuery("UPDATE `" + plugin.dbTable + "` SET `streak` = `streak`+1 WHERE `uid` = '" + pid + "'");
            }
            if (plugin.dbKillTable != null) {
                mysqlQuery("INSERT INTO " + plugin.dbKillTable + " (`name`,`uid`,`kill`,`time`) VALUES(" +
                        "'" + sPlayer + "', '" + pid + "', '" + (kill ? 1 : 0) + "', '" + (long) (System.currentTimeMillis() / 1000) + "')");
            }
        } else {
            if (!mysqlExists("SELECT * FROM `" + plugin.dbTable + "` WHERE `name` = '" + sPlayer
                    + "';")) {
                final int kills = kill ? 1 : 0;
                final int deaths = kill ? 0 : 1;
                mysqlQuery("INSERT INTO `" + plugin.dbTable + "` (`name`, `uid`, `kills`,`deaths`,`elo`) VALUES ('"
                        + sPlayer + "', '', " + kills + ", " + deaths + ", " + elo + ")");
                PVPData.setKills(sPlayer, kills);
                PVPData.setDeaths(sPlayer, deaths);
                return;
            } else {
                final String var = kill ? "kills" : "deaths";
                mysqlQuery("UPDATE `" + plugin.dbTable + "` SET `" + var + "` = `" + var
                        + "`+1, `elo` = '" + elo + "' WHERE `name` = '" + sPlayer + "'");
            }
            if (addStreak && kill) {
                mysqlQuery("UPDATE `" + plugin.dbTable + "` SET `streak` = `streak`+1 WHERE `name` = '" + sPlayer + "'");
            }
            if (plugin.dbKillTable != null) {
                mysqlQuery("INSERT INTO " + plugin.dbKillTable + " (`name`,`uid`,`kill`,`time`) VALUES(" +
                        "'" + sPlayer + "', '', '" + (kill ? 1 : 0) + "', '" + (long) (System.currentTimeMillis() / 1000) + "')");
            }
        }
    }

    /**
     * @param count the amount to fetch
     * @param sort  sorting string
     * @return a sorted array
     */
    public static String[] top(final int count, String sort) {
        if (!plugin.mySQL) {
            plugin.getLogger().severe("MySQL is not set!");
            return null;
        }

        sort = sort.toUpperCase();
        ResultSet result = null;
        final Map<String, Double> results = new HashMap<String, Double>();

        final List<String> sortedValues = new ArrayList<String>();

        String order = null;
        try {

            if (sort.equals("KILLS")) {
                order = "kills";
            } else if (sort.equals("DEATHS")) {
                order = "deaths";
            } else if (sort.equals("STREAK")) {
                order = "streak";
            } else if (sort.equals("ELO")) {
                order = "elo";
            } else if (sort.equals("K-D")) {
                order = "kills";
            } else {
                order = "kills";
            }

            int limit = sort.equals("K-D") ? 50 : count;

            String sorting = sort.equals("ELO") ? "DESC" : "ASC";

            String query = "SELECT `name`,`kills`,`deaths`,`streak`,`elo` FROM `" +
                    plugin.dbTable + "` WHERE 1 ORDER BY `" + order + "` " + sorting + " LIMIT " + limit + ";";

            result = plugin.sqlHandler
                    .executeQuery(query, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            while (result != null && result.next()) {
                if (sort.equals("KILLS")) {
                    sortedValues.add(ChatColor.RED + result.getString("name") + ":" + ChatColor.GRAY + " " + result.getInt(order));
                } else if (sort.equals("DEATHS")) {
                    sortedValues.add(ChatColor.RED + result.getString("name") + ":" + ChatColor.GRAY + " " + result.getInt(order));
                } else if (sort.equals("ELO")) {
                    sortedValues.add(ChatColor.RED + result.getString("name") + ":" + ChatColor.GRAY + " " + result.getInt(order));
                } else if (sort.equals("STREAK")) {
                    sortedValues.add(ChatColor.RED + result.getString("name") + ":" + ChatColor.GRAY + " " + result.getInt(order));
                } else {
                    results.put(
                            result.getString("name"),
                            calcResult(result.getInt("kills"),
                                    result.getInt("deaths"),
                                    result.getInt("streak"), PVPData.getStreak(result.getString("name"))));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (sort.equals("KILLS") || sort.equals("DEATHS") || sort.equals("ELO") || sort.equals("STREAK")) {
            String[] output = new String[sortedValues.size()];

            int pos = 0;

            for (String s : sortedValues) {
                output[pos++] = s;
            }
            return output;
        }

        return sortParse(results, count);
    }

    private static String[] sortParse(final Map<String, Double> results,
                                      final int count) {
        String[] result = new String[results.size()];
        Double[] sort = new Double[results.size()];

        int pos = 0;

        DecimalFormat df = new DecimalFormat("#.##");

        for (String key : results.keySet()) {
            sort[pos] = results.get(key);
            result[pos] = ChatColor.RED + key + ":" + ChatColor.GRAY + " " + df.format(sort[pos]);
            pos++;
        }

        int pos2 = results.size();
        boolean doMore = true;
        while (doMore) {
            pos2--;
            doMore = false; // assume this is our last pass over the array
            for (int i = 0; i < pos2; i++) {
                if (sort[i] < sort[i + 1]) {
                    // exchange elements

                    final double tempI = sort[i];
                    sort[i] = sort[i + 1];
                    sort[i + 1] = tempI;

                    final String tempR = result[i];
                    result[i] = result[i + 1];
                    result[i + 1] = tempR;

                    doMore = true; // after an exchange, must look again
                }
            }
        }
        if (result.length < count) {
            return result;
        }
        String[] output = new String[count];
        System.arraycopy(result, 0, output, 0, output.length);

        return output;
    }

    private static Double calcResult(final int kills, final int deaths, final int streak,
                                     final int maxstreak) {

        String string = plugin.getConfig().getString("kdcalculation");

        string = string.replaceAll("&k", "(" + kills + ")");
        string = string.replaceAll("&d", "(" + deaths + ")");
        string = string.replaceAll("&s", "(" + streak + ")");
        string = string.replaceAll("&m", "(" + maxstreak + ")");

        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        StringBuilder saneString = new StringBuilder();

        for (char c : string.toCharArray()) {
            switch (c) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '-':
                case '+':
                case '*':
                case '/':
                case '(':
                case ')':
                    saneString.append(c);
                    break;
                default:
            }
        }

        try {
            return (Double) engine.eval(saneString.toString());
        } catch (ScriptException e) {
            plugin.getLogger().severe("SaneString: " + saneString.toString());
            e.printStackTrace();
            return 0d;
        }
    }

    /**
     * @param string the player name to get
     * @return the player info
     */
    public static String[] info(final String string) {
        if (!plugin.mySQL) {
            plugin.getLogger().severe("MySQL is not set!");
            return null;
        }
        ResultSet result = null;
        try {
            result = plugin.sqlHandler
                    .executeQuery("SELECT `name`,`kills`,`deaths`,`streak` FROM `" + plugin.dbTable + "` WHERE `name` LIKE '%" + string + "%' LIMIT 1;", false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        String[] output = null;
        try {
            while (result != null && result.next()) {
                String name = result.getString("name");
                Integer streak = PVPData.getStreak(name);
                if (streak == null) {
                    streak = 0;
                }
                output = new String[6];

                output[0] = Language.INFO_FORMAT.toString(
                        Language.INFO_NAME.toString(),
                        name);
                output[1] = Language.INFO_FORMAT.toString(
                        Language.INFO_KILLS.toString(),
                        String.valueOf(result.getInt("kills")));
                output[2] = Language.INFO_FORMAT.toString(
                        Language.INFO_DEATHS.toString(),
                        String.valueOf(result.getInt("deaths")));
                output[3] = Language.INFO_FORMAT.toString(
                        Language.INFO_RATIO.toString(),
                        String.valueOf(calcResult(result.getInt("kills"), result.getInt("deaths"), result.getInt("streak"), streak)));
                output[4] = Language.INFO_FORMAT.toString(
                        Language.INFO_STREAK.toString(),
                        String.valueOf(streak));
                output[5] = Language.INFO_FORMAT.toString(
                        Language.INFO_MAXSTREAK.toString(),
                        String.valueOf(result.getInt("streak")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (output != null) {
            return output;
        }

        output = new String[1];
        output[0] = Language.INFO_PLAYERNOTFOUND.toString(string);
        return output;
    }

    public static Integer getEntry(String player, String entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry can not be null!");
        }

        if (!entry.equals("kills") &&
                !entry.equals("deaths") &&
                !entry.equals("streak")) {
            throw new IllegalArgumentException("entry can not be '" + entry + "'. Valid values: kills, deaths, streak");
        }

        if (!plugin.mySQL) {
            plugin.getLogger().severe("MySQL is not set!");
            return null;
        }
        ResultSet result = null;
        try {
            result = plugin.sqlHandler
                    .executeQuery("SELECT `" + entry + "` FROM `" + plugin.dbTable + "` WHERE `name` LIKE '%" + player + "%' LIMIT 1;", false);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            while (result != null && result.next()) {
                return result.getInt(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static void initiate(final PVPStats pvpStats) {
        plugin = pvpStats;
    }

    public static void wipe(final String name) {
        if (name == null) {
            mysqlQuery("DELETE FROM `" + plugin.dbTable + "` WHERE 1;");
            if (plugin.dbKillTable != null) {
                mysqlQuery("DELETE FROM `" + plugin.dbKillTable + "` WHERE 1;");
            }
        } else {
            PVPData.setDeaths(name, 0);
            PVPData.setKills(name, 0);
            PVPData.setMaxStreak(name, 0);
            PVPData.setStreak(name, 0);

            mysqlQuery("DELETE FROM `" + plugin.dbTable + "` WHERE `name` = '" + name
                    + "';");
            if (plugin.dbKillTable != null) {
                mysqlQuery("DELETE FROM `" + plugin.dbKillTable + "` WHERE `name` = '" + name
                        + "';");
            }
        }
    }

    public static int clean() {
        if (!plugin.mySQL) {
            plugin.getLogger().severe("MySQL is not set!");
            return 0;
        }
        ResultSet result;

        List<Integer> ints = new ArrayList<Integer>();
        Map<String, Integer> players = new HashMap<String, Integer>();

        try {

            result = plugin.sqlHandler
                    .executeQuery("SELECT `id`, `name` FROM `" + plugin.dbTable + "` WHERE 1 ORDER BY `kills` DESC;", false);

            while (result != null && result.next()) {
                String playerName = result.getString("name");

                if (players.containsKey(playerName)) {
                    ints.add(result.getInt("id"));
                    players.put(playerName, players.get(playerName) + 1);
                } else {
                    players.put(playerName, 1);
                }
            }

            if (ints.size() > 0) {
                StringBuilder buff = new StringBuilder("DELETE FROM `");
                buff.append(plugin.dbTable);
                buff.append("` WHERE `id` IN (");

                boolean first = true;

                for (Integer i : ints) {
                    if (!first) {
                        buff.append(',');
                    }
                    first = false;
                    buff.append(i);
                }

                buff.append(");");

                mysqlQuery(buff.toString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int i = 10;

        for (String name : players.keySet()) {
            plugin.getLogger().info(name + ": " + players.get(name));
            if (--i < 0) {
                plugin.getLogger().info("...");
                break;
            }
        }

        return ints.size();
    }

    public static List<String> getAllPlayers(String dbTable) {
        if (!plugin.mySQL) {
            plugin.getLogger().severe("MySQL is not set!");
            return null;
        }
        List<String> output = new ArrayList<String>();

        ResultSet result = null;

        try {
            result = plugin.sqlHandler
                    .executeQuery("SELECT `name` FROM `" + dbTable + "` GROUP BY `name`;", false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            while (result != null && result.next()) {
                output.add(result.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return output;
    }

    public static void commit(String dbTable, Map<String, UUID> map) {

        for (Entry<String, UUID> set : map.entrySet()) {
            mysqlQuery("UPDATE `" + dbTable + "` SET `uid` = '" + set.getValue() + "' WHERE `name` = '" + set.getKey() + "';");
        }
    }

    public static void AkilledB(Player attacker, Player player) {
        if (attacker == null && player == null) {
            return;
        }

        if (player == null) {
            incKill(attacker, PVPData.getEloScore(attacker.getName()));
            return;
        }
        if (attacker == null) {
            incDeath(player, PVPData.getEloScore(player.getName()));
            return;
        }

        ConfigurationSection sec = PVPStats.getInstance().getConfig().getConfigurationSection("eloscore");

        if (!sec.getBoolean("active")) {
            incKill(attacker, PVPData.getEloScore(attacker.getName()));
            incDeath(player, PVPData.getEloScore(player.getName()));
            return;
        }

        final int min = sec.getInt("minimum", 18);
        final int max = sec.getInt("maximum", 3000);
        final int kBelow = sec.getInt("k-factor.below", 32);
        final int kAbove = sec.getInt("k-factor.above", 16);
        final int kThreshold = sec.getInt("k-factor.threshold", 2000);

        final int oldA = PVPData.getEloScore(attacker.getName());
        final int oldP = PVPData.getEloScore(player.getName());

        final int kA = oldA >= kThreshold ? kAbove : kBelow;
        final int kP = oldP >= kThreshold ? kAbove : kBelow;

        final int newA = calcElo(oldA, oldP, kA, true, min, max);
        final int newP = calcElo(oldP, oldA, kP, false, min, max);
        incKill(attacker, newA);
        incDeath(player, newP);
    }

    private static int calcElo(int myOld, int otherOld, int k, boolean win, int min, int max) {
        double expected = 1 / (1 + Math.pow(10, (otherOld - myOld) / 400));

        int newVal;
        if (win) {
            newVal = (int) Math.round(myOld + k * (1 - expected));
        } else {
            newVal = (int) Math.round(myOld + k * (0 - expected));
        }

        if (min > -1 && newVal < min) {
            return min;
        }

        if (max > -1 && newVal > max) {
            return max;
        }

        return newVal;
    }

}
