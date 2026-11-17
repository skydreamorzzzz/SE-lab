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

    public ProductService(ProductRepository products, AccountService accounts, ReservationRepository reservations, ActivityService activities) {
        this.products = products;
        this.accounts = accounts;
        this.activities = activities;
        this.reservations = reservations;
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
        BusinessException.require(input.getName() == null || input.getName().length() <= 100, "名称过长");
        BusinessException.require(input.getDescription() == null || input.getDescription().length() <= 2000,
                "描述不超过 2000 字");
        boolean changed = !Objects.equals(product.getName(), input.getName())
                || !Objects.equals(product.getDescription(), input.getDescription())
                || !Objects.equals(product.getMinPrice(), input.getMinPrice())
                || !Objects.equals(product.getMaxPrice(), input.getMaxPrice())
                || !Objects.equals(product.getImageUrl(), input.getImageUrl())
                || !Objects.equals(product.getCategory(), input.getCategory())
                || !Objects.equals(product.getConditionLabel(), input.getConditionLabel());
        product.setName(input.getName());
        product.setCategory(input.getCategory());
        product.setConditionLabel(input.getConditionLabel());
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
        activities.record(username, "PRODUCT", id, "AUDIT", product.getStatus().name());
    }

    public void withdraw(String username, Long id) {
        Product product = locked(id);
        owner(accounts.current(username), product);
        BusinessException.require(isPublic(product), "仅公开商品可下架");
        product.setStatus(ProductStatus.OFF_SHELF);
        product.setReviewNote("卖家主动下架，再发布需审核");
        activities.record(username, "PRODUCT", id, "WITHDRAW", product.getStatus().name());
    }

    public Product detail(Long id, String username) {
        Product product = products.findById(id).orElseThrow(() -> new BusinessException("商品不存在"));
        if (!isPublic(product)) {
            Account actor = accounts.current(username);
            BusinessException.require(actor.getId().equals(product.getSellerId())
                    || actor.getRole().equals("ADMIN"), "无权查看未公开商品");
        }
        return product;
    }

    public List<Product> search(String keyword, String category, BigDecimal min, BigDecimal max,
                                ProductStatus status, String sort) {
        BusinessException.require(min == null || min.signum() >= 0, "最低价不能为负");
        BusinessException.require(max == null || max.signum() >= 0, "最高价不能为负");
        BusinessException.require(min == null || max == null || min.compareTo(max) <= 0, "价格区间倒置");
        BusinessException.require(status == null || status == ProductStatus.ON_SALE
                || status == ProductStatus.SOLD_OUT, "只能检索公开状态");
        Specification<Product> spec = (root, query, cb) -> cb.and(
                root.get("status").in(ProductStatus.ON_SALE, ProductStatus.SOLD_OUT));
        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and((r, q, c) -> c.like(c.lower(r.get("name")),
                    "%" + keyword.toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%", '\\'));
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
        Sort order = "price".equals(sort) ? Sort.by("minPrice").ascending().and(Sort.by("id"))
                : Sort.by("id").descending();
        return products.findAll(spec, order);
    }

    public Product locked(Long id) {
        return products.lockById(id).orElseThrow(() -> new BusinessException("商品不存在"));
    }

    public void owner(Account actor, Product product) {
        accounts.requireRole(actor, "SELLER");
        BusinessException.require(actor.getId().equals(product.getSellerId()), "只能管理自己的商品");
    }

    public boolean isPublic(Product product) {
        return product.getStatus() == ProductStatus.ON_SALE || product.getStatus() == ProductStatus.SOLD_OUT;
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
                || product.getImageUrl().equals("/images/sample.svg")), "请提供 HTTPS 图片地址或使用本地示例图");
        BusinessException.require(product.getMinPrice() != null && product.getMaxPrice() != null
                && product.getMinPrice().signum() > 0
                && product.getMinPrice().scale() <= 2 && product.getMaxPrice().scale() <= 2
                && product.getMaxPrice().compareTo(new BigDecimal("99999999")) <= 0
                && product.getMinPrice().compareTo(product.getMaxPrice()) <= 0, "价格须为合法的正数区间，最多两位小数");
    }
}
