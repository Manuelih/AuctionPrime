package me.manudev.auctionprime.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

/**
 * Utility per la gestione delle teste personalizzate.
 * Supporta: hdb:<id>, texture:<base64>, player_head, Material standard.
 */
public final class HeadUtil {

    private HeadUtil() {}

    /**
     * Crea un ItemStack in base al formato specificato nel config.
     * Formati supportati:
     * - "hdb:<id>" -> HeadDatabase (richiede plugin esterno)
     * - "texture:<base64>" -> Texture personalizzata
     * - "player_head" -> Testa del giocatore
     * - Qualsiasi Material valido
     */
    public static ItemStack parseItem(String input) {
        if (input == null || input.isEmpty()) {
            return new ItemStack(Material.STONE);
        }

        if (input.startsWith("hdb:")) {
            // HeadDatabase integration - restituisce testa di default se plugin non presente
            return new ItemStack(Material.PLAYER_HEAD);
        }

        if (input.startsWith("texture:")) {
            String base64 = input.substring(8);
            return createSkullFromBase64(base64);
        }

        if (input.equalsIgnoreCase("player_head")) {
            return new ItemStack(Material.PLAYER_HEAD);
        }

        try {
            Material mat = Material.valueOf(input.toUpperCase());
            return new ItemStack(mat);
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.STONE);
        }
    }

    /**
     * Crea una testa con una texture base64.
     */
    public static ItemStack createSkullFromBase64(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();

            // Decodifica base64 per estrarre l'URL della skin
            String decoded = new String(Base64.getDecoder().decode(base64));
            // Formato: {"textures":{"SKIN":{"url":"http://textures.minecraft.net/..."}}}
            int urlStart = decoded.indexOf("\"url\":\"") + 7;
            int urlEnd = decoded.indexOf("\"", urlStart);
            String urlStr = decoded.substring(urlStart, urlEnd);

            URL url = URI.create(urlStr).toURL();
            textures.setSkin(url);
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            skull.setItemMeta(meta);
        } catch (Exception e) {
            // Fallback: restituisce testa senza texture
        }

        return skull;
    }

    /**
     * Crea una testa del giocatore specifico.
     */
    public static ItemStack createPlayerHead(UUID playerId) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerId));
        skull.setItemMeta(meta);
        return skull;
    }
}
