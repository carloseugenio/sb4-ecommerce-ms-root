package br.com.cepp.ecommerce.platform.domain.event;

/** Kafka topic names: <domain>.<event>.v1, dead letter topic: <topic>.dlt */
public final class Topics {

    public static final String ORDER_PLACED = "order.placed.v1";
    public static final String ORDER_CONFIRMED = "order.confirmed.v1";
    public static final String ORDER_CANCELLED = "order.cancelled.v1";
    public static final String STOCK_RESERVED = "inventory.stock-reserved.v1";
    public static final String STOCK_REJECTED = "inventory.stock-rejected.v1";
    public static final String STOCK_RELEASED = "inventory.stock-released.v1";
    public static final String PAYMENT_AUTHORIZED = "payment.authorized.v1";
    public static final String PAYMENT_FAILED = "payment.failed.v1";
    public static final String PAYMENT_REFUNDED = "payment.refunded.v1";

    public static final String DLT_SUFFIX = ".dlt";

    private Topics() {
    }

    public static String dlt(String topic) {
        return topic + DLT_SUFFIX;
    }
}