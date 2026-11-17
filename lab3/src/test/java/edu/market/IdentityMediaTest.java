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

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:identity;DB_CLOSE_DELAY=-1", "market.media-dir=./target/test-media"})
@Transactional
class IdentityMediaTest {
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
    private IdentityService identity;
    @Autowired
    private MediaService media;
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
    void recoveryAndPhoneRequireProofAndTokensAreOneUse() {
        assertThrows(BusinessException.class, () -> identity.recoveryCode("buyer", "wrong"));
        String token = identity.recoveryCode("buyer", "Valid123!");
        assertThrows(BusinessException.class, () -> identity.recover("buyer", "wrong", "Reset123!"));
        identity.recover("buyer", token, "Reset123!");
        assertThrows(BusinessException.class, () -> identity.recover("buyer", token, "Again123!"));
        String code = identity.phoneChallenge("buyer", "Reset123!", "13800000000");
        assertFalse(identity.bindPhone("buyer", "wrong"));
        assertTrue(identity.bindPhone("buyer", code));
        assertEquals("13800000000", accounts.current("buyer").getPhone());
        assertThrows(BusinessException.class, () -> identity.bindPhone("buyer", code));
    }
    @Test
    void mediaRejectsDisguisedFilesAndDraftAssetsStayPrivate() throws Exception {
        var bad = new org.springframework.mock.web.MockMultipartFile("file", "photo.png", "image/png", "<script>bad</script>".getBytes());
        assertThrows(BusinessException.class, () -> media.upload("seller", bad));
        byte[] png = java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aobEAAAAASUVORK5CYII=");
        String url = media.upload("seller", new org.springframework.mock.web.MockMultipartFile("file", "../photo.png", "image/png", png));
        String name = url.substring(7);
        assertThrows(BusinessException.class, () -> media.metadata("buyer", name));
        Product data = input();
        data.setImageUrl(url);
        Product p = products.save("seller", null, data);
        products.submit("seller", p.getId());
        products.audit("admin", p.getId(), true, "通过");
        assertEquals("image/png", media.metadata(null, name).getContentType());
        Product other = input();
        other.setImageUrl(url);
        assertThrows(BusinessException.class, () -> products.save("other", null, other));
    }
}
