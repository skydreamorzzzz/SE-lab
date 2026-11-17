package edu.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PersonalService {
    private final AccountService accounts;
    private final AccountRepository accountStore;
    private final PersonalRepository entries;
    private final ProductService products;
    private final TradingService trading;
    private final FinanceService finance;

    public PersonalService(AccountService accounts, AccountRepository accountStore, PersonalRepository entries,
                           ProductService products, TradingService trading, FinanceService finance) {
        this.accounts = accounts;
        this.accountStore = accountStore;
        this.entries = entries;
        this.products = products;
        this.trading = trading;
        this.finance = finance;
    }

    public void cart(String username, Long productId, int quantity, String group) {
        Account actor = accounts.current(username);
        accounts.requireRole(actor, "BUYER");
        products.detail(productId, username);
        BusinessException.require(quantity > 0 && quantity <= 1000000, "数量必须为 1–1000000");
        BusinessException.require(group != null && group.length() <= 60, "分组不超过60字");
        PersonalEntry entry = upsert(actor, "CART", productId.toString(), productId);
        entry.setQuantity(quantity);
        entry.setGroupName(group);
    }

    public void toggle(String username, String kind, Long targetId, boolean enabled) {
        Product favorite = kind.equals("FAVORITE") ? products.locked(targetId) : null;
        Account actor = accounts.current(username);
        BusinessException.require(!actor.getBanned(), "账户已封禁");
        BusinessException.require(List.of("FAVORITE", "FOLLOW", "BLOCK").contains(kind), "关系类型无效");
        if (kind.equals("FAVORITE")) {
            products.detail(targetId, username);
        } else {
            Account target = accountStore.findById(targetId).orElseThrow(() -> new BusinessException("卖家不存在"));
            BusinessException.require(target.getTrader() && !targetId.equals(actor.getId()), "请选择其他卖家");
        }
        if (kind.equals("BLOCK")) {
            accounts.requireRole(actor, "BUYER");
        }
        accountStore.locked(actor.getId()).orElseThrow();
        boolean existed = entries.findByOwnerIdAndKindAndTargetKey(actor.getId(), kind, targetId.toString()).isPresent();
        if (favorite != null && enabled != existed) {
            favorite.setFavoriteCount(favorite.getFavoriteCount() + (enabled ? 1 : -1));
        }
        if (enabled) {
            upsert(actor, kind, targetId.toString(), targetId);
        } else {
            entries.findByOwnerIdAndKindAndTargetKey(actor.getId(), kind, targetId.toString()).ifPresent(entries::delete);
        }
    }

    public void history(String username, String kind, String keyword, Long productId) {
        if (username == null) {
            return;
        }
        BusinessException.require(kind.equals("SEARCH") || kind.equals("VIEW"), "历史类型无效");
        if (keyword == null || keyword.isBlank() || keyword.length() > 100) {
            return;
        }
        upsert(accounts.current(username), kind, keyword, productId);
    }

    public void clearHistory(String username) {
        Long owner = accounts.current(username).getId();
        accountStore.locked(owner).orElseThrow();
        entries.deleteAll(entries.findByOwnerIdAndKindOrderByUpdatedAtDesc(owner, "SEARCH"));
    }

    public List<PersonalEntry> list(String username, String kind) {
        BusinessException.require(List.of("CART", "FAVORITE", "FOLLOW", "BLOCK", "VIEW", "SEARCH").contains(kind), "分类无效");
        return entries.findByOwnerIdAndKindOrderByUpdatedAtDesc(accounts.current(username).getId(), kind);
    }

    public List<CartRow> cartRows(String username, String keyword, String group) {
        return list(username, "CART").stream().filter(e -> group == null || group.isBlank() || group.equals(e.getGroupName()))
                .map(e -> row(username, e)).filter(r -> keyword == null || keyword.isBlank() || r.name().contains(keyword)).toList();
    }

    public void remove(String username, Long id) {
        PersonalEntry entry = entries.findById(id).orElseThrow(() -> new BusinessException("记录不存在"));
        BusinessException.require(entry.getOwnerId().equals(accounts.current(username).getId()), "不能删除他人的记录");
        if (entry.getKind().equals("FAVORITE")) {
            toggle(username, "FAVORITE", entry.getTargetId(), false);
        } else {
            entries.delete(entry);
        }
    }

    public PurchaseOrder checkout(String username, Long entryId, BigDecimal displayedPrice, String requestKey) {
        PersonalEntry entry = entries.findById(entryId).orElseThrow(() -> new BusinessException("购物车项不存在"));
        BusinessException.require(entry.getKind().equals("CART") && entry.getOwnerId().equals(accounts.current(username).getId()),
                "无权操作此购物车项");
        PurchaseOrder order = trading.buy(username, entry.getTargetId(), entry.getQuantity(), displayedPrice, requestKey);
        // Keep the cart entry for retry safety and history; its availability is always recomputed.
        return order;
    }

    public boolean blocked(String username, Long sellerId) {
        return username != null && entries.findByOwnerIdAndKindAndTargetKey(accounts.current(username).getId(),
                "BLOCK", sellerId.toString()).isPresent();
    }

    public List<Long> blockedSellers(String username) {
        return username == null ? List.of() : list(username, "BLOCK").stream().map(PersonalEntry::getTargetId).toList();
    }

    private CartRow row(String username, PersonalEntry entry) {
        try {
            Product product = products.detail(entry.getTargetId(), username);
            String reason = !products.isPublic(product) ? "商品已下架" : products.sellerBanned(product) ? "卖家已封禁"
                    : product.getStock() < entry.getQuantity() ? "库存不足或售罄" : blocked(username, product.getSellerId()) ? "已屏蔽卖家" : "";
            return new CartRow(entry.getId(), product.getId(), product.getName(), entry.getQuantity(),
                    entry.getGroupName(), finance.price(product.getMinPrice()), reason);
        } catch (BusinessException unavailable) {
            return new CartRow(entry.getId(), entry.getTargetId(), "商品 #" + entry.getTargetId(), entry.getQuantity(),
                    entry.getGroupName(), BigDecimal.ZERO, "商品下架或卖家被封");
        }
    }

    private PersonalEntry upsert(Account actor, String kind, String key, Long targetId) {
        accountStore.locked(actor.getId()).orElseThrow();
        PersonalEntry entry = entries.findByOwnerIdAndKindAndTargetKey(actor.getId(), kind, key).orElseGet(() -> {
            PersonalEntry created = new PersonalEntry();
            created.setOwnerId(actor.getId());
            created.setKind(kind);
            created.setTargetKey(key);
            created.setTargetId(targetId);
            return created;
        });
        entry.setUpdatedAt(Instant.now());
        return entries.save(entry);
    }

    public record CartRow(Long id, Long productId, String name, int quantity, String groupName, BigDecimal price, String reason) {
    }
}
