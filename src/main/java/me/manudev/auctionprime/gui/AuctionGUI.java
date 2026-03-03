package me.manudev.auctionprime.gui;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.managers.LanguageManager;
import me.manudev.auctionprime.model.AuctionListing;
import me.manudev.auctionprime.model.SortType;
import me.manudev.auctionprime.util.ChatUtil;
import me.manudev.auctionprime.util.HeadUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI principale paginata del mercato.
 * Mostra tutti gli oggetti in vendita con prezzo, venditore, tempo rimanente.
 * - Compratore clicca → compra
 * - Venditore clicca sul proprio → cancella e restituisce oggetto
 */
public class AuctionGUI implements InventoryHolder {

    private final AuctionPrime plugin;
    private final Player viewer;
    private final int page;
    private final SortType sortType;
    private final String searchQuery;
    private final String guiType;
    private Inventory inventory;
    private final List<AuctionListing> displayedListings = new ArrayList<>();

    // Slot fissi nella barra inferiore (riga 6, slot 45-53)
    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_SEARCH = 47;
    private static final int SLOT_SORT = 48;
    private static final int SLOT_SELL = 49;
    private static final int SLOT_MY_LISTINGS = 50;
    private static final int SLOT_EXPIRED = 51;
    private static final int SLOT_SOLD = 52;
    private static final int SLOT_NEXT_PAGE = 53;
    private static final int SLOT_BACK = 46;

    public AuctionGUI(AuctionPrime plugin, Player viewer, int page, SortType sortType, String searchQuery, String guiType) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.page = page;
        this.sortType = sortType;
        this.searchQuery = searchQuery;
        this.guiType = guiType;
    }

    /**
     * Apre la GUI principale del mercato (paginata).
     */
    public void openMainMenu() {
        LanguageManager lang = plugin.getLanguageManager();
        int rows = plugin.getConfig().getInt("gui.righe", 6);
        int size = rows * 9;
        int itemSlots = (rows - 1) * 9; // L'ultima riga è per i pulsanti

        String title = lang.get("gui.titolo-principale", "{pagina}", String.valueOf(page + 1));
        inventory = Bukkit.createInventory(this, size, title);

        // Ottieni le inserzioni filtrate e ordinate
        String worldGroup = plugin.getWorldGroupManager().getWorldGroup(viewer.getWorld().getName());
        List<AuctionListing> listings = plugin.getStorageManager().getActiveListings(worldGroup);

        // Applica ricerca
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase();
            listings = listings.stream().filter(l -> {
                String itemName = plugin.getAuctionManager().getItemDisplayName(l.getItem()).toLowerCase();
                String typeName = l.getItem().getType().name().toLowerCase();
                String seller = l.getSellerName().toLowerCase();
                boolean matchName = itemName.contains(query);
                boolean matchType = typeName.contains(query);
                boolean matchSeller = seller.contains(query);
                boolean matchLore = false;
                if (l.getItem().hasItemMeta() && l.getItem().getItemMeta().hasLore()) {
                    for (String line : l.getItem().getItemMeta().getLore()) {
                        if (ChatUtil.stripColor(line).toLowerCase().contains(query)) {
                            matchLore = true;
                            break;
                        }
                    }
                }
                boolean matchEnchants = l.getItem().getEnchantments().keySet().stream()
                        .anyMatch(e -> e.getKey().getKey().toLowerCase().contains(query));
                return matchName || matchType || matchSeller || matchLore || matchEnchants;
            }).collect(Collectors.toList());
        }

        // Ordina
        listings = sortListings(listings);

        // Paginazione
        int totalPages = Math.max(1, (int) Math.ceil((double) listings.size() / itemSlots));
        int start = page * itemSlots;
        int end = Math.min(start + itemSlots, listings.size());

        displayedListings.clear();

        // Popola slot con oggetti
        for (int i = start; i < end; i++) {
            AuctionListing listing = listings.get(i);
            displayedListings.add(listing);

            ItemStack displayItem = listing.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                // Scegli il lore in base al proprietario
                if (listing.getSellerId().equals(viewer.getUniqueId())) {
                    lore.addAll(lang.getList("formato-oggetto.lore-proprio",
                            "{prezzo}", ChatUtil.formatPrice(listing.getPrice()),
                            "{tempo}", ChatUtil.formatTime(listing.getTimeRemaining(), lang)));
                } else {
                    lore.addAll(lang.getList("formato-oggetto.lore-aggiuntiva",
                            "{prezzo}", ChatUtil.formatPrice(listing.getPrice()),
                            "{venditore}", listing.getSellerName(),
                            "{tempo}", ChatUtil.formatTime(listing.getTimeRemaining(), lang)));
                }

                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }

            inventory.setItem(i - start, displayItem);
        }

        // Barra inferiore con bordi e pulsanti
        fillBottomBar(lang, totalPages);

        viewer.openInventory(inventory);
    }

    /**
     * Apre la GUI delle inserzioni personali del giocatore.
     */
    public void openMyListings() {
        LanguageManager lang = plugin.getLanguageManager();
        String title = lang.get("gui.titolo-inserzioni");
        inventory = Bukkit.createInventory(this, 54, title);

        List<AuctionListing> myListings = plugin.getStorageManager().getPlayerListings(viewer.getUniqueId());
        displayedListings.clear();

        for (int i = 0; i < Math.min(myListings.size(), 45); i++) {
            AuctionListing listing = myListings.get(i);
            displayedListings.add(listing);

            ItemStack displayItem = listing.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.addAll(lang.getList("formato-oggetto.lore-proprio",
                        "{prezzo}", ChatUtil.formatPrice(listing.getPrice()),
                        "{tempo}", ChatUtil.formatTime(listing.getTimeRemaining(), lang)));
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }

            inventory.setItem(i, displayItem);
        }

        addBackButton(lang);
        viewer.openInventory(inventory);
    }

    /**
     * Apre la GUI degli oggetti scaduti.
     */
    public void openExpiredItems() {
        LanguageManager lang = plugin.getLanguageManager();
        String title = lang.get("gui.titolo-scaduti");
        inventory = Bukkit.createInventory(this, 54, title);

        List<AuctionListing> expired = plugin.getStorageManager().getExpiredItems(viewer.getUniqueId());
        displayedListings.clear();

        for (int i = 0; i < Math.min(expired.size(), 45); i++) {
            AuctionListing listing = expired.get(i);
            displayedListings.add(listing);
            inventory.setItem(i, listing.getItem().clone());
        }

        addBackButton(lang);
        viewer.openInventory(inventory);
    }

    /**
     * Apre la GUI degli oggetti venduti.
     */
    public void openSoldItems() {
        LanguageManager lang = plugin.getLanguageManager();
        String title = lang.get("gui.titolo-venduti");
        inventory = Bukkit.createInventory(this, 54, title);

        List<AuctionListing> sold = plugin.getStorageManager().getSoldItems(viewer.getUniqueId());
        displayedListings.clear();

        for (int i = 0; i < Math.min(sold.size(), 45); i++) {
            AuctionListing listing = sold.get(i);
            displayedListings.add(listing);

            ItemStack displayItem = listing.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.addAll(lang.getList("formato-oggetto.lore-venduto",
                        "{compratore}", listing.getBuyerName() != null ? listing.getBuyerName() : "???",
                        "{prezzo}", ChatUtil.formatPrice(listing.getPrice()),
                        "{tempo}", ChatUtil.formatTime(System.currentTimeMillis() - listing.getSoldTime(), lang)));
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }

            inventory.setItem(i, displayItem);
        }

        addBackButton(lang);
        viewer.openInventory(inventory);
    }

    /**
     * Riempie la barra inferiore con pulsanti di navigazione e azioni.
     */
    private void fillBottomBar(LanguageManager lang, int totalPages) {
        int lastRowStart = (plugin.getConfig().getInt("gui.righe", 6) - 1) * 9;
        String borderMat = plugin.getConfig().getString("gui.materiali.bordo", "BLACK_STAINED_GLASS_PANE");

        // Riempi tutta la riga con bordi
        for (int i = lastRowStart; i < lastRowStart + 9; i++) {
            inventory.setItem(i, createButton(borderMat, lang.get("pulsanti.bordo"), null));
        }

        // Pagina precedente
        if (page > 0) {
            String mat = plugin.getConfig().getString("gui.materiali.pagina-precedente", "ARROW");
            inventory.setItem(SLOT_PREV_PAGE, createButton(mat, lang.get("pulsanti.pagina-precedente"), null));
        }

        // Pagina successiva
        if (page < totalPages - 1) {
            String mat = plugin.getConfig().getString("gui.materiali.pagina-successiva", "ARROW");
            inventory.setItem(SLOT_NEXT_PAGE, createButton(mat, lang.get("pulsanti.pagina-successiva"), null));
        }

        // Indietro
        String backCmd = plugin.getConfig().getString("comando-indietro", "");
        if (!backCmd.isEmpty()) {
            String mat = plugin.getConfig().getString("gui.materiali.indietro", "BARRIER");
            inventory.setItem(SLOT_BACK, createButton(mat, lang.get("pulsanti.indietro"), lang.getList("pulsanti.indietro-lore")));
        }

        // Cerca
        String searchMat = plugin.getConfig().getString("gui.materiali.cerca", "COMPASS");
        List<String> searchLore = lang.getList("pulsanti.cerca-lore");
        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchLore.add(lang.get("messaggi.ricerca-attiva", "{query}", searchQuery));
        }
        inventory.setItem(SLOT_SEARCH, createButton(searchMat, lang.get("pulsanti.cerca"), searchLore));

        // Ordina
        String sortMat = plugin.getConfig().getString("gui.materiali.ordina", "HOPPER");
        inventory.setItem(SLOT_SORT, createButton(sortMat, lang.get("pulsanti.ordina"),
                lang.getList("pulsanti.ordina-lore", "{ordinamento}", lang.get(sortType.getLangKey()))));

        // Vendi
        String sellMat = plugin.getConfig().getString("gui.materiali.vendi", "GOLD_INGOT");
        inventory.setItem(SLOT_SELL, createButton(sellMat, lang.get("pulsanti.vendi"), lang.getList("pulsanti.vendi-lore")));

        // Inserzioni personali
        String myMat = plugin.getConfig().getString("gui.materiali.inserzioni-personali", "BOOK");
        inventory.setItem(SLOT_MY_LISTINGS, createButton(myMat, lang.get("pulsanti.inserzioni-personali"),
                lang.getList("pulsanti.inserzioni-personali-lore")));

        // Oggetti scaduti
        String expMat = plugin.getConfig().getString("gui.materiali.oggetti-scaduti", "CHEST");
        inventory.setItem(SLOT_EXPIRED, createButton(expMat, lang.get("pulsanti.oggetti-scaduti"),
                lang.getList("pulsanti.oggetti-scaduti-lore")));

        // Oggetti venduti
        String soldMat = plugin.getConfig().getString("gui.materiali.oggetti-venduti", "EMERALD");
        inventory.setItem(SLOT_SOLD, createButton(soldMat, lang.get("pulsanti.oggetti-venduti"),
                lang.getList("pulsanti.oggetti-venduti-lore")));
    }

    /**
     * Aggiunge il pulsante Indietro allo slot 49 dell'ultima riga.
     */
    private void addBackButton(LanguageManager lang) {
        String mat = plugin.getConfig().getString("gui.materiali.indietro", "BARRIER");
        inventory.setItem(49, createButton(mat, lang.get("pulsanti.indietro"), lang.getList("pulsanti.indietro-lore")));
    }

    /**
     * Crea un pulsante per la GUI.
     */
    private ItemStack createButton(String materialStr, String name, List<String> lore) {
        ItemStack item = HeadUtil.parseItem(materialStr);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatUtil.colorize(name));
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Ordina le inserzioni in base al tipo selezionato.
     */
    private List<AuctionListing> sortListings(List<AuctionListing> listings) {
        return switch (sortType) {
            case RECENTE -> listings.stream()
                    .sorted(Comparator.comparingLong(AuctionListing::getCreationTime).reversed())
                    .collect(Collectors.toList());
            case VECCHIO -> listings.stream()
                    .sorted(Comparator.comparingLong(AuctionListing::getCreationTime))
                    .collect(Collectors.toList());
            case PREZZO_CRESCENTE -> listings.stream()
                    .sorted(Comparator.comparingDouble(AuctionListing::getPrice))
                    .collect(Collectors.toList());
            case PREZZO_DECRESCENTE -> listings.stream()
                    .sorted(Comparator.comparingDouble(AuctionListing::getPrice).reversed())
                    .collect(Collectors.toList());
        };
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getViewer() {
        return viewer;
    }

    public int getPage() {
        return page;
    }

    public SortType getSortType() {
        return sortType;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public String getGuiType() {
        return guiType;
    }

    public List<AuctionListing> getDisplayedListings() {
        return displayedListings;
    }

    // Costanti per accesso esterno
    public static int getSlotPrevPage() { return SLOT_PREV_PAGE; }
    public static int getSlotNextPage() { return SLOT_NEXT_PAGE; }
    public static int getSlotSearch() { return SLOT_SEARCH; }
    public static int getSlotSort() { return SLOT_SORT; }
    public static int getSlotSell() { return SLOT_SELL; }
    public static int getSlotMyListings() { return SLOT_MY_LISTINGS; }
    public static int getSlotExpired() { return SLOT_EXPIRED; }
    public static int getSlotSold() { return SLOT_SOLD; }
    public static int getSlotBack() { return SLOT_BACK; }
}
