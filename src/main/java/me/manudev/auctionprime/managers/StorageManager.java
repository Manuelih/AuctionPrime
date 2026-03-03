package me.manudev.auctionprime.managers;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.model.AuctionListing;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gestisce il salvataggio e caricamento delle inserzioni su file YAML.
 */
public class StorageManager {

    private final AuctionPrime plugin;
    private final Map<UUID, AuctionListing> listings = new ConcurrentHashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public StorageManager(AuctionPrime plugin) {
        this.plugin = plugin;
        loadAll();
    }

    /**
     * Carica tutte le inserzioni dal file.
     */
    public void loadAll() {
        listings.clear();
        dataFile = new File(plugin.getDataFolder(), "listings.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossibile creare il file listings.yml: " + e.getMessage());
                return;
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.getConfigurationSection("listings") == null) return;

        for (String key : dataConfig.getConfigurationSection("listings").getKeys(false)) {
            String path = "listings." + key;
            try {
                UUID id = UUID.fromString(key);
                UUID sellerId = UUID.fromString(dataConfig.getString(path + ".sellerId"));
                String sellerName = dataConfig.getString(path + ".sellerName", "Sconosciuto");
                ItemStack item = dataConfig.getItemStack(path + ".item");
                double price = dataConfig.getDouble(path + ".price");
                long creationTime = dataConfig.getLong(path + ".creationTime");
                long expirationTime = dataConfig.getLong(path + ".expirationTime");
                String worldGroup = dataConfig.getString(path + ".worldGroup", "default");
                boolean serverListing = dataConfig.getBoolean(path + ".serverListing", false);
                boolean unlimited = dataConfig.getBoolean(path + ".unlimited", false);

                AuctionListing listing = new AuctionListing(
                        id, sellerId, sellerName, item, price,
                        creationTime, expirationTime, worldGroup, serverListing, unlimited
                );

                listing.setSold(dataConfig.getBoolean(path + ".sold", false));
                listing.setCancelled(dataConfig.getBoolean(path + ".cancelled", false));
                listing.setExpired(dataConfig.getBoolean(path + ".expired", false));

                if (dataConfig.contains(path + ".buyerId")) {
                    listing.setBuyerId(UUID.fromString(dataConfig.getString(path + ".buyerId")));
                    listing.setBuyerName(dataConfig.getString(path + ".buyerName", ""));
                    listing.setSoldTime(dataConfig.getLong(path + ".soldTime", 0));
                }

                listings.put(id, listing);
            } catch (Exception e) {
                plugin.getLogger().warning("Errore nel caricamento inserzione " + key + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Caricate " + listings.size() + " inserzioni.");
    }

    /**
     * Salva tutte le inserzioni su file.
     */
    public void saveAll() {
        dataConfig = new YamlConfiguration();

        for (Map.Entry<UUID, AuctionListing> entry : listings.entrySet()) {
            String path = "listings." + entry.getKey().toString();
            AuctionListing l = entry.getValue();

            dataConfig.set(path + ".sellerId", l.getSellerId().toString());
            dataConfig.set(path + ".sellerName", l.getSellerName());
            dataConfig.set(path + ".item", l.getItem());
            dataConfig.set(path + ".price", l.getPrice());
            dataConfig.set(path + ".creationTime", l.getCreationTime());
            dataConfig.set(path + ".expirationTime", l.getExpirationTime());
            dataConfig.set(path + ".worldGroup", l.getWorldGroup());
            dataConfig.set(path + ".serverListing", l.isServerListing());
            dataConfig.set(path + ".unlimited", l.isUnlimited());
            dataConfig.set(path + ".sold", l.isSold());
            dataConfig.set(path + ".cancelled", l.isCancelled());
            dataConfig.set(path + ".expired", l.isExpired());

            if (l.getBuyerId() != null) {
                dataConfig.set(path + ".buyerId", l.getBuyerId().toString());
                dataConfig.set(path + ".buyerName", l.getBuyerName());
                dataConfig.set(path + ".soldTime", l.getSoldTime());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossibile salvare listings.yml: " + e.getMessage());
        }
    }

    /**
     * Aggiunge un'inserzione e salva immediatamente.
     */
    public void addListing(AuctionListing listing) {
        listings.put(listing.getId(), listing);
        saveAll();
    }

    /**
     * Rimuove un'inserzione e salva.
     */
    public void removeListing(UUID id) {
        listings.remove(id);
        saveAll();
    }

    /**
     * Aggiorna un'inserzione e salva.
     */
    public void updateListing(AuctionListing listing) {
        listings.put(listing.getId(), listing);
        saveAll();
    }

    /**
     * Ottiene un'inserzione per ID.
     */
    public AuctionListing getListing(UUID id) {
        return listings.get(id);
    }

    /**
     * Restituisce tutte le inserzioni attive per un gruppo mondo specifico.
     */
    public List<AuctionListing> getActiveListings(String worldGroup) {
        return listings.values().stream()
                .filter(AuctionListing::isActive)
                .filter(l -> l.getWorldGroup().equals(worldGroup))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce le inserzioni attive di un giocatore.
     */
    public List<AuctionListing> getPlayerListings(UUID playerId) {
        return listings.values().stream()
                .filter(AuctionListing::isActive)
                .filter(l -> l.getSellerId().equals(playerId))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce gli oggetti scaduti o cancellati di un giocatore non ancora ritirati.
     */
    public List<AuctionListing> getExpiredItems(UUID playerId) {
        return listings.values().stream()
                .filter(l -> l.getSellerId().equals(playerId))
                .filter(l -> (l.isCancelled() || l.isExpired() || l.isTimeExpired()) && !l.isSold())
                .collect(Collectors.toList());
    }

    /**
     * Restituisce gli oggetti venduti di recente da un giocatore.
     */
    public List<AuctionListing> getSoldItems(UUID playerId) {
        return listings.values().stream()
                .filter(l -> l.getSellerId().equals(playerId))
                .filter(AuctionListing::isSold)
                .sorted(Comparator.comparingLong(AuctionListing::getSoldTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Conta le inserzioni attive di un giocatore.
     */
    public int countPlayerActiveListings(UUID playerId) {
        return (int) listings.values().stream()
                .filter(AuctionListing::isActive)
                .filter(l -> l.getSellerId().equals(playerId))
                .count();
    }

    /**
     * Rimuove le inserzioni abbandonate (scadute e non ritirate dopo N giorni).
     */
    public List<AuctionListing> cleanupAbandoned(int maxDays) {
        long maxAge = maxDays * 24L * 60 * 60 * 1000;
        long now = System.currentTimeMillis();

        List<AuctionListing> toRemove = listings.values().stream()
                .filter(l -> !l.isActive() && !l.isSold())
                .filter(l -> (now - l.getExpirationTime()) > maxAge)
                .collect(Collectors.toList());

        for (AuctionListing l : toRemove) {
            listings.remove(l.getId());
        }

        if (!toRemove.isEmpty()) saveAll();
        return toRemove;
    }

    public Map<UUID, AuctionListing> getAllListings() {
        return Collections.unmodifiableMap(listings);
    }
}
