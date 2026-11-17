package edu.market;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class MigrationTest {
    @Autowired
    private AccountService accounts;
    @Autowired
    private ProductService products;
    @Autowired
    private TradingService trading;

    @DynamicPropertySource
    static void legacySchema(DynamicPropertyRegistry registry) throws Exception {
        Path folder = Path.of("target/migration", UUID.randomUUID().toString()).toAbsolutePath();
        Files.createDirectories(folder);
        String url = "jdbc:h2:file:" + folder.resolve("market");
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.execute("create table accounts (id bigint primary key, username varchar(40) unique not null, "
                    + "password_hash varchar(255) not null, role varchar(255) not null, display_name varchar(255), address varchar(255))");
            statement.execute("create table product (id bigint primary key, seller_id bigint, name varchar(255), category varchar(255), "
                    + "condition_label varchar(255), description varchar(2000), image_url varchar(255), min_price numeric(38,2), "
                    + "max_price numeric(38,2), stock integer not null, reservation_limit integer not null, status varchar(255), review_note varchar(255))");
            statement.execute("create sequence accounts_seq start with 51 increment by 50");
            statement.execute("create sequence account_seq start with 51 increment by 50");
            statement.execute("create sequence product_seq start with 51 increment by 50");
            try (var insert = connection.prepareStatement("insert into accounts values(1, 'oldseller', ?, 'SELLER', '旧版卖家', '')")) {
                insert.setString(1, new BCryptPasswordEncoder().encode("Legacy123!"));
                insert.executeUpdate();
            }
            statement.execute("insert into product values(1,1,'旧版商品','学习/书籍','9成新','原MVP存量商品',"
                    + "'/images/sample.svg',10,10,3,5,'ON_SALE',null)");
        }
        registry.add("spring.datasource.url", () -> url);
    }

    @Test
    void oldMvpAccountsAndProductsRemainReadableAndTradable() {
        assertFalse(accounts.current("oldseller").getBanned());
        assertFalse(accounts.current("oldseller").getSuperAdmin());
        Product product = products.detail(1L, null);
        assertEquals(3, product.getStock());
        accounts.register("migrationbuyer", "Valid123!", "BUYER");
        accounts.profile("migrationbuyer", "迁移验收", "虚拟收货地址");
        PurchaseOrder order = trading.buy("migrationbuyer", 1L, 1, product.getMinPrice(), "migration-request-1");
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals(2, products.detail(1L, null).getStock());
    }
}
