package edu.market;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishingService {
    private final ProductService products;
    private final TradingService trading;

    public PublishingService(ProductService products, TradingService trading) {
        this.products = products;
        this.trading = trading;
    }

    public Product save(String username, Long id, Product input, int stock, int expectedStock,
                        long expectedRevision, boolean publish) {
        BusinessException.require(stock >= 0 && stock <= 1000000, "库存必须为0–1000000的整数");
        if (id != null) {
            Product current = products.locked(id);
            BusinessException.require(current.getStock() == expectedStock && current.getListingRevision() == expectedRevision,
                    "商品或库存已发生变化，请重新打开编辑页后保存");
        }
        Product product = products.save(username, id, input);
        if (stock != product.getStock()) {
            trading.inventory(username, product.getId(), stock - product.getStock());
        }
        if (publish && (product.getStatus() == ProductStatus.DRAFT || product.getStatus() == ProductStatus.OFF_SHELF)) {
            products.submit(username, product.getId());
        }
        return product;
    }
}
