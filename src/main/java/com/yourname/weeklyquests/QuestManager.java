package com.yourname.weeklyquests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.time.temporal.IsoFields;
import java.util.*;

public class QuestManager {
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private int currentWeek;
    private int maxConfiguredWeek;
    private File dataFile;
    private ZoneId zoneId;

    public QuestManager() {
        dataFile = new File(Main.getInstance().getDataFolder(), "playerdata.yml");
        loadTimeSettings();
        loadMaxConfiguredWeek();
        calculateCurrentWeek();
        loadAllData();
        scheduleWeekCheckTask();
        scheduleAutoSaveTask();
    }

    private void loadTimeSettings() {
        String zoneStr = Main.getInstance().getConfig().getString("general.time-zone", "Asia/Ho_Chi_Minh");
        try { zoneId = ZoneId.of(zoneStr); }
        catch (Exception e) { zoneId = ZoneId.of("Asia/Ho_Chi_Minh"); }
    }

    private void calculateCurrentWeek() {
        int weekOfYear = ZonedDateTime.now(zoneId).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        boolean loop = Main.getInstance().getConfig().getBoolean("general.loop-weeks", false);
        currentWeek = loop && weekOfYear > maxConfiguredWeek ?
                ((weekOfYear - 1) % maxConfiguredWeek) + 1 : Math.min(weekOfYear, maxConfiguredWeek);
    }

    public void checkWeekChange() {
        int old = currentWeek;
        calculateCurrentWeek();
        if (currentWeek != old) {
            resetAllPlayerQuests();
            saveAllData();
            ConfigurationSection q = getCurrentWeekQuest();
            String name = q != null ? q.getString("name") : "Chưa có tên";
            Bukkit.broadcast(Component.text("🔄 Tuần mới! Tuần " + currentWeek + ": " + name, NamedTextColor.GOLD));
            for (Player p : Bukkit.getOnlinePlayers())
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
            Main.getInstance().getLogger().info("🔄 Chuyển sang tuần " + currentWeek);
        }
    }

    private void scheduleWeekCheckTask() {
        int sec = Main.getInstance().getConfig().getInt("general.check-interval", 60);
        Bukkit.getScheduler().runTaskTimer(Main.getInstance(), this::checkWeekChange, sec*20L, sec*20L);
    }

    private void loadMaxConfiguredWeek() {
        ConfigurationSection s = Main.getInstance().getConfig().getConfigurationSection("week-quests");
        maxConfiguredWeek = s != null ? s.getKeys(false).stream().mapToInt(Integer::parseInt).max().orElse(1) : 1;
    }

    public ConfigurationSection getCurrentWeekQuest() {
        return Main.getInstance().getConfig().getConfigurationSection("week-quests." + currentWeek);
    }

    public int getCurrentWeek() { return currentWeek; }
    public int getMaxConfiguredWeek() { return maxConfiguredWeek; }

    public PlayerData getPlayerData(UUID uuid) {
        PlayerData d = playerDataMap.get(uuid);
        if (d == null || d.getWeek() != currentWeek) {
            d = new PlayerData(uuid, currentWeek);
            playerDataMap.put(uuid, d);
        }
        return d;
    }

    public boolean isAllObjectivesCompleted(UUID uuid) {
        ConfigurationSection q = getCurrentWeekQuest();
        if (q == null) return false;
        List<Map<?, ?>> objs = q.getMapList("objectives");
        PlayerData d = getPlayerData(uuid);
        for (int i = 0; i < objs.size(); i++)
            if (d.getProgress(i) < (int) objs.get(i).get("amount")) return false;
        return true;
    }

    public void updateProgress(UUID uuid, String type, String target, int amount) {
        ConfigurationSection q = getCurrentWeekQuest();
        if (q == null) return;
        List<Map<?, ?>> objs = q.getMapList("objectives");
        PlayerData d = getPlayerData(uuid);
        for (int i = 0; i < objs.size(); i++) {
            Map<?, ?> o = objs.get(i);
            String ot = (String) o.get("type");
            String og = o.containsKey("material") ? (String) o.get("material") :
                    o.containsKey("entity") ? (String) o.get("entity") :
                    o.containsKey("world") ? (String) o.get("world") : "ANY";
            if ((ot.equals("kill_any") && type.equals("kill_any")) ||
                (ot.equals("travel_distance") && type.equals("travel_distance")) ||
                (ot.equalsIgnoreCase(type) && og.equalsIgnoreCase(target))) {
                addObjectiveProgress(uuid, d, i, o, amount);
                break;
            }
        }
    }

    private void addObjectiveProgress(UUID uuid, PlayerData d, int i, Map<?, ?> o, int amount) {
        int req = (int) o.get("amount");
        int cur = d.getProgress(i);
        if (cur < req) {
            d.addProgress(i, amount);
            if (cur + amount >= req) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    String t = o.containsKey("material") ? (String) o.get("material") :
                            o.containsKey("entity") ? (String) o.get("entity") :
                            o.containsKey("world") ? (String) o.get("world") : "mục tiêu";
                    p.sendMessage(Component.text("✅ Hoàn thành: " + t, NamedTextColor.GREEN));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.2f);
                }
            }
        }
    }

    public void claimReward(Player player) {
        PlayerData d = getPlayerData(player.getUniqueId());
        if (d.isClaimed()) {
            player.sendMessage(Component.text("⚠️ Bạn đã hoàn thành tuần này rồi!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        if (!isAllObjectivesCompleted(player.getUniqueId())) {
            player.sendMessage(Component.text("⚠️ Hoàn thành tất cả mục tiêu đã!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
            return;
        }
        d.setClaimed(true);
        saveAllData();
        boolean last = currentWeek == maxConfiguredWeek &&
                !Main.getInstance().getConfig().getBoolean("general.loop-weeks", false);
        if (last) {
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("🏆 CHÚC MỪNG! HOÀN THÀNH TẤT CẢ 8 TUẦN!",
                    NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            player.sendMessage(Component.text("─────────────────────────────────────────", NamedTextColor.GRAY));
            player.sendMessage(Component.text("🎉 Bạn đã chinh phục toàn bộ mùa nhiệm vụ!", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("💪 Thành tựu tuyệt vời, chứng tỏ sự kiên trì của bạn.", NamedTextColor.GREEN));
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("👉 Bạn có thể tiếp tục ở lại xây dựng, khám phá thêm.", NamedTextColor.AQUA));
            player.sendMessage(Component.text("👉 Hoặc rời khỏi máy chủ bất cứ lúc nào.", NamedTextColor.AQUA));
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("❤️ Cảm ơn bạn đã đồng hành!", NamedTextColor.LIGHT_PURPLE));
            player.sendMessage(Component.text("─────────────────────────────────────────", NamedTextColor.GRAY));
            player.sendMessage(Component.text(""));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 0.8f);
            Bukkit.getScheduler().runTaskLater(Main.getInstance(), () ->
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f), 20L);
            Bukkit.broadcast(Component.text("🏆 " + player.getName() + " đã hoàn thành TẤT CẢ 8 tuần!", NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("🎉 Hoàn thành tuần này! Hãy tiếp tục cố gắng tuần sau", NamedTextColor.GREEN));
            player.sendMessage(Component.text(""));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    public void resetAllPlayerQuests() { playerDataMap.clear(); }

    public void saveAllData() {
        try {
            if (!dataFile.exists()) { dataFile.getParentFile().mkdirs(); dataFile.createNewFile(); }
            YamlConfiguration c = new YamlConfiguration();
            c.set("current-week", currentWeek);
            for (Map.Entry<UUID, PlayerData> e : playerDataMap.entrySet()) {
                String p = "players." + e.getKey();
                c.set(p + ".week", e.getValue().getWeek());
                c.set(p + ".claimed", e.getValue().isClaimed());
                for (Map.Entry<Integer, Integer> pr : e.getValue().getProgressMap().entrySet())
                    c.set(p + ".progress." + pr.getKey(), pr.getValue());
            }
            c.save(dataFile);
        } catch (IOException e) { Main.getInstance().getLogger().severe("Lỗi lưu: " + e.getMessage()); }
    }

    public void loadAllData() {
        if (!dataFile.exists()) return;
        try {
            YamlConfiguration c = YamlConfiguration.loadConfiguration(dataFile);
            ConfigurationSection ps = c.getConfigurationSection("players");
            if (ps == null) return;
            for (String u : ps.getKeys(false)) {
                UUID uuid = UUID.fromString(u);
                int w = c.getInt("players." + u + ".week");
                if (w != currentWeek) continue;
                PlayerData d = new PlayerData(uuid, w);
                d.setClaimed(c.getBoolean("players." + u + ".claimed"));
                ConfigurationSection pr = c.getConfigurationSection("players." + u + ".progress");
                if (pr != null) for (String i : pr.getKeys(false))
                    d.setProgress(Integer.parseInt(i), pr.getInt(i));
                playerDataMap.put(uuid, d);
            }
        } catch (Exception e) { Main.getInstance().getLogger().severe("Lỗi tải: " + e.getMessage()); }
    }

    private void scheduleAutoSaveTask() {
        int m = Main.getInstance().getConfig().getInt("general.auto-save", 5);
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), this::saveAllData, m*60*20L, m*60*20L);
    }
}
