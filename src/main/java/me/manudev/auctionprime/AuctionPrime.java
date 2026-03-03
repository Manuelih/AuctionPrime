package me.manudev.auctionprime;

import me.manudev.auctionprime.commands.MercatoCommand;
import me.manudev.auctionprime.discord.DiscordWebhook;
import me.manudev.auctionprime.gui.GUIListener;
import me.manudev.auctionprime.listeners.InteractionListener;
import me.manudev.auctionprime.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classe principale del plugin AuctionPrime.
 * Gestisce l'inizializzazione e lo spegnimento di tutti i moduli.
 */
public final class AuctionPrime extends JavaPlugin {

    private static AuctionPrime instance;

    private LanguageManager languageManager;
    private StorageManager storageManager;
    private AuctionManager auctionManager;
    private EconomyManager economyManager;
    private ItemRestrictionManager restrictionManager;
    private CooldownManager cooldownManager;
    private WorldGroupManager worldGroupManager;
    private TransactionLogger transactionLogger;
    private NotificationManager notificationManager;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        instance = this;

        // Salva config di default
        saveDefaultConfig();
        saveResource("lang/it_IT.yml", false);
        saveResource("lang/en_US.yml", false);

        // Inizializza managers
        this.languageManager = new LanguageManager(this);
        this.economyManager = new EconomyManager(this);
        this.restrictionManager = new ItemRestrictionManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.worldGroupManager = new WorldGroupManager(this);
        this.transactionLogger = new TransactionLogger(this);
        this.notificationManager = new NotificationManager(this);
        this.storageManager = new StorageManager(this);
        this.auctionManager = new AuctionManager(this);

        // Discord
        if (getConfig().getBoolean("discord.abilitato", false)) {
            this.discordWebhook = new DiscordWebhook(this);
        }

        // Registra comando principale (tutto sotto /mercato)
        MercatoCommand mercatoCmd = new MercatoCommand(this);
        getCommand("mercato").setExecutor(mercatoCmd);
        getCommand("mercato").setTabCompleter(mercatoCmd);

        // Registra listener
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);

        // Verifica Vault con un tick di ritardo per dare tempo ai provider di registrarsi
        getServer().getScheduler().runTaskLater(this, () -> {
            if (!economyManager.setupEconomy()) {
                getLogger().severe("Nessun provider economia trovato! Assicurati di avere un plugin economia (es. EssentialsX, CMI) installato con Vault.");
                getLogger().severe("AuctionPrime funzionerà ma le transazioni economiche non saranno disponibili.");
            } else {
                getLogger().info("Provider economia collegato con successo: " + economyManager.getEconomy().getName());
            }
        }, 1L);

        // Task pulizia automatica (ogni ora)
        getServer().getScheduler().runTaskTimer(this, () -> {
            auctionManager.cleanupExpired();
        }, 20L * 60 * 60, 20L * 60 * 60);

        getLogger().info("AuctionPrime abilitato con successo!");
    }

    @Override
    public void onDisable() {
        if (storageManager != null) {
            storageManager.saveAll();
        }
        getLogger().info("AuctionPrime disabilitato.");
    }

    /**
     * Ricarica tutta la configurazione del plugin.
     */
    public void ricarica() {
        reloadConfig();
        languageManager.reload();
        restrictionManager.reload();
        cooldownManager.reload();
        worldGroupManager.reload();
        if (getConfig().getBoolean("discord.abilitato", false)) {
            this.discordWebhook = new DiscordWebhook(this);
        } else {
            this.discordWebhook = null;
        }
    }

    public static AuctionPrime getInstance() {
        return instance;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ItemRestrictionManager getRestrictionManager() {
        return restrictionManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public WorldGroupManager getWorldGroupManager() {
        return worldGroupManager;
    }

    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }

    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
}
