package edu.market;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {
    private final AccountService accounts;
    private final ProductService products;
    private final CommentRepository comments;
    private final AccountRepository accountStore;
    private final ActivityService activities;

    public CommentService(AccountService accounts, ProductService products, CommentRepository comments,
                          AccountRepository accountStore, ActivityService activities) {
        this.accounts = accounts;
        this.products = products;
        this.comments = comments;
        this.accountStore = accountStore;
        this.activities = activities;
    }

    @Transactional(readOnly = true)
    public List<CommentView> list(Long productId, String username) {
        products.detail(productId, username);
        return comments.findByProductIdOrderByIdDesc(productId).stream().map(comment -> new CommentView(comment.getId(),
                accountStore.findById(comment.getAuthorId()).orElseThrow().getDisplayName(), comment.getContent(),
                comment.getReply(), comment.getCreatedAt(), comment.getRepliedAt())).toList();
    }

    public void add(String username, Long productId, String content) {
        Account author = accounts.current(username);
        BusinessException.require(author.getTrader() && !author.getBanned(), "仅正常普通用户可留言");
        Product product = products.locked(productId);
        products.detail(productId, username);
        BusinessException.require(products.isPublic(product), "商品未公开，不能留言");
        text(content);
        ProductComment comment = new ProductComment();
        comment.setProductId(productId);
        comment.setAuthorId(author.getId());
        comment.setContent(content.strip());
        comment.setCreatedAt(Instant.now());
        comments.save(comment);
        activities.record(username, "PRODUCT", productId, "COMMENT", "SAVED");
    }

    public void reply(String username, Long productId, Long commentId, String content) {
        Product product = products.locked(productId);
        products.owner(accounts.current(username), product);
        ProductComment comment = comments.findById(commentId).orElseThrow(() -> new BusinessException("留言不存在"));
        BusinessException.require(comment.getProductId().equals(productId), "留言不属于此商品");
        text(content);
        comment.setReply(content.strip());
        comment.setRepliedAt(Instant.now());
        activities.record(username, "PRODUCT", productId, "COMMENT_REPLY", "SAVED");
    }

    private void text(String value) {
        BusinessException.require(value != null && !value.isBlank() && value.length() <= 1000, "留言或回复为1–1000字");
    }

    public record CommentView(Long id, String author, String content, String reply, Instant createdAt, Instant repliedAt) {
    }
}
