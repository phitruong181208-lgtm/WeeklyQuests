package com.yourname.weeklyquests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestGUI implements InventoryHolder {
    private final Inventory inv;
    private final Player p;
    private final int page;
    private final QuestManager qm = Main.getInstance().getQuestManager();
    private static final int[] SLOTS = {19,20,21,22,23,24,25};
    private static final int PER_PAGE = 7;

    public QuestGUI(Player p, int page) {
        this.p = p;
        this.page = page;
        this.inv = Bukkit.createInventory(this, 54,
                Component.text("📅 Nhiệm vụ tuần " + qm.getCurrentWeek() + " | Trang " + page, NamedTextColor.GOLD));
        build();
    }
    public QuestGUI(Player p) { this(p, 1); }

    private void build() {
        ItemStack fill = createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "));
        for (int i = 0; i < 54; i++) inv.setItem(i, fill);

        ConfigurationSection q = qm.getCurrentWeekQuest();
        if (q == null) { inv.setItem(22, createItem(Material.BARRIER, Component.text("⚠️ Chưa có nhiệm vụ", NamedTextColor.RED))); return; }

        inv.setItem(4, createQuestInfo(q));
        List<Map<?, ?>> objs = q.getMapList("objectives");
        PlayerData d = qm.getPlayerData(p.getUniqueId());
        int totalPages = (int) Math.ceil((double) objs.size() / PER_PAGE);
        int start = (page-1)*PER_PAGE;
        int end = Math.min(start+PER_PAGE, objs.size());

        for (int i = start; i < end; i++) {
            Map<?, ?> o = objs.get(i);
            int cur = d.getProgress(i);
            int req = (int) o.get("amount");
            inv.setItem(SLOTS[i-start], createObjective(o, cur, req, i));
        }

        inv.setItem(40, createReward(d));
        if (page > 1) inv.setItem(47, createItem(Material.ARROW, Component.text("← Trang " + (page-1), NamedTextColor.YELLOW)));
        if (page < totalPages) inv.setItem(51, createItem(Material.ARROW, Component.text("Trang " + (page+1) + " →", NamedTextColor.YELLOW)));
        inv.setItem(49, createItem(Material.BARRIER, Component.text("✕ Đóng", NamedTextColor.RED)));
    }

    private ItemStack createQuestInfo(ConfigurationSection q) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("📜 Mô tả:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        for (String l : q.getStringList("description"))
            lore.add(Component.text("  " + l, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        return createItem(Material.BOOK, Component.text(q.getString("name"), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false), lore);
    }

    private ItemStack createObjective(Map<?, ?> o, int cur, int req, int idx) {
        String type = (String) o.get("type");
        String target;
        Material icon;
        Component tText, uText;
        double pct = Math.min(100.0, cur*100.0/req);
        boolean done = cur >= req;

        if (type.equals("kill_any")) {
            target = "Bất kỳ quái nào"; icon = Material.DIAMOND_SWORD;
            tText = Component.text("Tiêu diệt quái vật bất kỳ", NamedTextColor.YELLOW);
            uText = Component.text("  " + cur + " / " + req + " quái", done?NamedTextColor.GREEN:NamedTextColor.GRAY);
        } else if (type.equals("travel_distance")) {
            target = "Khoảng cách"; icon = Material.FEATHER;
            tText = Component.text("Tổng khoảng cách di chuyển", NamedTextColor.YELLOW);
            uText = Component.text("  " + String.format("%,d", cur) + " / " + String.format("%,d", req) + " block", done?NamedTextColor.GREEN:NamedTextColor.GRAY);
        } else {
            target = o.containsKey("material")?(String)o.get("material"):(String)o.get("entity");
            icon = switch(type) {
                case "mine","break"->Material.IRON_PICKAXE;
                case "kill"->Material.IRON_SWORD;
                case "craft"->Material.CRAFTING_TABLE;
                case "harvest"->Material.WHEAT;
                case "fish"->Material.FISHING_ROD;
                case "cook"->Material.FURNACE;
                case "world"->Material.COMPASS;
                case "collect"->Material.CHEST;
                default->Material.PAPER;
            };
            tText = Component.text("Mục tiêu: " + target, NamedTextColor.YELLOW);
            uText = Component.text("  " + cur + " / " + req, done?NamedTextColor.GREEN:NamedTextColor.GRAY);
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(Component.text("🎯 Loại: " + type, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(tText.decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(uText.decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("  " + bar(pct, 20), done?NamedTextColor.GREEN:NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(""));
        lore.add(Component.text(done?"✅ Đã hoàn thành":"⏳ Đang thực hiện", done?NamedTextColor.GREEN:NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        ItemStack it = createItem(icon, Component.text("Mục tiêu #" + (idx+1), done?NamedTextColor.GREEN:NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false), lore);
        if (done) {
            it.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.LUCK, 1);
            ItemMeta m = it.getItemMeta(); m.addItemFlags(ItemFlag.HIDE_ENCHANTS); it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack createReward(PlayerData d) {
        boolean all = qm.isAllObjectivesCompleted(p.getUniqueId());
        boolean claimed = d.isClaimed();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        if (claimed) lore.add(Component.text("✅ Đã hoàn thành tuần này!", NamedTextColor.GREEN));
        else if (all) lore.add(Component.text("🎉 Nhấn để xác nhận hoàn thành!", NamedTextColor.GOLD));
        else lore.add(Component.text("⚠️ Hoàn thành tất cả mục tiêu đã!", NamedTextColor.RED));
        Material ic = claimed ? Material.CHEST_MINECART : all ? Material.ENDER_CHEST : Material.CHEST;
        return createItem(ic, Component.text("🎯 Xác nhận hoàn thành", NamedTextColor.GOLD), lore);
    }

    private String bar(double pct, int n) {
        int f = (int) Math.round(pct/100*n);
        return "█".repeat(f) + "░".repeat(n-f);
    }

    private ItemStack createItem(Material m, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name); meta.lore(lore); it.setItemMeta(meta);
        return it;
    }
    private ItemStack createItem(Material m, Component name) { return createItem(m, name, new ArrayList<>()); }
    public void open() { p.openInventory(inv); }
    public int getPage() { return page; }
    @Override public @NotNull Inventory getInventory() { return inv; }
}
