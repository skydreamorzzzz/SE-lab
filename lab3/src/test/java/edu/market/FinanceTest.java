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
class FinanceTest {
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
    private FinanceService finance;
    @Autowired
    private SettlementRepository settlements;
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
    void directRefundReleasesOnceAndRejectsDuplicates() {
        Product product = publish(2);
        PurchaseOrder order = buy(product, 1);
        trading.action("buyer", order.getId(), "pay");
        finance.request("buyer", order.getId(), "不需要了");
        assertThrows(BusinessException.class, () -> finance.request("buyer", order.getId(), "重复"));
        assertThrows(BusinessException.class, () -> trading.action("seller", order.getId(), "ship"));
        assertThrows(BusinessException.class, () -> finance.decide("buyer", order.getId(), true, "越权"));
        finance.decide("seller", order.getId(), true, "同意");
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
        assertEquals(2, product.getStock());
        assertThrows(BusinessException.class, () -> finance.decide("seller", order.getId(), true, "重复"));
        assertTrue(settlements.findByOrderId(order.getId()).isEmpty());
    }
    @Test
    void returnRefundTimeoutRequiresPhysicalReturn() {
        Product product = publish(1);
        PurchaseOrder order = buy(product, 1);
        trading.action("buyer", order.getId(), "pay");
        trading.action("seller", order.getId(), "ship");
        finance.request("buyer", order.getId(), "退货");
        finance.timeout(order.getId(), Instant.now().plus(Duration.ofHours(49)));
        finance.timeout(order.getId(), Instant.now().plus(Duration.ofHours(50)));
        assertEquals(OrderStatus.RETURN_PENDING, order.getStatus());
        assertEquals(0, product.getStock());
        finance.returnGoods("buyer", order.getId(), "SIM-RETURN");
        finance.receivedReturn("seller", order.getId());
        assertEquals(1, product.getStock());
        assertEquals(OrderStatus.REFUNDED, order.getStatus());
    }
    @Test
    void settlementUsesSuperAdminRateAfterReceiptOnly() {
        Account admin = accounts.current("admin");
        assertThrows(BusinessException.class, () -> platform.configure("admin", new BigDecimal("0.1"), BigDecimal.ZERO));
        admin.setSuperAdmin(true);
        platform.configure("admin", new BigDecimal("0.1"), BigDecimal.ZERO);
        PurchaseOrder order = buy(publish(1), 1);
        trading.action("buyer", order.getId(), "pay");
        assertTrue(settlements.findByOrderId(order.getId()).isEmpty());
        trading.action("seller", order.getId(), "ship");
        trading.action("buyer", order.getId(), "receive");
        assertEquals(new BigDecimal("108.00"), settlements.findByOrderId(order.getId()).orElseThrow().getNetAmount());
        finance.settle(order);
        assertEquals(1, settlements.count());
    }
}
