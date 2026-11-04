package br.com.cepp.ecommerce.inventory.messaging;

import br.com.cepp.ecommerce.inventory.application.InventoryService;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderCancelled;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderConfirmed;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.OrderPlaced;
import br.com.cepp.ecommerce.platform.domain.event.CheckoutEvents.PaymentFailed;
import br.com.cepp.ecommerce.platform.domain.event.EventEnvelope;
import br.com.cepp.ecommerce.platform.domain.event.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Kafka entry point (anti-corruption layer): parses event JSON and delegates to the service. */
@Component
class InventoryListener {

    private static final String GROUP = "inventory-service";

    private final InventoryService service;
    private final JsonMapper json;

    InventoryListener(InventoryService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = Topics.ORDER_PLACED, groupId = GROUP)
    void orderPlaced(String message) {
        service.onOrderPlaced(json.readValue(message, new TypeReference<EventEnvelope<OrderPlaced>>() { }));
    }

    @KafkaListener(topics = Topics.ORDER_CONFIRMED, groupId = GROUP)
    void orderConfirmed(String message) {
        service.onOrderConfirmed(json.readValue(message, new TypeReference<EventEnvelope<OrderConfirmed>>() { }));
    }

    @KafkaListener(topics = Topics.ORDER_CANCELLED, groupId = GROUP)
    void orderCancelled(String message) {
        service.onOrderCancelled(json.readValue(message, new TypeReference<EventEnvelope<OrderCancelled>>() { }));
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = GROUP)
    void paymentFailed(String message) {
        service.onPaymentFailed(json.readValue(message, new TypeReference<EventEnvelope<PaymentFailed>>() { }));
    }
}