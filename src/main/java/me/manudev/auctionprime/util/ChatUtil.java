package me.manudev.auctionprime.util;

import org.bukkit.ChatColor;

/**
 * Utility per la gestione dei colori e formattazione chat.
 */
public final class ChatUtil {

    private ChatUtil() {}

    /**
     * Traduce i codici colore con '&' in codici Minecraft.
     */
    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Formatta un prezzo con due decimali e simbolo valuta.
     */
    public static String formatPrice(double price) {
        return String.format("$%.2f", price);
    }

    /**
     * Formatta il tempo rimanente in formato leggibile.
     */
    public static String formatTime(long millis, me.manudev.auctionprime.managers.LanguageManager lang) {
        if (millis <= 0) return lang.get("tempo.scaduto");

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(lang.get("tempo.giorni").replace("{n}", String.valueOf(days))).append(" ");
        if (hours % 24 > 0) sb.append(lang.get("tempo.ore").replace("{n}", String.valueOf(hours % 24))).append(" ");
        if (minutes % 60 > 0 && days == 0) sb.append(lang.get("tempo.minuti").replace("{n}", String.valueOf(minutes % 60))).append(" ");
        if (seconds % 60 > 0 && hours == 0 && days == 0) sb.append(lang.get("tempo.secondi").replace("{n}", String.valueOf(seconds % 60)));

        return sb.toString().trim();
    }

    /**
     * Rimuove i codici colore da una stringa.
     */
    public static String stripColor(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(colorize(message));
    }
}
