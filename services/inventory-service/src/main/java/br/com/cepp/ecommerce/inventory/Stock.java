package br.com.cepp.ecommerce.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/** Stock of one product: available units can be reserved; reserved units are held for pending orders. */
@Entity
@Table(name = "stock")
public class Stock {

    @Id
    private UUID productId;
    private int available;
    private int reserved;

    @Version
    private long version;

    protected Stock() {
    }

    public Stock(UUID productId, int available) {
        if (available < 0) {
            throw new IllegalArgumentException("available must be >= 0");
        }
        this.productId = productId;
        this.available = available;
    }

    public boolean canReserve(int quantity) {
        return quantity > 0 && available >= quantity;
    }

    public void reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new IllegalStateException("insufficient stock for " + productId);
        }
        available -= quantity;
        reserved += quantity;
    }

    public void release(int quantity) {
        reserved -= quantity;
        available += quantity;
    }

    public void commit(int quantity) {
        reserved -= quantity;
    }

    public void setAvailable(int available) {
        if (available < 0) {
            throw new IllegalArgumentException("available must be >= 0");
        }
        this.available = available;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getAvailable() {
        return available;
    }

    public int getReserved() {
        return reserved;
    }
}