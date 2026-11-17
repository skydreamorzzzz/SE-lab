package edu.market;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FinanceService {
    private final AccountService accounts;
    private final ProductService products;
    private final PurchaseOrderRepository orders;
    private final RefundRepository refunds;
    private final SettlementRepository settlements;
    private final PlatformRuleRepository rules;
    private final ActivityService activities;

    public FinanceService(AccountService accounts, ProductService products, PurchaseOrderRepository orders,
                          RefundRepository refunds, SettlementRepository settlements,
                          PlatformRuleRepository rules, ActivityService activities) {
        this.accounts = accounts;
        this.products = products;
        this.orders = orders;
        this.refunds = refunds;
        this.settlements = settlements;
        this.rules = rules;
        this.activities = activities;
    }

    public RefundRequest request(String username, Long orderId, String reason) {
        Product product = lockProduct(orderId);
        PurchaseOrder order = orders.lockById(orderId).orElseThrow();
        BusinessException.require(accounts.current(username).getId().equals(order.getBuyerId()), "仅买家可申请退款");
        BusinessException.require(order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.SHIPPED,
                "只可对已付款且未确认收货的订单申请售后");
        text(reason);
        RefundRequest refund = new RefundRequest();
        refund.setOrderId(orderId);
        refund.setPreviousStatus(order.getStatus().name());
        refund.setReturnRequired(order.getStatus() == OrderStatus.SHIPPED);
        refund.setStatus("REQUESTED");
        refund.setReason(reason.strip());
        refund.setCreatedAt(Instant.now());
        order.setStatus(OrderStatus.REFUND_PENDING);
        activities.record(username, "ORDER", orderId, "REFUND_REQUEST", "REQUESTED");
        return refunds.save(refund);
    }

    public void decide(String username, Long orderId, boolean approve, String note) {
        Product product = lockProduct(orderId);
        PurchaseOrder order = orders.lockById(orderId).orElseThrow();
        BusinessException.require(accounts.current(username).getId().equals(order.getSellerId()), "仅卖家处理退款");
        RefundRequest refund = latest(orderId);
        BusinessException.require(refund.getStatus().equals("REQUESTED"), "申请已经处理");
        text(note);
        refund.setDecision(note);
        if (approve) {
            approve(refund, order, product);
        } else {
            refund.setStatus("REJECTED");
            order.setStatus(OrderStatus.valueOf(refund.getPreviousStatus()));
        }
        activities.record(username, "ORDER", orderId, "REFUND_DECIDE", refund.getStatus());
    }

    public void returnGoods(String username, Long orderId, String tracking) {
        lockProduct(orderId);
        PurchaseOrder order = orders.lockById(orderId).orElseThrow();
        BusinessException.require(accounts.current(username).getId().equals(order.getBuyerId()), "仅买家提交退货物流");
        RefundRequest refund = latest(orderId);
        BusinessException.require(refund.getStatus().equals("APPROVED_RETURN"), "当前状态不能提交退货物流");
        text(tracking);
        refund.setReturnTracking(tracking);
        refund.setStatus("RETURNED");
        activities.record(username, "ORDER", orderId, "RETURN_SHIP", "RETURNED");
    }

    public void receivedReturn(String username, Long orderId) {
        Product product = lockProduct(orderId);
        PurchaseOrder order = orders.lockById(orderId).orElseThrow();
        BusinessException.require(accounts.current(username).getId().equals(order.getSellerId()), "仅卖家确认收到退货");
        RefundRequest refund = latest(orderId);
        BusinessException.require(refund.getStatus().equals("RETURNED"), "尚无退货物流或已经退款");
        finish(refund, order, product);
        activities.record(username, "ORDER", orderId, "RETURN_RECEIVE", "REFUNDED");
    }

    public void timeout(Long orderId, Instant now) {
        Product product = lockProduct(orderId);
        PurchaseOrder order = orders.lockById(orderId).orElseThrow();
        RefundRequest refund = latest(orderId);
        if (refund.getStatus().equals("REQUESTED")
                && !refund.getCreatedAt().plus(Duration.ofHours(48)).isAfter(now)) {
            refund.setDecision("48 小时未处理，系统自动同意");
            approve(refund, order, product);
            activities.record("SYSTEM", "ORDER", orderId, "REFUND_TIMEOUT", refund.getStatus());
        }
    }

    public java.math.BigDecimal price(java.math.BigDecimal original) {
        BigDecimal discount = rules.findById(1L).map(PlatformRule::getPromotionRate).orElse(BigDecimal.ZERO);
        return original.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
    }

    public void settle(PurchaseOrder order) {
        BusinessException.require(order.getStatus() == OrderStatus.COMPLETED, "只有确认收货后才能结算");
        if (settlements.findByOrderId(order.getId()).isPresent()) {
            return;
        }
        BigDecimal rate = rules.findById(1L).map(PlatformRule::getCommissionRate).orElse(BigDecimal.ZERO);
        Settlement settlement = new Settlement();
        settlement.setOrderId(order.getId());
        settlement.setSellerId(order.getSellerId());
        settlement.setGrossAmount(order.getTotal());
        settlement.setFee(order.getTotal().multiply(rate).setScale(2, RoundingMode.HALF_UP));
        settlement.setNetAmount(order.getTotal().subtract(settlement.getFee()));
        settlement.setCreatedAt(Instant.now());
        settlements.save(settlement);
        activities.record("SYSTEM", "ORDER", order.getId(), "SETTLE", "POSTED");
    }

    private void approve(RefundRequest refund, PurchaseOrder order, Product product) {
        if (refund.getReturnRequired()) {
            refund.setStatus("APPROVED_RETURN");
            order.setStatus(OrderStatus.RETURN_PENDING);
        } else {
            finish(refund, order, product);
        }
    }

    private void finish(RefundRequest refund, PurchaseOrder order, Product product) {
        refund.setStatus("REFUNDED");
        order.setStatus(OrderStatus.REFUNDED);
        product.setStock(product.getStock() + order.getQuantity());
        products.allocateStock(product);
        if (products.isPublic(product)) {
            product.setStatus(product.getStock() == 0 ? ProductStatus.SOLD_OUT : ProductStatus.ON_SALE);
        }
    }

    private Product lockProduct(Long orderId) {
        return products.locked(orders.productId(orderId).orElseThrow(() -> new BusinessException("订单不存在")));
    }

    private RefundRequest latest(Long orderId) {
        return refunds.findFirstByOrderIdOrderByIdDesc(orderId).orElseThrow(() -> new BusinessException("退款申请不存在"));
    }

    private void text(String value) {
        BusinessException.require(value != null && !value.isBlank() && value.length() <= 1000, "说明为 1–1000 字");
    }
}
