package me.manudev.auctionprime.discord;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.util.ChatUtil;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Gestisce l'integrazione con Discord tramite webhook.
 * Invia notifiche per nuove inserzioni e vendite completate.
 */
public class DiscordWebhook {

    private final AuctionPrime plugin;
    private final String webhookUrl;
    private final int embedColor;
    private final String botName;
    private final String avatarUrl;

    public DiscordWebhook(AuctionPrime plugin) {
        this.plugin = plugin;
        this.webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        this.embedColor = plugin.getConfig().getInt("discord.colore-embed", 65280);
        this.botName = plugin.getConfig().getString("discord.nome-bot", "AuctionPrime");
        this.avatarUrl = plugin.getConfig().getString("discord.avatar-url", "");
    }

    /**
     * Invia una notifica per una nuova inserzione.
     */
    public void sendNewListing(String seller, String item, double price) {
        String json = buildEmbed(
                "Nuova Inserzione",
                "**" + seller + "** ha messo in vendita **" + ChatUtil.stripColor(item)
                        + "** per **" + ChatUtil.formatPrice(price) + "**"
        );
        sendAsync(json);
    }

    /**
     * Invia una notifica per una vendita completata.
     */
    public void sendSale(String seller, String buyer, String item, double price) {
        String json = buildEmbed(
                "Vendita Completata",
                "**" + buyer + "** ha acquistato **" + ChatUtil.stripColor(item)
                        + "** da **" + seller + "** per **" + ChatUtil.formatPrice(price) + "**"
        );
        sendAsync(json);
    }

    /**
     * Costruisce il payload JSON per un embed Discord.
     */
    private String buildEmbed(String title, String description) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (!botName.isEmpty()) {
            sb.append("\"username\":\"").append(escapeJson(botName)).append("\",");
        }
        if (!avatarUrl.isEmpty()) {
            sb.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\",");
        }
        sb.append("\"embeds\":[{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(description)).append("\",");
        sb.append("\"color\":").append(embedColor);
        sb.append("}]}");
        return sb.toString();
    }

    /**
     * Invia il payload al webhook in modo asincrono.
     */
    private void sendAsync(String json) {
        if (webhookUrl.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != 200 && responseCode != 204) {
                    plugin.getLogger().warning("Discord webhook ha risposto con codice: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Errore invio webhook Discord: " + e.getMessage());
            }
        });
    }

    /**
     * Escape dei caratteri speciali JSON.
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
