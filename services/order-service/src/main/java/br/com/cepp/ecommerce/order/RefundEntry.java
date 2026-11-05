package br.com.cepp.ecommerce.order;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/** One refund in the history of an order: the share given to each item (comma separated, item order). */
@Embeddable
public record RefundEntry(int seq, String shares, boolean undone) {

    static RefundEntry of(int seq, List<BigDecimal> shares) {
        return new RefundEntry(seq, String.join(",", shares.stream().map(BigDecimal::toPlainString).toList()), false);
    }

    List<BigDecimal> shareList() {
        return Arrays.stream(shares.split(",")).map(BigDecimal::new).toList();
    }

    RefundEntry withUndone(boolean value) {
        return new RefundEntry(seq, shares, value);
    }
}
