package edu.market;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:concurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
class ConcurrencyTest {
    @Autowired
    private AccountService accounts;
    @Autowired
    private AccountRepository accountStore;
    @Autowired
    private ProductService products;
    @Autowired
    private ProductRepository productStore;
    @Autowired
    private TradingService trading;
    @Autowired
    private PurchaseOrderRepository orders;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private TransactionTemplate transaction;

    @Test
    void lastItemCannotBeSoldTwiceAndFailedTransactionRollsBack() throws Exception {
        accounts.register("conbuyer", "Valid123!", "BUYER");
        accounts.register("conseller", "Valid123!", "SELLER");
        accounts.profile("conbuyer", "并发买家", "校园地址");
        accountStore.save(new Account("conadmin", encoder.encode("Valid123!"), "ADMIN"));
        Product input = new Product();
        input.setName("最后一件");
        input.setCategory("学习/书籍");
        input.setConditionLabel("近全新");
        input.setDescription("并发与回滚测试商品");
        input.setImageUrl("/images/sample.svg");
        input.setMinPrice(BigDecimal.TEN);
        input.setMaxPrice(BigDecimal.TEN);
        Product product = products.save("conseller", null, input);
        Long id = product.getId();
        trading.inventory("conseller", id, 1);
        products.submit("conseller", id);
        products.audit("conadmin", id, true, "通过");
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            trading.buy("conbuyer", id, 1, BigDecimal.TEN, UUID.randomUUID().toString());
            throw new IllegalStateException("模拟订单写入后事务失败");
        }));
        assertEquals(1, productStore.findById(id).orElseThrow().getStock());
        assertEquals(0, orders.count());
        CyclicBarrier barrier = new CyclicBarrier(2);
        Callable<Boolean> buy = () -> {
            barrier.await(10, TimeUnit.SECONDS);
            try {
                trading.buy("conbuyer", id, 1, BigDecimal.TEN, UUID.randomUUID().toString());
                return true;
            } catch (BusinessException expected) {
                return false;
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(buy);
            var second = executor.submit(buy);
            int success = (first.get(20, TimeUnit.SECONDS) ? 1 : 0) + (second.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, success);
        }
        assertEquals(0, productStore.findById(id).orElseThrow().getStock());
        assertEquals(1, orders.count());
    }
}
