package br.com.cepp.ecommerce.inventory;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public record ReservationLine(UUID productId, int quantity) {
}