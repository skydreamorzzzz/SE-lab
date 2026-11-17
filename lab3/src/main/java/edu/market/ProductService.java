package edu.market;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {
    private final ProductRepository products;
    private final ActivityService activities;
    private final AccountService accounts;
    private final ReservationRepository reservations;
    private final AccountRepository accountStore;
    private final PersonalRepository personal;
    private final MediaRepository media;
    private final AuditSnapshotRepository snapshots;

    public ProductService(ProductRepository products, AccountService accounts, ReservationRepository reservations, ActivityService activities, AccountRepository accountStore, PersonalRepository personal, MediaRepository media, AuditSnapshotRepository snapshots) {
        this.products = products;
        this.accounts = accounts;
        this.activities = activities;
        this.reservations = reservations;
        this.accountStore = accountStore;
        this.personal = personal;
        this.media = media;
        this.snapshots = snapshots;
    }

    public Product save(String username, Long id, Product input) {
        Account seller = accounts.current(username);
        accounts.requireRole(seller, "SELLER");
        Product product;
        if (id == null) {
            product = new Product();
            product.setSellerId(seller.getId());
            product.setStatus(ProductStatus.DRAFT);
            product.setStock(0);
        } else {
            product = locked(id);
            owner(seller, product);
            BusinessException.require(product.getStatus() != ProductStatus.VIOLATION
                    && product.getStatus() != ProductStatus.PENDING_REVIEW, "当前状态不能编辑");
        }
        BusinessException.require(input.getReservationLimit() >= 0, "预约计划不能为负数");
        BusinessException.require(input.getReservationLimit() <= 1000000, "预约计划不得超过 1000000");
        BusinessException.require(input.getName() == null || input.getName().length() <= 100, "名称过长");
        BusinessException.require(input.getCategory() == null || input.getCategory().length() <= 100, "分类过长");
        BusinessException.require(input.getConditionLabel() == null || input.getConditionLabel().length() <= 40,
                "成色说明不得超过 40 字");
        BusinessException.require(input.getImageUrl() == null || input.getImageUrl().length() <= 250,
                "图片地址不得超过 250 字");
        if (input.getMinPrice() != null) {
            BusinessException.require(input.getMinPrice().signum() > 0 && input.getMinPrice().scale() <= 2,
                    "最低价必须为正数，最多两位小数");
        }
        if (input.getMaxPrice() != null) {
            BusinessException.require(input.getMaxPrice().signum() > 0 && input.getMaxPrice().scale() <= 2,
                    "最高价必须为正数，最多两位小数");
        }
        BusinessException.require(input.getDescription() == null || input.getDescription().length() <= 2000,
                "描述不超过 2000 字");
        validateOptional(input);
        String mediaText = (input.getImageUrl() == null ? "" : input.getImageUrl()) + " "
                + (input.getExtraImages() == null ? "" : input.getExtraImages()) + " "
                + (input.getVideoUrl() == null ? "" : input.getVideoUrl());
        for (String url : mediaText.split("\\s+")) {
            if (url.startsWith("/media/")) {
                MediaAsset asset = media.findByStoredName(url.substring(7)).orElseThrow(() -> new BusinessException("媒体不存在"));
                BusinessException.require(asset.getOwnerId().equals(seller.getId()), "只能引用自己上传的媒体");
                boolean isVideo = url.equals(input.getVideoUrl());
                BusinessException.require(asset.getContentType().startsWith(isVideo ? "video/" : "image/"), "图片或视频媒体类型不匹配");
            }
        }
        if (input.getExtraImages() != null && !input.getExtraImages().isBlank()) {
            input.setExtraImages(String.join(" ", input.getExtraImages().strip().split("\\s+")));
        }
        Geo.validate(input.getLatitude(), input.getLongitude());
        BusinessException.require(input.getCity() == null || input.getCity().length() <= 40, "地区不超过40字");
        BusinessException.require(input.getTradeTags() == null || input.getTradeTags().length() <= 200, "交易标签过长");
        boolean locationChanged = !Objects.equals(product.getCity(), input.getCity())
                || !Objects.equals(product.getLatitude(), input.getLatitude())
                || !Objects.equals(product.getLongitude(), input.getLongitude())
                || !Objects.equals(product.getTradeTags(), input.getTradeTags());
        boolean optionalChanged = locationChanged || !Objects.equals(product.getBrand(), input.getBrand())
                || !Objects.equals(product.getModelName(), input.getModelName())
                || !Objects.equals(product.getUsageYears(), input.getUsageYears())
                || !Objects.equals(product.getOriginPlace(), input.getOriginPlace())
                || !Objects.equals(product.getContactChannel(), input.getContactChannel())
                || !Objects.equals(product.getShippingAddress(), input.getShippingAddress())
                || product.getReturnSupported() != input.getReturnSupported();
        boolean mediaChanged = !Objects.equals(product.getExtraImages(), input.getExtraImages())
                || !Objects.equals(product.getVideoUrl(), input.getVideoUrl());
        boolean changed = optionalChanged || mediaChanged || product.getNegotiable() != input.getNegotiable() || !Objects.equals(product.getName(), input.getName())
                || !Objects.equals(product.getDescription(), input.getDescription())
                || !Objects.equals(product.getMinPrice(), input.getMinPrice())
                || !Objects.equals(product.getMaxPrice(), input.getMaxPrice())
                || !Objects.equals(product.getImageUrl(), input.getImageUrl())
                || !Objects.equals(product.getCategory(), input.getCategory())
                || !Objects.equals(product.getConditionLabel(), input.getConditionLabel());
        product.setCity(input.getCity());
        product.setLatitude(input.getLatitude());
        product.setLongitude(input.getLongitude());
        product.setTradeTags(input.getTradeTags());
        product.setExtraImages(input.getExtraImages());
        product.setVideoUrl(input.getVideoUrl());
        product.setBrand(input.getBrand());
        product.setModelName(input.getModelName());
        product.setUsageYears(input.getUsageYears());
        product.setOriginPlace(input.getOriginPlace());
        product.setContactChannel(input.getContactChannel());
        product.setShippingAddress(input.getShippingAddress());
        product.setReturnSupported(input.getReturnSupported());
        product.setNegotiable(input.getNegotiable());
        if (changed) {
            product.setListingRevision(product.getListingRevision() + 1);
        }
        product.setName(input.getName());
        product.setCategory(input.getCategory());
        product.setConditionLabel(input.getConditionLabel());
        String condition = input.getConditionLabel();
        product.setConditionLevel("全新".equals(condition) ? 100 : "近全新".equals(condition) ? 99
                : condition != null && condition.matches("[1-9]成新.*") ? (condition.charAt(0) - '0') * 10 : 0);
        product.setDescription(input.getDescription());
        product.setImageUrl(input.getImageUrl());
        product.setMinPrice(input.getMinPrice());
        product.setMaxPrice(input.getMaxPrice());
        // Stock is changed through the inventory operation, never mass-assigned from an edit form.
        long reserved = reservations.findByProductIdAndCancelledFalseAndConfirmedFalseOrderByIdAsc(product.getId())
                .stream().mapToLong(Reservation::getQuantity).sum();
        BusinessException.require(input.getReservationLimit() >= reserved, "计划不能低于当前有效预约总量");
        product.setReservationLimit(input.getReservationLimit());
        if (changed && (product.getStatus() == ProductStatus.ON_SALE
                || product.getStatus() == ProductStatus.SOLD_OUT)) {
            validate(product);
            product.setStatus(ProductStatus.PENDING_REVIEW);
            product.setReviewNote("关键字段修改，等待复审");
        }
        Product saved = products.save(product);
        activities.record(username, "PRODUCT", saved.getId(), "SAVE", saved.getStatus().name());
        return saved;
    }

    public void submit(String username, Long id) {
        Product product = locked(id);
        owner(accounts.current(username), product);
        BusinessException.require(product.getStatus() == ProductStatus.DRAFT
                || product.getStatus() == ProductStatus.OFF_SHELF, "仅草稿或普通下架商品可提交");
        validate(product);
        product.setStatus(ProductStatus.PENDING_REVIEW);
        product.setReviewNote("已提交审核");
        activities.record(username, "PRODUCT", id, "SUBMIT", product.getStatus().name());
    }

    public void audit(String username, Long id, boolean approve, String note) {
        accounts.requireRole(accounts.current(username), "ADMIN");
        Product product = locked(id);
        BusinessException.require(product.getStatus() == ProductStatus.PENDING_REVIEW, "商品不在待审核状态");
        BusinessException.require(approve || (note != null && !note.isBlank()), "驳回需填写理由");
        BusinessException.require(note == null || note.length() <= 200, "审核意见不超过 200 字");
        product.setStatus(approve ? (product.getStock() > 0 ? ProductStatus.ON_SALE : ProductStatus.SOLD_OUT)
                : ProductStatus.DRAFT);
        product.setReviewNote(note);
        AuditSnapshot snapshot = new AuditSnapshot();
        snapshot.setProductId(id);
        snapshot.setSellerId(product.getSellerId());
        snapshot.setName(product.getName());
        snapshot.setCategory(product.getCategory());
        snapshot.setDescription(product.getDescription());
        snapshot.setConditionLabel(product.getConditionLabel());
        snapshot.setImageUrl(product.getImageUrl());
        snapshot.setPrice(product.getMinPrice());
        snapshot.setQuantity(product.getStock());
        snapshot.setStatus(approve ? "approved" : "rejected");
        snapshot.setNote(note);
        snapshot.setReviewedAt(java.time.Instant.now());
        snapshots.save(snapshot);
        if (approve) {
            product.setPublishedAt(java.time.Instant.now());
            allocateStock(product);
            product.setStatus(product.getStock() == 0 ? ProductStatus.SOLD_OUT : ProductStatus.ON_SALE);
        }
        activities.record(username, "PRODUCT", id, "AUDIT", product.getStatus().name());
    }

    public void withdraw(String username, Long id) {
        Product product = locked(id);
        owner(accounts.current(username), product);
        BusinessException.require(isPublic(product), "仅公开商品可下架");
        product.setListingRevision(product.getListingRevision() + 1);
        product.setStatus(ProductStatus.OFF_SHELF);
        product.setReviewNote("卖家主动下架，再发布需审核");
        activities.record(username, "PRODUCT", id, "WITHDRAW", product.getStatus().name());
    }

    public Product detail(Long id, String username) {
        Product product = products.findById(id).orElseThrow(() -> new BusinessException("商品不存在"));
        if (!isPublic(product) || sellerBanned(product)) {
            Account actor = accounts.current(username);
            BusinessException.require(actor.getId().equals(product.getSellerId())
                    || (product.getStatus() != ProductStatus.DRAFT && actor.getRole().equals("ADMIN")), "无权查看未公开商品");
        }
        if (username != null) {
            BusinessException.require(personal.findByOwnerIdAndKindAndTargetKey(accounts.current(username).getId(),
                    "BLOCK", product.getSellerId().toString()).isEmpty(), "已屏蔽此卖家");
        }
        return product;
    }

    public List<Product> search(String keyword, String category, BigDecimal min, BigDecimal max,
                                ProductStatus status, String sort) {
        return searchPage(null, keyword, category, min, max, status, sort, 0).getContent();
    }

    public org.springframework.data.domain.Page<Product> searchPage(String username, String keyword, String category,
            BigDecimal min, BigDecimal max, ProductStatus status, String sort, int page) {
        return searchPage(username, keyword, category, min, max, status, sort, page, CatalogFilter.none());
    }

    public org.springframework.data.domain.Page<Product> searchPage(String username, String keyword, String category,
            BigDecimal min, BigDecimal max, ProductStatus status, String sort, int page, CatalogFilter filter) {
        BusinessException.require(page >= 0 && page <= 100000, "页码无效");
        BusinessException.require(keyword == null || keyword.length() <= 100, "关键词不超过100字");
        BusinessException.require(category == null || category.isBlank() || category.matches("[^/%_]{1,50}(/[^/%_]{1,50})?"), "分类无效");
        BusinessException.require(min == null || min.signum() >= 0, "最低价不能为负");
        BusinessException.require(max == null || max.signum() >= 0, "最高价不能为负");
        BusinessException.require(min == null || max == null || min.compareTo(max) <= 0, "价格区间倒置");
        BusinessException.require(status == null || status == ProductStatus.ON_SALE
                || status == ProductStatus.SOLD_OUT, "只能检索公开状态");
        Specification<Product> spec = (root, query, cb) -> {
            var sellers = query.subquery(Long.class);
            var seller = sellers.from(Account.class);
            sellers.select(seller.get("id")).where(cb.isFalse(seller.get("banned")));
            return cb.and(root.get("status").in(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT), root.get("sellerId").in(sellers));
        };
        if (username != null) {
            Long userId = accounts.current(username).getId();
            List<Long> blocked = personal.findByOwnerIdAndKindOrderByUpdatedAtDesc(userId, "BLOCK")
                    .stream().map(PersonalEntry::getTargetId).toList();
            if (!blocked.isEmpty()) {
                spec = spec.and((r, q, c) -> c.not(r.get("sellerId").in(blocked)));
            }
        }
        spec = spec.and(filter.specification());
        if (keyword != null && !keyword.isBlank()) {
            String term = "%" + keyword.toLowerCase(java.util.Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%";
            spec = spec.and((r, q, c) -> c.or(c.like(c.lower(r.get("name")), term, '\\'),
                    c.like(c.lower(r.get("brand")), term, '\\'), c.like(c.lower(r.get("modelName")), term, '\\')));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and((r, q, c) -> c.or(c.equal(r.get("category"), category),
                    c.like(r.get("category"), category + "/%")));
        }
        if (min != null) {
            spec = spec.and((r, q, c) -> c.greaterThanOrEqualTo(r.get("maxPrice"), min));
        }
        if (max != null) {
            spec = spec.and((r, q, c) -> c.lessThanOrEqualTo(r.get("minPrice"), max));
        }
        if (status != null) {
            spec = spec.and((r, q, c) -> c.equal(r.get("status"), status));
        }
        String field = switch (sort == null ? "newest" : sort) {
            case "price", "priceDesc" -> "minPrice";
            case "favorites" -> "favoriteCount";
            case "sales" -> "soldQuantity";
            case "reputation", "positiveRate" -> "ratingScore";
            case "condition" -> "conditionLevel";
            case "newest", "default" -> "publishedAt";
            default -> throw new BusinessException("排序方式无效");
        };
        Sort order = "price".equals(sort) ? Sort.by(field).ascending() : Sort.by(field).descending();
        if ("reputation".equals(sort) || "positiveRate".equals(sort)) {
            spec = spec.and((r, q, c) -> {
                if (q.getResultType() != Long.class) {
                    var average = q.subquery(Double.class);
                    var review = average.from(Review.class);
                    average.select(c.avg(review.get("rating"))).where(c.equal(review.get("recipientId"), r.get("sellerId")));
                    if ("positiveRate".equals(sort)) {
                        var count = q.subquery(Long.class);
                        var all = count.from(Review.class);
                        count.select(c.count(all)).where(c.equal(all.get("recipientId"), r.get("sellerId")));
                        var positive = q.subquery(Long.class);
                        var good = positive.from(Review.class);
                        positive.select(c.count(good)).where(c.equal(good.get("recipientId"), r.get("sellerId")), c.ge(good.get("rating"), 4));
                        var denominator = c.sum(count, c.<Long>selectCase().when(c.equal(count, 0L), 1L).otherwise(0L));
                        q.orderBy(c.desc(c.quot(c.prod(positive, 100.0), denominator)), c.desc(r.get("id")));
                    } else {
                        q.orderBy(c.desc(c.coalesce(average, 0.0)), c.desc(r.get("id")));
                    }
                }
                return c.conjunction();
            });
            order = Sort.unsorted();
        } else {
            order = order.and(Sort.by("id").descending());
        }
        return products.findAll(spec, org.springframework.data.domain.PageRequest.of(page, 48, order));
    }

    public Product locked(Long id) {
        return products.lockById(id).orElseThrow(() -> new BusinessException("商品不存在"));
    }

    public void allocateStock(Product product) {
        if (!isPublic(product) || sellerBanned(product)) {
            return;
        }
        List<Reservation> queue = reservations.findByProductIdAndCancelledFalseAndConfirmedFalseOrderByIdAsc(product.getId());
        for (Reservation item : queue) {
            int available = product.getStock();
            int remaining = item.getQuantity() - item.getAllocated();
            int allocated = Math.min(available, remaining);
            item.setAllocated(item.getAllocated() + allocated);
            product.setStock(available - allocated);
        }
    }

    public void owner(Account actor, Product product) {
        accounts.requireRole(actor, "SELLER");
        BusinessException.require(actor.getId().equals(product.getSellerId()), "只能管理自己的商品");
    }

    public boolean sellerBanned(Product product) {
        return accountStore.findById(product.getSellerId()).map(Account::getBanned).orElse(true);
    }

    public boolean isPublic(Product product) {
        return product.getStatus() == ProductStatus.ON_SALE || product.getStatus() == ProductStatus.SOLD_OUT;
    }

    private void validateOptional(Product product) {
        for (String value : new String[] {product.getBrand(), product.getModelName(), product.getUsageYears(),
                product.getOriginPlace(), product.getContactChannel(), product.getShippingAddress()}) {
            BusinessException.require(value == null || value.length() <= 200, "可选字段不超过200字");
        }
        String extras = product.getExtraImages();
        BusinessException.require(extras == null || extras.length() <= 2000, "附图地址过长");
        if (extras != null && !extras.isBlank()) {
            String[] urls = extras.strip().split("\\s+");
            BusinessException.require(urls.length <= 8, "最多8张附图");
            for (String url : urls) {
                BusinessException.require(url.matches("https://[^\\s]+\\.(?i:png|jpg|jpeg|webp)")
                        || url.matches("/media/[a-zA-Z0-9.-]+") || url.equals("/images/sample.svg")
                        || url.matches("/images/lab2/(camera|headphones|keyboard|lamp)\\.svg"), "附图地址无效");
            }
        }
        String video = product.getVideoUrl();
        BusinessException.require(video == null || video.isBlank() || video.matches("https://[^\\s]+\\.(?i:mp4|webm)")
                || video.matches("/media/[a-zA-Z0-9.-]+\\.(mp4|webm)"), "至多一个MP4/WebM视频地址");
    }

    private void validate(Product product) {
        BusinessException.require(product.getName() != null && !product.getName().isBlank(), "请填写名称");
        BusinessException.require(product.getCategory() != null && product.getCategory().matches("[^/]+/[^/]+"),
                "分类格式为 一级/二级，例如 数码电子/相机");
        BusinessException.require(product.getConditionLabel() != null && !product.getConditionLabel().isBlank(),
                "请填写成色");
        BusinessException.require(product.getDescription() != null && !product.getDescription().isBlank(),
                "请填写描述");
        BusinessException.require(product.getImageUrl() != null
                && (product.getImageUrl().matches("https://[^\\s]+\\.(?i:png|jpg|jpeg|webp)")
                || product.getImageUrl().matches("/images/lab2/(camera|headphones|keyboard|lamp)\\.svg") || product.getImageUrl().equals("/images/sample.svg") || product.getImageUrl().matches("/media/[a-zA-Z0-9.-]+")), "请提供 HTTPS 图片地址或使用本地示例图");
        BusinessException.require(product.getMinPrice() != null && product.getMaxPrice() != null
                && product.getMinPrice().signum() > 0
                && product.getMinPrice().scale() <= 2 && product.getMaxPrice().scale() <= 2
                && product.getMaxPrice().compareTo(new BigDecimal("99999999")) <= 0
                && product.getMinPrice().compareTo(product.getMaxPrice()) <= 0, "价格须为合法的正数区间，最多两位小数");
    }
}
