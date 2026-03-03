package me.manudev.auctionprime.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Rappresenta un'inserzione nel mercato.
 */
public class AuctionListing {

    private final UUID id;
    private UUID sellerId;
    private String sellerName;
    private final ItemStack item;
    private double price;
    private final long creationTime;
    private final long expirationTime;
    private final String worldGroup;
    private final boolean serverListing;
    private final boolean unlimited;

    private boolean sold;
    private boolean cancelled;
    private boolean expired;
    private UUID buyerId;
    private String buyerName;
    private long soldTime;

    public AuctionListing(UUID id, UUID sellerId, String sellerName, ItemStack item,
                          double price, long creationTime, long expirationTime,
                          String worldGroup, boolean serverListing, boolean unlimited) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.creationTime = creationTime;
        this.expirationTime = expirationTime;
        this.worldGroup = worldGroup;
        this.serverListing = serverListing;
        this.unlimited = unlimited;
        this.sold = false;
        this.cancelled = false;
        this.expired = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public double getPrice() {
        return price;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public String getWorldGroup() {
        return worldGroup;
    }

    public boolean isServerListing() {
        return serverListing;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public boolean isSold() {
        return sold;
    }

    public void setSold(boolean sold) {
        this.sold = sold;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(UUID buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public long getSoldTime() {
        return soldTime;
    }

    public void setSoldTime(long soldTime) {
        this.soldTime = soldTime;
    }

    /**
     * Verifica se l'inserzione è ancora attiva (non venduta, cancellata o scaduta).
     */
    public boolean isActive() {
        if (sold || cancelled || expired) return false;
        if (!unlimited && System.currentTimeMillis() > expirationTime) return false;
        return true;
    }

    /**
     * Verifica se l'inserzione è scaduta per tempo.
     */
    public boolean isTimeExpired() {
        return !unlimited && System.currentTimeMillis() > expirationTime;
    }

    /**
     * Restituisce il tempo rimanente in millisecondi. 0 se scaduto.
     */
    public long getTimeRemaining() {
        if (unlimited) return Long.MAX_VALUE;
        long remaining = expirationTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setSellerId(UUID sellerId) {
        this.sellerId = sellerId;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }
}
