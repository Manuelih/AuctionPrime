package me.manudev.auctionprime.gui;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.model.AuctionListing;
import me.manudev.auctionprime.model.SortType;
import me.manudev.auctionprime.util.SignInputUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * Listener per gestire i click nelle GUI del mercato e admin.
 * Gestisce navigazione, acquisto, cancellazione, ricerca tramite cartello e admin.
 */
public class GUIListener implements Listener {

    private final AuctionPrime plugin;

    public GUIListener(AuctionPrime plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Gestione AdminGUI
        if (event.getInventory().getHolder() instanceof AdminGUI adminGui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= event.getInventory().getSize()) return;

            String adminType = adminGui.getGuiType();
            if ("admin-lista".equals(adminType)) {
                handleAdminListClick(player, adminGui, slot);
            } else if ("admin-dettaglio".equals(adminType)) {
                handleAdminDetailClick(player, adminGui, slot);
            }
            return;
        }

        // Gestione AuctionGUI
        if (!(event.getInventory().getHolder() instanceof AuctionGUI gui)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        String guiType = gui.getGuiType();

        // Se è una sub-GUI (inserzioni, scaduti, venduti)
        if ("inserzioni".equals(guiType) || "scaduti".equals(guiType) || "venduti".equals(guiType)) {
            handleSubGUIClick(player, gui, slot, guiType);
            return;
        }

        // GUI principale - barra inferiore
        int lastRowStart = (plugin.getConfig().getInt("gui.righe", 6) - 1) * 9;

        if (slot >= lastRowStart) {
            handleBottomBarClick(player, gui, slot);
            return;
        }

        // Click su un oggetto nel mercato
        if (slot < gui.getDisplayedListings().size()) {
            AuctionListing listing = gui.getDisplayedListings().get(slot);
            // buyItem gestisce sia acquisto che cancellazione se è il proprio
            plugin.getAuctionManager().buyItem(player, listing.getId());
            // Riapri la GUI aggiornata
            reopenMainGUI(player, gui.getPage(), gui.getSortType(), gui.getSearchQuery());
        }
    }

    /**
     * Gestisce i click nella barra inferiore della GUI principale.
     */
    private void handleBottomBarClick(Player player, AuctionGUI gui, int slot) {
        if (slot == AuctionGUI.getSlotPrevPage() && gui.getPage() > 0) {
            reopenMainGUI(player, gui.getPage() - 1, gui.getSortType(), gui.getSearchQuery());
        } else if (slot == AuctionGUI.getSlotNextPage()) {
            reopenMainGUI(player, gui.getPage() + 1, gui.getSortType(), gui.getSearchQuery());
        } else if (slot == AuctionGUI.getSlotSearch()) {
            // Apri cartello per la ricerca
            player.closeInventory();
            String[] signLines = new String[]{
                    "",
                    "^^^^^^^^",
                    plugin.getLanguageManager().get("messaggi.cartello-cerca-riga1"),
                    plugin.getLanguageManager().get("messaggi.cartello-cerca-riga2")
            };
            final SortType currentSort = gui.getSortType();
            SignInputUtil.openSignInput(plugin, player, signLines, (query) -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (query.isEmpty()) {
                        reopenMainGUI(player, 0, currentSort, null);
                    } else {
                        reopenMainGUI(player, 0, currentSort, query);
                    }
                });
            });
        } else if (slot == AuctionGUI.getSlotSort()) {
            SortType next = gui.getSortType().next();
            reopenMainGUI(player, 0, next, gui.getSearchQuery());
        } else if (slot == AuctionGUI.getSlotSell()) {
            player.closeInventory();
            player.sendMessage(plugin.getLanguageManager().getWithPrefix("messaggi.uso-vendi"));
        } else if (slot == AuctionGUI.getSlotMyListings()) {
            openSubGUI(player, "inserzioni", gui.getSortType(), gui.getSearchQuery());
        } else if (slot == AuctionGUI.getSlotExpired()) {
            // Raccogli oggetti scaduti
            int collected = plugin.getAuctionManager().collectExpiredItems(player);
            if (collected > 0) {
                player.sendMessage(plugin.getLanguageManager().getWithPrefix("messaggi.oggetti-scaduti-ritirati",
                        "{quantita}", String.valueOf(collected)));
            } else {
                player.sendMessage(plugin.getLanguageManager().getWithPrefix("messaggi.nessun-oggetto-scaduto"));
            }
            reopenMainGUI(player, gui.getPage(), gui.getSortType(), gui.getSearchQuery());
        } else if (slot == AuctionGUI.getSlotSold()) {
            openSubGUI(player, "venduti", gui.getSortType(), gui.getSearchQuery());
        } else if (slot == AuctionGUI.getSlotBack()) {
            String backCmd = plugin.getConfig().getString("comando-indietro", "");
            if (!backCmd.isEmpty()) {
                player.closeInventory();
                player.performCommand(backCmd);
            } else {
                player.closeInventory();
            }
        }
    }

    /**
     * Gestisce i click nelle sub-GUI (inserzioni personali, scaduti, venduti).
     */
    private void handleSubGUIClick(Player player, AuctionGUI gui, int slot, String type) {
        // Slot 49 = pulsante indietro
        if (slot == 49) {
            reopenMainGUI(player, 0, gui.getSortType(), gui.getSearchQuery());
            return;
        }

        if (slot < gui.getDisplayedListings().size()) {
            AuctionListing listing = gui.getDisplayedListings().get(slot);

            if ("inserzioni".equals(type)) {
                // Cancella l'inserzione
                plugin.getAuctionManager().cancelListing(player, listing.getId());
                openSubGUI(player, "inserzioni", gui.getSortType(), gui.getSearchQuery());
            }
            // scaduti e venduti sono solo visualizzazione (scaduti già raccolti sopra)
        }
    }

    /**
     * Gestisce i click nella lista admin.
     */
    private void handleAdminListClick(Player player, AdminGUI gui, int slot) {
        // Barra inferiore
        if (slot >= 45) {
            if (slot == AdminGUI.getSlotPrev() && gui.getPage() > 0) {
                AdminGUI newGui = new AdminGUI(plugin, player, gui.getPage() - 1, "admin-lista");
                newGui.setFilterActiveOnly(gui.isFilterActiveOnly());
                newGui.openAdminList();
            } else if (slot == AdminGUI.getSlotNext()) {
                AdminGUI newGui = new AdminGUI(plugin, player, gui.getPage() + 1, "admin-lista");
                newGui.setFilterActiveOnly(gui.isFilterActiveOnly());
                newGui.openAdminList();
            } else if (slot == AdminGUI.getSlotFilterActive()) {
                AdminGUI newGui = new AdminGUI(plugin, player, 0, "admin-lista");
                newGui.setFilterActiveOnly(true);
                newGui.openAdminList();
            } else if (slot == AdminGUI.getSlotFilterAll()) {
                AdminGUI newGui = new AdminGUI(plugin, player, 0, "admin-lista");
                newGui.setFilterActiveOnly(false);
                newGui.openAdminList();
            } else if (slot == AdminGUI.getSlotClose()) {
                player.closeInventory();
            }
            return;
        }

        // Click su un'inserzione -> apri dettaglio
        if (slot < gui.getDisplayedListings().size()) {
            AuctionListing listing = gui.getDisplayedListings().get(slot);
            AdminGUI detailGui = new AdminGUI(plugin, player, gui.getPage(), "admin-dettaglio");
            detailGui.openAdminDetail(listing);
        }
    }

    /**
     * Gestisce i click nel dettaglio admin di una singola inserzione.
     */
    private void handleAdminDetailClick(Player player, AdminGUI gui, int slot) {
        AuctionListing listing = gui.getSelectedListing();
        if (listing == null) return;

        if (slot == AdminGUI.getDetailRemove()) {
            // Rimuovi inserzione
            plugin.getStorageManager().removeListing(listing.getId());
            player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-rimosso"));
            reopenAdminList(player, gui.getPage());

        } else if (slot == AdminGUI.getDetailEditPrice()) {
            // Modifica prezzo tramite cartello
            player.closeInventory();
            String[] signLines = new String[]{
                    "",
                    "^^^^^^^^",
                    plugin.getLanguageManager().get("admin.cartello-prezzo-riga1"),
                    plugin.getLanguageManager().get("admin.cartello-prezzo-riga2")
            };
            final int adminPage = gui.getPage();
            SignInputUtil.openSignInput(plugin, player, signLines, (input) -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        double newPrice = Double.parseDouble(input.trim());
                        if (newPrice > 0) {
                            listing.setPrice(newPrice);
                            plugin.getStorageManager().updateListing(listing);
                            player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-prezzo-modificato",
                                    "{prezzo}", me.manudev.auctionprime.util.ChatUtil.formatPrice(newPrice)));
                        } else {
                            player.sendMessage(plugin.getLanguageManager().getWithPrefix("messaggi.prezzo-non-valido"));
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getLanguageManager().getWithPrefix("messaggi.prezzo-non-valido"));
                    }
                    reopenAdminList(player, adminPage);
                });
            });

        } else if (slot == AdminGUI.getDetailHalfPrice()) {
            // Dimezza prezzo
            double newPrice = listing.getPrice() / 2;
            listing.setPrice(newPrice);
            plugin.getStorageManager().updateListing(listing);
            player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-prezzo-modificato",
                    "{prezzo}", me.manudev.auctionprime.util.ChatUtil.formatPrice(newPrice)));
            AdminGUI detailGui = new AdminGUI(plugin, player, gui.getPage(), "admin-dettaglio");
            detailGui.openAdminDetail(listing);

        } else if (slot == AdminGUI.getDetailDoublePrice()) {
            // Raddoppia prezzo
            double newPrice = listing.getPrice() * 2;
            listing.setPrice(newPrice);
            plugin.getStorageManager().updateListing(listing);
            player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-prezzo-modificato",
                    "{prezzo}", me.manudev.auctionprime.util.ChatUtil.formatPrice(newPrice)));
            AdminGUI detailGui = new AdminGUI(plugin, player, gui.getPage(), "admin-dettaglio");
            detailGui.openAdminDetail(listing);

        } else if (slot == AdminGUI.getDetailChangeSeller()) {
            // Cambia venditore tramite cartello
            player.closeInventory();
            String[] signLines = new String[]{
                    "",
                    "^^^^^^^^",
                    plugin.getLanguageManager().get("admin.cartello-venditore-riga1"),
                    plugin.getLanguageManager().get("admin.cartello-venditore-riga2")
            };
            final int adminPage = gui.getPage();
            SignInputUtil.openSignInput(plugin, player, signLines, (input) -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    String newSellerName = input.trim();
                    if (!newSellerName.isEmpty()) {
                        @SuppressWarnings("deprecation")
                        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(newSellerName);
                        if (target.hasPlayedBefore() || target.isOnline()) {
                            listing.setSellerId(target.getUniqueId());
                            listing.setSellerName(target.getName() != null ? target.getName() : newSellerName);
                            plugin.getStorageManager().updateListing(listing);
                            player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-venditore-cambiato",
                                    "{venditore}", listing.getSellerName()));
                        } else {
                            player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-giocatore-non-trovato"));
                        }
                    }
                    reopenAdminList(player, adminPage);
                });
            });

        } else if (slot == AdminGUI.getDetailToggleExpired()) {
            // Toggle scaduto/attivo
            if (listing.isExpired()) {
                listing.setExpired(false);
                player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-riattivato"));
            } else {
                listing.setExpired(true);
                player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-scaduto"));
            }
            plugin.getStorageManager().updateListing(listing);
            AdminGUI detailGui = new AdminGUI(plugin, player, gui.getPage(), "admin-dettaglio");
            detailGui.openAdminDetail(listing);

        } else if (slot == AdminGUI.getDetailToggleUnlimited()) {
            // Toggle illimitato - serve ricreare l'inserzione perché unlimited è final
            // Per ora mostriamo un messaggio informativo
            player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-toggle-illimitato"));
            AdminGUI detailGui = new AdminGUI(plugin, player, gui.getPage(), "admin-dettaglio");
            detailGui.openAdminDetail(listing);

        } else if (slot == AdminGUI.getDetailGiveCopy()) {
            // Dai una copia dell'oggetto all'admin
            plugin.getAuctionManager().giveItem(player, listing.getItem());
            player.sendMessage(plugin.getLanguageManager().getWithPrefix("admin.messaggio-copia-data"));

        } else if (slot == AdminGUI.getDetailBack()) {
            // Torna alla lista admin
            reopenAdminList(player, gui.getPage());
        }
    }

    /**
     * Riapre la lista admin.
     */
    private void reopenAdminList(Player player, int page) {
        AdminGUI gui = new AdminGUI(plugin, player, page, "admin-lista");
        gui.openAdminList();
    }

    /**
     * Gestisce il completamento della modifica del cartello (input ricerca).
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!SignInputUtil.hasPending(player.getUniqueId())) return;

        event.setCancelled(true);
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = event.getLine(i) != null ? event.getLine(i) : "";
        }
        SignInputUtil.handleSignComplete(player, lines);
    }

    /**
     * Pulisce i dati pendenti quando un giocatore si disconnette.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SignInputUtil.removePending(event.getPlayer().getUniqueId());
    }

    /**
     * Riapre la GUI principale con i parametri specificati.
     */
    private void reopenMainGUI(Player player, int page, SortType sortType, String searchQuery) {
        AuctionGUI gui = new AuctionGUI(plugin, player, page, sortType, searchQuery, "principale");
        gui.openMainMenu();
    }

    /**
     * Apre una sub-GUI (inserzioni, scaduti, venduti).
     */
    private void openSubGUI(Player player, String type, SortType sortType, String searchQuery) {
        AuctionGUI gui = new AuctionGUI(plugin, player, 0, sortType, searchQuery, type);
        switch (type) {
            case "inserzioni" -> gui.openMyListings();
            case "scaduti" -> gui.openExpiredItems();
            case "venduti" -> gui.openSoldItems();
        }
    }
}
