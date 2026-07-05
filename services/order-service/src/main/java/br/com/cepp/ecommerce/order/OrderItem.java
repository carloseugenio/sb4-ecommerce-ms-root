package br.com.cepp.ecommerce.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private UUID productId;

    @NotBlank
    private String productName;

    @Min(1)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal unitPrice;

}
