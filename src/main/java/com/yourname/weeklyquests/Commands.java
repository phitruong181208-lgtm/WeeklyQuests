package com.yourname.weeklyquests;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {
    private final QuestManager qm = Main.getInstance().getQuestManager();

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player p)) { s.sendMessage("Chỉ người chơi!"); return true; }
        if (a.length == 0 || a[0].equalsIgnoreCase("gui") || a[0].equalsIgnoreCase("xem")) {
            new QuestGUI(p).open(); return true;
        }
        if (a[0].equalsIgnoreCase("reload") && p.hasPermission("weeklyquest.admin")) {
            Main.getInstance().reloadConfig();
            p.sendMessage(Component.text("✅ Đã tải lại cấu hình!", NamedTextColor.GREEN));
            return true;
        }
        p.sendMessage(Component.text("❌ Dùng: /weeklyquest [gui|reload]", NamedTextColor.RED));
        return true;
    }
}
