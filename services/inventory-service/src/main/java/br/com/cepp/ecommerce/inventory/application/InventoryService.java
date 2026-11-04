package br.com.cepp.ecommerce.inventory.application;

import br.com.cepp.ecommerce.inventory.Reservation;
import br.com.cepp.ecommerce.inventory.ReservationLine;
import br.com.cepp.ecommerce.inventory.ReservationRepository;
import br.com.cepp.ecommerce.inventory.Stock;
import br.com.cepp.ecommerce.inventory.StockRepository;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderCancelled;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderConfirmed;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderLine;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderPlaced;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.PaymentFailed;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.StockRejected;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.StockReleased;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.StockReserved;
import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import br.com.cepp.ecommerce.platform.domain.event.Topics;
import br.com.cepp.ecommerce.platform.messaging.OutboxRecorder;
import br.com.cepp.ecommerce.platform.messaging.ProcessedMessages;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saga participant: each handler is idempotent and writes its outbox event in the same transaction. */
@Service
public class InventoryService {

    static final String CONSUMER = "inventory-service";

    private final StockRepository stocks;
    private final ReservationRepository reservations;
    private final ProcessedMessages processed;
    private final OutboxRecorder outbox;

    InventoryService(StockRepository stocks, ReservationRepository reservations, ProcessedMessages processed,
                     OutboxRecorder outbox) {
        this.stocks = stocks;
        this.reservations = reservations;
        this.processed = processed;
        this.outbox = outbox;
    }

    @Transactional
    public void onOrderPlaced(EventEnvelope<OrderPlaced> event) {
        if (!processed.firstTime(event.eventId(), CONSUMER)) {
            return;
        }
        OrderPlaced order = event.payload();
        if (reservations.existsById(order.orderId())) {
            return;
        }
        Map<UUID, Integer> wanted = order.lines().stream()
                .collect(Collectors.toMap(OrderLine::productId, OrderLine::quantity, Integer::sum));
        Map<UUID, Stock> locked = stocks.lockAllById(wanted.keySet()).stream()
                .collect(Collectors.toMap(Stock::getProductId, Function.identity()));

        boolean enough = wanted.entrySet().stream()
                .allMatch(e -> locked.containsKey(e.getKey()) && locked.get(e.getKey()).canReserve(e.getValue()));
        if (!enough) {
            publish(Topics.STOCK_REJECTED, "StockRejected", event, order.orderId(),
                    new StockRejected(order.orderId(), "OUT_OF_STOCK"));
            return;
        }
        wanted.forEach((productId, quantity) -> locked.get(productId).reserve(quantity));
        List<ReservationLine> lines = wanted.entrySet().stream()
                .map(e -> new ReservationLine(e.getKey(), e.getValue())).toList();
        reservations.save(Reservation.reserve(order.orderId(), lines));
        publish(Topics.STOCK_RESERVED, "StockReserved", event, order.orderId(),
                new StockReserved(order.orderId(), order.totalAmount(), order.currency()));
    }

    @Transactional
    public void onOrderConfirmed(EventEnvelope<OrderConfirmed> event) {
        if (!processed.firstTime(event.eventId(), CONSUMER)) {
            return;
        }
        reservations.findById(event.payload().orderId()).ifPresent(reservation -> {
            if (reservation.commit()) {
                forEachLockedStock(reservation, (stock, quantity) -> stock.commit(quantity));
            }
        });
    }

    @Transactional
    public void onOrderCancelled(EventEnvelope<OrderCancelled> event) {
        release(event, event.payload().orderId());
    }

    @Transactional
    public void onPaymentFailed(EventEnvelope<PaymentFailed> event) {
        release(event, event.payload().orderId());
    }

    private void release(EventEnvelope<?> event, UUID orderId) {
        if (!processed.firstTime(event.eventId(), CONSUMER)) {
            return;
        }
        reservations.findById(orderId).ifPresent(reservation -> {
            if (reservation.release()) {
                forEachLockedStock(reservation, (stock, quantity) -> stock.release(quantity));
                publish(Topics.STOCK_RELEASED, "StockReleased", event, orderId, new StockReleased(orderId));
            }
        });
    }

    private void forEachLockedStock(Reservation reservation, java.util.function.ObjIntConsumer<Stock> action) {
        Map<UUID, Integer> quantities = reservation.getLines().stream()
                .collect(Collectors.toMap(ReservationLine::productId, ReservationLine::quantity));
        stocks.lockAllById(quantities.keySet()).forEach(stock -> action.accept(stock, quantities.get(stock.getProductId())));
    }

    private void publish(String topic, String type, EventEnvelope<?> cause, UUID orderId, Object payload) {
        outbox.record(topic, EventEnvelope.of(type, orderId.toString(), cause.correlationId(),
                cause.eventId().toString(), payload));
    }
}