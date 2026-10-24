package id.kalanusantara.core.listeners;

import id.kalanusantara.core.KalaCorePlugin;
import id.kalanusantara.core.identity.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * PlayerJoinListener — sambutan dan inisialisasi profil saat login
 */
public class PlayerJoinListener implements Listener {

    private final KalaCorePlugin plugin;

    public PlayerJoinListener(KalaCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load/create profil
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerProfile profile = plugin.getIdentityManager()
                .loadOrCreate(player.getUniqueId(), player.getName());

            // Kembali ke main thread untuk pesan & efek
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                boolean isNew = !profile.isTutorialDone();
                var locale = plugin.getLocaleManager();

                if (isNew) {
                    // Pemain baru — beri bonus awal
                    plugin.getEconomy().deposit(player.getUniqueId(),
                        plugin.getKalaConfig().getStartingBalance(),
                        "Bonus pemain baru");

                    // Pesan sambutan
                    player.sendMessage(Component.empty());
                    sendWelcome(player, locale.get(player, "welcome.first-join"));
                    player.sendMessage(Component.text(
                        locale.get(player, "economy.starting-bonus",
                            (int) plugin.getKalaConfig().getStartingBalance()),
                        NamedTextColor.GOLD));
                    player.sendMessage(Component.text(
                        locale.get(player, "welcome.tutorial-hint"),
                        NamedTextColor.YELLOW));
                    player.sendMessage(Component.empty());

                    // Jadwalkan tab-list info setelah 3 detik
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            player.sendMessage(Component.text(
                                locale.get(player, "tutorial.step.1"),
                                NamedTextColor.WHITE));
                        }
                    }, 60L); // 3 detik

                } else {
                    // Pemain kembali
                    player.sendMessage(Component.text(
                        locale.get(player, "welcome.returning", player.getName()),
                        NamedTextColor.GOLD));
                }

                // Tampilkan scoreboard & tab list untuk semua jenis pemain
                plugin.getScoreboardManager().update(player);
                plugin.getTabListManager().update(player);
                // Load quest progress cache & kosmetik & fauna
                plugin.getQuestProgressTracker().onLogin(player);
                plugin.getCosmeticManager().loadPlayer(player);
                if (plugin.getFaunaManager() != null) {
                    plugin.getFaunaManager().loadPlayer(player.getUniqueId());
                }
                if (plugin.getKalaEventManager() != null) {
                    plugin.getKalaEventManager().showBossBarToPlayer(player);
                }
                if (plugin.getVillageContractManager() != null) {
                    plugin.getVillageContractManager().loadPlayerProgress(player.getUniqueId());
                }
                if (plugin.getDiscoveryManager() != null) {
                    plugin.getDiscoveryManager().loadPlayerDiscoveries(player.getUniqueId());
                }
                // Claim pending entitlements if player was offline when reward occurred (Audit V9)
                plugin.getStorageManager().claimPendingEntitlements(player, plugin.getEconomy());
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Reset tab/scoreboard
        plugin.getScoreboardManager().remove(player);
        plugin.getTabListManager().reset(player);
        if (plugin.getKalaEventManager() != null) {
            plugin.getKalaEventManager().hideBossBarFromPlayer(player);
        }
        if (plugin.getFaunaManager() != null) {
            plugin.getFaunaManager().unloadPlayer(player.getUniqueId());
        }
        // Flush quest progress ke DB
        plugin.getQuestProgressTracker().onLogout(player);
        plugin.getCosmeticManager().unloadPlayer(player.getUniqueId());
        if (plugin.getVillageContractManager() != null) {
            plugin.getVillageContractManager().onPlayerQuit(player.getUniqueId());
        }
        // Update playtime dan simpan profil
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getIdentityManager().get(player.getUniqueId()).ifPresent(p -> {
                p.setLastSeen(System.currentTimeMillis());
            });
            plugin.getIdentityManager().unload(player.getUniqueId());
        });
    }

    private void sendWelcome(Player player, String text) {
        // Split multi-line teks
        for (String line : text.split("\n")) {
            if (!line.isEmpty()) {
                player.sendMessage(Component.text(line));
            }
        }
    }
}
