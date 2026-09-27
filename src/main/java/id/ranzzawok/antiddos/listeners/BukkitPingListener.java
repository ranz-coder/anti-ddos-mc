package id.ranzzawok.antiddos.listeners;

import id.ranzzawok.antiddos.AntiDDoSPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

/**
 * Fallback for plain Bukkit/Spigot servers (no Paper). The vanilla
 * ServerListPingEvent cannot be cancelled, so this only tracks ping rate
 * for visibility/logging purposes - real blocking still happens at the
 * join stage in ConnectionListener.
 */
public class BukkitPingListener implements Listener {

    private final AntiDDoSPlugin plugin;

    public BukkitPingListener(AntiDDoSPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        String ip = event.getAddress().getHostAddress();
        plugin.getTracker().registerPing(ip);
    }
}
