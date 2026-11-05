package br.com.cepp.ecommerce.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderRefundHistoryTest {

    private Order order() {
        Order o = Order.place(UUID.randomUUID(), "BRL", List.of(
                new OrderItem(UUID.randomUUID(), "A", 1, new BigDecimal("10.00")),
                new OrderItem(UUID.randomUUID(), "B", 1, new BigDecimal("10.00")),
                new OrderItem(UUID.randomUUID(), "C", 1, new BigDecimal("10.00"))));
        o.confirm();
        return o;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void undoRestoresItemsAndTotalAndReturnsTheShares() {
        Order o = order();
        o.refund(bd("10"));

        List<BigDecimal> undone = o.undoRefund();

        assertEquals(List.of(bd("3.34"), bd("3.33"), bd("3.33")), undone);
        assertEquals(bd("0.00"), o.getRefundedAmount());
        for (OrderItem i : o.getItems()) {
            assertEquals(bd("10.00"), i.remaining(2));
        }
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());
    }

    @Test
    void undoTakesBackTheLatestRefundFirst() {
        Order o = order();
        o.refund(bd("6.00"));
        o.refund(bd("3.00"));

        o.undoRefund();

        assertEquals(bd("6.00"), o.getRefundedAmount());
        o.undoRefund();
        assertEquals(bd("0.00"), o.getRefundedAmount());
        assertThrows(IllegalStateException.class, o::undoRefund);
    }

    @Test
    void undoOfAFullRefundMakesTheOrderConfirmedAgainAndRedoRefundsIt() {
        Order o = order();
        o.refund(bd("30.00"));
        assertEquals(OrderStatus.REFUNDED, o.getStatus());

        o.undoRefund();
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());
        assertEquals(bd("0.00"), o.getRefundedAmount());

        o.redoRefund();
        assertEquals(OrderStatus.REFUNDED, o.getStatus());
        assertEquals(bd("30.00"), o.getRefundedAmount());
    }

    @Test
    void redoAppliesTheOldestUndoneRefundFirst() {
        Order o = order();
        o.refund(bd("3.00"));
        o.refund(bd("6.00"));
        o.undoRefund();
        o.undoRefund();

        assertEquals(List.of(bd("1.00"), bd("1.00"), bd("1.00")), o.redoRefund());
        assertEquals(bd("3.00"), o.getRefundedAmount());
        o.redoRefund();
        assertEquals(bd("9.00"), o.getRefundedAmount());
        assertThrows(IllegalStateException.class, o::redoRefund);
    }

    @Test
    void undoAfterRedoTakesBackTheMostRecentActiveRefund() {
        Order o = order();
        o.refund(bd("3.00"));
        o.refund(bd("6.00"));
        o.undoRefund();
        o.undoRefund();
        o.redoRefund();

        o.undoRefund();

        assertEquals(bd("0.00"), o.getRefundedAmount());
    }

    @Test
    void aNewRefundDiscardsWhatWasUndone() {
        Order o = order();
        o.refund(bd("3.00"));
        o.undoRefund();
        o.refund(bd("6.00"));

        assertThrows(IllegalStateException.class, o::redoRefund);
        assertEquals(bd("6.00"), o.getRefundedAmount());
    }

    @Test
    void undoAndRedoNeedTheRightStateAndHistory() {
        Order fresh = order();
        assertThrows(IllegalStateException.class, fresh::undoRefund);
        assertThrows(IllegalStateException.class, fresh::redoRefund);
        Order pending = Order.place(UUID.randomUUID(), "BRL",
                List.of(new OrderItem(UUID.randomUUID(), "A", 1, new BigDecimal("1.00"))));
        assertThrows(IllegalStateException.class, pending::undoRefund);
        assertThrows(IllegalStateException.class, pending::redoRefund);
    }

    @Test
    void secondRefundUndoLeavesTheExactRemainingOfTheFirst() {
        Order o = order();
        o.refund(bd("10"));
        o.refund(bd("10"));

        o.undoRefund();

        assertEquals(bd("10.00"), o.getRefundedAmount());
        assertEquals(bd("6.66"), o.getItems().get(0).remaining(2));
        assertEquals(bd("6.67"), o.getItems().get(1).remaining(2));
        assertEquals(bd("6.67"), o.getItems().get(2).remaining(2));
    }

    @Test
    void undoWorksInZeroDecimalCurrencies() {
        Order o = Order.place(UUID.randomUUID(), "JPY", List.of(
                new OrderItem(UUID.randomUUID(), "A", 1, new BigDecimal("300")),
                new OrderItem(UUID.randomUUID(), "B", 1, new BigDecimal("300"))));
        o.confirm();
        o.refund(bd("101"));

        o.undoRefund();

        assertEquals(bd("0"), o.getRefundedAmount());
        assertEquals(bd("300"), o.getItems().get(0).remaining(0));
    }
}
