package br.com.cepp.ecommerce.inventory.api;

import br.com.cepp.ecommerce.inventory.Stock;
import br.com.cepp.ecommerce.inventory.StockRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
class StockController {

    record SetStockRequest(@Min(0) int available) {
    }

    record StockResponse(UUID productId, int available, int reserved) {

        static StockResponse from(Stock s) {
            return new StockResponse(s.getProductId(), s.getAvailable(), s.getReserved());
        }
    }

    private final StockRepository stocks;

    StockController(StockRepository stocks) {
        this.stocks = stocks;
    }

    @PutMapping("/{productId}")
    @Transactional
    StockResponse set(@PathVariable UUID productId, @Valid @RequestBody SetStockRequest request) {
        Stock stock = stocks.findById(productId).orElseGet(() -> new Stock(productId, 0));
        stock.setAvailable(request.available());
        return StockResponse.from(stocks.save(stock));
    }

    @GetMapping("/{productId}")
    StockResponse get(@PathVariable UUID productId) {
        return stocks.findById(productId).map(StockResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product " + productId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail notFound(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}