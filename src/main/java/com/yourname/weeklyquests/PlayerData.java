package com.yourname.weeklyquests;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private final int week;
    private final Map<Integer, Integer> progress = new HashMap<>();
    private boolean claimed = false;

    public PlayerData(UUID uuid, int week) {
        this.uuid = uuid;
        this.week = week;
    }

    public UUID getUuid() { return uuid; }
    public int getWeek() { return week; }
    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }
    public int getProgress(int i) { return progress.getOrDefault(i, 0); }
    public void setProgress(int i, int amount) { progress.put(i, amount); }
    public void addProgress(int i, int amount) { progress.put(i, getProgress(i) + amount); }
    public Map<Integer, Integer> getProgressMap() { return progress; }
}
