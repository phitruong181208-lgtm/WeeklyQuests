package com.yourname.weeklyquests;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Main instance;
    private QuestManager questManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        questManager = new QuestManager();
        getCommand("weeklyquest").setExecutor(new Commands());
        getServer().getPluginManager().registerEvents(new EventListener(), this);
        getLogger().info("✅ WeeklyQuests khởi động! Tuần: " + questManager.getCurrentWeek() +
                "/" + questManager.getMaxConfiguredWeek());
    }

    @Override
    public void onDisable() {
        if (questManager != null) questManager.saveAllData();
        getLogger().info("❌ WeeklyQuests đã tắt!");
    }

    public static Main getInstance() { return instance; }
    public QuestManager getQuestManager() { return questManager; }
}
