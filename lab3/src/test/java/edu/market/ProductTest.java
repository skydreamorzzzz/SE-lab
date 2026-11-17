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
class ProductTest {
    @Autowired
    private AccountService accounts;
    @Autowired
    private AccountRepository accountStore;
    @Autowired
    private ProductService products;
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
        product.setStock(stock);
        products.submit("seller", product.getId());
        products.audit("admin", product.getId(), true, "通过");
        return product;
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
        product.setStock(product.getStock() + 1);
        assertEquals(ProductStatus.ON_SALE, product.getStatus());
        Product edit = input();
        edit.setDescription("修改后的描述");
        products.save("seller", product.getId(), edit);
        assertEquals(ProductStatus.PENDING_REVIEW, product.getStatus());
        assertTrue(products.search(null, null, null, null, null, null).isEmpty());
        products.audit("admin", product.getId(), true, "复审通过");
        products.withdraw("seller", product.getId());
        product.setStock(product.getStock() + 2);
        assertEquals(ProductStatus.OFF_SHELF, product.getStatus());
        products.submit("seller", product.getId());
        assertEquals(ProductStatus.PENDING_REVIEW, product.getStatus());
    }

}
