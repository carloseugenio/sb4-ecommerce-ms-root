# ADR 0003: Money rounding

Status: accepted

All money in the platform goes through `MoneyPolicy` in `common-domain`. It defines how many decimal places a currency has (BRL, USD, EUR and CLP 2, JPY 0, KWD 3, any other currency 2) and the rounding mode, which is HALF_EVEN (banker's rounding), because the acquirer settles with the same rule and any other mode produces differences in reconciliation. The decimal places come from this table and not from `java.util.Currency`, since the acquirer settles CLP in centavos. New code must call `MoneyPolicy.scale` and `MoneyPolicy.round` or use `MoneyPolicy.ROUNDING` and must not hardcode a rounding mode or a table of fraction digits.
