package me.manudev.auctionprime.commands;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.gui.AdminGUI;
import me.manudev.auctionprime.gui.AuctionGUI;
import me.manudev.auctionprime.managers.LanguageManager;
import me.manudev.auctionprime.model.SortType;
import me.manudev.auctionprime.util.SignInputUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando /mercato - Comando principale del plugin.
 * Senza argomenti: apre la GUI paginata con tutte le inserzioni.
 * Sub-comandi: vendi, cerca, inserzioni, scaduti, venduti, reload
 */
public class MercatoCommand implements CommandExecutor, TabCompleter {

    private final AuctionPrime plugin;

    public MercatoCommand(AuctionPrime plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        // Console: solo reload
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("auctionprime.admin.reload")) {
                    plugin.ricarica();
                    sender.sendMessage(lang.getWithPrefix("messaggi.config-ricaricata"));
                } else {
                    sender.sendMessage(lang.getWithPrefix("messaggi.nessun-permesso"));
                }
            } else {
                sender.sendMessage(lang.getWithPrefix("messaggi.solo-giocatori"));
            }
            return true;
        }

        // Permesso base
        if (!player.hasPermission("auctionprime.use")) {
            player.sendMessage(lang.getWithPrefix("messaggi.nessun-permesso"));
            return true;
        }

        // Verifica mondo
        if (!plugin.getWorldGroupManager().isWorldEnabled(player.getWorld().getName())) {
            player.sendMessage(lang.getWithPrefix("messaggi.mondo-non-abilitato"));
            return true;
        }

        // Nessun argomento -> apri GUI principale
        if (args.length == 0) {
            AuctionGUI gui = new AuctionGUI(plugin, player, 0, SortType.RECENTE, null, "principale");
            gui.openMainMenu();
            return true;
        }

        // Sub-comandi
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "vendi", "sell" -> handleVendi(player, args, lang);
            case "cerca", "search" -> handleCerca(player, args, lang);
            case "inserzioni", "selling" -> {
                AuctionGUI gui = new AuctionGUI(plugin, player, 0, SortType.RECENTE, null, "inserzioni");
                gui.openMyListings();
            }
            case "scaduti", "expired" -> {
                int collected = plugin.getAuctionManager().collectExpiredItems(player);
                if (collected > 0) {
                    player.sendMessage(lang.getWithPrefix("messaggi.oggetti-scaduti-ritirati",
                            "{quantita}", String.valueOf(collected)));
                } else {
                    player.sendMessage(lang.getWithPrefix("messaggi.nessun-oggetto-scaduto"));
                }
            }
            case "venduti", "sold" -> {
                AuctionGUI gui = new AuctionGUI(plugin, player, 0, SortType.RECENTE, null, "venduti");
                gui.openSoldItems();
            }
            case "admin" -> {
                if (player.hasPermission("auctionprime.admin")) {
                    AdminGUI adminGui = new AdminGUI(plugin, player, 0, "admin-lista");
                    adminGui.openAdminList();
                } else {
                    player.sendMessage(lang.getWithPrefix("messaggi.nessun-permesso"));
                }
            }
            case "reload" -> {
                if (player.hasPermission("auctionprime.admin.reload")) {
                    plugin.ricarica();
                    player.sendMessage(lang.getWithPrefix("messaggi.config-ricaricata"));
                } else {
                    player.sendMessage(lang.getWithPrefix("messaggi.nessun-permesso"));
                }
            }
            case "help", "aiuto", "?" -> sendHelp(player, lang);
            default -> sendHelp(player, lang);
        }

        return true;
    }

    /**
     * Gestisce /mercato vendi <prezzo>
     */
    private void handleVendi(Player player, String[] args, LanguageManager lang) {
        if (!player.hasPermission("auctionprime.sell")) {
            player.sendMessage(lang.getWithPrefix("messaggi.nessun-permesso"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(lang.getWithPrefix("messaggi.uso-vendi"));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.getWithPrefix("messaggi.prezzo-non-valido"));
            return;
        }

        if (price <= 0) {
            player.sendMessage(lang.getWithPrefix("messaggi.prezzo-non-valido"));
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(lang.getWithPrefix("messaggi.oggetto-non-in-mano"));
            return;
        }

        plugin.getAuctionManager().createListing(player, itemInHand, price);
    }

    /**
     * Gestisce /mercato cerca - Apre un cartello per inserire la query di ricerca.
     * Se viene fornita una query direttamente (es. /mercato cerca diamante), cerca subito.
     */
    private void handleCerca(Player player, String[] args, LanguageManager lang) {
        // Se c'è già una query come argomento, cerca direttamente
        if (args.length > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(args[i]);
            }
            String query = sb.toString().trim();
            AuctionGUI gui = new AuctionGUI(plugin, player, 0, SortType.RECENTE, query, "principale");
            gui.openMainMenu();
            return;
        }

        // Nessuna query fornita: apri un cartello per l'input
        String[] signLines = new String[]{
                "",
                "^^^^^^^^",
                lang.get("messaggi.cartello-cerca-riga1"),
                lang.get("messaggi.cartello-cerca-riga2")
        };

        SignInputUtil.openSignInput(plugin, player, signLines, (query) -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (query.isEmpty()) {
                    // Nessun input: apri la GUI normale
                    AuctionGUI gui = new AuctionGUI(plugin, player, 0, SortType.RECENTE, null, "principale");
                    gui.openMainMenu();
                } else {
                    AuctionGUI gui = new AuctionGUI(plugin, player, 0, SortType.RECENTE, query, "principale");
                    gui.openMainMenu();
                }
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Primo argomento: subcomandi
            completions.add("help");

            if (player.hasPermission("auctionprime.sell")) {
                completions.add("vendi");
            }

            completions.add("cerca");
            completions.add("inserzioni");
            completions.add("scaduti");
            completions.add("venduti");

            if (player.hasPermission("auctionprime.admin")) {
                completions.add("admin");
            }

            if (player.hasPermission("auctionprime.admin.reload")) {
                completions.add("reload");
            }

            String input = args[0].toLowerCase();
            return completions.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("vendi") && player.hasPermission("auctionprime.sell")) {
                // Suggerisci un placeholder prezzo
                completions.add("<prezzo>");
            }
            // cerca: nessun suggerimento specifico (testo libero)
        }

        return completions;
    }

    /**
     * Mostra l'help dei comandi disponibili in base ai permessi del giocatore.
     */
    private void sendHelp(Player player, LanguageManager lang) {
        player.sendMessage(lang.get("help.intestazione"));
        player.sendMessage(lang.get("help.mercato"));

        if (player.hasPermission("auctionprime.sell")) {
            player.sendMessage(lang.get("help.vendi"));
        }

        player.sendMessage(lang.get("help.cerca"));
        player.sendMessage(lang.get("help.inserzioni"));
        player.sendMessage(lang.get("help.scaduti"));
        player.sendMessage(lang.get("help.venduti"));

        if (player.hasPermission("auctionprime.admin")) {
            player.sendMessage(lang.get("help.admin"));
        }

        if (player.hasPermission("auctionprime.admin.reload")) {
            player.sendMessage(lang.get("help.reload"));
        }

        player.sendMessage(lang.get("help.piede"));
    }
}
