package br.com.cepp.ecommerce.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockTest {

    @Test
    void reserveMovesUnitsFromAvailableToReserved() {
        Stock stock = new Stock(UUID.randomUUID(), 10);

        stock.reserve(4);

        assertEquals(6, stock.getAvailable());
        assertEquals(4, stock.getReserved());
    }

    @Test
    void cannotReserveMoreThanAvailable() {
        Stock stock = new Stock(UUID.randomUUID(), 3);

        assertFalse(stock.canReserve(4));
        assertThrows(IllegalStateException.class, () -> stock.reserve(4));
    }

    @Test
    void releaseReturnsUnitsAndCommitConsumesThem() {
        Stock stock = new Stock(UUID.randomUUID(), 10);
        stock.reserve(5);

        stock.release(2);
        stock.commit(3);

        assertEquals(7, stock.getAvailable());
        assertEquals(0, stock.getReserved());
    }

    @Test
    void reservationTransitionsOnlyOnceFromReserved() {
        Reservation reservation = Reservation.reserve(UUID.randomUUID(), List.of(new ReservationLine(UUID.randomUUID(), 1)));

        assertTrue(reservation.release());
        assertFalse(reservation.release());
        assertFalse(reservation.commit());
        assertEquals(Reservation.Status.RELEASED, reservation.getStatus());
    }
}