package id.ranzzawok.antiddos;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final AntiDDoSPlugin plugin;
    private FileConfiguration cfg;

    public ConfigManager(AntiDDoSPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    public int maxJoinsPerIp() { return cfg.getInt("max-joins-per-ip", 4); }
    public int joinWindowSeconds() { return cfg.getInt("join-window-seconds", 10); }
    public int tempBanDurationSeconds() { return cfg.getInt("temp-ban-duration-seconds", 300); }
    public int maxGlobalJoinsPerSecond() { return cfg.getInt("max-global-joins-per-second", 20); }
    public int lockdownDurationSeconds() { return cfg.getInt("lockdown-duration-seconds", 30); }
    public boolean strictUsernameValidation() { return cfg.getBoolean("strict-username-validation", true); }
    public int maxPingsPerIpPer10s() { return cfg.getInt("max-pings-per-ip-per-10s", 10); }
    public boolean logToConsole() { return cfg.getBoolean("log-to-console", true); }

    public String msgBlacklisted() { return color(cfg.getString("kick-message-blacklisted", "&cBlocked.")); }
    public String msgTempBlocked() { return color(cfg.getString("kick-message-temp-blocked", "&cToo many attempts.")); }
    public String msgLockdown() { return color(cfg.getString("kick-message-lockdown", "&cServer under protection.")); }
    public String msgInvalidUsername() { return color(cfg.getString("kick-message-invalid-username", "&cInvalid username.")); }

    public List<String> whitelist() {
        List<String> l = cfg.getStringList("whitelist");
        return l != null ? l : new ArrayList<>();
    }

    public List<String> blacklist() {
        List<String> l = cfg.getStringList("blacklist");
        return l != null ? l : new ArrayList<>();
    }

    public void addToWhitelist(String ip) {
        List<String> l = whitelist();
        if (!l.contains(ip)) l.add(ip);
        cfg.set("whitelist", l);
        plugin.saveConfig();
    }

    public void addToBlacklist(String ip) {
        List<String> l = blacklist();
        if (!l.contains(ip)) l.add(ip);
        cfg.set("blacklist", l);
        plugin.saveConfig();
    }

    public void removeFromWhitelist(String ip) {
        List<String> l = whitelist();
        l.remove(ip);
        cfg.set("whitelist", l);
        plugin.saveConfig();
    }

    public void removeFromBlacklist(String ip) {
        List<String> l = blacklist();
        l.remove(ip);
        cfg.set("blacklist", l);
        plugin.saveConfig();
    }

    private String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }
}
