package br.com.cepp.ecommerce.platform.domain;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private UUID id;

    @NotNull
    private UUID customerId;

    @NotEmpty
    private List<OrderItem> items;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal totalAmount;

    @NotNull
    private OrderStatus status;

    private Instant createdAt;
    private Instant updatedAt;
}
