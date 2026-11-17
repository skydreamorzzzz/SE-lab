package edu.market;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BargainService {
    private final AccountService accounts;
    private final ProductService products;
    private final BargainRepository bargains;
    private final TradingService trading;
    private final ActivityService activities;

    public BargainService(AccountService accounts, ProductService products, BargainRepository bargains,
                          TradingService trading, ActivityService activities) {
        this.accounts = accounts;
        this.products = products;
        this.bargains = bargains;
        this.trading = trading;
        this.activities = activities;
    }

    public Bargain propose(String username, Long productId, int quantity, BigDecimal price) {
        Account buyer = accounts.current(username);
        accounts.requireRole(buyer, "BUYER");
        Product product = products.locked(productId);
        BusinessException.require(product.getNegotiable() && product.getStatus() == ProductStatus.ON_SALE
                && !products.sellerBanned(product), "商品不支持议价或不可售");
        BusinessException.require(quantity > 0 && quantity <= 1000000, "数量无效");
        BusinessException.require(price != null && price.signum() > 0 && price.scale() <= 2
                && price.compareTo(product.getMinPrice()) >= 0 && price.compareTo(product.getMaxPrice()) <= 0,
                "报价必须在商品价格区间内");
        Bargain bargain = new Bargain();
        bargain.setProductId(productId);
        bargain.setBuyerId(buyer.getId());
        bargain.setSellerId(product.getSellerId());
        bargain.setQuantity(quantity);
        bargain.setPrice(price);
        bargain.setStatus("PROPOSED");
        bargain.setCreatedAt(Instant.now());
        bargain.setListingRevision(product.getListingRevision());
        Bargain saved = bargains.save(bargain);
        activities.record(username, "BARGAIN", saved.getId(), "PROPOSE", "PROPOSED");
        return saved;
    }

    public void decide(String username, Long id, boolean accept) {
        Product product = lockProduct(id);
        products.owner(accounts.current(username), product);
        Bargain bargain = bargains.locked(id).orElseThrow();
        BusinessException.require(bargain.getStatus().equals("PROPOSED"), "报价已经处理");
        if (accept) {
            BusinessException.require(product.getStatus() == ProductStatus.ON_SALE && product.getNegotiable()
                    && product.getListingRevision() == bargain.getListingRevision(), "商品信息已变化，需重新议价");
            BusinessException.require(product.getStock() >= bargain.getQuantity(), "库存不足，不能锁定");
            product.setStock(product.getStock() - bargain.getQuantity());
            refresh(product);
            bargain.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
            bargain.setStatus("ACCEPTED");
        } else {
            bargain.setStatus("REJECTED");
        }
        activities.record(username, "BARGAIN", id, "DECIDE", bargain.getStatus());
    }

    public PurchaseOrder order(String username, Long id) {
        Product product = lockProduct(id);
        Bargain bargain = bargains.locked(id).orElseThrow();
        BusinessException.require(accounts.current(username).getId().equals(bargain.getBuyerId()), "仅议价买家可下单");
        if (bargain.getStatus().equals("CONSUMED")) {
            return trading.detail(username, bargain.getOrderId());
        }
        BusinessException.require(bargain.getStatus().equals("ACCEPTED")
                && bargain.getExpiresAt().isAfter(Instant.now()), "锁价已过期或尚未接受");
        BusinessException.require(products.isPublic(product) && !products.sellerBanned(product)
                && product.getListingRevision() == bargain.getListingRevision(), "商品下架或信息改变，锁价失效");
        PurchaseOrder order = trading.fromBargain(username, product, bargain.getQuantity(), bargain.getPrice(), id);
        bargain.setStatus("CONSUMED");
        bargain.setOrderId(order.getId());
        activities.record(username, "BARGAIN", id, "ORDER", "CONSUMED");
        return order;
    }

    public void expire(Long id, Instant now) {
        Product product = lockProduct(id);
        Bargain bargain = bargains.locked(id).orElseThrow();
        if (bargain.getStatus().equals("ACCEPTED") && !bargain.getExpiresAt().isAfter(now)) {
            product.setStock(product.getStock() + bargain.getQuantity());
            products.allocateStock(product);
            refresh(product);
            bargain.setStatus("EXPIRED");
            activities.record("SYSTEM", "BARGAIN", id, "EXPIRE", "RELEASED");
        }
    }

    private Product lockProduct(Long id) {
        return products.locked(bargains.productId(id).orElseThrow(() -> new BusinessException("议价不存在")));
    }

    private void refresh(Product product) {
        if (products.isPublic(product)) {
            product.setStatus(product.getStock() == 0 ? ProductStatus.SOLD_OUT : ProductStatus.ON_SALE);
        }
    }
}
