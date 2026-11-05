package br.com.cepp.ecommerce.platform.domain.refund;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Splits a refund over order lines in exact cents, so the shares always add up to the refunded amount. */
public final class RefundAllocator {

    private RefundAllocator() {
    }

    /**
     * Splits {@code amount} proportionally to the remaining {@code capacities}. Every share is rounded down to the
     * cent and the cents left over go, one each, to the lines with the largest fractional remainder (ties: lower
     * index first). All results have scale 2.
     *
     * @throws IllegalArgumentException if the amount is not positive, has more than two decimals, a capacity is
     *                                  negative or has more than two decimals, or the amount exceeds the capacities
     */
    public static List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> capacities) {
        return allocate(amount, capacities, 2);
    }

    /** Same as {@link #allocate(BigDecimal, List)} for a currency with {@code scale} decimal places (0 for JPY, 3 for KWD). */
    public static List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> capacities, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("scale must not be negative");
        }
        BigInteger refund = cents(amount, "amount", scale);
        if (refund.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        List<BigInteger> caps = new ArrayList<>();
        BigInteger total = BigInteger.ZERO;
        for (BigDecimal capacity : capacities) {
            BigInteger c = cents(capacity, "capacity", scale);
            if (c.signum() < 0) {
                throw new IllegalArgumentException("capacity must not be negative");
            }
            caps.add(c);
            total = total.add(c);
        }
        if (refund.compareTo(total) > 0) {
            throw new IllegalArgumentException("amount exceeds the remaining capacity");
        }
        int n = caps.size();
        BigInteger[] shares = new BigInteger[n];
        BigInteger[] remainders = new BigInteger[n];
        BigInteger assigned = BigInteger.ZERO;
        for (int i = 0; i < n; i++) {
            BigInteger[] qr = refund.multiply(caps.get(i)).divideAndRemainder(total);
            shares[i] = qr[0];
            remainders[i] = qr[1];
            assigned = assigned.add(qr[0]);
        }
        int leftover = refund.subtract(assigned).intValueExact();
        List<Integer> byRemainder = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            byRemainder.add(i);
        }
        byRemainder.sort(Comparator.<Integer, BigInteger>comparing(i -> remainders[i]).reversed()
                .thenComparing(Comparator.naturalOrder()));
        for (int k = 0; k < leftover; k++) {
            int i = byRemainder.get(k);
            shares[i] = shares[i].add(BigInteger.ONE);
        }
        List<BigDecimal> result = new ArrayList<>();
        for (BigInteger share : shares) {
            result.add(new BigDecimal(share, scale));
        }
        return result;
    }

    private static BigInteger cents(BigDecimal value, String name, int scale) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        try {
            return value.setScale(scale, RoundingMode.UNNECESSARY).unscaledValue();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(name + " has more than " + scale + " decimals");
        }
    }
}
