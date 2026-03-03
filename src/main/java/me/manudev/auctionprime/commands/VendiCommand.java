package me.manudev.auctionprime.commands;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.managers.LanguageManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Comando /vendi <prezzo> - Vende l'oggetto in mano nel mercato.
 */
public class VendiCommand implements CommandExecutor {

    private final AuctionPrime plugin;

    public VendiCommand(AuctionPrime plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LanguageManager lang = plugin.getLanguageManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.getWithPrefix("messaggi.solo-giocatori"));
            return true;
        }

        if (!player.hasPermission("auctionprime.sell")) {
            player.sendMessage(lang.getWithPrefix("messaggi.nessun-permesso"));
            return true;
        }

        // Verifica mondo
        if (!plugin.getWorldGroupManager().isWorldEnabled(player.getWorld().getName())) {
            player.sendMessage(lang.getWithPrefix("messaggi.mondo-non-abilitato"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(lang.getWithPrefix("messaggi.uso-vendi"));
            return true;
        }

        // Parse prezzo
        double price;
        try {
            price = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(lang.getWithPrefix("messaggi.prezzo-non-valido"));
            return true;
        }

        if (price <= 0) {
            player.sendMessage(lang.getWithPrefix("messaggi.prezzo-non-valido"));
            return true;
        }

        // Verifica oggetto in mano
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(lang.getWithPrefix("messaggi.oggetto-non-in-mano"));
            return true;
        }

        // Crea l'inserzione
        plugin.getAuctionManager().createListing(player, itemInHand, price);

        return true;
    }
}
