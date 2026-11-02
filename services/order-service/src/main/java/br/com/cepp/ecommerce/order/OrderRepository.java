package br.com.cepp.ecommerce.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, Instant before);
}