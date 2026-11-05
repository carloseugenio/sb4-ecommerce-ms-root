package br.com.cepp.ecommerce.platform.domain.money;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyPolicyTest {

    @Test
    void scalesFollowThePolicyTable() {
        assertEquals(2, MoneyPolicy.scale("BRL"));
        assertEquals(0, MoneyPolicy.scale("JPY"));
        assertEquals(3, MoneyPolicy.scale("KWD"));
        assertEquals(2, MoneyPolicy.scale("CLP"));
        assertEquals(2, MoneyPolicy.scale("XYZ"));
    }

    @Test
    void roundsHalfToEven() {
        assertEquals(new BigDecimal("1.02"), MoneyPolicy.round(new BigDecimal("1.025"), "BRL"));
        assertEquals(new BigDecimal("1.04"), MoneyPolicy.round(new BigDecimal("1.035"), "BRL"));
        assertEquals(new BigDecimal("700"), MoneyPolicy.round(new BigDecimal("700.50"), "JPY"));
        assertEquals(new BigDecimal("702"), MoneyPolicy.round(new BigDecimal("701.5"), "JPY"));
    }
}
