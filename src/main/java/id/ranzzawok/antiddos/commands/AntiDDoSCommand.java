package id.ranzzawok.antiddos.commands;

import id.ranzzawok.antiddos.AntiDDoSPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public class AntiDDoSCommand implements CommandExecutor {

    private final AntiDDoSPlugin plugin;

    public AntiDDoSCommand(AntiDDoSPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antiddos.admin")) {
            sender.sendMessage(ChatColor.RED + "Anda tidak punya izin.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status": {
                sender.sendMessage(ChatColor.GOLD + "=== AntiDDoS-RanzDev Status ===");
                sender.sendMessage(ChatColor.YELLOW + "Lockdown aktif: " + ChatColor.WHITE
                        + (plugin.getTracker().isLockdownActive() ? ChatColor.RED + "YA" : ChatColor.GREEN + "TIDAK"));
                sender.sendMessage(ChatColor.YELLOW + "IP saat ini diblokir sementara: " + ChatColor.WHITE
                        + plugin.getTracker().currentlyBlockedCount());
                sender.sendMessage(ChatColor.YELLOW + "Whitelist: " + ChatColor.WHITE
                        + plugin.getConfigManager().whitelist().size() + " IP");
                sender.sendMessage(ChatColor.YELLOW + "Blacklist: " + ChatColor.WHITE
                        + plugin.getConfigManager().blacklist().size() + " IP");
                return true;
            }
            case "reload": {
                plugin.getConfigManager().reload();
                sender.sendMessage(ChatColor.GREEN + "[AntiDDoS] Konfigurasi dimuat ulang.");
                return true;
            }
            case "log": {
                int lines = 10;
                if (args.length >= 2) {
                    try { lines = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
                }
                sendRecentLog(sender, lines);
                return true;
            }
            case "unlock": {
                plugin.getTracker().clearLockdown();
                sender.sendMessage(ChatColor.GREEN + "[AntiDDoS] Lockdown mode dimatikan manual.");
                return true;
            }
            case "unban": {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Gunakan: /antiddos unban <ip>");
                    return true;
                }
                plugin.getTracker().unblockIp(args[1]);
                sender.sendMessage(ChatColor.GREEN + "[AntiDDoS] IP " + args[1] + " dibuka blokirnya.");
                return true;
            }
            case "whitelist": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Gunakan: /antiddos whitelist <add|remove> <ip>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("add")) {
                    plugin.getConfigManager().addToWhitelist(args[2]);
                    sender.sendMessage(ChatColor.GREEN + "[AntiDDoS] " + args[2] + " ditambahkan ke whitelist.");
                } else if (args[1].equalsIgnoreCase("remove")) {
                    plugin.getConfigManager().removeFromWhitelist(args[2]);
                    sender.sendMessage(ChatColor.GREEN + "[AntiDDoS] " + args[2] + " dihapus dari whitelist.");
                }
                return true;
            }
            case "blacklist": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Gunakan: /antiddos blacklist <add|remove> <ip>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("add")) {
                    plugin.getConfigManager().addToBlacklist(args[2]);
                    sender.sendMessage(ChatColor.GREEN + "[AntiDDoS] " + args[2] + " ditambahkan ke blacklist.");
                } else if (args[1].equalsIgnoreCase("remove")) {
                    plugin.getConfigManager().removeFromBlacklist(args[2]);
                    sender.sendMessage(ChatColor.GREEN + "[AntiDDoS] " + args[2] + " dihapus dari blacklist.");
                }
                return true;
            }
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendRecentLog(CommandSender sender, int count) {
        java.io.File logFile = plugin.getAttackLogger().getLogFile();
        if (!logFile.exists()) {
            sender.sendMessage(ChatColor.YELLOW + "[AntiDDoS] Belum ada log serangan.");
            return;
        }
        Deque<String> lastLines = new ArrayDeque<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lastLines.addLast(line);
                if (lastLines.size() > count) lastLines.pollFirst();
            }
        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Gagal membaca log: " + e.getMessage());
            return;
        }
        if (lastLines.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "[AntiDDoS] Log kosong.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "=== " + lastLines.size() + " entri log terakhir ===");
        for (String l : lastLines) {
            sender.sendMessage(ChatColor.GRAY + l);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== AntiDDoS-RanzDev ===");
        sender.sendMessage(ChatColor.YELLOW + "/antiddos status" + ChatColor.GRAY + " - lihat status proteksi");
        sender.sendMessage(ChatColor.YELLOW + "/antiddos log [jumlah]" + ChatColor.GRAY + " - lihat log serangan terakhir");
        sender.sendMessage(ChatColor.YELLOW + "/antiddos reload" + ChatColor.GRAY + " - muat ulang config.yml");
        sender.sendMessage(ChatColor.YELLOW + "/antiddos unlock" + ChatColor.GRAY + " - matikan lockdown manual");
        sender.sendMessage(ChatColor.YELLOW + "/antiddos unban <ip>" + ChatColor.GRAY + " - buka blokir sebuah IP");
        sender.sendMessage(ChatColor.YELLOW + "/antiddos whitelist <add|remove> <ip>");
        sender.sendMessage(ChatColor.YELLOW + "/antiddos blacklist <add|remove> <ip>");
    }
}
