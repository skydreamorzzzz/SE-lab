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
class PersonalTest {
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
    private PersonalService personal;
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
    void cartUpsertCheckoutAndDynamicInvalidity() {
        Product p = publish(2);
        personal.cart("buyer", p.getId(), 1, "学习");
        personal.cart("buyer", p.getId(), 2, "数码");
        assertEquals(1, personal.list("buyer", "CART").size());
        assertEquals(2, personal.cartRows("buyer", "相机", "数码").getFirst().quantity());
        PersonalEntry entry = personal.list("buyer", "CART").getFirst();
        PurchaseOrder o = personal.checkout("buyer", entry.getId(), p.getMinPrice(), "cart-request-1");
        assertEquals(o.getId(), personal.checkout("buyer", entry.getId(), p.getMinPrice(), "cart-request-1").getId());
        assertFalse(personal.cartRows("buyer", "", "").getFirst().reason().isBlank());
        assertThrows(BusinessException.class, () -> personal.remove("buyer2", entry.getId()));
    }
    @Test
    void uniqueRelationsHistoriesAndOneWayBlocking() {
        Product p = publish(2);
        personal.toggle("buyer", "FAVORITE", p.getId(), true);
        personal.toggle("buyer", "FAVORITE", p.getId(), true);
        assertEquals(1, personal.list("buyer", "FAVORITE").size());
        assertEquals(1, p.getFavoriteCount());
        personal.history("buyer", "SEARCH", "相机", null);
        personal.history("buyer", "VIEW", p.getId().toString(), p.getId());
        assertEquals(1, personal.list("buyer", "SEARCH").size());
        personal.toggle("buyer", "BLOCK", accounts.current("seller").getId(), true);
        assertTrue(products.searchPage("buyer", "", "", null, null, null, "sales", 0).isEmpty());
        assertEquals(1, products.searchPage("buyer2", "", "", null, null, null, "sales", 0).getTotalElements());
        assertThrows(BusinessException.class, () -> products.detail(p.getId(), "buyer"));
        assertThrows(BusinessException.class, () -> buy(p, 1));
        personal.toggle("buyer", "BLOCK", accounts.current("seller").getId(), false);
        assertEquals(1, products.searchPage("buyer", "", "", null, null, null, "newest", 0).getTotalElements());
    }
}
