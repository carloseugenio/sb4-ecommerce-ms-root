package br.com.cepp.ecommerce.order.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.cepp.ecommerce.order.Order;
import br.com.cepp.ecommerce.order.OrderItem;
import br.com.cepp.ecommerce.order.application.OrderNotFoundException;
import br.com.cepp.ecommerce.order.application.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrderRefundApiTest {

    private final OrderService service = mock(OrderService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
            .setControllerAdvice(new OrderExceptionHandler()).build();

    private Order order() {
        Order o = Order.place(UUID.randomUUID(), "BRL",
                List.of(new OrderItem(UUID.randomUUID(), "A", 1, new BigDecimal("10.00"))));
        o.confirm();
        return o;
    }

    @Test
    void refundReturnsTheOrderWithTheRefundedAmount() throws Exception {
        Order o = order();
        o.refund(new BigDecimal("4.00"));
        when(service.refund(o.getId(), new BigDecimal("4.00"))).thenReturn(o);

        mvc.perform(post("/api/orders/{id}/refunds", o.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 4.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.refundedAmount").value(4.0))
                .andExpect(jsonPath("$.totalAmount").value(10.0));
    }

    @Test
    void fullRefundShowsRefundedStatus() throws Exception {
        Order o = order();
        o.refund(new BigDecimal("10"));
        when(service.refund(o.getId(), new BigDecimal("10"))).thenReturn(o);

        mvc.perform(post("/api/orders/{id}/refunds", o.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void errorsAreMappedToStatusCodes() throws Exception {
        UUID missing = UUID.randomUUID();
        when(service.refund(missing, new BigDecimal("1.00"))).thenThrow(new OrderNotFoundException(missing));
        UUID bad = UUID.randomUUID();
        when(service.refund(bad, new BigDecimal("1.00"))).thenThrow(new IllegalArgumentException("bad amount"));
        UUID wrong = UUID.randomUUID();
        when(service.refund(wrong, new BigDecimal("1.00"))).thenThrow(new IllegalStateException("not confirmed"));

        for (var c : List.of(new Object[] {missing, 404}, new Object[] {bad, 400}, new Object[] {wrong, 409})) {
            mvc.perform(post("/api/orders/{id}/refunds", c[0]).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"amount\": 1.00}"))
                    .andExpect(status().is((Integer) c[1]));
        }
    }
}
