package edu.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
@Entity
public class PlatformRule {
    @Id
    private Long id = 1L;
    private BigDecimal commissionRate = BigDecimal.ZERO;
    private BigDecimal promotionRate = BigDecimal.ZERO;
    public Long getId() { return id; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal value) { commissionRate = value; }
    public BigDecimal getPromotionRate() { return promotionRate; }
    public void setPromotionRate(BigDecimal value) { promotionRate = value; }
}
