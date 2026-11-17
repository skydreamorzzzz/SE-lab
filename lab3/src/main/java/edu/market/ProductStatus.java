package edu.market;

public enum ProductStatus {
    DRAFT("草稿"), PENDING_REVIEW("待审核"), ON_SALE("在售"),
    OFF_SHELF("已下架"), VIOLATION("违规下架"), SOLD_OUT("已售罄");

    private final String label;

    ProductStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
