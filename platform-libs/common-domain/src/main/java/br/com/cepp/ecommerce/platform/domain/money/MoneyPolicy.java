package br.com.cepp.ecommerce.platform.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/** Platform money policy, see docs/adr/0003-money-rounding.md: decimal places per currency and the rounding mode. */
public final class MoneyPolicy {

    /** Banker's rounding, the same rule the acquirer uses when it settles. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private static final int DEFAULT_SCALE = 2;
    private static final Map<String, Integer> SCALES = Map.of(
            "BRL", 2, "USD", 2, "EUR", 2, "CLP", 2, "JPY", 0, "KWD", 3);

    private MoneyPolicy() {
    }

    /** Decimal places of the currency; currencies that are not listed use 2. */
    public static int scale(String currency) {
        return SCALES.getOrDefault(currency, DEFAULT_SCALE);
    }

    public static BigDecimal round(BigDecimal amount, String currency) {
        return amount.setScale(scale(currency), ROUNDING);
    }
}
