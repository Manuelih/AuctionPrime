package me.manudev.auctionprime.managers;

import me.manudev.auctionprime.AuctionPrime;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestisce i gruppi di mondi per il multi-mondo.
 * I mondi nello stesso gruppo condividono lo stesso mercato.
 */
public class WorldGroupManager {

    private final AuctionPrime plugin;
    private final Map<String, String> worldToGroup = new HashMap<>();
    private boolean enabled;

    public WorldGroupManager(AuctionPrime plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        worldToGroup.clear();
        this.enabled = plugin.getConfig().getBoolean("multi-mondo.abilitato", false);

        if (!enabled) return;

        ConfigurationSection groups = plugin.getConfig().getConfigurationSection("multi-mondo.gruppi");
        if (groups == null) return;

        for (String groupName : groups.getKeys(false)) {
            List<String> worlds = groups.getStringList(groupName);
            for (String world : worlds) {
                worldToGroup.put(world.toLowerCase(), groupName);
            }
        }
    }

    /**
     * Ottiene il gruppo di un mondo. Se il multi-mondo è disabilitato, restituisce "default".
     */
    public String getWorldGroup(String worldName) {
        if (!enabled) return "default";
        return worldToGroup.getOrDefault(worldName.toLowerCase(), "default");
    }

    /**
     * Verifica se un mondo è abilitato per il mercato.
     */
    public boolean isWorldEnabled(String worldName) {
        if (!enabled) return true;
        return worldToGroup.containsKey(worldName.toLowerCase());
    }

    public boolean isEnabled() {
        return enabled;
    }
}
