package me.manudev.auctionprime.model;

/**
 * Tipi di ordinamento per le inserzioni nel mercato.
 */
public enum SortType {

    RECENTE,
    VECCHIO,
    PREZZO_CRESCENTE,
    PREZZO_DECRESCENTE;

    /**
     * Passa al prossimo tipo di ordinamento ciclicamente.
     */
    public SortType next() {
        SortType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /**
     * Restituisce la chiave per il file lingua.
     */
    public String getLangKey() {
        return switch (this) {
            case RECENTE -> "ordinamenti.recente";
            case VECCHIO -> "ordinamenti.vecchio";
            case PREZZO_CRESCENTE -> "ordinamenti.prezzo-crescente";
            case PREZZO_DECRESCENTE -> "ordinamenti.prezzo-decrescente";
        };
    }
}
