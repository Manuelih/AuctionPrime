package me.manudev.auctionprime.managers;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Gestisce le notifiche e gli annunci globali.
 */
public class NotificationManager {

    private final AuctionPrime plugin;

    public NotificationManager(AuctionPrime plugin) {
        this.plugin = plugin;
    }

    /**
     * Annuncia una nuova inserzione a tutti i giocatori con il permesso di notifica.
     */
    public void broadcastNewListing(String sellerName, String itemName, double price) {
        LanguageManager lang = plugin.getLanguageManager();
        String message = lang.getWithPrefix("messaggi.annuncio-nuova-inserzione",
                "{giocatore}", sellerName,
                "{oggetto}", itemName,
                "{prezzo}", ChatUtil.formatPrice(price));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("auctionprime.notify")) {
                player.sendMessage(message);
            }
        }
    }
}
