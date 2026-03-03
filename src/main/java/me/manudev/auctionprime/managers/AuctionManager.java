package me.manudev.auctionprime.managers;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.model.AuctionListing;
import me.manudev.auctionprime.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Gestore principale delle aste. Contiene la logica per creare inserzioni,
 * acquistare oggetti, cancellare inserzioni, gestire scadenze e pulizia.
 */
public class AuctionManager {

    private final AuctionPrime plugin;

    public AuctionManager(AuctionPrime plugin) {
        this.plugin = plugin;
    }

    /**
     * Crea una nuova inserzione nel mercato.
     * @return true se l'inserzione è stata creata con successo
     */
    public boolean createListing(Player seller, ItemStack item, double price) {
        LanguageManager lang = plugin.getLanguageManager();
        StorageManager storage = plugin.getStorageManager();
        EconomyManager economy = plugin.getEconomyManager();
        CooldownManager cooldown = plugin.getCooldownManager();
        WorldGroupManager worlds = plugin.getWorldGroupManager();

        // Verifica mondo
        if (!worlds.isWorldEnabled(seller.getWorld().getName())) {
            seller.sendMessage(lang.getWithPrefix("messaggi.mondo-non-abilitato"));
            return false;
        }

        // Verifica cooldown
        if (!seller.hasPermission("auctionprime.bypass.cooldown")) {
            int remaining = cooldown.getRemainingCooldown(seller.getUniqueId());
            if (remaining > 0) {
                seller.sendMessage(lang.getWithPrefix("messaggi.cooldown-attivo",
                        "{tempo}", String.valueOf(remaining)));
                return false;
            }
        }

        // Verifica max inserzioni
        int maxListings = plugin.getConfig().getInt("generale.max-inserzioni", 10);
        if (!seller.hasPermission("auctionprime.sell.unlimited")) {
            int current = storage.countPlayerActiveListings(seller.getUniqueId());
            if (current >= maxListings) {
                seller.sendMessage(lang.getWithPrefix("messaggi.max-inserzioni-raggiunto",
                        "{max}", String.valueOf(maxListings)));
                return false;
            }
        }

        // Verifica restrizioni
        if (!seller.hasPermission("auctionprime.bypass.restrictions")) {
            if (plugin.getRestrictionManager().isRestricted(item)) {
                seller.sendMessage(lang.getWithPrefix("messaggi.oggetto-vietato"));
                return false;
            }
        }

        // Verifica prezzo
        if (!economy.isPriceValid(price)) {
            if (price < economy.getMinPrice()) {
                seller.sendMessage(lang.getWithPrefix("messaggi.prezzo-troppo-basso",
                        "{min}", ChatUtil.formatPrice(economy.getMinPrice())));
            } else {
                seller.sendMessage(lang.getWithPrefix("messaggi.prezzo-troppo-alto",
                        "{max}", ChatUtil.formatPrice(economy.getMaxPrice())));
            }
            return false;
        }

        // Calcola e applica costi inserzione
        if (!seller.hasPermission("auctionprime.bypass.fees")) {
            double fee = economy.calculateListingFee(price);
            if (fee > 0) {
                if (!economy.has(seller, fee)) {
                    seller.sendMessage(lang.getWithPrefix("messaggi.soldi-insufficienti"));
                    return false;
                }
                economy.withdraw(seller, fee);
            }
        }

        // Crea l'inserzione
        long now = System.currentTimeMillis();
        int durationHours = plugin.getConfig().getInt("generale.durata-inserzione", 48);
        long expiration = now + (durationHours * 3600000L);
        String worldGroup = worlds.getWorldGroup(seller.getWorld().getName());

        AuctionListing listing = new AuctionListing(
                UUID.randomUUID(),
                seller.getUniqueId(),
                seller.getName(),
                item.clone(),
                price,
                now,
                expiration,
                worldGroup,
                false,
                false
        );

        // Rimuovi l'oggetto dalla mano del giocatore
        seller.getInventory().setItemInMainHand(null);

        // Salva
        storage.addListing(listing);
        cooldown.setCooldown(seller.getUniqueId());

        // Messaggio al venditore
        String itemName = getItemDisplayName(item);
        seller.sendMessage(lang.getWithPrefix("messaggi.inserzione-creata",
                "{oggetto}", itemName,
                "{prezzo}", ChatUtil.formatPrice(price)));

        // Suono
        playSound(seller, "generale.suono-vendita");

        // Annuncio globale
        if (plugin.getConfig().getBoolean("generale.annuncia-inserzioni", true)) {
            plugin.getNotificationManager().broadcastNewListing(seller.getName(), itemName, price);
        }

        // Log transazione
        plugin.getTransactionLogger().logListing(seller.getName(), itemName, price);

        // Discord
        if (plugin.getDiscordWebhook() != null) {
            plugin.getDiscordWebhook().sendNewListing(seller.getName(), itemName, price);
        }

        return true;
    }

    /**
     * Gestisce l'acquisto di un oggetto dal mercato.
     */
    public boolean buyItem(Player buyer, UUID listingId) {
        LanguageManager lang = plugin.getLanguageManager();
        StorageManager storage = plugin.getStorageManager();
        EconomyManager economy = plugin.getEconomyManager();

        AuctionListing listing = storage.getListing(listingId);

        if (listing == null || !listing.isActive()) {
            buyer.sendMessage(lang.getWithPrefix("messaggi.inserzione-non-trovata"));
            return false;
        }

        // Il venditore clicca sul proprio oggetto -> cancella
        if (listing.getSellerId().equals(buyer.getUniqueId())) {
            return cancelListing(buyer, listingId);
        }

        // Verifica economia pronta
        if (!economy.isReady()) {
            buyer.sendMessage(lang.getWithPrefix("messaggi.economia-non-pronta"));
            return false;
        }

        // Verifica soldi
        if (!economy.has(buyer, listing.getPrice())) {
            buyer.sendMessage(lang.getWithPrefix("messaggi.soldi-insufficienti"));
            return false;
        }

        // Preleva soldi dal compratore
        if (!economy.withdraw(buyer, listing.getPrice())) {
            buyer.sendMessage(lang.getWithPrefix("messaggi.soldi-insufficienti"));
            return false;
        }

        // Calcola tassa e deposita al venditore
        double tax = economy.calculateSaleTax(listing.getPrice());
        double sellerReceives = listing.getPrice() - tax;

        // Deposita al venditore (anche offline)
        Player sellerPlayer = Bukkit.getPlayer(listing.getSellerId());
        if (sellerPlayer != null && sellerPlayer.isOnline()) {
            economy.deposit(sellerPlayer, sellerReceives);
            String itemName = getItemDisplayName(listing.getItem());
            sellerPlayer.sendMessage(lang.getWithPrefix("messaggi.oggetto-venduto",
                    "{oggetto}", itemName,
                    "{compratore}", buyer.getName(),
                    "{prezzo}", ChatUtil.formatPrice(listing.getPrice()),
                    "{tassa}", ChatUtil.formatPrice(tax)));
        } else {
            economy.deposit(listing.getSellerName(), sellerReceives);
        }

        // Dai l'oggetto al compratore
        giveItem(buyer, listing.getItem());

        // Aggiorna inserzione
        listing.setSold(true);
        listing.setBuyerId(buyer.getUniqueId());
        listing.setBuyerName(buyer.getName());
        listing.setSoldTime(System.currentTimeMillis());

        // Se non è illimitata, rimuovi. Altrimenti crea una copia venduta.
        if (!listing.isUnlimited()) {
            storage.updateListing(listing);
        }

        // Messaggio al compratore
        String itemName = getItemDisplayName(listing.getItem());
        buyer.sendMessage(lang.getWithPrefix("messaggi.oggetto-comprato",
                "{oggetto}", itemName,
                "{venditore}", listing.getSellerName(),
                "{prezzo}", ChatUtil.formatPrice(listing.getPrice())));

        playSound(buyer, "generale.suono-vendita");

        // Log
        plugin.getTransactionLogger().logSale(
                listing.getSellerName(), buyer.getName(), itemName, listing.getPrice(), tax);

        // Discord
        if (plugin.getDiscordWebhook() != null) {
            plugin.getDiscordWebhook().sendSale(
                    listing.getSellerName(), buyer.getName(), itemName, listing.getPrice());
        }

        return true;
    }

    /**
     * Cancella un'inserzione e restituisce l'oggetto al venditore.
     */
    public boolean cancelListing(Player seller, UUID listingId) {
        LanguageManager lang = plugin.getLanguageManager();
        StorageManager storage = plugin.getStorageManager();

        AuctionListing listing = storage.getListing(listingId);
        if (listing == null) {
            seller.sendMessage(lang.getWithPrefix("messaggi.inserzione-non-trovata"));
            return false;
        }

        // Solo il proprietario o un admin può cancellare
        if (!listing.getSellerId().equals(seller.getUniqueId()) &&
                !seller.hasPermission("auctionprime.admin")) {
            seller.sendMessage(lang.getWithPrefix("messaggi.nessun-permesso"));
            return false;
        }

        listing.setCancelled(true);

        // Restituisci l'oggetto
        giveItem(seller, listing.getItem());

        // Rimuovi l'inserzione dallo storage
        storage.removeListing(listingId);

        seller.sendMessage(lang.getWithPrefix("messaggi.inserzione-cancellata"));
        return true;
    }

    /**
     * Raccoglie tutti gli oggetti scaduti/cancellati di un giocatore.
     */
    public int collectExpiredItems(Player player) {
        StorageManager storage = plugin.getStorageManager();
        List<AuctionListing> expired = storage.getExpiredItems(player.getUniqueId());

        if (expired.isEmpty()) return 0;

        int count = 0;
        for (AuctionListing listing : expired) {
            giveItem(player, listing.getItem());
            storage.removeListing(listing.getId());
            count++;
        }

        return count;
    }

    /**
     * Pulisce gli oggetti abbandonati (scaduti e non ritirati dopo N giorni).
     */
    public void cleanupExpired() {
        // Marca come scaduti quelli che hanno superato il tempo
        for (AuctionListing listing : plugin.getStorageManager().getAllListings().values()) {
            if (!listing.isSold() && !listing.isCancelled() && !listing.isExpired()
                    && listing.isTimeExpired()) {
                listing.setExpired(true);
                plugin.getStorageManager().updateListing(listing);
            }
        }

        // Rimuovi abbandonati
        int days = plugin.getConfig().getInt("generale.giorni-pulizia-automatica", 7);
        List<AuctionListing> removed = plugin.getStorageManager().cleanupAbandoned(days);
        if (!removed.isEmpty()) {
            plugin.getLogger().info("Rimossi " + removed.size() + " oggetti abbandonati.");
        }
    }

    /**
     * Consegna un oggetto al giocatore. Se l'inventario è pieno, lo droppa ai piedi.
     */
    public void giveItem(Player player, ItemStack item) {
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage(plugin.getLanguageManager().getWithPrefix("messaggi.inventario-pieno"));
        }
    }

    /**
     * Ottiene il nome visualizzato di un oggetto.
     */
    public String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
        }
        // Formatta il nome del Material in modo leggibile
        String name = item.getType().name().replace("_", " ").toLowerCase();
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Riproduce un suono dalla configurazione.
     */
    private void playSound(Player player, String configPath) {
        String soundName = plugin.getConfig().getString(configPath, "");
        if (soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }
}
