package br.com.cepp.ecommerce.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderRefundTest {

    private static Order order() {
        return Order.place(UUID.randomUUID(), "BRL", List.of(
                new OrderItem(UUID.randomUUID(), "A", 3, new BigDecimal("3.33")),
                new OrderItem(UUID.randomUUID(), "B", 1, new BigDecimal("0.01"))));
    }

    private static List<BigDecimal> d(String... v) {
        return java.util.Arrays.stream(v).map(BigDecimal::new).toList();
    }

    @Test
    void refundsAreSplitByRemainingCapacityAndRepeatable() {
        Order o = order();
        o.confirm();

        assertEquals(d("5.00", "0.00"), o.refund(new BigDecimal("5.00")));
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());
        assertEquals(new BigDecimal("5.00"), o.getRefundedAmount());
        assertEquals(new BigDecimal("5.00"), o.getItems().get(0).refundedAmount());
        assertEquals(new BigDecimal("4.99"), o.getItems().get(0).remaining(2));
        assertEquals(new BigDecimal("0.01"), o.getItems().get(1).remaining(2));

        assertEquals(d("4.99", "0.01"), o.refund(new BigDecimal("5.00")));
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
        assertEquals(new BigDecimal("10.00"), o.getRefundedAmount());
    }

    @Test
    void refundedStatusIgnoresTheScaleOfTheTotalAndOfTheAmount() {
        Order o = Order.place(UUID.randomUUID(), "BRL",
                List.of(new OrderItem(UUID.randomUUID(), "A", 3, new BigDecimal("10"))));
        o.confirm();
        o.refund(new BigDecimal("12.5"));
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());
        o.refund(new BigDecimal("17.500"));
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
        assertEquals(new BigDecimal("30.00"), o.getRefundedAmount());
        assertEquals(2, o.getRefundedAmount().scale());
    }

    @Test
    void newOrderStartsWithZeroRefundedAtScaleTwo() {
        assertEquals(new BigDecimal("0.00"), order().getRefundedAmount());
    }

    @Test
    void invalidAmountsLeaveTheOrderUntouched() {
        Order o = order();
        o.confirm();
        for (String bad : List.of("0", "-1.00", "0.005", "10.01")) {
            assertThrows(IllegalArgumentException.class, () -> o.refund(new BigDecimal(bad)));
        }
        assertThrows(IllegalArgumentException.class, () -> o.refund(null));
        assertEquals(new BigDecimal("0.00"), o.getRefundedAmount());
        assertEquals(0, o.getItems().get(0).refundedAmount().signum());
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());
    }

    @Test
    void onlyConfirmedOrdersCanBeRefundedAndTheStateIsCheckedFirst() {
        Order pending = order();
        assertThrows(IllegalStateException.class, () -> pending.refund(new BigDecimal("1.00")));
        assertThrows(IllegalStateException.class, () -> pending.refund(new BigDecimal("-5")));

        Order cancelled = order();
        cancelled.cancel("x");
        assertThrows(IllegalStateException.class, () -> cancelled.refund(new BigDecimal("1.00")));

        Order done = order();
        done.confirm();
        done.refund(new BigDecimal("10.00"));
        assertThrows(IllegalStateException.class, () -> done.refund(new BigDecimal("0.01")));
        assertEquals(OrderStatus.REFUNDED, done.getStatus());
    }

    @Test
    void refundedStatusRoundTripsThroughOrderItemHelpers() {
        OrderItem item = new OrderItem(UUID.randomUUID(), "A", 2, new BigDecimal("1.005"));
        assertEquals(new BigDecimal("2.01"), item.remaining(2));
        assertEquals(0, item.refundedAmount().signum());
        assertEquals(0, new OrderItem(UUID.randomUUID(), "A", 1, BigDecimal.ONE, null).refundedAmount().signum());
    }

    @Test
    void fullyRefundedMeansNoLineHasAnythingLeftEvenWhenLineRoundingExceedsTheTotal() {
        Order o = Order.place(UUID.randomUUID(), "BRL", List.of(
                new OrderItem(UUID.randomUUID(), "A", 1, new BigDecimal("1.005")),
                new OrderItem(UUID.randomUUID(), "B", 1, new BigDecimal("1.005"))));
        o.confirm();
        assertEquals(d("1.01", "1.00"), o.refund(new BigDecimal("2.01")));
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());
        assertEquals(d("0.00", "0.01"), o.refund(new BigDecimal("0.01")));
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
        assertEquals(new BigDecimal("2.02"), o.getRefundedAmount());
        assertThrows(IllegalStateException.class, () -> o.refund(new BigDecimal("0.01")));
    }

    @Test
    void zeroDecimalCurrenciesUseWholeUnits() {
        Order o = Order.place(UUID.randomUUID(), "JPY", List.of(
                new OrderItem(UUID.randomUUID(), "A", 1, new BigDecimal("333")),
                new OrderItem(UUID.randomUUID(), "B", 1, new BigDecimal("333")),
                new OrderItem(UUID.randomUUID(), "C", 1, new BigDecimal("333"))));
        o.confirm();
        assertEquals(new BigDecimal("0"), o.getRefundedAmount());
        assertEquals(d("34", "33", "33"), o.refund(new BigDecimal("100")));
        assertEquals(new BigDecimal("100"), o.getRefundedAmount());
        assertThrows(IllegalArgumentException.class, () -> o.refund(new BigDecimal("0.5")));
        assertEquals(d("30", "30", "30"), o.refund(new BigDecimal("90.0")));
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());
        assertEquals(d("269", "270", "270"), o.refund(new BigDecimal("809")));
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
        assertEquals(new BigDecimal("999"), o.getRefundedAmount());
        assertEquals(0, o.getRefundedAmount().scale());
    }

    @Test
    void threeDecimalCurrenciesUseThousandths() {
        Order o = Order.place(UUID.randomUUID(), "KWD", List.of(
                new OrderItem(UUID.randomUUID(), "A", 1, new BigDecimal("1.001")),
                new OrderItem(UUID.randomUUID(), "B", 1, new BigDecimal("2.002"))));
        o.confirm();
        assertEquals(d("0.333", "0.667"), o.refund(new BigDecimal("1.000")));
        assertThrows(IllegalArgumentException.class, () -> o.refund(new BigDecimal("0.0005")));
        o.refund(new BigDecimal("2.003"));
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
        assertEquals(new BigDecimal("3.003"), o.getRefundedAmount());
    }
}