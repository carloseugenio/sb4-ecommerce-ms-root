package br.com.cepp.ecommerce.order.messaging;

import br.com.cepp.ecommerce.order.application.OrderSagaHandler;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.PaymentAuthorized;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.PaymentFailed;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.StockRejected;
import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import br.com.cepp.ecommerce.platform.domain.event.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Kafka entry point (anti-corruption layer): parses event JSON and delegates to the handler. */
@Component
class OrderSagaListener {

    private static final String GROUP = "order-service";

    private final OrderSagaHandler handler;
    private final JsonMapper json;

    OrderSagaListener(OrderSagaHandler handler, JsonMapper json) {
        this.handler = handler;
        this.json = json;
    }

    @KafkaListener(topics = Topics.PAYMENT_AUTHORIZED, groupId = GROUP)
    void paymentAuthorized(String message) {
        handler.onPaymentAuthorized(json.readValue(message, new TypeReference<EventEnvelope<PaymentAuthorized>>() { }));
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = GROUP)
    void paymentFailed(String message) {
        handler.onPaymentFailed(json.readValue(message, new TypeReference<EventEnvelope<PaymentFailed>>() { }));
    }

    @KafkaListener(topics = Topics.STOCK_REJECTED, groupId = GROUP)
    void stockRejected(String message) {
        handler.onStockRejected(json.readValue(message, new TypeReference<EventEnvelope<StockRejected>>() { }));
    }
}