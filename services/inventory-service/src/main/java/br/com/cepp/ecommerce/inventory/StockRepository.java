package br.com.cepp.ecommerce.inventory;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    /** Locks rows in product id order so concurrent reservations cannot deadlock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.productId in :ids order by s.productId")
    List<Stock> lockAllById(@Param("ids") Collection<UUID> ids);
}