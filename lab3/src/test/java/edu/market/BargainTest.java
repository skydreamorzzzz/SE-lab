package edu.market;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:market;DB_CLOSE_DELAY=-1")
@Transactional
class BargainTest {
    @Autowired
    private AccountService accounts;
    @Autowired
    private AccountRepository accountStore;
    @Autowired
    private ProductService products;
    @Autowired
    private TradingService trading;
    @Autowired
    private ReviewService reviews;
    @Autowired
    private PurchaseOrderRepository orders;
    @Autowired
    private ReservationRepository reservations;
    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private BargainService bargains;
    @BeforeEach
    void users() {
        accounts.register("buyer", "Valid123!", "BUYER");
        accounts.register("buyer2", "Valid123!", "BUYER");
        accounts.register("seller", "Valid123!", "SELLER");
        accounts.register("other", "Valid123!", "SELLER");
        accountStore.save(new Account("admin", encoder.encode("Valid123!"), "ADMIN"));
        accounts.profile("buyer", "买家一", "南京学校宿舍");
        accounts.profile("buyer2", "买家二", "南京学校宿舍二");
    }

    private Product input() {
        Product product = new Product();
        product.setName("二手相机");
        product.setDescription("功能完好，配件齐全");
        product.setCategory("数码电子/相机");
        product.setConditionLabel("9成新");
        product.setImageUrl("/images/sample.svg");
        product.setMinPrice(new BigDecimal("120.00"));
        product.setMaxPrice(new BigDecimal("150.00"));
        product.setReservationLimit(5);
        product.setNegotiable(true);
        return product;
    }

    private Product publish(int stock) {
        Product product = products.save("seller", null, input());
        trading.inventory("seller", product.getId(), stock);
        products.submit("seller", product.getId());
        products.audit("admin", product.getId(), true, "通过");
        return product;
    }

    private PurchaseOrder buy(Product product, int quantity) {
        return trading.buy("buyer", product.getId(), quantity, product.getMinPrice(), UUID.randomUUID().toString());
    }

    @Test
    void stockHoldConvertsToExactlyOneOrderAndTimeoutReleasesOnce() {
        Product p = publish(1);
        Bargain first = bargains.propose("buyer", p.getId(), 1, new BigDecimal("130.00"));
        Bargain second = bargains.propose("buyer2", p.getId(), 1, new BigDecimal("140.00"));
        bargains.decide("seller", first.getId(), true);
        assertEquals(0, p.getStock());
        assertThrows(BusinessException.class, () -> bargains.decide("seller", second.getId(), true));
        PurchaseOrder order = bargains.order("buyer", first.getId());
        assertEquals(new BigDecimal("130.00"), order.getTotal());
        assertEquals(order.getId(), bargains.order("buyer", first.getId()).getId());
        bargains.expire(first.getId(), Instant.now().plus(Duration.ofMinutes(16)));
        assertEquals(0, p.getStock());
        trading.action("buyer", order.getId(), "cancel");
        assertEquals(1, p.getStock());
    }
    @Test
    void expiredOrChangedListingCannotUseOldQuote() {
        Product p = publish(1);
        Bargain quote = bargains.propose("buyer", p.getId(), 1, new BigDecimal("130.00"));
        bargains.decide("seller", quote.getId(), true);
        bargains.expire(quote.getId(), Instant.now().plus(Duration.ofMinutes(16)));
        bargains.expire(quote.getId(), Instant.now().plus(Duration.ofMinutes(17)));
        assertEquals(1, p.getStock());
        assertThrows(BusinessException.class, () -> bargains.order("buyer", quote.getId()));
        Bargain another = bargains.propose("buyer", p.getId(), 1, new BigDecimal("130.00"));
        bargains.decide("seller", another.getId(), true);
        products.withdraw("seller", p.getId());
        assertThrows(BusinessException.class, () -> bargains.order("buyer", another.getId()));
    }
}
