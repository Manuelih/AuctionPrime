package me.manudev.auctionprime.commands;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.gui.AuctionGUI;
import me.manudev.auctionprime.managers.LanguageManager;
import me.manudev.auctionprime.model.SortType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando /cerca <query> - Cerca oggetti nel mercato per nome, tipo, venditore, lore o incantesimi.
 */
public class CercaCommand implements CommandExecutor {

    private final AuctionPrime plugin;

    public CercaCommand(AuctionPrime plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.getWithPrefix("messaggi.solo-giocatori"));
            return true;
        }

        if (!player.hasPermission("auctionprime.use")) {
            player.sendMessage(lang.getWithPrefix("messaggi.nessun-permesso"));
            return true;
        }

        if (!plugin.getWorldGroupManager().isWorldEnabled(player.getWorld().getName())) {
            player.sendMessage(lang.getWithPrefix("messaggi.mondo-non-abilitato"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(lang.getWithPrefix("messaggi.uso-cerca"));
            return true;
        }

        // Unisci tutti gli argomenti come query di ricerca
        String query = String.join(" ", args);

        AuctionGUI gui = new AuctionGUI(plugin, player, 0, SortType.RECENTE, query, "principale");
        gui.openMainMenu();

        return true;
    }
}
