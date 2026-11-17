package edu.market;

import java.math.BigDecimal;
import java.util.HashSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:publishing;DB_CLOSE_DELAY=-1")
class PublishingContractTest {
    @Autowired
    private PublishingService publishing;
    @Autowired
    private ProductRepository productStore;
    @Autowired
    private ProductService products;
    @Autowired
    private TradingService trading;
    @Autowired
    private AccountService accounts;
    @Autowired
    private AccountRepository accountStore;
    @Autowired
    private CommentService comments;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private DataSource database;

    @Test
    void publishingIsAtomicRejectsStaleStockAndAllowsAuthorizedSellerReplies() {
        accounts.register("publishowner", "Valid123!", "BUYER");
        accounts.register("commentauthor", "Valid123!", "BUYER");
        accountStore.save(new Account("publishadmin", encoder.encode("Valid123!"), "ADMIN"));
        assertThrows(BusinessException.class, () -> publishing.save("publishowner", null, new Product(), 3, 0, 0, true));
        assertEquals(0, productStore.count());
        Product draft = publishing.save("publishowner", null, new Product(), 3, 0, 0, false);
        Product input = new Product();
        input.setName("原型发布表单商品");
        input.setCategory("数码电子/相机");
        input.setConditionLabel("9成新");
        input.setImageUrl("/images/lab2/camera.svg");
        input.setDescription("原型图片网格与标签实际接入");
        input.setMinPrice(BigDecimal.TEN);
        input.setMaxPrice(BigDecimal.TEN);
        input.setReservationLimit(5);
        trading.inventory("publishowner", draft.getId(), -1);
        assertThrows(BusinessException.class, () -> publishing.save("publishowner", draft.getId(), input, 3, 3, draft.getListingRevision(), true));
        Product submitted = publishing.save("publishowner", draft.getId(), input, 2, 2, draft.getListingRevision(), true);
        assertEquals(ProductStatus.PENDING_REVIEW, submitted.getStatus());
        products.audit("publishadmin", submitted.getId(), true, "通过");
        comments.add("commentauthor", submitted.getId(), "请问配件齐全吗？");
        Long id = comments.list(submitted.getId(), null).getFirst().id();
        assertThrows(BusinessException.class, () -> comments.reply("commentauthor", submitted.getId(), id, "不能冒充卖家"));
        comments.reply("publishowner", submitted.getId(), id, "配件齐全，已如实展示");
        assertEquals("配件齐全，已如实展示", comments.list(submitted.getId(), null).getFirst().reply());
    }

    @Test
    void persistedUmlRelationshipsHaveRealDatabaseForeignKeys() throws Exception {
        try (var connection = database.getConnection(); var keys = connection.getMetaData().getImportedKeys(null, "PUBLIC", "PURCHASE_ORDER")) {
            var columns = new HashSet<String>();
            while (keys.next()) {
                columns.add(keys.getString("FKCOLUMN_NAME"));
            }
            assertTrue(columns.containsAll(java.util.Set.of("BUYER_ID", "SELLER_ID", "PRODUCT_ID", "RESERVATION_ID")));
        }
    }
}
