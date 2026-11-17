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
class ModerationTest {
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
    private ModerationService moderation;
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
    void dailyReportingFeedbackAndAppealRequireReview() {
        Product p = publish(1);
        Instant today = Instant.parse("2026-11-17T02:00:00Z");
        ModerationCase report = moderation.report("buyer", "PRODUCT", p.getId(), "疑似违规", "", today);
        assertEquals(ProductStatus.ON_SALE, p.getStatus());
        assertThrows(BusinessException.class, () -> moderation.report("buyer", "PRODUCT", p.getId(), "重复", "", today));
        moderation.report("buyer", "PRODUCT", p.getId(), "次日", "", today.plus(Duration.ofDays(1)));
        assertThrows(BusinessException.class, () -> moderation.punish("buyer", "PRODUCT", p.getId(), "越权"));
        moderation.decide("admin", report.getId(), true, "已核实");
        moderation.punish("admin", "PRODUCT", p.getId(), "违规商品");
        assertEquals(ProductStatus.VIOLATION, p.getStatus());
        assertThrows(BusinessException.class, () -> products.submit("seller", p.getId()));
        ModerationCase appeal = moderation.appeal("seller", "PRODUCT", p.getId(), "申请复核");
        moderation.decide("admin", appeal.getId(), true, "证据充分");
        assertEquals(ProductStatus.PENDING_REVIEW, p.getStatus());
        assertTrue(products.search(null, null, null, null, null, null).isEmpty());
        products.audit("admin", p.getId(), true, "复审通过");
        assertEquals(ProductStatus.ON_SALE, p.getStatus());
    }
    @Test
    void bannedSellerIsHiddenWithoutDeletingHistoricalOrders() {
        Product p = publish(2);
        PurchaseOrder order = buy(p, 1);
        moderation.punish("admin", "ACCOUNT", accounts.current("seller").getId(), "违规封禁");
        assertTrue(products.search(null, null, null, null, null, null).isEmpty());
        assertThrows(BusinessException.class, () -> buy(p, 1));
        assertEquals(order.getId(), trading.detail("buyer", order.getId()).getId());
        ModerationCase appeal = moderation.appeal("seller", "ACCOUNT", accounts.current("seller").getId(), "申请解封");
        moderation.decide("admin", appeal.getId(), true, "恢复账号");
        assertFalse(accounts.current("seller").getBanned());
    }
}
