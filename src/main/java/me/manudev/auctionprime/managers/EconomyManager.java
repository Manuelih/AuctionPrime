package me.manudev.auctionprime.managers;

import me.manudev.auctionprime.AuctionPrime;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Gestisce l'economia tramite Vault.
 * Calcola tasse, commissioni e verifica limiti di prezzo.
 */
public class EconomyManager {

    private final AuctionPrime plugin;
    private Economy economy;

    public EconomyManager(AuctionPrime plugin) {
        this.plugin = plugin;
    }

    /**
     * Configura il collegamento con Vault Economy.
     * @return true se Vault è stato trovato e configurato
     */
    public boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * Verifica se il provider economia è pronto.
     */
    public boolean isReady() {
        return economy != null;
    }

    /**
     * Verifica se il giocatore ha abbastanza soldi.
     */
    public boolean has(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    /**
     * Preleva soldi dal giocatore.
     */
    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Deposita soldi al giocatore.
     */
    public boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Deposita soldi a un giocatore offline tramite nome.
     */
    public boolean deposit(String playerName, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(playerName, amount).transactionSuccess();
    }

    /**
     * Calcola il costo totale dell'inserzione (fisso + percentuale).
     */
    public double calculateListingFee(double price) {
        double flat = plugin.getConfig().getDouble("economia.costo-fisso-inserzione", 0.0);
        double percent = plugin.getConfig().getDouble("economia.percentuale-costo-inserzione", 0.0);
        return flat + (price * percent / 100.0);
    }

    /**
     * Calcola la tassa sulla vendita.
     */
    public double calculateSaleTax(double price) {
        double taxPercent = plugin.getConfig().getDouble("economia.tassa-vendita", 5.0);
        double maxTax = plugin.getConfig().getDouble("economia.tassa-massima", 0.0);
        double tax = price * taxPercent / 100.0;
        if (maxTax > 0 && tax > maxTax) {
            tax = maxTax;
        }
        return tax;
    }

    /**
     * Restituisce il prezzo minimo configurato.
     */
    public double getMinPrice() {
        return plugin.getConfig().getDouble("economia.prezzo-minimo", 1.0);
    }

    /**
     * Restituisce il prezzo massimo configurato (0 = nessun limite).
     */
    public double getMaxPrice() {
        return plugin.getConfig().getDouble("economia.prezzo-massimo", 0.0);
    }

    /**
     * Verifica se il prezzo è nei limiti consentiti.
     */
    public boolean isPriceValid(double price) {
        if (price < getMinPrice()) return false;
        double max = getMaxPrice();
        if (max > 0 && price > max) return false;
        return true;
    }

    public Economy getEconomy() {
        return economy;
    }
}
