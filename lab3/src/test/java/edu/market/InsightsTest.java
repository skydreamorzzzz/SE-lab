package edu.market;

import java.math.BigDecimal;
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
class InsightsTest {
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
    private StatisticsService statistics;
    @Autowired
    private RankingService ranking;
    @Autowired
    private PlatformService platform;
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
    void rankingsAndSuppressedStatisticsDoNotExposePersonalDetails() {
        Product p = publish(6);
        PurchaseOrder order = buy(p, 1);
        trading.action("buyer", order.getId(), "pay");
        trading.action("seller", order.getId(), "ship");
        trading.action("buyer", order.getId(), "receive");
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        var summary = statistics.summary("admin", today, today);
        assertTrue(summary.suppressed());
        assertEquals(null, summary.gross());
        assertFalse(summary.toString().contains("南京"));
        assertThrows(BusinessException.class, () -> statistics.summary("buyer", today, today));
        assertEquals(p.getId(), ranking.rank("buyer", "day", "数码电子").getFirst().productId());
        reviews.submit("buyer", order.getId(), 5, "很好");
        assertEquals(p.getId(), products.searchPage("buyer", "", "", null, null, null, "reputation", 0).getContent().getFirst().getId());
    }
    @Test
    void promotionBecomesRealOrderPriceAndStalePriceIsRejected() {
        accounts.current("admin").setSuperAdmin(true);
        platform.configure("admin", BigDecimal.ZERO, new BigDecimal("0.1"));
        Product p = publish(1);
        assertThrows(BusinessException.class, () -> buy(p, 1));
        PurchaseOrder order = trading.buy("buyer", p.getId(), 1, new BigDecimal("108.00"), "promo-request-1");
        assertEquals(new BigDecimal("108.00"), order.getTotal());
    }
}
