package br.com.cepp.ecommerce.inventory;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Stock held for one order. RESERVED -> COMMITTED (order confirmed) or RELEASED (order cancelled / payment failed). */
@Entity
@Table(name = "reservation")
public class Reservation {

    public enum Status { RESERVED, COMMITTED, RELEASED }

    @Id
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection
    @CollectionTable(name = "reservation_line", joinColumns = @JoinColumn(name = "order_id"))
    private List<ReservationLine> lines = new ArrayList<>();

    private Instant createdAt;

    protected Reservation() {
    }

    public static Reservation reserve(UUID orderId, List<ReservationLine> lines) {
        Reservation r = new Reservation();
        r.orderId = orderId;
        r.lines = new ArrayList<>(lines);
        r.status = Status.RESERVED;
        r.createdAt = Instant.now();
        return r;
    }

    /** @return true if it moved to COMMITTED; false if it was not RESERVED (duplicate or late event). */
    public boolean commit() {
        return transition(Status.COMMITTED);
    }

    /** @return true if it moved to RELEASED; false if it was not RESERVED (duplicate or late event). */
    public boolean release() {
        return transition(Status.RELEASED);
    }

    private boolean transition(Status target) {
        if (status != Status.RESERVED) {
            return false;
        }
        status = target;
        return true;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Status getStatus() {
        return status;
    }

    public List<ReservationLine> getLines() {
        return List.copyOf(lines);
    }
}