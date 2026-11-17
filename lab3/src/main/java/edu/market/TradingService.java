package edu.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TradingService {
    private final ActivityService activities;
    private final AccountService accounts;
    private final ProductService products;
    private final PurchaseOrderRepository orders;
    private final ReservationRepository reservations;
    private final AccountRepository accountStore;
    private final FinanceService finance;
    private final PersonalRepository personal;

    public TradingService(AccountService accounts, ProductService products,
                          PurchaseOrderRepository orders, ReservationRepository reservations, AccountRepository accountStore, ActivityService activities, FinanceService finance, PersonalRepository personal) {
        this.accounts = accounts;
        this.activities = activities;
        this.products = products;
        this.orders = orders;
        this.reservations = reservations;
        this.accountStore = accountStore;
        this.finance = finance;
        this.personal = personal;
    }

    public PurchaseOrder buy(String username, Long productId, int quantity, BigDecimal displayedPrice,
                             String requestKey) {
        Account buyer = buyer(username);
        Product product = products.locked(productId);
        BusinessException.require(personal.findByOwnerIdAndKindAndTargetKey(buyer.getId(), "BLOCK", product.getSellerId().toString()).isEmpty(), "已屏蔽此卖家");
        String key = buyer.getId() + ":" + requestKey;
        BusinessException.require(requestKey != null && requestKey.matches("[a-zA-Z0-9-]{8,80}"), "下单凭证无效");
        var previous = orders.findByRequestKey(key);
        if (previous.isPresent()) {
            BusinessException.require(previous.get().getProductId().equals(productId)
                    && previous.get().getQuantity() == quantity, "下单凭证已用于其他请求");
            return previous.get();
        }
        BusinessException.require(!buyer.getId().equals(product.getSellerId()), "不能购买自己的商品");
        BusinessException.require(!products.sellerBanned(product), "卖家已封禁");
        BusinessException.require(product.getStatus() == ProductStatus.ON_SALE, "商品不可购买，请刷新");
        BusinessException.require(quantity > 0 && quantity <= product.getStock(), "库存不足或数量无效");
        BusinessException.require(displayedPrice != null && displayedPrice.compareTo(finance.price(product.getMinPrice())) == 0,
                "商品价格已变化，请刷新后下单");
        requireAddress(buyer);
        product.setStock(product.getStock() - quantity);
        refreshStockState(product);
        return newOrder(buyer, product, quantity, key);
    }

    public void inventory(String username, Long productId, int delta) {
        Product product = products.locked(productId);
        products.owner(accounts.current(username), product);
        BusinessException.require(product.getStatus() != ProductStatus.VIOLATION, "违规下架商品不能补货");
        long next = (long) product.getStock() + delta;
        BusinessException.require(next >= 0 && next <= 1000000, "库存必须在 0–1000000 之间");
        product.setStock((int) next);
        activities.record(username, "PRODUCT", productId, "STOCK", "DELTA=" + delta);
        allocate(product);
        refreshStockState(product);
    }

    public Reservation reserve(String username, Long productId, int quantity) {
        Account buyer = buyer(username);
        Product product = products.locked(productId);
        BusinessException.require(!products.sellerBanned(product), "卖家已封禁");
        BusinessException.require(product.getStatus() == ProductStatus.SOLD_OUT && product.getStock() == 0,
                "仅售罄商品可预约");
        BusinessException.require(quantity > 0 && quantity <= 1000000, "预约数量无效");
        List<Reservation> queue = active(productId);
        long total = queue.stream().mapToLong(Reservation::getQuantity).sum();
        BusinessException.require(total + quantity <= product.getReservationLimit(), "超过计划预约总量");
        Reservation item = queue.stream().filter(r -> r.getBuyerId().equals(buyer.getId()))
                .findFirst().orElseGet(() -> {
                    Reservation created = new Reservation();
                    created.setBuyerId(buyer.getId());
                    created.setProductId(productId);
                    created.setCreatedAt(Instant.now());
                    return created;
                });
        // REQ-012: repeated requests merge into the same active queue entry, preserving its position.
        item.setQuantity(item.getQuantity() + quantity);
        Reservation saved = reservations.save(item);
        activities.record(username, "RESERVATION", saved.getId(), "RESERVE", "ACTIVE");
        return saved;
    }

    public void cancelReservation(String username, Long id) {
        Product product = products.locked(reservations.productId(id)
                .orElseThrow(() -> new BusinessException("预约不存在")));
        Reservation item = reservation(id);
        BusinessException.require(item.getBuyerId().equals(accounts.current(username).getId()), "只能取消自己的预约");
        BusinessException.require(!item.getCancelled() && !item.getConfirmed(), "预约已结束");
        product.setStock(product.getStock() + item.getAllocated());
        item.setAllocated(0);
        item.setCancelled(true);
        activities.record(username, "RESERVATION", id, "CANCEL", "CANCELLED");
        allocate(product);
        refreshStockState(product);
    }

    public PurchaseOrder confirmReservation(String username, Long id) {
        Product product = products.locked(reservations.productId(id)
                .orElseThrow(() -> new BusinessException("预约不存在")));
        products.owner(accounts.current(username), product);
        Reservation item = reservation(id);
        BusinessException.require(products.isPublic(product), "商品未公开，不能确认预约");
        BusinessException.require(!item.getCancelled() && !item.getConfirmed() && item.getAllocated() > 0,
                "预约尚未分配库存或已结束");
        Account buyer = accountStore.findById(item.getBuyerId()).orElseThrow(() -> new BusinessException("买家不存在"));
        requireAddress(buyer);
        int quantity = item.getAllocated();
        item.setQuantity(item.getQuantity() - quantity);
        item.setAllocated(0);
        item.setConfirmed(item.getQuantity() == 0);
        activities.record(username, "RESERVATION", id, "CONFIRM", "ORDER_CREATED");
        return newOrder(buyer, product, quantity, "reservation:" + UUID.randomUUID());
    }

    public void action(String username, Long id, String action) {
        Product product = products.locked(orders.productId(id)
                .orElseThrow(() -> new BusinessException("订单不存在")));
        PurchaseOrder order = orders.lockById(id).orElseThrow(() -> new BusinessException("订单不存在"));
        Account actor = accounts.current(username);
        boolean isBuyer = actor.getId().equals(order.getBuyerId());
        boolean isSeller = actor.getId().equals(order.getSellerId());
        BusinessException.require(isBuyer || isSeller, "无权操作此订单");
        switch (action) {
            case "pay" -> {
                BusinessException.require(isBuyer, "仅买家可支付");
                state(order, OrderStatus.PENDING_PAYMENT);
                BusinessException.require(!expired(order, Instant.now()), "支付已超时，订单将自动取消");
                order.setPaymentReference("SIM-" + UUID.randomUUID());
                order.setStatus(OrderStatus.PAID);
            }
            case "cancel" -> {
                BusinessException.require(isBuyer, "仅买家可取消未付款订单");
                state(order, OrderStatus.PENDING_PAYMENT);
                release(order, product);
            }
            case "ship" -> {
                BusinessException.require(isSeller, "仅卖家可发货");
                state(order, OrderStatus.PAID);
                order.setTrackingNumber("LOCAL-" + UUID.randomUUID());
                order.setStatus(OrderStatus.SHIPPED);
            }
            case "sign" -> {
                BusinessException.require(isBuyer, "仅买家可模拟签收");
                state(order, OrderStatus.SHIPPED);
                BusinessException.require(order.getSignedAt() == null, "已记录签收");
                order.setSignedAt(Instant.now());
            }
            case "receive" -> {
                BusinessException.require(isBuyer, "仅买家可确认收货");
                state(order, OrderStatus.SHIPPED);
                product.setSoldQuantity(product.getSoldQuantity() + order.getQuantity());
                order.setCompletedAt(Instant.now());
                order.setStatus(OrderStatus.COMPLETED);
                finance.settle(order);
            }
            default -> throw new BusinessException("未知订单操作");
        }
        activities.record(username, "ORDER", id, action, order.getStatus().name());
    }

    public void expire(Long id, Instant now) {
        Product product = products.locked(orders.productId(id)
                .orElseThrow(() -> new BusinessException("订单不存在")));
        PurchaseOrder order = orders.lockById(id).orElseThrow();
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT && expired(order, now)) {
            release(order, product);
            activities.record("SYSTEM", "ORDER", id, "EXPIRE", "CANCELLED");
        }
        if (order.getStatus() == OrderStatus.SHIPPED && order.getSignedAt() != null
                && !order.getSignedAt().plus(Duration.ofDays(7)).isAfter(now)) {
            product.setSoldQuantity(product.getSoldQuantity() + order.getQuantity());
                order.setCompletedAt(Instant.now());
                order.setStatus(OrderStatus.COMPLETED);
                finance.settle(order);
        }
    }

    public PurchaseOrder detail(String username, Long id) {
        PurchaseOrder order = order(id);
        Long actor = accounts.current(username).getId();
        BusinessException.require(actor.equals(order.getBuyerId()) || actor.equals(order.getSellerId()),
                "只有交易双方可查看订单");
        return order;
    }

    public PurchaseOrder fromBargain(String username, Product product, int quantity, BigDecimal price, Long bargainId) {
        Account buyer = buyer(username);
        requireAddress(buyer);
        PurchaseOrder order = newOrder(buyer, product, quantity, "bargain:" + bargainId);
        order.setUnitPrice(price);
        order.setTotal(price.multiply(BigDecimal.valueOf(quantity)));
        return order;
    }

    private PurchaseOrder newOrder(Account buyer, Product product, int quantity, String requestKey) {
        PurchaseOrder order = new PurchaseOrder();
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setBuyerId(buyer.getId());
        order.setSellerId(product.getSellerId());
        order.setQuantity(quantity);
        order.setUnitPrice(finance.price(product.getMinPrice()));
        order.setTotal(order.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setAddress(buyer.getAddress());
        order.setCreatedAt(Instant.now());
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setRequestKey(requestKey);
        PurchaseOrder saved = orders.save(order);
        activities.record(buyer.getUsername(), "ORDER", saved.getId(), "CREATE", "PENDING_PAYMENT");
        return saved;
    }

    private void release(PurchaseOrder order, Product product) {
        order.setStatus(OrderStatus.CANCELLED);
        product.setStock(product.getStock() + order.getQuantity());
        allocate(product);
        refreshStockState(product);
    }

    private void allocate(Product product) {
        products.allocateStock(product);
    }

    private List<Reservation> active(Long productId) {
        return reservations.findByProductIdAndCancelledFalseAndConfirmedFalseOrderByIdAsc(productId);
    }

    private void refreshStockState(Product product) {
        if (products.isPublic(product)) {
            product.setStatus(product.getStock() == 0 ? ProductStatus.SOLD_OUT : ProductStatus.ON_SALE);
        }
    }

    private Account buyer(String username) {
        Account account = accounts.current(username);
        accounts.requireRole(account, "BUYER");
        return account;
    }

    private void requireAddress(Account buyer) {
        BusinessException.require(buyer.getAddress() != null && !buyer.getAddress().isBlank(), "请先在个人中心填写收货地址");
    }

    private PurchaseOrder order(Long id) {
        return orders.findById(id).orElseThrow(() -> new BusinessException("订单不存在"));
    }

    private Reservation reservation(Long id) {
        return reservations.findById(id).orElseThrow(() -> new BusinessException("预约不存在"));
    }

    private void state(PurchaseOrder order, OrderStatus expected) {
        BusinessException.require(order.getStatus() == expected, "订单状态不允许此操作");
    }

    private boolean expired(PurchaseOrder order, Instant now) {
        return !order.getCreatedAt().plus(Duration.ofMinutes(15)).isAfter(now);
    }
}
