package id.ranzzawok.antiddos.listeners;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import id.ranzzawok.antiddos.AntiDDoSPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Only registered when the server is running Paper (or a Paper fork).
 * PaperServerListPingEvent can actually be cancelled, unlike the vanilla
 * Bukkit ServerListPingEvent, so this gives real ping-flood mitigation.
 *
 * This class is kept separate from BukkitPingListener on purpose: the JVM
 * only tries to resolve PaperServerListPingEvent when THIS class is loaded,
 * which AntiDDoSPlugin only does after confirming the class exists. This
 * keeps the plugin from crashing with NoClassDefFoundError on plain Spigot.
 */
public class PaperPingListener implements Listener {

    private final AntiDDoSPlugin plugin;

    public PaperPingListener(AntiDDoSPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        String ip = event.getAddress().getHostAddress();
        if (plugin.getConfigManager().whitelist().contains(ip)) return;
        if (plugin.getConfigManager().blacklist().contains(ip)) {
            event.setCancelled(true);
            return;
        }
        if (!plugin.getTracker().registerPing(ip)) {
            event.setCancelled(true);
        }
    }
}
