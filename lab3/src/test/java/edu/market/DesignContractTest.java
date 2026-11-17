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
class DesignContractTest {
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
    private DesignViewService design;
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
    void ordinaryAccountCanPublishAndBuyWithTheSameIdentity() {
        Product p = products.save("buyer", null, input());
        trading.inventory("buyer", p.getId(), 1);
        products.submit("buyer", p.getId());
        products.audit("admin", p.getId(), true, "审核通过");
        accounts.profile("seller", "卖家也可以买", "校园收货点");
        PurchaseOrder order = trading.buy("seller", p.getId(), 1, p.getMinPrice(), "dual-role-order");
        assertEquals(accounts.current("seller").getId(), order.getBuyerId());
        assertEquals(accounts.current("buyer").getId(), order.getSellerId());
        assertTrue(accounts.current("buyer").getRoles().containsAll(java.util.Set.of("BUYER", "SELLER")));
    }
    @Test
    void designFiltersUseActualLocationSellerSalesAndPositiveReviews() throws Exception {
        Product p = publish(3);
        p.setBrand("DesignBrand");
        p.setLatitude(32.0);
        p.setLongitude(118.0);
        p.setCity("南京");
        CatalogFilter near = new CatalogFilter("9成新", 32.001, 118.001, 3, 0, 0);
        assertEquals(1, products.searchPage("buyer", "DesignBrand", "", null, null, null, "priceDesc", 0, near).getTotalElements());
        CatalogFilter far = new CatalogFilter("all", 33.0, 118.0, 3, 0, 0);
        assertTrue(products.searchPage("buyer", "", "", null, null, null, "newest", 0, far).isEmpty());
        CatalogFilter reputation = new CatalogFilter("all", null, null, null, 1, 98);
        assertTrue(products.searchPage("buyer", "", "", null, null, null, "positiveRate", 0, reputation).isEmpty());
        PurchaseOrder order = buy(p, 1);
        trading.action("buyer", order.getId(), "pay");
        trading.action("seller", order.getId(), "ship");
        trading.action("buyer", order.getId(), "receive");
        reviews.submit("buyer", order.getId(), 5, "真实完成交易评价");
        assertEquals(1, products.searchPage("buyer", "", "", null, null, null, "positiveRate", 0, reputation).getTotalElements());
        String json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(design.cards(java.util.List.of(p), "buyer", 32.0, 118.0));
        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("address"));
        assertFalse(json.contains("recoveryHash"));
    }

    @Autowired
    private DesignAccountService profiles;
    @Autowired
    private DesignAdminService workspace;
    @Autowired
    private PersonalService personal;
    @Autowired
    private AuditSnapshotRepository snapshots;

    @Test
    void profileDataBelongsToActorAndAdministrativeHistoryIsAnImmutableSnapshot() {
        Product product = products.save("seller", null, input());
        products.submit("seller", product.getId());
        products.audit("admin", product.getId(), false, "请补充清晰的实物照片");
        Product draft = input();
        draft.setName("未再次提交的私人草稿内容");
        products.save("seller", product.getId(), draft);
        var data = workspace.dashboard("admin").toString();
        assertTrue(data.contains("二手相机"));
        assertFalse(data.contains("未再次提交的私人草稿内容"));
        assertEquals("二手相机", snapshots.findAllByOrderByIdDesc().getFirst().getName());
        assertThrows(BusinessException.class, () -> workspace.dashboard("buyer"));
        assertThrows(BusinessException.class, () -> workspace.statistics("buyer", 7));
        Product publicProduct = publish(2);
        personal.toggle("buyer", "FAVORITE", publicProduct.getId(), true);
        assertEquals(1, ((java.util.Map<?, ?>) profiles.profile("buyer").get("counts")).get("FAVORITE"));
        assertEquals(0, ((java.util.Map<?, ?>) profiles.profile("buyer2").get("counts")).get("FAVORITE"));
        profiles.update("buyer", "原型昵称", "仅我与交易方可见的地址", "南京", "诚信交易");
        assertEquals("南京", profiles.profile("buyer").get("city"));
        String adminData = workspace.dashboard("admin").toString();
        assertFalse(adminData.contains("仅我与交易方可见的地址"));
        assertFalse(adminData.contains("passwordHash"));
    }

    @Test
    void dashboardStatisticsPreserveLowSamplePrivacy() {
        Product product = publish(2);
        PurchaseOrder order = buy(product, 1);
        trading.action("buyer", order.getId(), "pay");
        trading.action("seller", order.getId(), "ship");
        trading.action("buyer", order.getId(), "receive");
        var result = workspace.statistics("admin", 7);
        StatisticsService.Summary totals = (StatisticsService.Summary) result.get("summary");
        assertTrue(totals.suppressed());
        assertEquals(null, totals.gross());
        assertTrue(result.get("series").toString().contains("1–4"));
    }
}
