package edu.market;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SellerService {
    private final AccountRepository accounts;
    private final ProductRepository products;
    private final ReviewRepository reviews;

    public SellerService(AccountRepository accounts, ProductRepository products, ReviewRepository reviews) {
        this.accounts = accounts;
        this.products = products;
        this.reviews = reviews;
    }

    public SellerCard profile(Long id) {
        Account seller = accounts.findById(id).orElseThrow(() -> new BusinessException("卖家不存在"));
        BusinessException.require(!seller.getBanned(), "卖家已封禁");
        BusinessException.require(seller.getTrader(), "该用户不是卖家");
        List<Review> received = reviews.findByRecipientIdOrderByIdDesc(id);
        double average = received.stream().mapToInt(Review::getRating).average().orElse(0);
        long positive = received.stream().filter(r -> r.getRating() >= 4).count();
        List<Product> listings = products.findBySellerIdOrderByIdDesc(id).stream()
                .filter(p -> p.getStatus() == ProductStatus.ON_SALE || p.getStatus() == ProductStatus.SOLD_OUT)
                .toList();
        return new SellerCard(seller.getDisplayName(), received.size(), average, positive, listings,
                received.stream().limit(20).map(r -> new PublicReview(r.getRating(), r.getContent())).toList());
    }

    public record PublicReview(int rating, String content) {
    }

    public record SellerCard(String displayName, int reviewCount, double averageRating, long positiveCount,
                             List<Product> listings, List<PublicReview> recentReviews) {
        public String formattedRating() {
            return reviewCount == 0 ? "暂无评分" : String.format(java.util.Locale.ROOT, "%.1f / 5", averageRating);
        }

        public String positiveRate() {
            if (reviewCount == 0) {
                return "暂无评价";
            }
            return String.format(java.util.Locale.ROOT, "%.0f%%", 100.0 * positiveCount / reviewCount);
        }
    }
}
