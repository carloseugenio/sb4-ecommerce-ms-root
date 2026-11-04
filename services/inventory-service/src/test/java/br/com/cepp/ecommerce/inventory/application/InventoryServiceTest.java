package br.com.cepp.ecommerce.inventory.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.cepp.ecommerce.inventory.Reservation;
import br.com.cepp.ecommerce.inventory.ReservationLine;
import br.com.cepp.ecommerce.inventory.ReservationRepository;
import br.com.cepp.ecommerce.inventory.Stock;
import br.com.cepp.ecommerce.inventory.StockRepository;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderCancelled;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderLine;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderPlaced;
import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import br.com.cepp.ecommerce.platform.domain.event.Topics;
import br.com.cepp.ecommerce.platform.messaging.OutboxRecorder;
import br.com.cepp.ecommerce.platform.messaging.ProcessedMessages;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {

    private final StockRepository stocks = mock(StockRepository.class);
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final ProcessedMessages processed = mock(ProcessedMessages.class);
    private final OutboxRecorder outbox = mock(OutboxRecorder.class);
    private final InventoryService service = new InventoryService(stocks, reservations, processed, outbox);

    private final UUID orderId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    private EventEnvelope<OrderPlaced> placed(int quantity) {
        return EventEnvelope.of("OrderPlaced", orderId.toString(), "corr", null,
                new OrderPlaced(orderId, UUID.randomUUID(), List.of(new OrderLine(productId, quantity)),
                        new BigDecimal("20.00"), "BRL"));
    }

    @Test
    void reservesStockAndPublishesStockReserved() {
        Stock stock = new Stock(productId, 5);
        when(processed.firstTime(any(), any())).thenReturn(true);
        when(stocks.lockAllById(anyCollection())).thenReturn(List.of(stock));

        service.onOrderPlaced(placed(2));

        assertEquals(3, stock.getAvailable());
        assertEquals(2, stock.getReserved());
        verify(reservations).save(any(Reservation.class));
        verify(outbox).record(eq(Topics.STOCK_RESERVED), any());
    }

    @Test
    void rejectsOrderWhenStockIsInsufficient() {
        Stock stock = new Stock(productId, 1);
        when(processed.firstTime(any(), any())).thenReturn(true);
        when(stocks.lockAllById(anyCollection())).thenReturn(List.of(stock));

        service.onOrderPlaced(placed(2));

        assertEquals(1, stock.getAvailable());
        verify(reservations, never()).save(any());
        verify(outbox).record(eq(Topics.STOCK_REJECTED), any());
    }

    @Test
    void duplicateEventIsIgnored() {
        when(processed.firstTime(any(), any())).thenReturn(false);

        service.onOrderPlaced(placed(1));

        verify(stocks, never()).lockAllById(anyCollection());
        verify(outbox, never()).record(any(), any());
    }

    @Test
    void cancelledOrderReleasesReservationOnce() {
        Stock stock = new Stock(productId, 5);
        stock.reserve(2);
        Reservation reservation = Reservation.reserve(orderId, List.of(new ReservationLine(productId, 2)));
        when(processed.firstTime(any(), any())).thenReturn(true);
        when(reservations.findById(orderId)).thenReturn(Optional.of(reservation));
        when(stocks.lockAllById(anyCollection())).thenReturn(List.of(stock));
        var cancelled = EventEnvelope.of("OrderCancelled", orderId.toString(), "corr", null,
                new OrderCancelled(orderId, "PAYMENT_FAILED"));

        service.onOrderCancelled(cancelled);
        service.onOrderCancelled(cancelled);

        assertEquals(5, stock.getAvailable());
        assertEquals(0, stock.getReserved());
        verify(outbox).record(eq(Topics.STOCK_RELEASED), any());
    }
}