# AuctionPrime

Sistema completo di casa d'aste per Minecraft 1.21.x+ sviluppato da **ManuDev**.

## Requisiti

- **Minecraft Server**: Spigot/Paper 1.21.x+
- **Java**: 21+
- **Vault**: Plugin economia (obbligatorio)
- **HeadDatabase**: (opzionale) per teste personalizzate

## Comandi

| Comando | Descrizione |
|---------|-------------|
| `/mercato` | Apre la GUI principale del mercato |
| `/mercato vendi <prezzo>` | Vendi l'oggetto in mano |
| `/mercato cerca` | Apre un cartello per cercare oggetti |
| `/mercato cerca <query>` | Cerca oggetti direttamente |
| `/mercato admin` | Pannello admin per gestire tutte le aste |
| `/mercato inserzioni` | Visualizza le tue inserzioni attive |
| `/mercato scaduti` | Ritira oggetti scaduti/cancellati |
| `/mercato venduti` | Visualizza oggetti venduti di recente |
| `/mercato reload` | Ricarica la configurazione |

**Alias**: `/asta`, `/negozio`

## Permessi

| Permesso | Descrizione | Default |
|----------|-------------|---------|
| `auctionprime.use` | Accesso base al mercato | `true` |
| `auctionprime.sell` | Vendere oggetti | `true` |
| `auctionprime.sell.unlimited` | Inserzioni illimitate | `op` |
| `auctionprime.admin` | Permessi admin | `op` |
| `auctionprime.admin.reload` | Ricarica config | `op` |
| `auctionprime.bypass.cooldown` | Ignora cooldown | `op` |
| `auctionprime.bypass.fees` | Ignora tasse | `op` |
| `auctionprime.bypass.restrictions` | Ignora restrizioni oggetti | `op` |
| `auctionprime.notify` | Ricevi notifiche inserzioni | `true` |

## Funzionalità

- **GUI Paginata**: `/mercato` apre una GUI con tutti gli oggetti, prezzo e venditore
- **Acquisto/Cancellazione**: clicca per comprare; il venditore clicca sul proprio oggetto per cancellarlo
- **Ricerca tramite Cartello**: clicca "Cerca" nella GUI o `/mercato cerca` per aprire un cartello e scrivere la query
- **Ordinamento**: per data o prezzo (crescente/decrescente)
- **Economia**: tasse di inserzione (fisso + %), tassa sulla vendita con cap, limiti prezzo
- **Restrizioni**: blocca oggetti per tipo, nome (regex), lore, incantesimi, custom_model_data, maledizioni, soulbound
- **Interazione**: apri il mercato tramite cartelli, entità nominate o blocchi nominati
- **Multi-Mondo**: raggruppa mondi che condividono lo stesso mercato
- **Cooldown**: tempo di attesa tra inserzioni
- **Discord**: webhook per notifiche su nuove inserzioni e vendite
- **Logging**: tutte le transazioni registrate su file
- **i18n**: Italiano (default) e Inglese, facilmente estensibile
- **Teste Custom**: supporto `hdb:<id>`, `texture:<base64>`, `player_head`
- **Pulizia Automatica**: rimuove oggetti abbandonati dopo N giorni

## Compilazione

```bash
mvn clean package
```

Il JAR compilato si troverà in `target/AuctionPrime-1.0.0.jar`.

## Struttura Progetto

```
src/main/java/me/manudev/auctionprime/
├── AuctionPrime.java              # Classe principale
├── commands/
│   └── MercatoCommand.java        # /mercato (vendi, cerca, inserzioni, ecc.)
├── discord/
│   └── DiscordWebhook.java        # Integrazione Discord
├── gui/
│   ├── AuctionGUI.java            # GUI paginata principale
│   └── GUIListener.java           # Gestione click GUI
├── listeners/
│   └── InteractionListener.java   # Cartelli, entità, blocchi
├── managers/
│   ├── AuctionManager.java        # Logica aste
│   ├── CooldownManager.java       # Cooldown inserzioni
│   ├── EconomyManager.java        # Integrazione Vault
│   ├── ItemRestrictionManager.java# Restrizioni oggetti
│   ├── LanguageManager.java       # Sistema lingue
│   ├── NotificationManager.java   # Annunci globali
│   ├── StorageManager.java        # Persistenza YAML
│   ├── TransactionLogger.java     # Log transazioni
│   └── WorldGroupManager.java     # Multi-mondo
├── model/
│   ├── AuctionListing.java        # Modello inserzione
│   └── SortType.java              # Enum ordinamento
└── util/
    ├── ChatUtil.java              # Colori e formattazione
    ├── HeadUtil.java              # Teste personalizzate
    └── SignInputUtil.java         # Input ricerca tramite cartello
```

## Configurazione

Tutti i file di configurazione vengono generati automaticamente al primo avvio:
- `config.yml` - Impostazioni generali
- `lang/it_IT.yml` - Lingua italiana
- `lang/en_US.yml` - Lingua inglese
- `listings.yml` - Dati inserzioni (auto-generato)
- `transazioni.log` - Log transazioni (auto-generato)
