package me.manudev.auctionprime.managers;

import me.manudev.auctionprime.AuctionPrime;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Gestisce le restrizioni sugli oggetti vendibili.
 * Supporta restrizioni per tipo, nome (anche regex), lore, incantesimi,
 * custom_model_data, oggetti maledetti/soulbound, danneggiati e indistruttibili.
 */
public class ItemRestrictionManager {

    private final AuctionPrime plugin;
    private List<String> bannedTypes;
    private List<String> bannedNames;
    private List<String> bannedLore;
    private List<String> bannedEnchants;
    private List<Integer> bannedCustomModelData;
    private boolean blockCurses;
    private boolean blockSoulbound;
    private boolean blockDamaged;
    private boolean blockUnbreakable;

    public ItemRestrictionManager(AuctionPrime plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Ricarica le restrizioni dalla configurazione.
     */
    public void reload() {
        this.bannedTypes = plugin.getConfig().getStringList("restrizioni.tipi-vietati");
        this.bannedNames = plugin.getConfig().getStringList("restrizioni.nomi-vietati");
        this.bannedLore = plugin.getConfig().getStringList("restrizioni.lore-vietate");
        this.bannedEnchants = plugin.getConfig().getStringList("restrizioni.incantesimi-vietati");
        this.bannedCustomModelData = plugin.getConfig().getIntegerList("restrizioni.custom-model-data-vietati");
        this.blockCurses = plugin.getConfig().getBoolean("restrizioni.blocca-maledizioni", true);
        this.blockSoulbound = plugin.getConfig().getBoolean("restrizioni.blocca-soulbound", true);
        this.blockDamaged = plugin.getConfig().getBoolean("restrizioni.blocca-danneggiati", false);
        this.blockUnbreakable = plugin.getConfig().getBoolean("restrizioni.blocca-indistruttibili", false);
    }

    /**
     * Verifica se un oggetto è vendibile. Restituisce true se è vietato.
     */
    public boolean isRestricted(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return true;

        // Tipo vietato
        if (isTypeBanned(item.getType())) return true;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        // Nome vietato
        if (meta.hasDisplayName() && isNameBanned(meta.getDisplayName())) return true;

        // Lore vietata
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                if (isLoreBanned(line)) return true;
            }
        }

        // Maledizioni
        if (blockCurses && hasCurses(item)) return true;

        // Soulbound (controlla lore)
        if (blockSoulbound && isSoulbound(meta)) return true;

        // Incantesimi vietati
        if (isEnchantBanned(item)) return true;

        // Custom model data vietato
        if (meta.hasCustomModelData() && bannedCustomModelData.contains(meta.getCustomModelData())) return true;

        // Danneggiato
        if (blockDamaged && meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            if (damageable.hasDamage() && damageable.getDamage() > 0) return true;
        }

        // Indistruttibile
        if (blockUnbreakable && meta.isUnbreakable()) return true;

        return false;
    }

    private boolean isTypeBanned(Material type) {
        for (String banned : bannedTypes) {
            if (banned.equalsIgnoreCase(type.name())) return true;
        }
        return false;
    }

    private boolean isNameBanned(String name) {
        for (String banned : bannedNames) {
            if (matchesPattern(name, banned)) return true;
        }
        return false;
    }

    private boolean isLoreBanned(String loreLine) {
        for (String banned : bannedLore) {
            if (matchesPattern(loreLine, banned)) return true;
        }
        return false;
    }

    /**
     * Verifica un pattern. Supporta il formato regex:<expr> per espressioni regolari.
     */
    private boolean matchesPattern(String text, String pattern) {
        if (pattern.startsWith("regex:")) {
            String regex = pattern.substring(6);
            try {
                return Pattern.matches(regex, text);
            } catch (Exception e) {
                plugin.getLogger().warning("Regex non valida: " + regex);
                return false;
            }
        }
        return text.toLowerCase().contains(pattern.toLowerCase());
    }

    private boolean hasCurses(ItemStack item) {
        for (Enchantment ench : item.getEnchantments().keySet()) {
            if (ench.isCursed()) return true;
        }
        return false;
    }

    private boolean isSoulbound(ItemMeta meta) {
        if (!meta.hasLore()) return false;
        for (String line : meta.getLore()) {
            String stripped = line.toLowerCase().replaceAll("§[0-9a-fk-or]", "");
            if (stripped.contains("soulbound")) return true;
        }
        return false;
    }

    private boolean isEnchantBanned(ItemStack item) {
        for (String entry : bannedEnchants) {
            String[] parts = entry.split(":");
            if (parts.length < 1) continue;
            String enchName = parts[0].toUpperCase();
            int minLevel = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            for (var enchEntry : item.getEnchantments().entrySet()) {
                if (enchEntry.getKey().getKey().getKey().equalsIgnoreCase(enchName)) {
                    if (minLevel == 0 || enchEntry.getValue() >= minLevel) return true;
                }
            }
        }
        return false;
    }
}
