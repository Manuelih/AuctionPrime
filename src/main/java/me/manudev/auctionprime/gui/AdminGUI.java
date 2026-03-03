package me.manudev.auctionprime.gui;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.managers.LanguageManager;
import me.manudev.auctionprime.model.AuctionListing;
import me.manudev.auctionprime.util.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI di amministrazione per gestire tutte le inserzioni.
 * Permette di: visualizzare tutte le aste, rimuoverle, modificare prezzo,
 * cambiare venditore e altro.
 *
 * guiType values:
 * - "admin-lista"   : lista paginata di tutte le inserzioni
 * - "admin-dettaglio": dettaglio di una singola inserzione con azioni
 */
public class AdminGUI implements InventoryHolder {

    private final AuctionPrime plugin;
    private final Player viewer;
    private final int page;
    private final String guiType;
    private Inventory inventory;
    private final List<AuctionListing> displayedListings = new ArrayList<>();
    private AuctionListing selectedListing;

    // Slot per la lista admin
    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_BACK_LIST = 48;
    private static final int SLOT_FILTER_ACTIVE = 46;
    private static final int SLOT_FILTER_ALL = 47;
    private static final int SLOT_CLOSE = 50;

    // Slot per il dettaglio admin
    private static final int DETAIL_ITEM_SLOT = 4;
    private static final int DETAIL_REMOVE = 19;
    private static final int DETAIL_EDIT_PRICE = 21;
    private static final int DETAIL_HALF_PRICE = 22;
    private static final int DETAIL_DOUBLE_PRICE = 23;
    private static final int DETAIL_CHANGE_SELLER = 25;
    private static final int DETAIL_TOGGLE_EXPIRED = 29;
    private static final int DETAIL_TOGGLE_UNLIMITED = 31;
    private static final int DETAIL_GIVE_COPY = 33;
    private static final int DETAIL_BACK = 40;

    private boolean filterActiveOnly = false;

    public AdminGUI(AuctionPrime plugin, Player viewer, int page, String guiType) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.page = page;
        this.guiType = guiType;
    }

    /**
     * Apre la lista admin di tutte le inserzioni.
     */
    public void openAdminList() {
        LanguageManager lang = plugin.getLanguageManager();
        int size = 54;
        int itemSlots = 45;

        String title = lang.get("admin.titolo-lista", "{pagina}", String.valueOf(page + 1));
        inventory = Bukkit.createInventory(this, size, title);

        List<AuctionListing> allListings;
        if (filterActiveOnly) {
            allListings = plugin.getStorageManager().getAllListings().values().stream()
                    .filter(AuctionListing::isActive)
                    .sorted(Comparator.comparingLong(AuctionListing::getCreationTime).reversed())
                    .collect(Collectors.toList());
        } else {
            allListings = plugin.getStorageManager().getAllListings().values().stream()
                    .sorted(Comparator.comparingLong(AuctionListing::getCreationTime).reversed())
                    .collect(Collectors.toList());
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) allListings.size() / itemSlots));
        int start = page * itemSlots;
        int end = Math.min(start + itemSlots, allListings.size());

        displayedListings.clear();

        for (int i = start; i < end; i++) {
            AuctionListing listing = allListings.get(i);
            displayedListings.add(listing);

            ItemStack displayItem = listing.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                String status;
                if (listing.isSold()) {
                    status = lang.get("admin.stato-venduto");
                } else if (listing.isCancelled()) {
                    status = lang.get("admin.stato-cancellato");
                } else if (listing.isExpired() || listing.isTimeExpired()) {
                    status = lang.get("admin.stato-scaduto");
                } else {
                    status = lang.get("admin.stato-attivo");
                }

                lore.addAll(lang.getList("admin.lore-lista",
                        "{prezzo}", ChatUtil.formatPrice(listing.getPrice()),
                        "{venditore}", listing.getSellerName(),
                        "{stato}", status,
                        "{id}", listing.getId().toString().substring(0, 8)));

                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }

            inventory.setItem(i - start, displayItem);
        }

        // Barra inferiore
        fillAdminBottomBar(lang, totalPages);

        viewer.openInventory(inventory);
    }

    /**
     * Apre il dettaglio di una singola inserzione per le azioni admin.
     */
    public void openAdminDetail(AuctionListing listing) {
        this.selectedListing = listing;
        LanguageManager lang = plugin.getLanguageManager();

        String title = lang.get("admin.titolo-dettaglio");
        inventory = Bukkit.createInventory(this, 54, title);

        // Oggetto al centro in alto
        ItemStack displayItem = listing.getItem().clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

            String status;
            if (listing.isSold()) {
                status = lang.get("admin.stato-venduto");
            } else if (listing.isCancelled()) {
                status = lang.get("admin.stato-cancellato");
            } else if (listing.isExpired() || listing.isTimeExpired()) {
                status = lang.get("admin.stato-scaduto");
            } else {
                status = lang.get("admin.stato-attivo");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            lore.add("");
            lore.addAll(lang.getList("admin.lore-dettaglio",
                    "{prezzo}", ChatUtil.formatPrice(listing.getPrice()),
                    "{venditore}", listing.getSellerName(),
                    "{venditore-uuid}", listing.getSellerId().toString(),
                    "{stato}", status,
                    "{id}", listing.getId().toString(),
                    "{creazione}", sdf.format(new Date(listing.getCreationTime())),
                    "{scadenza}", sdf.format(new Date(listing.getExpirationTime())),
                    "{mondo}", listing.getWorldGroup()));

            meta.setLore(lore);
            displayItem.setItemMeta(meta);
        }
        inventory.setItem(DETAIL_ITEM_SLOT, displayItem);

        // Pulsanti azioni
        // Riga 2 (slot 18-26): Azioni principali
        inventory.setItem(DETAIL_REMOVE, createButton(Material.BARRIER,
                lang.get("admin.pulsante-rimuovi"),
                lang.getList("admin.pulsante-rimuovi-lore")));

        inventory.setItem(DETAIL_EDIT_PRICE, createButton(Material.NAME_TAG,
                lang.get("admin.pulsante-modifica-prezzo"),
                lang.getList("admin.pulsante-modifica-prezzo-lore")));

        inventory.setItem(DETAIL_HALF_PRICE, createButton(Material.IRON_NUGGET,
                lang.get("admin.pulsante-dimezza-prezzo"),
                lang.getList("admin.pulsante-dimezza-prezzo-lore",
                        "{prezzo}", ChatUtil.formatPrice(listing.getPrice() / 2))));

        inventory.setItem(DETAIL_DOUBLE_PRICE, createButton(Material.GOLD_NUGGET,
                lang.get("admin.pulsante-raddoppia-prezzo"),
                lang.getList("admin.pulsante-raddoppia-prezzo-lore",
                        "{prezzo}", ChatUtil.formatPrice(listing.getPrice() * 2))));

        inventory.setItem(DETAIL_CHANGE_SELLER, createButton(Material.PLAYER_HEAD,
                lang.get("admin.pulsante-cambia-venditore"),
                lang.getList("admin.pulsante-cambia-venditore-lore")));

        // Riga 3 (slot 27-35): Toggle e utility
        String toggleExpText = (listing.isExpired() || listing.isTimeExpired())
                ? lang.get("admin.pulsante-riattiva")
                : lang.get("admin.pulsante-segna-scaduto");
        inventory.setItem(DETAIL_TOGGLE_EXPIRED, createButton(Material.CLOCK,
                toggleExpText,
                lang.getList("admin.pulsante-toggle-scaduto-lore")));

        String toggleUnlText = listing.isUnlimited()
                ? lang.get("admin.pulsante-rendi-limitato")
                : lang.get("admin.pulsante-rendi-illimitato");
        inventory.setItem(DETAIL_TOGGLE_UNLIMITED, createButton(Material.BEDROCK,
                toggleUnlText,
                lang.getList("admin.pulsante-toggle-illimitato-lore")));

        inventory.setItem(DETAIL_GIVE_COPY, createButton(Material.CHEST,
                lang.get("admin.pulsante-dai-copia"),
                lang.getList("admin.pulsante-dai-copia-lore")));

        // Indietro
        inventory.setItem(DETAIL_BACK, createButton(Material.ARROW,
                lang.get("admin.pulsante-indietro"),
                null));

        viewer.openInventory(inventory);
    }

    /**
     * Riempie la barra inferiore della lista admin.
     */
    private void fillAdminBottomBar(LanguageManager lang, int totalPages) {
        // Bordi
        ItemStack border = createButton(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border);
        }

        // Pagina precedente
        if (page > 0) {
            inventory.setItem(SLOT_PREV, createButton(Material.ARROW,
                    lang.get("pulsanti.pagina-precedente"), null));
        }

        // Pagina successiva
        if (page < totalPages - 1) {
            inventory.setItem(SLOT_NEXT, createButton(Material.ARROW,
                    lang.get("pulsanti.pagina-successiva"), null));
        }

        // Filtro attivi
        Material filterMat = filterActiveOnly ? Material.LIME_DYE : Material.GRAY_DYE;
        inventory.setItem(SLOT_FILTER_ACTIVE, createButton(filterMat,
                lang.get("admin.pulsante-filtro-attivi"),
                lang.getList("admin.pulsante-filtro-attivi-lore")));

        // Filtro tutti
        Material filterAllMat = !filterActiveOnly ? Material.LIME_DYE : Material.GRAY_DYE;
        inventory.setItem(SLOT_FILTER_ALL, createButton(filterAllMat,
                lang.get("admin.pulsante-filtro-tutti"),
                lang.getList("admin.pulsante-filtro-tutti-lore")));

        // Info
        int total = plugin.getStorageManager().getAllListings().size();
        long active = plugin.getStorageManager().getAllListings().values().stream()
                .filter(AuctionListing::isActive).count();
        inventory.setItem(SLOT_INFO, createButton(Material.PAPER,
                lang.get("admin.pulsante-info"),
                lang.getList("admin.pulsante-info-lore",
                        "{totale}", String.valueOf(total),
                        "{attive}", String.valueOf(active),
                        "{pagina}", String.valueOf(page + 1),
                        "{pagine}", String.valueOf(totalPages))));

        // Chiudi
        inventory.setItem(SLOT_CLOSE, createButton(Material.BARRIER,
                lang.get("admin.pulsante-chiudi"), null));
    }

    private ItemStack createButton(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getViewer() { return viewer; }
    public int getPage() { return page; }
    public String getGuiType() { return guiType; }
    public List<AuctionListing> getDisplayedListings() { return displayedListings; }
    public AuctionListing getSelectedListing() { return selectedListing; }
    public boolean isFilterActiveOnly() { return filterActiveOnly; }
    public void setFilterActiveOnly(boolean filterActiveOnly) { this.filterActiveOnly = filterActiveOnly; }

    // Costanti per accesso esterno
    public static int getSlotPrev() { return SLOT_PREV; }
    public static int getSlotNext() { return SLOT_NEXT; }
    public static int getSlotInfo() { return SLOT_INFO; }
    public static int getSlotBackList() { return SLOT_BACK_LIST; }
    public static int getSlotFilterActive() { return SLOT_FILTER_ACTIVE; }
    public static int getSlotFilterAll() { return SLOT_FILTER_ALL; }
    public static int getSlotClose() { return SLOT_CLOSE; }
    public static int getDetailItemSlot() { return DETAIL_ITEM_SLOT; }
    public static int getDetailRemove() { return DETAIL_REMOVE; }
    public static int getDetailEditPrice() { return DETAIL_EDIT_PRICE; }
    public static int getDetailHalfPrice() { return DETAIL_HALF_PRICE; }
    public static int getDetailDoublePrice() { return DETAIL_DOUBLE_PRICE; }
    public static int getDetailChangeSeller() { return DETAIL_CHANGE_SELLER; }
    public static int getDetailToggleExpired() { return DETAIL_TOGGLE_EXPIRED; }
    public static int getDetailToggleUnlimited() { return DETAIL_TOGGLE_UNLIMITED; }
    public static int getDetailGiveCopy() { return DETAIL_GIVE_COPY; }
    public static int getDetailBack() { return DETAIL_BACK; }
}
