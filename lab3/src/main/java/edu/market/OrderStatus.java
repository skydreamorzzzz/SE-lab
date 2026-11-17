package edu.market;

public enum OrderStatus {
    PENDING_PAYMENT("待支付"), PAID("待发货"), SHIPPED("已发货"),
    COMPLETED("已完成"), CANCELLED("已取消"),
    REFUND_PENDING("退款处理中"), RETURN_PENDING("等待退货"), REFUNDED("已退款");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
