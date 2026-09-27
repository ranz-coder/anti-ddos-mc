package id.ranzzawok.antiddos;

import id.ranzzawok.antiddos.commands.AntiDDoSCommand;
import id.ranzzawok.antiddos.listeners.BukkitPingListener;
import id.ranzzawok.antiddos.listeners.ConnectionListener;
import id.ranzzawok.antiddos.listeners.PaperPingListener;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiDDoSPlugin extends JavaPlugin {

    private static AntiDDoSPlugin instance;

    private ConfigManager configManager;
    private ConnectionTracker tracker;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.tracker = new ConnectionTracker(this);

        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);

        // Detect Paper at runtime and register the stronger ping-flood
        // protection only if it's actually available, so this plugin still
        // works fine on plain Spigot/Bukkit.
        boolean isPaper = false;
        try {
            Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent");
            isPaper = true;
        } catch (ClassNotFoundException ignored) {}

        if (isPaper) {
            getServer().getPluginManager().registerEvents(new PaperPingListener(this), this);
            getLogger().info("Paper terdeteksi - proteksi ping-flood tingkat lanjut diaktifkan.");
        } else {
            getServer().getPluginManager().registerEvents(new BukkitPingListener(this), this);
            getLogger().info("Server non-Paper terdeteksi - menggunakan proteksi ping standar.");
        }

        AntiDDoSCommand executor = new AntiDDoSCommand(this);
        getCommand("antiddos").setExecutor(executor);

        tracker.startCleanupTask();

        getLogger().info("=======================================");
        getLogger().info(" AntiDDoS-RanzDev aktif!");
        getLogger().info(" Melindungi dari bot-flood / login-flood / connection-flood.");
        getLogger().info(" Catatan: untuk serangan UDP/volumetric murni, tetap gunakan");
        getLogger().info(" proteksi level jaringan (Cloudflare Spectrum / TCPShield / firewall).");
        getLogger().info("=======================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("AntiDDoS-RanzDev dimatikan.");
    }

    public static AntiDDoSPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ConnectionTracker getTracker() {
        return tracker;
    }
}
