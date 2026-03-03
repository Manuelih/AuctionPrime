package me.manudev.auctionprime.managers;

import me.manudev.auctionprime.AuctionPrime;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Registra tutte le transazioni su un file di log separato.
 */
public class TransactionLogger {

    private final AuctionPrime plugin;
    private File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public TransactionLogger(AuctionPrime plugin) {
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("logging.abilitato", true)) {
            String fileName = plugin.getConfig().getString("logging.file", "transazioni.log");
            this.logFile = new File(plugin.getDataFolder(), fileName);
            if (!logFile.exists()) {
                try {
                    logFile.getParentFile().mkdirs();
                    logFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().warning("Impossibile creare il file di log: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Registra una nuova inserzione.
     */
    public void logListing(String seller, String item, double price) {
        log("[INSERZIONE] " + seller + " ha messo in vendita '" + item + "' per $" + String.format("%.2f", price));
    }

    /**
     * Registra una vendita completata.
     */
    public void logSale(String seller, String buyer, String item, double price, double tax) {
        log("[VENDITA] " + buyer + " ha comprato '" + item + "' da " + seller
                + " per $" + String.format("%.2f", price) + " (tassa: $" + String.format("%.2f", tax) + ")");
    }

    /**
     * Registra una cancellazione.
     */
    public void logCancellation(String seller, String item) {
        log("[CANCELLAZIONE] " + seller + " ha cancellato l'inserzione di '" + item + "'");
    }

    /**
     * Registra una scadenza.
     */
    public void logExpiration(String seller, String item) {
        log("[SCADENZA] L'inserzione di '" + item + "' di " + seller + " è scaduta");
    }

    /**
     * Scrive una riga nel file di log.
     */
    private void log(String message) {
        if (logFile == null) return;

        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("[" + dateFormat.format(new Date()) + "] " + message);
            bw.newLine();
        } catch (IOException e) {
            plugin.getLogger().warning("Errore nella scrittura del log: " + e.getMessage());
        }
    }
}
