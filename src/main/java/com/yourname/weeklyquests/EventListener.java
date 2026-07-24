package com.yourname.weeklyquests;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

public class EventListener implements Listener {
    private final QuestManager qm = Main.getInstance().getQuestManager();

    @EventHandler public void onJoin(PlayerJoinEvent e) { qm.checkWeekChange(); }
    @EventHandler public void onQuit(PlayerQuitEvent e) { qm.saveAllData(); }

    @EventHandler public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Material t = e.getBlock().getType();
        qm.updateProgress(p.getUniqueId(), "mine", t.name(), 1);
        qm.updateProgress(p.getUniqueId(), "break", t.name(), 1);
        if (t == Material.DEEPSLATE_DIAMOND_ORE)
            qm.updateProgress(p.getUniqueId(), "mine", "DIAMOND_ORE", 1);
    }

    @EventHandler public void onKill(EntityDeathEvent e) {
        if (e.getEntity().getKiller() != null) {
            Player p = e.getEntity().getKiller();
            EntityType t = e.getEntityType();
            qm.updateProgress(p.getUniqueId(), "kill", t.name(), 1);
            if (e.getEntity() instanceof LivingEntity && t != EntityType.PLAYER)
                qm.updateProgress(p.getUniqueId(), "kill_any", "ANY", 1);
        }
    }

    @EventHandler public void onCraft(org.bukkit.event.inventory.CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Material t = e.getRecipe().getResult().getType();
        int a = e.getRecipe().getResult().getAmount() * e.getAmount();
        qm.updateProgress(p.getUniqueId(), "craft", t.name(), a);
    }

    @EventHandler public void onHarvest(PlayerHarvestBlockEvent e) {
        Player p = e.getPlayer();
        Material t = e.getHarvestedBlock().getType();
        qm.updateProgress(p.getUniqueId(), "harvest", t.name(), 1);
    }

    @EventHandler public void onFish(PlayerFishEvent e) {
        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(e.getCaught() instanceof org.bukkit.entity.Item item)) return;
        Player p = e.getPlayer();
        Material t = item.getItemStack().getType();
        if (t == Material.COD || t == Material.SALMON || t == Material.PUFFERFISH || t == Material.TROPICAL_FISH)
            qm.updateProgress(p.getUniqueId(), "fish", "COD", 1);
        else if (t == Material.INK_SAC)
            qm.updateProgress(p.getUniqueId(), "fish", "SQUID", 1);
        else
            qm.updateProgress(p.getUniqueId(), "fish", "TREASURE", 1);
    }

    @EventHandler public void onCook(org.bukkit.event.inventory.FurnaceExtractEvent e) {
        Player p = e.getPlayer();
        qm.updateProgress(p.getUniqueId(), "cook", e.getItemType().name(), e.getItemAmount());
    }

    @EventHandler public void onChangeWorld(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        String w = switch(p.getWorld().getEnvironment()) {
            case NETHER -> "NETHER";
            case THE_END -> "END";
            default -> "NORMAL";
        };
        qm.updateProgress(p.getUniqueId(), "world", w, 1);
    }

    @EventHandler public void onPickup(PlayerPickupItemEvent e) {
        Player p = e.getPlayer();
        Material t = e.getItem().getItemStack().getType();
        qm.updateProgress(p.getUniqueId(), "collect", t.name(), e.getItem().getItemStack().getAmount());
    }

    @EventHandler public void onMove(PlayerMoveEvent e) {
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() &&
            e.getFrom().getBlockY() == e.getTo().getBlockY() &&
            e.getFrom().getBlockZ() == e.getTo().getBlockZ()) return;
        Player p = e.getPlayer();
        double dist = e.getFrom().distance(e.getTo());
        if (dist >= 0.5)
            qm.updateProgress(p.getUniqueId(), "travel_distance", "DISTANCE", (int) Math.round(dist));
    }

    @EventHandler public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof QuestGUI gui)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getCurrentItem() == null) return;
        Material clicked = e.getCurrentItem().getType();
        int currentPage = gui.getPage();
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
        if (clicked == Material.ARROW && e.getSlot() == 47) { new QuestGUI(p, currentPage-1).open(); return; }
        if (clicked == Material.ARROW && e.getSlot() == 51) { new QuestGUI(p, currentPage+1).open(); return; }
        if (clicked == Material.ENDER_CHEST || clicked == Material.CHEST) {
            qm.claimReward(p); p.closeInventory(); new QuestGUI(p, currentPage).open(); return; }
        if (clicked == Material.BARRIER) p.closeInventory();
    }
}
