package edu.market;

import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderMaintenance {
    private final PurchaseOrderRepository orders;
    private final TradingService trading;
    private final RefundRepository refunds;
    private final FinanceService finance;

    public OrderMaintenance(PurchaseOrderRepository orders, TradingService trading, RefundRepository refunds, FinanceService finance) {
        this.orders = orders;
        this.trading = trading;
        this.refunds = refunds;
        this.finance = finance;
    }

    @Scheduled(fixedDelay = 30000)
    public void sweep() {
        Instant now = Instant.now();
        refunds.findByStatusAndCreatedAtBefore("REQUESTED", now.minus(Duration.ofHours(48)))
                .forEach(r -> finance.timeout(r.getOrderId(), now));
        orders.findByStatusAndCreatedAtBefore(OrderStatus.PENDING_PAYMENT, now.minus(Duration.ofMinutes(15)))
                .forEach(order -> trading.expire(order.getId(), now));
        orders.findByStatusAndSignedAtBefore(OrderStatus.SHIPPED, now.minus(Duration.ofDays(7)))
                .forEach(order -> trading.expire(order.getId(), now));
    }
}
