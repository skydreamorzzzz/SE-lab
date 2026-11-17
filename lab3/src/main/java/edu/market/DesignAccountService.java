package edu.market;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DesignAccountService {
    private final AccountService accounts;
    private final PersonalService personal;
    private final ProductRepository products;
    private final PurchaseOrderRepository orders;
    private final BargainRepository bargains;
    private final ModerationRepository cases;
    private final AccountRepository accountStore;
    private final DesignViewService design;
    private final jakarta.persistence.EntityManager database;

    public DesignAccountService(AccountService accounts, PersonalService personal, ProductRepository products,
            PurchaseOrderRepository orders, BargainRepository bargains, ModerationRepository cases, AccountRepository accountStore,
            DesignViewService design, jakarta.persistence.EntityManager database) {
        this.accounts = accounts;
        this.personal = personal;
        this.products = products;
        this.orders = orders;
        this.bargains = bargains;
        this.cases = cases;
        this.accountStore = accountStore;
        this.design = design;
        this.database = database;
    }

    public Map<String, Object> profile(String username) {
        Account actor = accounts.current(username);
        var result = new LinkedHashMap<String, Object>();
        result.put("name", actor.getDisplayName());
        result.put("city", text(actor.getCity()));
        result.put("bio", text(actor.getBio()));
        result.put("address", text(actor.getAddress()));
        result.put("role", actor.getTrader() ? "普通用户（可买卖）" : "管理员");
        result.put("banned", actor.getBanned());
        result.put("metric", design.metrics(Set.of(actor.getId())).get(actor.getId()));
        var counts = new LinkedHashMap<String, Integer>();
        var records = new LinkedHashMap<String, List<Row>>();
        for (String kind : List.of("FAVORITE", "VIEW", "FOLLOW", "BLOCK")) {
            var entries = personal.list(username, kind);
            counts.put(kind, entries.size());
            records.put(kind, entries.stream().map(entry -> {
                if (kind.equals("FOLLOW") || kind.equals("BLOCK")) {
                    String name = accountStore.findById(entry.getTargetId()).map(Account::getDisplayName).orElse("账户已移除");
                    return new Row(name, "卖家 #" + entry.getTargetId(), "查看", "/sellers/" + entry.getTargetId());
                }
                Product product = products.findById(entry.getTargetId()).orElse(null);
                boolean visible = product != null && (product.getStatus() == ProductStatus.ON_SALE || product.getStatus() == ProductStatus.SOLD_OUT);
                return new Row(visible ? product.getName() : "商品 #" + entry.getTargetId(),
                        visible ? product.getStatus().getLabel() : "当前不可公开浏览", visible ? "查看详情" : "查看记录", visible ? "/products/" + entry.getTargetId() : "/personal?kind=" + kind);
            }).toList());
        }
        records.put("LISTINGS", products.findBySellerIdOrderByIdDesc(actor.getId()).stream().map(p ->
                new Row(text(p.getName()).isBlank() ? "未命名草稿" : p.getName(), p.getStatus().getLabel() + " · 库存 " + p.getStock(), "管理商品", "/seller/products")).toList());
        var allOrders = orders.findByBuyerIdOrSellerIdOrderByIdDesc(actor.getId(), actor.getId());
        records.put("SOLD", allOrders.stream().filter(o -> o.getSellerId().equals(actor.getId())).map(this::orderRow).toList());
        records.put("BOUGHT", allOrders.stream().filter(o -> o.getBuyerId().equals(actor.getId())).map(this::orderRow).toList());
        var messages = new ArrayList<Row>();
        allOrders.stream().limit(20).map(this::orderRow).forEach(messages::add);
        bargains.findByBuyerIdOrSellerIdOrderByIdDesc(actor.getId(), actor.getId()).stream().limit(20).forEach(b ->
                messages.add(new Row("议价 #" + b.getId(), b.getStatus() + " · ¥" + b.getPrice(), "查看议价", "/bargains")));
        cases.findByAuthorIdOrderByIdDesc(actor.getId()).stream().limit(20).forEach(c ->
                messages.add(new Row((c.getKind().equals("APPEAL") ? "申诉 #" : "举报 #") + c.getId(), c.getStatus() + " " + text(c.getFeedback()), "查看反馈", "/moderation")));
        database.createQuery("select c from ProductComment c where c.authorId = :id and c.reply is not null order by c.repliedAt desc", ProductComment.class)
                .setParameter("id", actor.getId()).setMaxResults(20).getResultList().forEach(c ->
                        messages.add(new Row("卖家回复 · 商品 #" + c.getProductId(), c.getReply(), "查看留言", "/products/" + c.getProductId())));
        records.put("MESSAGES", messages);
        result.put("counts", counts);
        result.put("records", records);
        return result;
    }

    @Transactional
    public void update(String username, String name, String address, String city, String bio) {
        BusinessException.require(city != null && city.length() <= 40 && bio != null && bio.length() <= 200, "地区不超过40字，简介不超过200字");
        accounts.profile(username, name, address);
        Account actor = accounts.current(username);
        actor.setCity(city.strip());
        actor.setBio(bio.strip());
    }

    private Row orderRow(PurchaseOrder order) {
        return new Row(order.getProductName(), "订单 #" + order.getId() + " · " + order.getStatus().getLabel(), "查看订单", "/orders/" + order.getId());
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    public record Row(String title, String description, String action, String url) {
    }
}
