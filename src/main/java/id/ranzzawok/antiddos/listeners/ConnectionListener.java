package id.ranzzawok.antiddos.listeners;

import id.ranzzawok.antiddos.AntiDDoSPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.regex.Pattern;

/**
 * Fires as early as possible in the login sequence (before the player object
 * is fully created), so this is the earliest point a plugin can reject a
 * connection using only the public Bukkit API.
 */
public class ConnectionListener implements Listener {

    private static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    private final AntiDDoSPlugin plugin;

    public ConnectionListener(AntiDDoSPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String ip = event.getAddress().getHostAddress();
        String name = event.getName();

        // 1. Whitelist always bypasses everything below.
        if (plugin.getConfigManager().whitelist().contains(ip)) {
            return;
        }

        // 2. Permanent blacklist.
        if (plugin.getConfigManager().blacklist().contains(ip)) {
            deny(event, plugin.getConfigManager().msgBlacklisted());
            log("Blacklisted IP ditolak: " + ip);
            return;
        }

        // 3. Currently auto temp-blocked from a previous flood.
        if (plugin.getTracker().isTempBlocked(ip)) {
            deny(event, plugin.getConfigManager().msgTempBlocked());
            return;
        }

        // 4. Server-wide lockdown mode.
        if (plugin.getTracker().isLockdownActive()) {
            deny(event, plugin.getConfigManager().msgLockdown());
            return;
        }

        // 5. Username sanity check - most bot floods use garbage usernames.
        if (plugin.getConfigManager().strictUsernameValidation() && !VALID_USERNAME.matcher(name).matches()) {
            deny(event, plugin.getConfigManager().msgInvalidUsername());
            log("Username tidak valid ditolak: '" + name + "' dari " + ip);
            return;
        }

        // 6. Rate limiting (per-IP window + global window). This call also
        //    triggers temp-block / lockdown internally when thresholds are hit.
        boolean allowed = plugin.getTracker().registerJoinAttempt(ip);
        if (!allowed) {
            deny(event, plugin.getConfigManager().msgTempBlocked());
        }
    }

    private void deny(AsyncPlayerPreLoginEvent event, String message) {
        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        event.setKickMessage(message);
    }

    private void log(String message) {
        if (plugin.getConfigManager().logToConsole()) {
            plugin.getLogger().info("[AntiDDoS] " + message);
        }
    }
}
