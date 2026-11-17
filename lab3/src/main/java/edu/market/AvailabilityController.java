package edu.market;

import java.math.BigDecimal;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class AvailabilityController {
    private final ProductService products;
    private final FinanceService finance;
    public AvailabilityController(ProductService products, FinanceService finance) {
        this.products = products;
        this.finance = finance;
    }
    @GetMapping("/availability/{id}")
    public Availability availability(@PathVariable Long id, Principal principal) {
        Product p = products.detail(id, principal == null ? null : principal.getName());
        return new Availability(p.getStatus().name(), p.getStock(), p.getMinPrice() == null ? null : finance.price(p.getMinPrice()));
    }
    public record Availability(String status, int stock, BigDecimal price) {
    }
}
