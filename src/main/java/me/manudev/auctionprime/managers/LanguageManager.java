package me.manudev.auctionprime.managers;

import me.manudev.auctionprime.AuctionPrime;
import me.manudev.auctionprime.util.ChatUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestisce il sistema di localizzazione/internazionalizzazione.
 * Supporta più lingue con fallback alla lingua predefinita.
 */
public class LanguageManager {

    private final AuctionPrime plugin;
    private FileConfiguration langConfig;
    private String currentLang;

    public LanguageManager(AuctionPrime plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Ricarica il file di lingua corrente.
     */
    public void reload() {
        this.currentLang = plugin.getConfig().getString("lingua", "it_IT");
        File langFile = new File(plugin.getDataFolder(), "lang/" + currentLang + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("File lingua '" + currentLang + "' non trovato, uso it_IT.");
            this.currentLang = "it_IT";
            langFile = new File(plugin.getDataFolder(), "lang/it_IT.yml");
        }

        this.langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    /**
     * Ottiene un messaggio dalla lingua corrente con colori tradotti.
     */
    public String get(String path) {
        String msg = langConfig.getString(path, "§c[Messaggio mancante: " + path + "]");
        return ChatUtil.colorize(msg);
    }

    /**
     * Ottiene un messaggio con il prefisso.
     */
    public String getWithPrefix(String path) {
        return get("prefisso") + get(path);
    }

    /**
     * Ottiene un messaggio e sostituisce i placeholder.
     */
    public String get(String path, String... replacements) {
        String msg = get(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    /**
     * Ottiene un messaggio con prefisso e placeholder.
     */
    public String getWithPrefix(String path, String... replacements) {
        return get("prefisso") + get(path, replacements);
    }

    /**
     * Ottiene una lista di stringhe dalla lingua corrente.
     */
    public List<String> getList(String path) {
        return langConfig.getStringList(path).stream()
                .map(ChatUtil::colorize)
                .collect(Collectors.toList());
    }

    /**
     * Ottiene una lista di stringhe con placeholder sostituiti.
     */
    public List<String> getList(String path, String... replacements) {
        return langConfig.getStringList(path).stream()
                .map(line -> {
                    String colored = ChatUtil.colorize(line);
                    for (int i = 0; i < replacements.length - 1; i += 2) {
                        colored = colored.replace(replacements[i], replacements[i + 1]);
                    }
                    return colored;
                })
                .collect(Collectors.toList());
    }

    public String getCurrentLang() {
        return currentLang;
    }
}
