package br.com.cepp.ecommerce.platform.domain.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RefundAllocatorTest {

    private static List<BigDecimal> d(String... v) {
        return java.util.Arrays.stream(v).map(BigDecimal::new).toList();
    }

    @Test
    void equalCapacitiesLeftoverCentGoesToTheFirstLine() {
        assertEquals(d("3.34", "3.33", "3.33"), RefundAllocator.allocate(new BigDecimal("10.00"), d("10.00", "10.00", "10.00")));
    }

    @Test
    void leftoverCentsGoToTheLargestRemainder() {
        assertEquals(d("0.33", "0.67"), RefundAllocator.allocate(new BigDecimal("1.00"), d("1.00", "2.00")));
        assertEquals(d("0.01", "0.01", "0.02"), RefundAllocator.allocate(new BigDecimal("0.04"), d("0.01", "0.02", "0.03")));
    }

    @Test
    void sharesAlwaysSumToTheAmountAndNeverExceedCapacity() {
        List<BigDecimal> caps = d("19.99", "0.01", "7.35", "100.00", "3.33");
        for (int cents = 1; cents <= 13068; cents += 97) {
            BigDecimal amount = BigDecimal.valueOf(cents, 2);
            List<BigDecimal> shares = RefundAllocator.allocate(amount, caps);
            assertEquals(0, amount.compareTo(shares.stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
            for (int i = 0; i < caps.size(); i++) {
                assertEquals(2, shares.get(i).scale());
                org.junit.jupiter.api.Assertions.assertTrue(shares.get(i).compareTo(caps.get(i)) <= 0);
            }
        }
    }

    @Test
    void zeroCapacityLinesGetNothingAndFullRefundGivesEveryLineItsCapacity() {
        assertEquals(d("0.00", "2.00"), RefundAllocator.allocate(new BigDecimal("2"), d("0.00", "5.00")));
        assertEquals(d("19.99", "0.01", "7.35"), RefundAllocator.allocate(new BigDecimal("27.35"), d("19.99", "0.01", "7.35")));
    }

    @Test
    void inputsWithDifferentScalesAreAccepted() {
        assertEquals(d("3.00", "7.00"), RefundAllocator.allocate(new BigDecimal("10.000"), d("3", "7.0")));
    }

    @Test
    void invalidInputIsRejected() {
        List<BigDecimal> caps = d("1.00", "2.00");
        assertThrows(IllegalArgumentException.class, () -> RefundAllocator.allocate(null, caps));
        assertThrows(IllegalArgumentException.class, () -> RefundAllocator.allocate(BigDecimal.ZERO, caps));
        assertThrows(IllegalArgumentException.class, () -> RefundAllocator.allocate(new BigDecimal("-1.00"), caps));
        assertThrows(IllegalArgumentException.class, () -> RefundAllocator.allocate(new BigDecimal("0.005"), caps));
        assertThrows(IllegalArgumentException.class, () -> RefundAllocator.allocate(new BigDecimal("3.01"), caps));
        assertThrows(IllegalArgumentException.class, () -> RefundAllocator.allocate(new BigDecimal("1.00"), d("1.005", "2.00")));
        assertThrows(IllegalArgumentException.class, () -> RefundAllocator.allocate(new BigDecimal("1.00"), d("-1.00", "2.00")));
    }
}
