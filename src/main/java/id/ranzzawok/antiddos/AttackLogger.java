package id.ranzzawok.antiddos;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Writes every block/kick/lockdown event to plugins/AntiDDoS-RanzDev/logs/attack-log.txt
 * so admins have a persistent record even after the console scrolls away or
 * the server restarts. Also broadcasts to online admins in real time.
 */
public class AttackLogger {

    private final AntiDDoSPlugin plugin;
    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public AttackLogger(AntiDDoSPlugin plugin) {
        this.plugin = plugin;
        File logDir = new File(plugin.getDataFolder(), "logs");
        if (!logDir.exists()) logDir.mkdirs();
        this.logFile = new File(logDir, "attack-log.txt");
    }

    /** type examples: "BLOCKED_IP", "TEMP_BAN", "LOCKDOWN_START", "LOCKDOWN_END", "INVALID_USERNAME" */
    public synchronized void log(String type, String ip, String detail) {
        String line = String.format("[%s] [%s] IP=%s %s",
                dateFormat.format(new Date()), type, ip, detail == null ? "" : detail);

        // Console
        if (plugin.getConfigManager().logToConsole()) {
            plugin.getLogger().warning(line);
        }

        // File (append)
        try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
            out.println(line);
        } catch (IOException e) {
            plugin.getLogger().warning("[AntiDDoS] Gagal menulis attack-log.txt: " + e.getMessage());
        }

        // Real-time notify online admins
        String chatLine = ChatColor.RED + "[AntiDDoS] " + ChatColor.WHITE + type + " " + ChatColor.GRAY + ip
                + (detail != null ? ChatColor.DARK_GRAY + " - " + detail : "");
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.hasPermission("antiddos.admin")) {
                p.sendMessage(chatLine);
            }
        });
    }

    public File getLogFile() {
        return logFile;
    }
}
