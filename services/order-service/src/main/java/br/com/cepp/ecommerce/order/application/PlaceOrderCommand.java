package br.com.cepp.ecommerce.order.application;

import br.com.cepp.ecommerce.order.OrderItem;
import java.util.List;
import java.util.UUID;

public record PlaceOrderCommand(UUID customerId, String currency, List<OrderItem> items) {
}