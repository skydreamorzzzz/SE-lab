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
class MarketplaceTest {
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
    void completeTradeAndBothSidesReview() {
        Product product = publish(2);
        assertEquals(1, products.search("相机", "数码电子", BigDecimal.TEN, new BigDecimal("200"), null, "price").size());
        PurchaseOrder order = buy(product, 1);
        assertEquals(new BigDecimal("120.00"), order.getTotal());
        assertEquals(1, product.getStock());
        assertThrows(BusinessException.class, () -> reviews.submit("buyer", order.getId(), 5, "过早评价"));
        trading.action("buyer", order.getId(), "pay");
        trading.action("seller", order.getId(), "ship");
        trading.action("buyer", order.getId(), "receive");
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
        reviews.submit("buyer", order.getId(), 5, "商品完好");
        reviews.submit("seller", order.getId(), 4, "沟通顺畅");
        assertThrows(BusinessException.class, () -> reviews.submit("buyer", order.getId(), 5, "重复"));
        assertThrows(BusinessException.class, () -> reviews.submit("buyer2", order.getId(), 5, "越权"));
    }

    @Test
    void draftsArePrivateAndAuditRequiresAdmin() {
        Product draft = products.save("seller", null, new Product());
        assertTrue(products.search(null, null, null, null, null, null).isEmpty());
        assertThrows(BusinessException.class, () -> products.detail(draft.getId(), "buyer"));
        assertThrows(BusinessException.class, () -> products.save("other", draft.getId(), input()));
        assertThrows(BusinessException.class, () -> products.submit("seller", draft.getId()));
        Product product = products.save("seller", draft.getId(), input());
        products.submit("seller", product.getId());
        assertThrows(BusinessException.class, () -> products.audit("seller", product.getId(), true, ""));
        assertThrows(BusinessException.class, () -> products.audit("admin", product.getId(), false, ""));
        products.audit("admin", product.getId(), false, "请补充说明");
        assertEquals(ProductStatus.DRAFT, product.getStatus());
    }

    @Test
    void contentEditsTriggerReviewAndWithdrawNeedsResubmission() {
        Product product = publish(2);
        trading.inventory("seller", product.getId(), 1);
        assertEquals(ProductStatus.ON_SALE, product.getStatus());
        Product edit = input();
        edit.setDescription("修改后的描述");
        products.save("seller", product.getId(), edit);
        assertEquals(ProductStatus.PENDING_REVIEW, product.getStatus());
        assertTrue(products.search(null, null, null, null, null, null).isEmpty());
        products.audit("admin", product.getId(), true, "复审通过");
        products.withdraw("seller", product.getId());
        trading.inventory("seller", product.getId(), 2);
        assertEquals(ProductStatus.OFF_SHELF, product.getStatus());
        products.submit("seller", product.getId());
        assertEquals(ProductStatus.PENDING_REVIEW, product.getStatus());
    }

    @Test
    void insufficientStockStalePriceAndDuplicateOrderAreSafe() {
        Product product = publish(1);
        assertThrows(BusinessException.class, () -> buy(product, 2));
        assertThrows(BusinessException.class, () -> trading.buy("buyer", product.getId(), 1, BigDecimal.ONE, "request-1"));
        assertEquals(1, product.getStock());
        PurchaseOrder order = trading.buy("buyer", product.getId(), 1, product.getMinPrice(), "request-2");
        assertEquals(order.getId(), trading.buy("buyer", product.getId(), 1, product.getMinPrice(), "request-2").getId());
        assertEquals(0, product.getStock());
        assertThrows(BusinessException.class, () -> buy(product, 1));
        assertEquals(1, orders.count());
    }

    @Test
    void illegalTransitionsAndOrderPrivacyAreRejected() {
        PurchaseOrder order = buy(publish(2), 1);
        assertThrows(BusinessException.class, () -> trading.action("seller", order.getId(), "ship"));
        assertThrows(BusinessException.class, () -> trading.action("seller", order.getId(), "pay"));
        assertThrows(BusinessException.class, () -> trading.action("buyer2", order.getId(), "cancel"));
        assertThrows(BusinessException.class, () -> trading.detail("admin", order.getId()));
        trading.action("buyer", order.getId(), "cancel");
        assertThrows(BusinessException.class, () -> trading.action("buyer", order.getId(), "pay"));
        assertThrows(BusinessException.class, () -> trading.action("buyer", order.getId(), "cancel"));
    }

    @Test
    void paymentTimeoutReleasesStockExactlyOnce() {
        Product product = publish(1);
        PurchaseOrder order = buy(product, 1);
        order.setCreatedAt(Instant.now().minus(Duration.ofMinutes(16)));
        assertThrows(BusinessException.class, () -> trading.action("buyer", order.getId(), "pay"));
        trading.expire(order.getId(), Instant.now());
        trading.expire(order.getId(), Instant.now());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(1, product.getStock());
        assertEquals(ProductStatus.ON_SALE, product.getStatus());
    }

    @Test
    void reservationsMergeEnforceTotalAndCancel() {
        Product product = publish(0);
        Reservation first = trading.reserve("buyer", product.getId(), 2);
        Reservation merged = trading.reserve("buyer", product.getId(), 1);
        assertEquals(first.getId(), merged.getId());
        assertEquals(3, merged.getQuantity());
        trading.reserve("buyer2", product.getId(), 2);
        assertThrows(BusinessException.class, () -> trading.reserve("buyer2", product.getId(), 1));
        assertEquals(2, reservations.count());
        assertThrows(BusinessException.class, () -> trading.cancelReservation("buyer2", first.getId()));
        trading.cancelReservation("buyer", first.getId());
        Reservation again = trading.reserve("buyer", product.getId(), 3);
        assertTrue(again.getId() > first.getId());
    }

    @Test
    void restockAllocatesPartiallyInFifoOrderAndSellerConfirms() {
        Product product = publish(0);
        Reservation first = trading.reserve("buyer", product.getId(), 3);
        Reservation second = trading.reserve("buyer2", product.getId(), 1);
        trading.inventory("seller", product.getId(), 2);
        assertEquals(2, first.getAllocated());
        assertEquals(0, second.getAllocated());
        assertEquals(0, product.getStock());
        assertThrows(BusinessException.class, () -> trading.confirmReservation("other", first.getId()));
        PurchaseOrder partial = trading.confirmReservation("seller", first.getId());
        assertEquals(2, partial.getQuantity());
        assertEquals(1, first.getQuantity());
        assertFalse(first.getConfirmed());
        trading.inventory("seller", product.getId(), 2);
        assertEquals(1, first.getAllocated());
        assertEquals(1, second.getAllocated());
        trading.cancelReservation("buyer", first.getId());
        assertEquals(1, product.getStock());
    }

    @Test
    void autoReceiptRequiresSevenDaysAfterSigned() {
        PurchaseOrder order = buy(publish(1), 1);
        trading.action("buyer", order.getId(), "pay");
        trading.action("seller", order.getId(), "ship");
        trading.expire(order.getId(), Instant.now().plus(Duration.ofDays(20)));
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        trading.action("buyer", order.getId(), "sign");
        trading.expire(order.getId(), Instant.now().plus(Duration.ofDays(6)));
        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        trading.expire(order.getId(), Instant.now().plus(Duration.ofDays(8)));
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }
}
