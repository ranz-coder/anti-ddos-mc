package id.ranzzawok.antiddos;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-IP join attempts and global join rate, decides when to
 * temp-block an IP or trigger a server-wide lockdown.
 */
public class ConnectionTracker {

    private final AntiDDoSPlugin plugin;

    // IP -> timestamps (ms) of recent join attempts, for per-IP rate limiting
    private final Map<String, Deque<Long>> joinHistory = new ConcurrentHashMap<>();

    // IP -> timestamp (ms) until which the IP is auto-blocked
    private final Map<String, Long> tempBlocked = new ConcurrentHashMap<>();

    // IP -> timestamps of recent server-list pings, for ping-flood mitigation
    private final Map<String, Deque<Long>> pingHistory = new ConcurrentHashMap<>();

    // Global sliding window of join timestamps, used to detect a mass attack
    private final Deque<Long> globalJoinHistory = new ArrayDeque<>();

    private volatile long lockdownUntil = 0L;

    private File blockedFile;
    private YamlConfiguration blockedYaml;

    public ConnectionTracker(AntiDDoSPlugin plugin) {
        this.plugin = plugin;
        loadPersistedBlocks();
    }

    // ---------------------------------------------------------
    // Public API used by listeners
    // ---------------------------------------------------------

    /** Returns true if this IP is currently temp-blocked (and not expired). */
    public synchronized boolean isTempBlocked(String ip) {
        Long until = tempBlocked.get(ip);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            tempBlocked.remove(ip);
            return false;
        }
        return true;
    }

    /** Returns true if the server is currently in lockdown mode. */
    public boolean isLockdownActive() {
        return System.currentTimeMillis() < lockdownUntil;
    }

    public void clearLockdown() {
        lockdownUntil = 0L;
    }

    /**
     * Records a join attempt from this IP and checks both per-IP and global
     * rate limits. Returns true if this attempt should be ALLOWED.
     */
    public synchronized boolean registerJoinAttempt(String ip) {
        long now = System.currentTimeMillis();

        // --- per-IP window ---
        Deque<Long> history = joinHistory.computeIfAbsent(ip, k -> new ArrayDeque<>());
        history.addLast(now);
        long windowMs = plugin.getConfigManager().joinWindowSeconds() * 1000L;
        while (!history.isEmpty() && now - history.peekFirst() > windowMs) {
            history.pollFirst();
        }
        if (history.size() > plugin.getConfigManager().maxJoinsPerIp()) {
            blockIpTemporarily(ip);
            return false;
        }

        // --- global window (1 second) ---
        globalJoinHistory.addLast(now);
        while (!globalJoinHistory.isEmpty() && now - globalJoinHistory.peekFirst() > 1000L) {
            globalJoinHistory.pollFirst();
        }
        if (globalJoinHistory.size() > plugin.getConfigManager().maxGlobalJoinsPerSecond()) {
            triggerLockdown();
            return false;
        }

        return true;
    }

    /** Registers a server-list ping and returns true if it should be allowed. */
    public synchronized boolean registerPing(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> history = pingHistory.computeIfAbsent(ip, k -> new ArrayDeque<>());
        history.addLast(now);
        while (!history.isEmpty() && now - history.peekFirst() > 10_000L) {
            history.pollFirst();
        }
        return history.size() <= plugin.getConfigManager().maxPingsPerIpPer10s();
    }

    public void blockIpTemporarily(String ip) {
        long until = System.currentTimeMillis() + plugin.getConfigManager().tempBanDurationSeconds() * 1000L;
        tempBlocked.put(ip, until);
        persistBlocks();
        if (plugin.getConfigManager().logToConsole()) {
            plugin.getLogger().warning("[AntiDDoS] IP diblokir sementara (join-flood): " + ip);
        }
    }

    public void unblockIp(String ip) {
        tempBlocked.remove(ip);
        persistBlocks();
    }

    private void triggerLockdown() {
        long durationMs = plugin.getConfigManager().lockdownDurationSeconds() * 1000L;
        boolean wasActive = isLockdownActive();
        lockdownUntil = System.currentTimeMillis() + durationMs;
        if (!wasActive) {
            plugin.getLogger().warning("[AntiDDoS] LOCKDOWN MODE AKTIF! Deteksi lonjakan koneksi tidak wajar ("
                    + plugin.getConfigManager().maxGlobalJoinsPerSecond() + "+ joins/detik). Server dilindungi selama "
                    + plugin.getConfigManager().lockdownDurationSeconds() + " detik.");
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (p.hasPermission("antiddos.admin")) {
                    p.sendMessage("§c[AntiDDoS] §fLockdown mode diaktifkan - kemungkinan serangan bot/connection flood terdeteksi!");
                }
            });
        }
    }

    public int currentlyBlockedCount() {
        return (int) tempBlocked.entrySet().stream()
                .filter(e -> e.getValue() > System.currentTimeMillis())
                .count();
    }

    /** Periodic cleanup of expired entries so memory doesn't grow forever. */
    public void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            tempBlocked.entrySet().removeIf(e -> e.getValue() < now);
            joinHistory.entrySet().removeIf(e -> e.getValue().isEmpty()
                    || now - e.getValue().peekLast() > 3_600_000L);
            pingHistory.entrySet().removeIf(e -> e.getValue().isEmpty()
                    || now - e.getValue().peekLast() > 3_600_000L);
        }, 20L * 60, 20L * 60);
    }

    // ---------------------------------------------------------
    // Persistence of auto-blocked IPs across restarts
    // ---------------------------------------------------------

    private void loadPersistedBlocks() {
        blockedFile = new File(plugin.getDataFolder(), "blocked-ips.yml");
        if (!blockedFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                blockedFile.createNewFile();
            } catch (IOException ignored) {}
        }
        blockedYaml = YamlConfiguration.loadConfiguration(blockedFile);
        long now = System.currentTimeMillis();
        for (String ip : blockedYaml.getKeys(false)) {
            long until = blockedYaml.getLong(ip);
            if (until > now) {
                tempBlocked.put(ip, until);
            }
        }
    }

    private void persistBlocks() {
        try {
            for (String key : blockedYaml.getKeys(false)) {
                blockedYaml.set(key, null);
            }
            for (Map.Entry<String, Long> e : tempBlocked.entrySet()) {
                blockedYaml.set(e.getKey(), e.getValue());
            }
            blockedYaml.save(blockedFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[AntiDDoS] Gagal menyimpan blocked-ips.yml: " + e.getMessage());
        }
    }
}
