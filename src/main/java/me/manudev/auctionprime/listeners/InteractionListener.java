package me.manudev.auctionprime.listeners;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.gui.AuctionGUI;
import me.manudev.auctionprime.model.SortType;
import me.manudev.auctionprime.util.ChatUtil;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.List;

/**
 * Listener per l'interazione con cartelli, entità nominate e blocchi nominati
 * che aprono il mercato.
 */
public class InteractionListener implements Listener {

    private final AuctionPrime plugin;

    public InteractionListener(AuctionPrime plugin) {
        this.plugin = plugin;
    }

    /**
     * Interazione con blocchi (cartelli e blocchi nominati).
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("auctionprime.use")) return;

        BlockState state = event.getClickedBlock().getState();

        // Controlla cartelli
        if (state instanceof Sign sign) {
            if (plugin.getConfig().getBoolean("interazione.cartelli.abilitati", true)) {
                String targetText = ChatUtil.stripColor(
                        ChatUtil.colorize(plugin.getConfig().getString("interazione.cartelli.testo", "[Mercato]")));

                // Controlla entrambi i lati del cartello
                for (Side side : Side.values()) {
                    String[] lines = sign.getSide(side).getLines();
                    for (String line : lines) {
                        if (ChatUtil.stripColor(line).contains(targetText)) {
                            event.setCancelled(true);
                            openMarket(player);
                            return;
                        }
                    }
                }
            }
        }

        // Controlla blocchi nominati (casse, shulker, ecc.)
        if (plugin.getConfig().getBoolean("interazione.blocchi.abilitati", true)) {
            // Verifica se il blocco ha un custom name tramite BlockStateMeta o Nameable
            if (state instanceof org.bukkit.Nameable nameable) {
                String customName = nameable.getCustomName();
                if (customName != null) {
                    String stripped = ChatUtil.stripColor(customName);
                    List<String> targetNames = plugin.getConfig().getStringList("interazione.blocchi.nomi");
                    for (String name : targetNames) {
                        if (stripped.equalsIgnoreCase(ChatUtil.stripColor(ChatUtil.colorize(name)))) {
                            event.setCancelled(true);
                            openMarket(player);
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Interazione con entità nominate (NPC, armature, ecc.).
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.getConfig().getBoolean("interazione.entita.abilitate", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("auctionprime.use")) return;

        Entity entity = event.getRightClicked();
        String customName = entity.getCustomName();
        if (customName == null) return;

        String stripped = ChatUtil.stripColor(customName);
        List<String> targetNames = plugin.getConfig().getStringList("interazione.entita.nomi");

        for (String name : targetNames) {
            if (stripped.equalsIgnoreCase(ChatUtil.stripColor(ChatUtil.colorize(name)))) {
                event.setCancelled(true);
                openMarket(player);
                return;
            }
        }
    }

    /**
     * Apre il mercato per il giocatore.
     */
    private void openMarket(Player player) {
        if (!plugin.getWorldGroupManager().isWorldEnabled(player.getWorld().getName())) {
            player.sendMessage(plugin.getLanguageManager().getWithPrefix("messaggi.mondo-non-abilitato"));
            return;
        }

        AuctionGUI gui = new AuctionGUI(plugin, player, 0, SortType.RECENTE, null, "principale");
        gui.openMainMenu();
    }
}
