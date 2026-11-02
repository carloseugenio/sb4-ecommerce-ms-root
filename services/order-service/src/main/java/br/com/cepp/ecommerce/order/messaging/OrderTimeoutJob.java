package br.com.cepp.ecommerce.order.messaging;

import br.com.cepp.ecommerce.order.application.OrderSagaHandler;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class OrderTimeoutJob {

    private final OrderSagaHandler handler;
    private final Duration timeout;

    OrderTimeoutJob(OrderSagaHandler handler, @Value("${order.pending-timeout-minutes:15}") long minutes) {
        this.handler = handler;
        this.timeout = Duration.ofMinutes(minutes);
    }

    @Scheduled(fixedDelayString = "${order.timeout-check-delay-ms:60000}")
    void cancelStaleOrders() {
        handler.cancelStale(timeout);
    }
}