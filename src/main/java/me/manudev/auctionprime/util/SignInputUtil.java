package me.manudev.auctionprime.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Utility per aprire un cartello come input di testo.
 * Posiziona un cartello temporaneo, lo apre per la modifica,
 * cattura l'input tramite SignChangeEvent e ripristina il blocco originale.
 */
public class SignInputUtil {

    private static final Map<UUID, Consumer<String>> pendingCallbacks = new HashMap<>();
    private static final Map<UUID, BlockState> originalStates = new HashMap<>();

    /**
     * Apre un cartello di input per il giocatore.
     * @param plugin il plugin
     * @param player il giocatore
     * @param promptLines le linee iniziali del cartello (max 4)
     * @param callback callback con il testo inserito
     */
    public static void openSignInput(Plugin plugin, Player player, String[] promptLines, Consumer<String> callback) {
        // Posizione del cartello: 1 blocco sotto i piedi del giocatore
        Location loc = player.getLocation().clone();
        loc.setY(Math.max(loc.getBlockY() - 1, player.getWorld().getMinHeight() + 1));
        Block block = loc.getBlock();

        // Salva lo stato originale del blocco
        originalStates.put(player.getUniqueId(), block.getState());

        // Piazza un cartello temporaneo
        block.setType(Material.OAK_SIGN);
        BlockState state = block.getState();

        if (state instanceof Sign sign) {
            // Imposta le righe del prompt
            for (int i = 0; i < Math.min(promptLines.length, 4); i++) {
                sign.getSide(Side.FRONT).setLine(i, promptLines[i] != null ? promptLines[i] : "");
            }
            sign.update();

            // Registra il callback
            pendingCallbacks.put(player.getUniqueId(), callback);

            // Apri l'editor del cartello per il giocatore
            player.openSign(sign, Side.FRONT);
        } else {
            // Se non riesce a piazzare il cartello, ripristina e annulla
            restoreBlock(player.getUniqueId());
        }
    }

    /**
     * Controlla se il giocatore ha un input cartello pendente.
     */
    public static boolean hasPending(UUID playerId) {
        return pendingCallbacks.containsKey(playerId);
    }

    /**
     * Gestisce il completamento dell'input del cartello (chiamato da SignChangeEvent).
     * @return true se è stato gestito
     */
    public static boolean handleSignComplete(Player player, String[] lines) {
        UUID uuid = player.getUniqueId();
        Consumer<String> callback = pendingCallbacks.remove(uuid);

        if (callback == null) return false;

        // Ripristina il blocco originale
        restoreBlock(uuid);

        // Combina le righe non vuote come query
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(trimmed);
            }
        }

        callback.accept(sb.toString().trim());
        return true;
    }

    /**
     * Ripristina il blocco originale nella posizione del cartello temporaneo.
     */
    private static void restoreBlock(UUID playerId) {
        BlockState original = originalStates.remove(playerId);
        if (original != null) {
            original.update(true, false);
        }
    }

    /**
     * Rimuove un input pendente e ripristina il blocco (es. quando il giocatore si disconnette).
     */
    public static void removePending(UUID playerId) {
        pendingCallbacks.remove(playerId);
        restoreBlock(playerId);
    }
}
