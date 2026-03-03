package me.manudev.auctionprime.managers;

import me.manudev.auctionprime.AuctionPrime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestisce i cooldown tra le inserzioni dei giocatori.
 */
public class CooldownManager {

    private final AuctionPrime plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private int cooldownSeconds;

    public CooldownManager(AuctionPrime plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.cooldownSeconds = plugin.getConfig().getInt("generale.cooldown-inserzioni", 5);
    }

    /**
     * Verifica se il giocatore è ancora in cooldown.
     * @return secondi rimanenti, 0 se può procedere
     */
    public int getRemainingCooldown(UUID playerId) {
        if (!cooldowns.containsKey(playerId)) return 0;
        long elapsed = System.currentTimeMillis() - cooldowns.get(playerId);
        int remaining = cooldownSeconds - (int)(elapsed / 1000);
        return Math.max(0, remaining);
    }

    /**
     * Verifica se il giocatore può inserire un oggetto.
     */
    public boolean canList(UUID playerId) {
        return getRemainingCooldown(playerId) <= 0;
    }

    /**
     * Registra il timestamp dell'ultima inserzione del giocatore.
     */
    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    /**
     * Rimuove il cooldown di un giocatore.
     */
    public void removeCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
